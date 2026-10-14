package icbm.classic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import icbm.classic.command.sub.CommandHelp;
import icbm.classic.command.sub.CommandLag;
import icbm.classic.command.sub.CommandRemove;
import icbm.classic.command.sub.blast.CommandBlastList;
import icbm.classic.command.sub.blast.CommandBlastSpread;
import icbm.classic.command.sub.blast.CommandBlastTrigger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Created by Robin Seifert on 1/6/20.
 *
 * <p>S3.10 mapping of the 1.12 command system onto Brigadier:
 * <ul>
 *     <li>{@code system/CommandEntryPoint} -&gt; MERGED into {@link #buildICBMCommand()}: the Forge
 *     {@code ICommand} entry point that forwarded the raw argument array, the permission level and the tab
 *     completion is now the {@code icbm} {@link Commands#literal(String) literal} node. 1.12
 *     {@code getRequiredPermissionLevel()} returned 2, which is
 *     {@code Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)} in 26.3
 *     ({@code PermissionLevel.GAMEMASTERS} has id 2).</li>
 *     <li>{@code system/CommandGroup} + {@code system/ICommandGroup} -&gt; MERGED, a group is the Brigadier
 *     literal it is attached to ({@code icbm}, {@code blast}) and its children are the {@code then(...)}
 *     branches. 1.12's {@code MapWithDefault} also made an unknown sub command fall back to help; Brigadier
 *     reports its own "unknown command" error instead (a group with no argument still prints help through
 *     {@code executes(...)}).</li>
 *     <li>{@code system/SubCommand} + {@code system/ISubCommand} -&gt; MERGED, {@code handleCommand} became
 *     the {@code executes(...)} lambda of the command class and {@code collectHelpForAll/WorldOnly/
 *     ServerOnly} became {@link CommandHelp.HelpLine}.</li>
 * </ul>
 *
 * <p>The command output uses the {@code icbmclassic.command.*} keys that S2.10 migrated from the 1.12
 * {@code command.icbmclassic:icbm.*} keys, with the 1.12 argument order.
 */
public class ICBMCommands
{
    /** 1.12 {@code ICBMCommands.ICBM_COMMAND} */
    public static final String ICBM_COMMAND = "icbm";
    /** 1.12 {@code ICBMCommands.BLAST_COMMAND} */
    public static final String BLAST_COMMAND = "blast";

    public static final String TRANSLATION_UNKNOWN_COMMAND = "icbmclassic.command.error.unknown.command";

    /**
     * Called to register the command tree, 1.12 {@code ICBMCommands#init()}. Reached from
     * {@code ICBMClassic#onInitialize} through Fabric's {@code CommandRegistrationCallback}, which replaces the
     * 1.12 Forge command registry event.
     *
     * @param dispatcher - command dispatcher of the server
     */
    public static void init(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(buildICBMCommand());
    }

    /**
     * Builds the {@code /icbm} command tree, 1.12 {@code ICBMCommands#init()}.
     *
     * @return the root command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> buildICBMCommand()
    {
        //Sub commands, 1.12 registration order of ICBM_COMMAND
        final List<CommandHelp.HelpLine> helpLines = List.of(
                CommandHelp.helpLine("/" + ICBM_COMMAND + " " + CommandHelp.NAME),
                CommandHelp.helpLine("/" + ICBM_COMMAND + " " + BLAST_COMMAND),
                CommandRemove.HELP_LINE,
                CommandLag.HELP_LINE);

        return Commands.literal(ICBM_COMMAND)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                //1.12 CommandGroup#handleCommand used "help" when no argument was given
                .executes(context ->
                {
                    CommandHelp.displayHelp(context.getSource(), helpLines);
                    return 1;
                })
                .then(CommandHelp.build(helpLines))
                .then(buildBlastCommand())
                .then(CommandRemove.build())
                .then(CommandLag.build());
    }

    /**
     * Builds the {@code /icbm blast} command group, 1.12 {@code ICBMCommands#initBlastCommand()}.
     *
     * @return the blast command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> buildBlastCommand()
    {
        //Sub commands, 1.12 registration order of BLAST_COMMAND
        final List<CommandHelp.HelpLine> helpLines = List.of(
                CommandHelp.helpLine("/" + ICBM_COMMAND + " " + BLAST_COMMAND + " " + CommandHelp.NAME),
                CommandBlastList.HELP_LINE,
                CommandBlastTrigger.HELP_LINE,
                CommandBlastSpread.HELP_LINE);

        return Commands.literal(BLAST_COMMAND)
                .executes(context ->
                {
                    CommandHelp.displayHelp(context.getSource(), helpLines);
                    return 1;
                })
                .then(CommandHelp.build(helpLines))
                .then(CommandBlastList.build())
                .then(CommandBlastTrigger.build())
                .then(CommandBlastSpread.build());
    }

    /**
     * Creates the 1.12 {@code WrongUsageException(TRANSLATION_UNKNOWN_COMMAND, usage)} error, used when the
     * arguments did not match any of the forms of a command (1.12 checked the argument count itself, the
     * Brigadier tree only leaves forms the sender is allowed to run).
     *
     * @param usage - usage of the command, 1.12 {@code getUsage(sender)}
     * @return exception to throw
     */
    public static CommandSyntaxException unknownCommand(String usage)
    {
        return new SimpleCommandExceptionType(Component.translatable(TRANSLATION_UNKNOWN_COMMAND, usage)).create();
    }
}
