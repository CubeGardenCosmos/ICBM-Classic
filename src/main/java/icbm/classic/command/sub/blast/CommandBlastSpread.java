package icbm.classic.command.sub.blast;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Created by Robin Seifert on 1/6/20.
 *
 * <p>S3.10: the 1.12 {@code SubCommand} became a Brigadier node. 1.12 required exactly 8 arguments
 * ({@code args.length == 8} after the sub command name); the 26.3 tree carries that single form. The
 * {@code count}/{@code distance} pair keeps 1.12's {@code CommandBase.parseInt(value, 1)} minimum through
 * {@code IntegerArgumentType.integer(1)}, {@code scale} keeps {@code Float.parseFloat} plus the
 * {@code TRANSLATION_ERROR_SCALE_ZERO} check of {@link CommandBlastTrigger}.
 *
 * <p>1.12 resolved the position with {@code CommandUtils#getNumber(sender, value, sender.getPosition() + 0.5)},
 * so {@code ~} meant the block center of the sender; 26.3's {@code Vec3Argument} resolves {@code ~} against
 * the exact position of the source and the console check of {@code getNumber} is applied again.
 */
public class CommandBlastSpread
{
    private static final String TRANSLATION_KEY = "icbmclassic.command.spread";
    public static final String TRANSLATION_SPREAD_START = TRANSLATION_KEY + ".started";

    /** 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "spread";

    /** 1.12 {@code collectHelpForAll} */
    public static final CommandHelp.HelpLine HELP_LINE = new CommandHelp.HelpLine("/" + ICBMCommands.ICBM_COMMAND + " " + ICBMCommands.BLAST_COMMAND + " " + NAME,
            List.of("<count> <distance> <id> <dim> <x> <y> <z> <scale>"),
            List.of(),
            List.of());

    private static final String ARGUMENT_COUNT = "count";
    private static final String ARGUMENT_DISTANCE = "distance";
    private static final String ARGUMENT_EXPLOSIVE = "explosive";
    private static final String ARGUMENT_DIMENSION = "dimension";
    private static final String ARGUMENT_POSITION = "pos";
    private static final String ARGUMENT_SCALE = "scale";

    /** 1.12 {@code new SyntaxErrorException(CommandBlastTrigger.TRANSLATION_ERROR_SCALE_ZERO)} */
    private static final SimpleCommandExceptionType ERROR_SCALE_ZERO = new SimpleCommandExceptionType(
            Component.translatable(CommandBlastTrigger.TRANSLATION_ERROR_SCALE_ZERO));

    /**
     * Builds the {@code /icbm blast spread} node, 1.12 {@code CommandBlastSpread#handleCommand}.
     *
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(NAME)
                .then(Commands.argument(ARGUMENT_COUNT, IntegerArgumentType.integer(1)) //1.12: CommandBase.parseInt(args[0], 1)
                        .then(Commands.argument(ARGUMENT_DISTANCE, IntegerArgumentType.integer(1)) //1.12: CommandBase.parseInt(args[1], 1)
                                .then(Commands.argument(ARGUMENT_EXPLOSIVE, IdentifierArgument.id())
                                        .suggests(CommandBlastTrigger.SUGGEST_EXPLOSIVES)
                                        .then(Commands.argument(ARGUMENT_DIMENSION, DimensionArgument.dimension())
                                                .then(Commands.argument(ARGUMENT_POSITION, Vec3Argument.vec3(false))
                                                        .then(Commands.argument(ARGUMENT_SCALE, FloatArgumentType.floatArg())
                                                                .executes(CommandBlastSpread::doCommand)))))));
    }

    /**
     * 1.12 {@code CommandBlastSpread#doCommand}: trigger a grid of blasts around the target position.
     *
     * @param context - command context
     * @return number of blasts the grid is expected to spawn
     * @throws CommandSyntaxException - if the explosive id is unknown, the scale is not above zero or '~'
     *                               was used by the console
     */
    private static int doCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final CommandSourceStack source = context.getSource();
        final int count = IntegerArgumentType.getInteger(context, ARGUMENT_COUNT);
        final int distance = IntegerArgumentType.getInteger(context, ARGUMENT_DISTANCE);

        //Get explosive data from user
        final IExplosiveData explosiveData = CommandBlastTrigger.getExplosive(IdentifierArgument.getId(context, ARGUMENT_EXPLOSIVE));

        //Get world position
        final Coordinates coordinates = Vec3Argument.getCoordinates(context, ARGUMENT_POSITION);
        CommandUtils.checkRelativeAllowed(source, coordinates);
        final ServerLevel world = DimensionArgument.getDimension(context, ARGUMENT_DIMENSION);
        final Vec3 position = coordinates.getPosition(source);

        //Get scale from user
        final float scale = FloatArgumentType.getFloat(context, ARGUMENT_SCALE); //TODO turn into helper method
        if (scale <= 0)
        {
            throw ERROR_SCALE_ZERO.create();
        }

        final int expectedSpawnCount = (int) Math.floor(Math.pow((count * 2) + 1, 2));

        //1.12 passed world.provider.getDimension() (the numeric id) and world.getWorldType().getName();
        //26.3 has no numeric dimension ids and no world types, so the dimension identifier and the
        //dimension type name are used. The 1.12 argument order is kept as-is.
        source.sendSystemMessage(Component.translatable(TRANSLATION_SPREAD_START,
                explosiveData.getRegistryKey(), scale,
                world.dimension().identifier(), world.dimensionTypeRegistration().getRegisteredName(),
                position.x, position.y, position.z,
                count, distance,
                expectedSpawnCount));

        //Generate blasts in a grid
        for (int xi = -count; xi <= count; xi++)
        {
            for (int zi = -count; zi <= count; zi++)
            {
                //calc position
                final double x = position.x + xi * distance;
                final double z = position.z + zi * distance;

                //Trigger blast
                final IActionStatus result = explosiveData.create(world, x, position.y, z, new ActionSource(), new ActionFieldProvider().field(ActionFields.AREA_SIZE, () -> scale)).doAction();

                //Send translated message to user
                source.sendSystemMessage(result.message()); //1.12: result.getTooltip()
            }
        }

        return expectedSpawnCount;
    }
}
