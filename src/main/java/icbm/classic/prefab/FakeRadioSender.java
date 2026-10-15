package icbm.classic.prefab;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.radio.messages.ITextMessage;
import icbm.classic.lib.data.BoundBlockPos;
import icbm.classic.lib.radio.RadioRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Wrapper used by items to act as a radio wave sender.
 * <p>
 * 1.12 held a player; 26.3 sends the feedback through {@link ServerPlayer} (S5 phase notes) and
 * uses {@link ServerPlayer#sendOverlayMessage(Component)} for the 1.12
 * {@code sendStatusMessage(component, true)} action bar message.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 4/24/2016.
 */
public class FakeRadioSender implements IRadioSender
{
    private static final Object[] empty = new Object[0];

    public final ServerPlayer player;
    public final ItemStack item;
    IBoundBox<BlockPos> bounds;

    public FakeRadioSender(ServerPlayer player, ItemStack item, Integer range)
    {
        this.player = player;
        this.item = item;
        if (range != null)
        {
            this.bounds = new BoundBlockPos(player.blockPosition(), range); //1.12: player.getPosition()
        }
        else
        {
            this.bounds = RadioRegistry.INFINITE;
        }
    }

    @Override
    public BlockPos getBlockPos()
    {
        return player.blockPosition();
    }

    @Override
    public Level getWorld()
    {
        return player.level();
    }

    @Override
    public IBoundBox<BlockPos> getRange()
    {
        return bounds;
    }

    @Override
    public void onMessageCallback(IRadioReceiver receiver, IRadioMessage response)
    {
        if (response instanceof ITextMessage textMessage)
        {
            if (textMessage.shouldTranslate())
            {
                final Object[] data = Optional.ofNullable(textMessage.getTranslationInputs()).orElse(empty);
                //1.12: player.sendStatusMessage(new TextComponentTranslation(message, data), true)
                player.sendOverlayMessage(Component.translatable(textMessage.getMessage(), data));
            }
            else
            {
                //1.12: player.sendStatusMessage(new TextComponentString(message), true); literals are limited
                //to the values the sender provides (coordinates, channels, names)
                player.sendOverlayMessage(Component.literal(textMessage.getMessage()));
            }
        }
    }
}
