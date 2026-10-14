package icbm.classic.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import icbm.classic.command.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Created by Robin Seifert on 1/6/20.
 *
 * <p>S3.10: 1.12 {@code CommandHelp} printed every sibling {@code SubCommand} of its parent
 * {@code CommandGroup} (usage plus the collected argument help). Brigadier has no command objects to walk, so
 * the group's children are described by {@link HelpLine} records that each command class provides and that
 * the group builder ({@code ICBMCommands}) passes in - the parent/child relation itself is merged into the
 * {@code literal(...).then(...)} tree of Brigadier.
 */
public class CommandHelp
{
    /** Name of the sub command, 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "help";

    /**
     * One line of 1.12 help output: {@code SubCommand#getUsage(sender)} plus the strings collected by
     * {@code collectHelpForAll} / {@code collectHelpWorldOnly} / {@code collectHelpServerOnly}.
     *
     * @param usage      - 1.12 usage without arguments, e.g. {@code "/icbm blast trigger"}
     * @param forAll     - help shown to every sender, 1.12 {@code collectHelpForAll} (an empty string prints
     *                   the bare usage, which was the 1.12 default)
     * @param worldOnly  - help shown when the sender lives in the world, 1.12 {@code collectHelpWorldOnly}
     * @param serverOnly - help shown to the console, 1.12 {@code collectHelpServerOnly}
     */
    public record HelpLine(String usage, List<String> forAll, List<String> worldOnly, List<String> serverOnly)
    {
        public HelpLine
        {
            forAll = List.copyOf(forAll);
            worldOnly = List.copyOf(worldOnly);
            serverOnly = List.copyOf(serverOnly);
        }
    }

    /**
     * Creates the 1.12 default help line of a {@code SubCommand} that did not override
     * {@code collectHelpForAll} ({@code consumer.accept("")}).
     *
     * @param usage - usage of the command
     * @return help line printing only the usage
     */
    public static HelpLine helpLine(String usage)
    {
        return new HelpLine(usage, List.of(""), List.of(), List.of());
    }

    /**
     * Builds the {@code help} node of a command group, 1.12 registered one {@code CommandHelp} per
     * {@code CommandGroup}.
     *
     * @param siblings - children of the group in registration order
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build(List<HelpLine> siblings)
    {
        return Commands.literal(NAME).executes(context ->
        {
            displayHelp(context.getSource(), siblings);
            return 1;
        });
    }

    /**
     * Sends all help text to the sender, 1.12 {@code SubCommand#displayHelp}.
     *
     * @param source   - user running the command
     * @param siblings - children of the group in registration order
     */
    public static void displayHelp(CommandSourceStack source, List<HelpLine> siblings)
    {
        for (HelpLine command : siblings)
        {
            command.forAll().forEach(text -> sendHelpMessage(source, command.usage() + " " + text));

            //If we have a command sender entity then we can run world based commands
            if (source.getEntity() != null)
            {
                command.worldOnly().forEach(text -> sendHelpMessage(source, command.usage() + " " + text));
            }

            //1.12 checked `sender instanceof MinecraftServer`, see CommandUtils#isConsoleSender for the 26.3
            //equivalent used here
            if (CommandUtils.isConsoleSender(source))
            {
                command.serverOnly().forEach(text -> sendHelpMessage(source, command.usage() + " " + text));
            }
        }
    }

    private static void sendHelpMessage(CommandSourceStack source, String message)
    {
        //1.12 used TextComponentString and ICommandSender#sendMessage, so the help is plain unformatted text
        //with no language key (CommandHelp never translated anything)
        source.sendSystemMessage(Component.literal(message.trim()));
    }
}
