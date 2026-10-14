package icbm.classic.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.entity.EntityExplosion;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.entity.EntityFragments;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Robin Seifert on 1/2/20.
 *
 * <p>S3.10 port notes - the 1.12 helpers that parsed the raw command arguments are gone, because Brigadier
 * parses every argument from the command tree and 1.12's custom {@code CommandGroup}/{@code SubCommand}
 * system is merged into it:
 * <ul>
 *     <li>{@code removeFront(String[])} -&gt; MERGED, the tree splits the input into literal/argument nodes,
 *     each command only receives its own arguments.</li>
 *     <li>{@code parseRadius(String)} -&gt; not needed, {@code CommandRemove} uses
 *     {@code IntegerArgumentType} (1.12 accepted any int, {@code <= 0} meant "whole world").</li>
 *     <li>{@code getNumber(ICommandSender, String, double)} -&gt; {@code Vec3Argument.vec3(false)}, which
 *     resolves {@code ~} against the {@link CommandSourceStack} position and reads plain numbers as-is.
 *     {@link #checkRelativeAllowed(CommandSourceStack, Coordinates)} keeps the 1.12 "'~' can't be used from
 *     console" rule that lived inside this helper.</li>
 *     <li>{@code getWorld(ICommandSender, String, World)} -&gt; {@code DimensionArgument.dimension()}. 1.12
 *     resolved an integer dimension ID through {@code DimensionManager}, 26.3 has dimension identifiers and
 *     no numeric IDs left, so the dimension argument completes the registered dimension ids instead.</li>
 * </ul>
 */
public class CommandUtils
{
    /**
     * 1.12 {@code getNumber} threw {@code WrongUsageException("'~' can't be used from console")}, a hard coded
     * message (there is no language key for it), so the port keeps the literal text.
     */
    private static final SimpleCommandExceptionType ERROR_CONSOLE_RELATIVE = new SimpleCommandExceptionType(
            Component.literal("'~' can't be used from console"));

    /**
     * Checks if the entity is an ICBM entity
     *
     * @param entity - entity to check
     * @return true if the entity is from the mod ICBM
     */
    public static boolean isICBMEntity(Entity entity)
    {
        //TODO just check for ICBM namespace
        //1.12 also matched EntityBombDroplet (cluster bomblets) and EntityGrenade here. Those classes are not
        //ported yet, add them back together with the missiles/grenades that create them (S4/S5) - see the 1.12
        //list in CommandUtils#isICBMEntity.
        return entity instanceof EntityFragments
                || entity instanceof EntityFlyingBlock
                || isMissile(entity)
                || entity instanceof EntityExplosive
                || entity instanceof EntityExplosion
                || entity instanceof EntityRedmatter;
    }

    /**
     * Checks if the entity is a missile
     *
     * @param entity - entity to check
     * @return true if the entity is a missile
     */
    public static boolean isMissile(Entity entity)
    {
        //1.12: entity.hasCapability(ICBMClassicAPI.MISSILE_CAPABILITY, null)
        return ICBMClassicHelpers.isMissile(entity);
    }

    /**
     * Gets all entities within the world and range
     *
     * @param level  - level to check
     * @param x      - position
     * @param y      - position
     * @param z      - position
     * @param range  - range to check, -1 will return all entities in the world
     * @param filter - test the entity has to pass
     * @return entities found
     */
    public static List<Entity> getEntities(Level level, double x, double y, double z, double range, Predicate<Entity> filter)
    {
        if (range > 0)
        {
            final AABB bb = new AABB(
                    x - range, y - range, z - range,
                    x + range, y + range, z + range);

            //1.12: world.getEntitiesWithinAABB(Entity.class, bb, filter::test)
            return level.getEntitiesOfClass(Entity.class, bb, filter);
        }
        //Copy list to avoid modification while we are using said list
        final List<Entity> entities = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel)
        {
            //1.12: world.loadedEntityList
            serverLevel.getAllEntities().forEach(entity ->
            {
                if (filter.test(entity))
                {
                    entities.add(entity);
                }
            });
        }
        return entities;
    }

    /**
     * 1.12 used {@code sender instanceof MinecraftServer} to separate the console from a sender living in the
     * world ({@code CommandUtils#getNumber}, {@code SubCommand#displayHelp}, the short form of
     * {@code CommandBlastTrigger}/{@code CommandRemove}).
     *
     * <p>26.3 keeps the {@link net.minecraft.commands.CommandSource} of a {@link CommandSourceStack} private
     * (there is no accessor and Fabric's access wideners do not open it), so the port uses the closest signal
     * the stack exposes: a stack without an entity belongs to the console, a command block or a function.
     * The console therefore keeps the 1.12 behaviour, while command blocks and functions - which 1.12 treated
     * as senders in the world - have to use the long form now.
     *
     * @param source - user running the command
     * @return true if the sender is not an entity in the world
     */
    public static boolean isConsoleSender(CommandSourceStack source)
    {
        return source.getEntity() == null;
    }

    /**
     * Rejects a relative coordinate ({@code ~}) from the console, which is what 1.12's
     * {@code getNumber(ICommandSender, String, double)} did before it resolved the value. 26.3 resolves
     * {@code ~} against the position of any {@link CommandSourceStack} (the console sits on the respawn
     * position), so the restriction is applied to the parsed coordinates instead.
     *
     * @param source      - user running the command
     * @param coordinates - parsed coordinate argument
     * @throws CommandSyntaxException - if '~' was used by the console
     */
    public static void checkRelativeAllowed(CommandSourceStack source, Coordinates coordinates) throws CommandSyntaxException
    {
        if (isConsoleSender(source)
                && (coordinates.isXRelative() || coordinates.isYRelative() || coordinates.isZRelative()))
        {
            throw ERROR_CONSOLE_RELATIVE.create();
        }
    }
}
