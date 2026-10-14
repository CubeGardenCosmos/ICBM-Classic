package icbm.classic.command.sub;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import icbm.classic.command.CommandUtils;
import icbm.classic.command.ICBMCommands;
import icbm.classic.content.entity.EntityExplosive;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Dark(DarkGuardsman, Robin) on 4/13/2018.
 *
 * <p>S3.10: the 1.12 {@code SubCommand} became a Brigadier node. 1.12 split the two argument forms inside
 * {@code doCommand} with the raw argument count ({@code args.length >= 5} for the long version and
 * {@code args.length <= 2} plus a non console sender for the short one), the 26.3 tree carries the two forms
 * as separate branches and only the "short version needs a sender in the world" rule is checked while
 * running, so the 1.12 error text is kept.
 */
public class CommandRemove
{
    /** 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "remove";

    public static final String TRANSLATION_REMOVE = "icbmclassic.command.remove";

    /** 1.12 {@code collectHelpForAll}/{@code collectHelpWorldOnly} */
    public static final CommandHelp.HelpLine HELP_LINE = new CommandHelp.HelpLine("/" + ICBMCommands.ICBM_COMMAND + " " + NAME,
            List.of("<all/missiles/explosions> <dim> <x> <y> <z> <radius>"),
            List.of("<all/missiles/explosions> [radius]"),
            List.of());

    private static final String ARGUMENT_TYPE = "type";
    private static final String ARGUMENT_DIMENSION = "dimension";
    private static final String ARGUMENT_POSITION = "pos";
    private static final String ARGUMENT_RADIUS = "radius";

    /** 1.12 {@code getTabSuggestions} for the first argument */
    private static final SuggestionProvider<CommandSourceStack> TAB_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"all", "missile", "explosion"}, builder);

    /**
     * Builds the {@code /icbm remove} node, 1.12 {@code CommandRemove#handleCommand}.
     *
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        //Get type
        return Commands.literal(NAME)
                .then(Commands.argument(ARGUMENT_TYPE, StringArgumentType.word())
                        .suggests(TAB_SUGGESTIONS)
                        //Short version, 1 arg, 1.12: sender world/position and no radius
                        .executes(context -> shortVersion(context, -1))
                        //Short version, 2 args, 1.12: CommandBase.parseInt(args[1])
                        .then(Commands.argument(ARGUMENT_RADIUS, IntegerArgumentType.integer())
                                .executes(context -> shortVersion(context, IntegerArgumentType.getInteger(context, ARGUMENT_RADIUS))))
                        //Long version, 5 args, 1.12: CommandUtils.getWorld + getNumber for each position
                        .then(Commands.argument(ARGUMENT_DIMENSION, DimensionArgument.dimension())
                                .then(Commands.argument(ARGUMENT_POSITION, Vec3Argument.vec3(false))
                                        .executes(context -> longVersion(context, -1))
                                        //Long version, 6 args, range applied at the end
                                        .then(Commands.argument(ARGUMENT_RADIUS, IntegerArgumentType.integer())
                                                .executes(context -> longVersion(context, IntegerArgumentType.getInteger(context, ARGUMENT_RADIUS)))))));
    }

    /**
     * 1.12 {@code doCommand} short branch: {@code !(sender instanceof MinecraftServer) && args.length <= 2},
     * the position is the sender itself and the radius defaults to -1 (everything loaded).
     *
     * @param context - command context
     * @param range   - parsed radius, -1 for "no radius given"
     * @return number of entities removed
     * @throws CommandSyntaxException - if the sender is the console
     */
    private static int shortVersion(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException
    {
        final CommandSourceStack source = context.getSource();
        if (CommandUtils.isConsoleSender(source))
        {
            throw ICBMCommands.unknownCommand(HELP_LINE.usage());
        }

        //Get position
        final Vec3 position = source.getPosition();
        return doCommand(source, StringArgumentType.getString(context, ARGUMENT_TYPE), source.getLevel(),
                position.x, position.y, position.z, range);
    }

    /**
     * 1.12 {@code doCommand} long branch: {@code <dim> <x> <y> <z>} followed by an optional radius.
     *
     * @param context - command context
     * @param range   - parsed radius, -1 for "no radius given"
     * @return number of entities removed
     * @throws CommandSyntaxException - if '~' was used by the console
     */
    private static int longVersion(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException
    {
        //Get position
        final Coordinates coordinates = Vec3Argument.getCoordinates(context, ARGUMENT_POSITION);
        //1.12 CommandUtils#getNumber: '~' was rejected for the console
        CommandUtils.checkRelativeAllowed(context.getSource(), coordinates);
        final ServerLevel level = DimensionArgument.getDimension(context, ARGUMENT_DIMENSION);
        final Vec3 position = coordinates.getPosition(context.getSource());

        return doCommand(context.getSource(), StringArgumentType.getString(context, ARGUMENT_TYPE), level,
                position.x, position.y, position.z, range);
    }

    /**
     * 1.12 {@code CommandRemove#doCommand}: find the ICBM entities in the area and remove them.
     *
     * @param source - user running the command
     * @param type   - entity selector keyword from the user
     * @param level  - level to search
     * @param x      - position
     * @param y      - position
     * @param z      - position
     * @param range  - range to search, -1 for the whole level
     * @return number of entities removed
     */
    private static int doCommand(CommandSourceStack source, String type, Level level, double x, double y, double z, int range)
    {
        //Get type
        final Predicate<Entity> entitySelector = buildSelector(type);

        //Find and set entities dead
        final List<Entity> entities = CommandUtils.getEntities(level, x, y, z, range, entitySelector);
        entities.forEach(entity -> entity.discard()); //1.12: Entity::setDead

        //User feedback
        source.sendSystemMessage(Component.translatable(TRANSLATION_REMOVE, entities.size(), range));

        return entities.size();
    }

    private static boolean isRemoveMissile(String type)
    {
        return type.equalsIgnoreCase("missiles") || type.equalsIgnoreCase("missile");
    }

    private static boolean isRemoveExplosion(String type)
    {
        return type.equalsIgnoreCase("explosions")
                || type.equalsIgnoreCase("explosion")
                || type.equalsIgnoreCase("explosive")
                || type.equalsIgnoreCase("explosives")
                || type.equalsIgnoreCase("ex");
    }

    private static Predicate<Entity> buildSelector(String type)
    {
        final boolean remove_all = type.equalsIgnoreCase("all");
        final boolean remove_missiles = isRemoveMissile(type);
        final boolean remove_explosives = isRemoveExplosion(type);

        return (entity) ->
        {
            if (entity.isAlive()) //1.12: entity.isEntityAlive()
            {
                if (remove_all)
                {
                    return CommandUtils.isICBMEntity(entity);
                }
                else if (remove_explosives)
                {
                    return entity instanceof EntityExplosive;
                }
                return remove_missiles && CommandUtils.isMissile(entity);
            }
            return false;
        };
    }
}
