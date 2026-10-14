package icbm.classic.command.sub;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import icbm.classic.command.CommandUtils;
import icbm.classic.command.ICBMCommands;
import icbm.classic.lib.actions.WorkTickingActionHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Dark(DarkGuardsman, Robin) on 4/13/2018.
 *
 * <p>S3.10: the 1.12 {@code SubCommand} became a Brigadier node. 1.12 read the optional radius from the raw
 * argument array ({@code args.length > 1 ? Double.parseDouble(args[1]) : 1000}), the 26.3 tree has a
 * separate branch for it; the position of 1.12's {@code ICommandSender#getPositionVector()} is the position
 * of the {@link CommandSourceStack} now (the console sits on the world spawn/respawn position).
 */
public class CommandLag
{
    /** 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "lag";

    public static final String TRANSLATION_LAG_REMOVE = "icbmclassic.command.lag.remove";

    /** 1.12 {@code collectHelpForAll} is empty and {@code collectHelpWorldOnly} adds the radius */
    public static final CommandHelp.HelpLine HELP_LINE = new CommandHelp.HelpLine("/" + ICBMCommands.ICBM_COMMAND + " " + NAME,
            List.of(""), List.of("[radius]"), List.of());

    private static final String ARGUMENT_RADIUS = "radius";

    /** Range used when the user does not give one, 1.12 {@code args.length > 1 ? ... : 1000} */
    private static final double DEFAULT_RANGE = 1000;

    /** 1.12 {@code CommandLag#icbmEntitySelector} */
    private static final Predicate<Entity> ICBM_ENTITY_SELECTOR = (entity) -> entity.isAlive() && CommandUtils.isICBMEntity(entity); //1.12: entity.isEntityAlive()

    /**
     * Builds the {@code /icbm lag} node, 1.12 {@code CommandLag#handleCommand}.
     *
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(NAME)
                .executes(context -> runCommand(context.getSource(), DEFAULT_RANGE))
                .then(Commands.argument(ARGUMENT_RADIUS, DoubleArgumentType.doubleArg())
                        .executes(context -> runCommand(context.getSource(), DoubleArgumentType.getDouble(context, ARGUMENT_RADIUS))));
    }

    /**
     * 1.12 {@code CommandLag#handleCommand}: remove the ICBM entities and queued/running blasts around the
     * sender.
     *
     * @param source - user running the command
     * @param range  - radius to clear
     * @return number of things removed
     */
    private static int runCommand(CommandSourceStack source, double range)
    {
        //Parse range already handled by the argument type
        final Vec3 position = source.getPosition();

        //Remove ICBM entities
        final List<Entity> entities = CommandUtils.getEntities(source.getLevel(),
                position.x, position.y, position.z,
                range,
                ICBM_ENTITY_SELECTOR);
        entities.forEach(entity -> entity.discard()); //1.12: Entity::setDead

        //Remove blasts queue to run or currently running
        final int blastRemoveCount = WorkTickingActionHandler.removeNear(source.getLevel(),
                position.x, position.y, position.z,
                range);

        //Update user with data
        source.sendSystemMessage(Component.translatable(TRANSLATION_LAG_REMOVE, blastRemoveCount, entities.size(), range));

        return blastRemoveCount + entities.size();
    }
}
