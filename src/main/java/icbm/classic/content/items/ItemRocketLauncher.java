package icbm.classic.content.items;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.IMissileAiming;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.cluster.action.ActionDataCluster;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.content.missile.logic.flight.ArcFlightLogic;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.missile.logic.flight.move.MoveByVec3Logic;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1.12 handheld launcher; the boolean selects direct or ballistic flight. */
public class ItemRocketLauncher extends Item
{
    private final boolean fireUpDown;

    public ItemRocketLauncher(boolean fireUpDown, Properties properties)
    {
        super(properties);
        this.fireUpDown = fireUpDown;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack)
    {
        return ItemUseAnimation.BOW;
    }

    private final java.util.Map<java.util.UUID, Long> lastFireMillis = new java.util.HashMap<>();

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        Long lastFire = lastFireMillis.get(player.getUUID());
        if (lastFire != null && System.currentTimeMillis() - lastFire < 1_000)
        {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack launcher, Level level, LivingEntity user, int timeLeft)
    {
        if (!(user instanceof Player player) || level.isClientSide())
        {
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            ItemStack missileStack = player.getInventory().getItem(slot);
            var missileStackCapability = IcbmLookups.MISSILE_STACK.find(missileStack, null);
            if (missileStackCapability == null)
            {
                continue;
            }

            IMissile missile = missileStackCapability.newMissile(level);
            var missileEntity = missile.getMissileEntity();
            if (!(missileEntity instanceof IMissileAiming aiming))
            {
                player.sendSystemMessage(Component.translatable("icbmclassic.item.rocket_launcher.error.IMissileAiming", missileStack));
                return false;
            }
            aiming.initAimingPosition(player, 1, ConfigMissile.DIRECT_FLIGHT_SPEED);
            Vec3 eye = player.getEyePosition(1.0F);
            Vec3 look = player.getLookAngle();
            HitResult trace = level.clip(new ClipContext(eye, eye.add(look.scale(500)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (fireUpDown)
            {
                var initial = new MoveByVec3Logic().setDistance(3).setRelative(false).setDirection(look).setAcceleration(0.2);
                missile.setFlightLogic(initial.addStep(new ArcFlightLogic()));
            }
            else
            {
                missile.setFlightLogic(new DeadFlightLogic(ConfigMissile.HANDHELD_FUEL));
            }
            missile.setMissileSource(new ActionSource(level, missileEntity.position(), new EntityCause(player)));

            if (trace.getType() != HitResult.Type.MISS)
            {
                Vec3 target = trace.getLocation();
                if (fireUpDown && target.distanceTo(player.position()) < 20)
                {
                    player.sendSystemMessage(Component.translatable("icbmclassic.item.rocket_launcher.error.distance.min", 20));
                    return false;
                }
                missile.setTargetData(new BasicTargetData(target));
            }
            else if (fireUpDown)
            {
                player.sendSystemMessage(Component.translatable("icbmclassic.item.rocket_launcher.error.targeting"));
                return false;
            }
            if (fireUpDown && missileEntity instanceof EntityMissileActionable actionable
                    && actionable.getMainAction() != null
                    && actionable.getMainAction().getActionData() instanceof ActionDataCluster
                    && missile.getTargetData() instanceof BasicTargetData targetData)
            {
                targetData.setPosition(targetData.getPosition().add(0, 20, 0));
            }

            missile.launch();
            if (level.addFreshEntity(missileEntity))
            {
                lastFireMillis.put(player.getUUID(), System.currentTimeMillis());
                if (!player.getAbilities().instabuild)
                {
                    missileStack.shrink(1);
                    player.getInventory().setChanged();
                }
                if (player.isShiftKeyDown())
                {
                    player.startRiding(missileEntity);
                    player.setShiftKeyDown(false);
                }
                else if (player.getOffhandItem().is(Items.LEAD))
                {
                    for (var victim : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            new net.minecraft.world.phys.AABB(player.getX() - 7, player.getY() - 7, player.getZ() - 7,
                                    player.getX() + 7, player.getY() + 7, player.getZ() + 7)))
                    {
                        if (victim.getLeashHolder() == player)
                        {
                            victim.startRiding(missileEntity);
                            break;
                        }
                    }
                }
                return true;
            }
            player.sendSystemMessage(Component.translatable("icbmclassic.item.rocket_launcher.error.spawning"));
            return false;
        }
        return false;
    }
}
