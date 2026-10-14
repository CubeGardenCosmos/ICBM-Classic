package icbm.classic.content.cargo;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileThrowable;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.reg.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class ItemThrowableProjectile extends Item
{
    public static final int MAX_STACK_SIZE = 16;
    public static final int MAX_USE_DURATION = 3 * 20;
    public static final float THROW_VELOCITY = 0.5f;

    public ItemThrowableProjectile(Item.Properties properties)
    {
        super(properties.stacksTo(MAX_STACK_SIZE));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack)
    {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return MAX_USE_DURATION;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand)
    {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity entityLiving, int timeLeft)
    {
        if (!world.isClientSide())
        {
            if (throwProjectile(stack, world, entityLiving))
            {
                if (!(entityLiving instanceof Player player && player.isCreative()))
                {
                    stack.shrink(1);
                }
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean throwProjectile(ItemStack stack, Level world, Entity thrower)
    {
        final IProjectileData<Entity> projectileData = (IProjectileData<Entity>) ICBMClassicAPI.PROJECTILE_DATA_REGISTRY.find(stack);
        if (projectileData == null)
        {
            return false;
        }

        final boolean isCreative = thrower instanceof Player player && player.isCreative();
        final Entity projectileEntity = projectileData.newEntity(world, !isCreative);
        if (projectileEntity == null)
        {
            return false;
        }

        final float yaw = thrower.getYRot();
        final float pitch = thrower.getXRot();
        final double x = thrower.getX();
        final double y = thrower.getEyeY();
        final double z = thrower.getZ();

        if (projectileEntity instanceof IProjectileThrowable<?> throwable)
        {
            final IActionSource source = new ActionSource(world, new Vec3(x, y, z), new EntityCause(thrower));
            ((IProjectileThrowable<Entity>) throwable).throwProjectile(projectileEntity, source, x, y, z, yaw, pitch, THROW_VELOCITY, 0);
        }
        else
        {
            projectileEntity.setPos(x, y, z);
            final Vec3 look = thrower.getLookAngle();
            projectileEntity.setDeltaMovement(look.scale(THROW_VELOCITY));
        }

        if (world.addFreshEntity(projectileEntity))
        {
            final InteractionHand hand = thrower instanceof LivingEntity living ? living.getUsedItemHand() : InteractionHand.MAIN_HAND;
            projectileData.onEntitySpawned(projectileEntity, thrower, hand);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        final ItemStack held = stack.get(ModDataComponents.HELD_ITEM);
        if (held != null && !held.isEmpty())
        {
            tooltip.accept(Component.translatable("icbmclassic.projectile.holder.held").append(": ").append(held.getHoverName()));
        }
    }
}
