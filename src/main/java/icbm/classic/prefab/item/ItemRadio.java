package icbm.classic.prefab.item;

import icbm.classic.api.radio.IRadio;
import icbm.classic.api.radio.IRadioChannelAccess;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.lib.radio.RadioRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Item carrying a radio channel, 1.12 {@code icbm.classic.prefab.item.ItemRadio} ported onto 26.3 items.
 * Base of the remote and laser detonators (S5.08).
 * <p>
 * Differences to the 1.12 class:
 * <ul>
 *     <li>1.12 extends {@code ItemBase}, which S2.04 dropped (the Forge tooltip framework went with it), so
 *     this extends {@link Item} directly,</li>
 *     <li>the channel is the {@code icbmclassic:radio_channel} data component (S2.04) instead of the 1.12
 *     item NBT keys {@code NBTConstants.HZ} / {@code radio_channel}; ADR 0002 means no save migration, so the
 *     legacy {@code HZ} float fallback is not ported,</li>
 *     <li>1.12's {@code onItemUseFirst} ran before the block interaction (Forge hook). 26.3 has no such hook,
 *     {@link Item#useOn(UseOnContext)} runs when the clicked block does not consume the interaction, so the
 *     channel of a device that opens a GUI is not copied by hand any more (the machines expose their channel
 *     through {@code IRadioChannelAccess} and the UI, see S5.03).</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin).
 */
public class ItemRadio extends Item
{
    public ItemRadio(Item.Properties properties)
    {
        super(properties);
    }

    /**
     * 1.12 {@code onItemUseFirst}: copy the channel of the clicked radio device onto this item and tell the
     * player about it.
     */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        final Level level = context.getLevel();
        //1.12 asked the block for ICBMClassicAPI.RADIO_CAPABILITY; 26.3 radios implement IRadio directly
        if (level.getBlockEntity(context.getClickedPos()) instanceof IRadio radio)
        {
            if (!level.isClientSide() && radio instanceof IRadioChannelAccess channelAccess)
            {
                final ItemStack heldItem = context.getItemInHand();
                final String channel = channelAccess.getChannel();
                setRadioChannel(heldItem, channel);

                final Player player = context.getPlayer();
                if (player != null)
                {
                    //1.12: player.sendMessage(new TextComponentString(LanguageUtility.getLocal("chat.launcher.toolFrequencySet").replace("%s", channel)))
                    player.sendSystemMessage(Component.translatable("icbmclassic.message.launcher.toolFrequencySet", channel));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Gets the frequency this item broadcasts information on
     *
     * @param stack - this item
     * @return frequency
     */
    public String getRadioChannel(ItemStack stack) //TODO move to capability item
    {
        final String channel = stack.get(ModDataComponents.RADIO_CHANNEL);
        return channel != null ? channel : RadioRegistry.EMPTY_HZ;
    }

    /**
     * Sets the frequency of this item
     *
     * @param stack   - this item
     * @param channel - value to set
     */
    public void setRadioChannel(ItemStack stack, String channel)
    {
        stack.set(ModDataComponents.RADIO_CHANNEL, channel);
    }
}
