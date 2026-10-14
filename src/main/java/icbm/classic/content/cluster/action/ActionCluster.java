package icbm.classic.content.cluster.action;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.transform.RotationHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ActionCluster extends ActionBase
{
    private static final Vec3 SOUTH_VEC = new Vec3(0, 0, 1);
    private static final Vec3 UP_VEC = new Vec3(0, 1, 0);
    private static final float STACK_SCALE = 0.1f;
    private static final float OFFSET_SCALE = 0.25f;

    @Getter
    @Setter
    private NonNullList<ItemStack> spawnList = NonNullList.create();

    @Getter
    @Setter
    private boolean allowPickupItem = true;

    @Getter
    @Setter
    private float ejectionScale = 0.3f;

    @Getter
    @Setter
    private int projectilesPerLayer = 10;

    @Getter
    @Setter
    private float motionPitchOffset = -22.5f;

    @Getter
    @Setter
    private float discYawOffset = 22.5f;

    @Getter
    @Setter
    private float motionScale = 0.3f;

    @Getter
    @Setter
    private float motionScaleLayer = 0.1f;

    @Getter
    @Setter
    private float motionRandomScale = 0.1f;

    @Getter
    @Setter
    private float sourceYaw = 0;

    @Getter
    @Setter
    private float sourcePitch = 0;

    public ActionCluster(Level world, Vec3 position, IActionSource source, IActionData actionData)
    {
        super(world, position, source, actionData);
    }

    @Override
    public IActionStatus doAction()
    {
        if (this.getWorld().isClientSide())
        {
            return ActionResponses.COMPLETED;
        }

        boolean spawnedSomething = false;
        final float yawAmount = 360.0f / projectilesPerLayer;

        int bombsToFire = this.spawnList.size();
        int discIndex = 0;
        while (bombsToFire > 0)
        {
            for (int bombIndex = 0; bombIndex < this.projectilesPerLayer && bombsToFire > 0; bombIndex++)
            {
                bombsToFire -= 1;

                final double yaw = Mth.wrapDegrees(yawAmount * bombIndex + (discIndex * discYawOffset));
                final Vec3 offsetYaw = RotationHelper.rotateY(SOUTH_VEC, yaw);
                final Vec3 sourceVec = RotationHelper.rotateY(RotationHelper.rotateX(offsetYaw, sourcePitch - 90), sourceYaw);
                final Vec3 backVector = RotationHelper.rotateX(UP_VEC, motionPitchOffset);

                double motionX = (sourceVec.x + backVector.x) * motionScale;
                double motionY = (sourceVec.y + backVector.y) * motionScale;
                double motionZ = (sourceVec.z + backVector.z) * motionScale;

                motionX += (sourceVec.x + backVector.x) * motionScaleLayer * discIndex;
                motionY += (sourceVec.y + backVector.y) * motionScaleLayer * discIndex;
                motionZ += (sourceVec.z + backVector.z) * motionScaleLayer * discIndex;

                if (motionRandomScale > 0)
                {
                    motionX += (getWorld().getRandom().nextFloat() - getWorld().getRandom().nextFloat()) * motionScaleLayer;
                    motionY += (getWorld().getRandom().nextFloat() - getWorld().getRandom().nextFloat()) * motionScaleLayer;
                    motionZ += (getWorld().getRandom().nextFloat() - getWorld().getRandom().nextFloat()) * motionScaleLayer;
                }

                final Vec3 stackVector = RotationHelper.rotateY(RotationHelper.rotateX(SOUTH_VEC, sourcePitch - 180), sourceYaw);
                double x = (sourceVec.x * OFFSET_SCALE) + (stackVector.x * STACK_SCALE * discIndex);
                double y = (sourceVec.y * OFFSET_SCALE) + (stackVector.y * STACK_SCALE * discIndex);
                double z = (sourceVec.z * OFFSET_SCALE) + (stackVector.z * STACK_SCALE * discIndex);

                spawnedSomething = spawnProjectile(spawnList.get(bombsToFire), x, y, z, motionX, motionY, motionZ) || spawnedSomething;
            }
            discIndex += 1;
        }

        return spawnedSomething ? ActionResponses.COMPLETED : ActionResponses.ENTITY_SPAWN_FAILED;
    }

    private boolean spawnProjectile(ItemStack stackToSpawn, double x, double y, double z, double mx, double my, double mz)
    {
        final Entity entity = ICBMClassicAPI.PROJECTILE_DATA_REGISTRY.spawnProjectile(stackToSpawn, getWorld(),
                getPosition().x + x, getPosition().y + y, getPosition().z + z,
                null, this.allowPickupItem,
                newEntity -> {
                    newEntity.setPos(getPosition().x + x, getPosition().y + y, getPosition().z + z);
                    newEntity.setDeltaMovement(mx, my, mz);

                    final float f3 = Mth.sqrt((float) (mx * mx + mz * mz));
                    final float yaw = (float) (Mth.atan2(mx, mz) * (180.0D / Math.PI));
                    final float pitch = (float) (Mth.atan2(my, f3) * (180.0D / Math.PI));
                    newEntity.setYRot(yaw);
                    newEntity.setXRot(pitch);
                    newEntity.yRotO = yaw;
                    newEntity.xRotO = pitch;
                });

        if (entity == null)
        {
            final ItemEntity itemEntity = new ItemEntity(getWorld(), getPosition().x + x, getPosition().y + y, getPosition().z + z, stackToSpawn.copy());
            itemEntity.setDefaultPickUpDelay();
            itemEntity.setDeltaMovement(mx, my, mz);

            final float f3 = Mth.sqrt((float) (mx * mx + mz * mz));
            final float yaw = (float) (Mth.atan2(mx, mz) * (180.0D / Math.PI));
            final float pitch = (float) (Mth.atan2(my, f3) * (180.0D / Math.PI));
            itemEntity.setYRot(yaw);
            itemEntity.setXRot(pitch);
            itemEntity.yRotO = yaw;
            itemEntity.xRotO = pitch;

            return getWorld().addFreshEntity(itemEntity);
        }
        return true;
    }
}
