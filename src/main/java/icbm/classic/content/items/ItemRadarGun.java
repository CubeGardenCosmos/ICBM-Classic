package icbm.classic.content.items;

import icbm.classic.api.events.RadarGunTraceEvent;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.lib.data.GpsData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Stores a looked-at coordinate on this stack as GPS data. */
public class ItemRadarGun extends Item
{
    public static final double MAX_RANGE = 200;

    public ItemRadarGun(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand)
    {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide())
            {
                stack.remove(ModDataComponents.GPS_DATA);
                player.sendSystemMessage(Component.translatable("icbmclassic.message.gps.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide())
        {
            HitResult hit = player.pick(MAX_RANGE, 1.0F, false);
            if (hit.getType() != HitResult.Type.MISS)
            {
                setPosition(level, player, stack, hit.getLocation());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null)
        {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide())
            {
                context.getItemInHand().remove(ModDataComponents.GPS_DATA);
                player.sendSystemMessage(Component.translatable("icbmclassic.message.gps.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide())
        {
            setPosition(level, player, context.getItemInHand(), context.getClickLocation());
        }
        return InteractionResult.SUCCESS;
    }

    private static void setPosition(Level level, Player player, net.minecraft.world.item.ItemStack stack, net.minecraft.world.phys.Vec3 position)
    {
        var event = new RadarGunTraceEvent(level, position, player);
        if (RadarGunTraceEvent.EVENT.invoker().onTrace(event) && event.pos != null)
        {
            stack.set(ModDataComponents.GPS_DATA,
                    new GpsData(GlobalPos.of(level.dimension(), BlockPos.containing(event.pos))));
            player.sendSystemMessage(Component.translatable("icbmclassic.message.gps.pos.set"));
        }
    }
}
