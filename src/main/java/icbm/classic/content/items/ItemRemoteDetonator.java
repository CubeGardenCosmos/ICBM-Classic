package icbm.classic.content.items;

import icbm.classic.api.events.RemoteTriggerEvent;
import icbm.classic.lib.radio.messages.TriggerActionMessage;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.prefab.FakeRadioSender;
import icbm.classic.prefab.item.ItemRadio;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionResult;

/** Handheld radio trigger, with its channel stored on the item stack. */
public class ItemRemoteDetonator extends ItemRadio
{
    public ItemRemoteDetonator(Item.Properties properties)
    {
        super(properties);
    }

    public static void registerUseCallback()
    {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof ItemRemoteDetonator))
            {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
            {
                var event = new RemoteTriggerEvent(level, player, stack);
                if (RemoteTriggerEvent.EVENT.invoker().onTrigger(event))
                {
                    String channel = ((ItemRemoteDetonator) stack.getItem()).getRadioChannel(stack);
                    RadioRegistry.popMessage(level, new FakeRadioSender(serverPlayer, stack, null), new TriggerActionMessage(channel));
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
