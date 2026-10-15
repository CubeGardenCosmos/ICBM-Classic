package icbm.classic.content.items;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.events.LaserRemoteTriggerEvent;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.lib.data.GpsData;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.TriggerActionTargetMessage;
import icbm.classic.prefab.FakeRadioSender;
import icbm.classic.prefab.item.ItemRadio;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.HitResult;

/** Remote detonator variant that triggers a target position along the user's line of sight. */
public class ItemLaserDetonator extends ItemRadio
{
    public static final double RANGE = 200;

    public ItemLaserDetonator(Item.Properties properties)
    {
        super(properties);
    }

    public static void registerUseCallback()
    {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof ItemLaserDetonator))
            {
                return InteractionResult.PASS;
            }
            if (level.isClientSide())
            {
                return InteractionResult.SUCCESS;
            }
            HitResult hit = player.pick(RANGE, 1.0F, false);
            if (hit.getType() == HitResult.Type.MISS)
            {
                player.sendSystemMessage(Component.translatable("icbmclassic.item.laser_detonator.laser.missed", RANGE));
                return InteractionResult.SUCCESS;
            }
            if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit
                    && ICBMClassicHelpers.isLauncher(level.getBlockEntity(blockHit.getBlockPos()), null))
            {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer)
            {
                var event = new LaserRemoteTriggerEvent(level, hit.getLocation(), player);
                if (LaserRemoteTriggerEvent.EVENT.invoker().onTrigger(event))
                {
                    stack.set(ModDataComponents.GPS_DATA,
                            new GpsData(GlobalPos.of(level.dimension(), BlockPos.containing(event.getPos()))));
                    String channel = ((ItemLaserDetonator) stack.getItem()).getRadioChannel(stack);
                    RadioRegistry.popMessage(level, new FakeRadioSender(serverPlayer, stack, null),
                            new TriggerActionTargetMessage(channel, event.getPos()));
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
