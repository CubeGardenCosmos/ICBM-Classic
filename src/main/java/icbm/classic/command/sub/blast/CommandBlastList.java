package icbm.classic.command.sub.blast;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.command.ICBMCommands;
import icbm.classic.command.sub.CommandHelp;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.stream.Collectors;

/**
 * Created by Robin Seifert on 1/6/20.
 *
 * <p>S3.10: the 1.12 {@code SubCommand} became a Brigadier node; the command took no arguments so
 * {@code handleCommand} is the {@code executes(...)} lambda of that node.
 */
public class CommandBlastList
{
    /** 1.12 {@code SubCommand#getName()} */
    public static final String NAME = "list";

    /** 1.12 {@code collectHelpForAll} was not overridden, so the help only shows the usage */
    public static final CommandHelp.HelpLine HELP_LINE =
            CommandHelp.helpLine("/" + ICBMCommands.ICBM_COMMAND + " " + ICBMCommands.BLAST_COMMAND + " " + NAME);

    /**
     * Builds the {@code /icbm blast list} node, 1.12 {@code CommandBlastList#handleCommand}.
     *
     * @return the command node
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(NAME).executes(context ->
        {
            //Convert list of explosives to string registry names
            final String names = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosives().stream()
                    .map(IExplosiveData::getRegistryKey)
                    .map(Identifier::toString)
                    .sorted()
                    .collect(Collectors.joining(", "));

            //Output message TODO translate if possible?
            context.getSource().sendSystemMessage(Component.literal("Explosive Types: " + names));

            return 1;
        });
    }
}
