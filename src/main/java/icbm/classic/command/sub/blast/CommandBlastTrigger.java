package icbm.classic.command.sub.blast;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.command.CommandUtils;
import icbm.classic.command.ICBMCommands;
import icbm.classic.command.sub.CommandHelp;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.lib.actions.fields.ActionFieldProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Created by Robin Seifert on 1/6/20.
 *
 * <p>S3.10: the 1.12 {@code SubCommand} became a Brigadier node. 1.12 picked the short
 * ({@code <id> <size>}) or the long ({@code <id> <dim> <x> <y> <z> <size>}) form from the raw argument count,
 * the 26.3 tree carries both as branches; the short form keeps the 1.12 "no console sender" check and the
 * long form keeps 1.12's {@code CommandUtils#getNumber} console restriction on {@code ~}.
 *
 * <p>1.12 also declared {@code TRANSLATION_TRIGGERED}, {@code TRANSLATION_THREADING} and the
 * {@code .error}, {@code .error.blocked}, {@code .error.null}, {@code .error.triggered} and
 * {@code .error.unknown} keys here. Nothing referenced them (the blast carries its own status message, see
 * {@code icbm.classic.content.blast.BlastStatus}), so the port does not repeat them.
 */
public class CommandBlastTrigger
{
    /** 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "trigger";

    //Translations
    private static final String TRANSLATION_KEY = "icbmclassic.command.blast";

    //Translations: Errors
    public static final String TRANSLATION_ERROR = TRANSLATION_KEY + ".error";

    public static final String TRANSLATION_ERROR_SCALE_ZERO = TRANSLATION_ERROR + ".scale.zero";
    public static final String TRANSLATION_ERROR_EXPLOSIVE_ID = TRANSLATION_ERROR + ".explosive.id";

    /** 1.12 {@code collectHelpForAll}/{@code collectHelpWorldOnly} */
    public static final CommandHelp.HelpLine HELP_LINE = new CommandHelp.HelpLine("/" + ICBMCommands.ICBM_COMMAND + " " + ICBMCommands.BLAST_COMMAND + " " + NAME,
            List.of("<id> <dim> <x> <y> <z> <size>"),
            List.of("<id> <size>"),
            List.of());

    private static final String ARGUMENT_EXPLOSIVE = "explosive";
    private static final String ARGUMENT_DIMENSION = "dimension";
    private static final String ARGUMENT_POSITION = "pos";
    private static final String ARGUMENT_SIZE = "size";

