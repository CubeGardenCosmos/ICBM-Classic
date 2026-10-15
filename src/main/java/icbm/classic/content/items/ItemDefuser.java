package icbm.classic.content.items;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.events.ExplosiveDefuseEvent;
import icbm.classic.content.entity.EntityBombCart;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.PrimedTnt;

/** Disarms primed explosives, missiles, and ICBM bomb carts when attacked. */
public class ItemDefuser extends Item
{
    public ItemDefuser(Properties properties)
    {
        super(properties);
    }

    public static void registerAttackCallback()
    {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND
                    || !(player.getMainHandItem().getItem() instanceof ItemDefuser))
            {
                return InteractionResult.PASS;
            }
            return defuse(player.getMainHandItem(), player, level, target)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    public static boolean defuse(ItemStack stack, Player player, Level level, Entity entity)
    {
        if (level.isClientSide())
        {
            return isDefusable(entity);
        }
        IExplosive explosive = ICBMClassicHelpers.getExplosive(entity);
        if (explosive != null)
        {
            if (!ExplosiveDefuseEvent.ICBMExplosive.EVENT.invoker()
                    .onDefuse(new ExplosiveDefuseEvent.ICBMExplosive(player, entity, explosive)))
            {
                return false;
            }
            explosive.onDefuse();
            entity.discard();
            return true;
        }
        if (entity instanceof PrimedTnt)
        {
            if (!ExplosiveDefuseEvent.TNTExplosive.EVENT.invoker()
                    .onDefuse(new ExplosiveDefuseEvent.TNTExplosive(player, entity)))
            {
                return false;
            }
            level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.TNT)));
            entity.discard();
            return true;
        }
        if (entity instanceof EntityBombCart cart)
        {
            if (!ExplosiveDefuseEvent.ICBMBombCart.EVENT.invoker()
                    .onDefuse(new ExplosiveDefuseEvent.ICBMBombCart(player, entity)))
            {
                return false;
            }
            cart.destroy((ServerLevel) level, level.damageSources().generic());
            return true;
        }
        return false;
    }

    private static boolean isDefusable(Entity entity)
    {
        return ICBMClassicHelpers.isExplosive(entity) || entity instanceof PrimedTnt || entity instanceof MinecartTNT;
    }
}