    /**
     * 1.12 completed the first argument from {@code ICBMClassicAPI.EXPLOSIVE_REGISTRY} because the id was a
     * plain string; the 26.3 port registers the same ids as the suggestions of the identifier argument.
     */
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_EXPLOSIVES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosives().stream().map(IExplosiveData::getRegistryKey),
                    builder);

    /** 1.12 {@code new SyntaxErrorException(TRANSLATION_ERROR_EXPLOSIVE_ID, explosive_id)} */
    private static final DynamicCommandExceptionType ERROR_EXPLOSIVE_ID = new DynamicCommandExceptionType(
            explosiveId -> Component.translatable(TRANSLATION_ERROR_EXPLOSIVE_ID, explosiveId));

    /** 1.12 {@code new SyntaxErrorException(TRANSLATION_ERROR_SCALE_ZERO)} */
    private static final SimpleCommandExceptionType ERROR_SCALE_ZERO = new SimpleCommandExceptionType(
            Component.translatable(TRANSLATION_ERROR_SCALE_ZERO));

    /**
     * Builds the {@code /icbm blast trigger} node, 1.12 {@code CommandBlastTrigger#handleCommand}.
     *
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(NAME)
                .then(Commands.argument(ARGUMENT_EXPLOSIVE, IdentifierArgument.id())
                        .suggests(SUGGEST_EXPLOSIVES)
                        //Short version, 2 args, 1.12: only in-world senders (see shortVersion)
                        .then(Commands.argument(ARGUMENT_SIZE, FloatArgumentType.floatArg())
                                .executes(context -> shortVersion(context, getScale(context))))
                        //Long version, 6 args, 1.12: CommandUtils.getWorld + getNumber for each position
                        .then(Commands.argument(ARGUMENT_DIMENSION, DimensionArgument.dimension())
                                .then(Commands.argument(ARGUMENT_POSITION, Vec3Argument.vec3(false))
                                        .then(Commands.argument(ARGUMENT_SIZE, FloatArgumentType.floatArg())
                                                .executes(context -> longVersion(context, getScale(context)))))));
    }

    /**
     * 1.12 {@code CommandBlastTrigger#doCommand} short branch: {@code !(sender instanceof MinecraftServer) &&
     * args.length == 2}, the blast is triggered where the sender stands.
     *
     * @param context - command context
     * @param size    - parsed size
     * @return 1 if the blast was triggered, 0 if it reported an error
     * @throws CommandSyntaxException - if the sender is the console
     */
    private static int shortVersion(CommandContext<CommandSourceStack> context, float size) throws CommandSyntaxException
    {
        final CommandSourceStack source = context.getSource();
        if (CommandUtils.isConsoleSender(source))
        {
            throw ICBMCommands.unknownCommand(HELP_LINE.usage());
        }

        //Get position data
        final ServerLevel level = source.getLevel();
        final Vec3 position = source.getPosition();

        //Trigger blast
        return trigger(source, level, position.x, position.y, position.z, getExplosive(context), size);
    }

    /**
     * 1.12 {@code CommandBlastTrigger#doCommand} long branch: {@code <id> <dim> <x> <y> <z> <size>}.
     *
     * @param context - command context
     * @param size    - parsed size
     * @return 1 if the blast was triggered, 0 if it reported an error
     * @throws CommandSyntaxException - if '~' was used by the console
     */
    private static int longVersion(CommandContext<CommandSourceStack> context, float size) throws CommandSyntaxException
    {
        //Get position data
        final Coordinates coordinates = Vec3Argument.getCoordinates(context, ARGUMENT_POSITION);
        //1.12 CommandUtils#getNumber: '~' was rejected for the console. 1.12 also rejected '~' as dimension,
        //26.3 DimensionArgument has no relative form at all
        CommandUtils.checkRelativeAllowed(context.getSource(), coordinates);
        final ServerLevel level = DimensionArgument.getDimension(context, ARGUMENT_DIMENSION);
        final Vec3 position = coordinates.getPosition(context.getSource());

        //Trigger blast
        return trigger(context.getSource(), level, position.x, position.y, position.z, getExplosive(context), size);
    }

    /**
     * 1.12 {@code CommandBlastTrigger#getScale}: {@code Float.parseFloat(args[1]/args[5])} plus the
     * "scale must be greater than zero" check.
     *
     * @param context - command context
     * @return parsed size
     * @throws CommandSyntaxException - if the scale is not above zero
     */
    private static float getScale(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final float scale = FloatArgumentType.getFloat(context, ARGUMENT_SIZE);
        if (scale <= 0)
        {
            throw ERROR_SCALE_ZERO.create();
        }
        return scale;
    }

    /**
     * Gets the explosive data based on the ID from the user
     *
     * @param context - command context
     * @return explosive data
     * @throws CommandSyntaxException - if ID is not found
     */
    private static IExplosiveData getExplosive(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        return getExplosive(IdentifierArgument.getId(context, ARGUMENT_EXPLOSIVE));
    }

    /**
     * Gets the explosive data based on the ID from the user
     *
     * <p>1.12 read this through {@code ICBMClassicHelpers#getExplosive(id, true)}. That helper asked the
     * registry for a non null result, and the registry returns condensed for an unregistered name, so the
     * {@code TRANSLATION_ERROR_EXPLOSIVE_ID} message of 1.12 was unreachable and a bad id silently ran
     * condensed. The port queries the registry with {@code allowNull = true} so the 1.12 error message
     * actually fires.
     *
     * @param explosive_id - ID from user
     * @return explosive data
     * @throws CommandSyntaxException - if ID is not found
     */
    public static IExplosiveData getExplosive(Identifier explosive_id) throws CommandSyntaxException
    {
        final IExplosiveData explosiveData = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(explosive_id, true);
        if (explosiveData == null)
        {
            throw ERROR_EXPLOSIVE_ID.create(explosive_id.toString());
        }
        return explosiveData;
    }

    /**
     * Triggers the explosive at the location
     *
     * @param source        - user running the command
     * @param level         - position data
     * @param x             - position data
     * @param y             - position data
     * @param z             - position data
     * @param explosiveData - explosive to run
     * @param size          - size to set
     * @return 1 if the blast was triggered, 0 if it reported an error
     */
    private static int trigger(CommandSourceStack source, ServerLevel level, double x, double y, double z, IExplosiveData explosiveData, float size)
    {
        final IActionStatus result = explosiveData.create(level, x, y, z, new ActionSource(), new ActionFieldProvider().field(ActionFields.AREA_SIZE, () -> size)).doAction();

        //Send translated message to user
        source.sendSystemMessage(result.message()); //1.12: result.getTooltip()

        return result.isError() ? 0 : 1;
    }
}
