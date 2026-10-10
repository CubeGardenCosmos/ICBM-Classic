package icbm.classic.content.actions.entity;

import com.google.common.collect.ImmutableList;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.actions.status.MissingFieldStatus;
import icbm.classic.lib.projectile.EntityProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ActionSpawnEntity extends ActionBase {

    public static final List<ActionField> FIELDS = ImmutableList.of(
        ActionFields.YAW, ActionFields.PITCH, ActionFields.MOTION_VECTOR,
        ActionFields.ENTITY_REG_NAME, ActionFields.ENTITY_DATA);

    private ResourceLocation entityID;
    private NBTTagCompound entityData;
    private Vec3d motion;
    private Float yaw;
    private Float pitch;

    public ActionSpawnEntity(World world, Vec3d position, IActionSource source, IActionData actionData) {
        super(world, position, source, actionData);
    }

    @Nonnull
    @Override
    public IActionStatus doAction() {
        if (entityID == null) {
            return new MissingFieldStatus().setSource("ActionSpawnEntity").setField("entityId");
        }
        final Entity entity = EntityList.createEntityByIDFromName(entityID, getWorld());
        if (entity == null) {
            return new MissingFieldStatus().setSource("ActionSpawnEntity#doAction()").setField("entityInstance");
        }

        entity.setPosition(getPosition().x, getPosition().y, getPosition().z);
        if (motion != null) {
            if(entity instanceof EntityProjectile) {
                ((EntityProjectile<?>) entity).setMotionVector(motion.x, motion.y, motion.z);
            }
            else {
                entity.motionX = motion.x;
                entity.motionY = motion.y;
                entity.motionZ = motion.z;
            }
        }

        // Apply yaw
        if (yaw != null) {
            entity.rotationYaw = entity.prevRotationYaw = yaw;
        } else {
            entity.rotationYaw = entity.prevRotationYaw = MathHelper.wrapDegrees(getWorld().rand.nextFloat() * 360.0F);
        }
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase) entity).rotationYawHead = entity.rotationYaw;
            ((EntityLivingBase) entity).renderYawOffset = entity.rotationYaw;
        }

        // Apply pitch
        if (pitch != null) {
            entity.rotationPitch = entity.prevRotationPitch = pitch;
        } else {
            entity.rotationYaw = entity.prevRotationYaw = MathHelper.wrapDegrees(getWorld().rand.nextFloat() * 360.0F);
        }

        // Handle data loading
        if (entityData != null) {
            final UUID uuid = entity.getUniqueID();

            NBTTagCompound mergedData = entity.writeToNBT(new NBTTagCompound());
            mergedData.merge(entityData);
            entity.readFromNBT(mergedData);

            entity.setUniqueId(uuid);

        }

        if (!getWorld().spawnEntity(entity)) {
            return ActionResponses.ENTITY_SPAWN_FAILED;
        }
        return ActionResponses.COMPLETED;
    }

    @Override
    public <VALUE, TAG extends NBTBase> void setValue(ActionField<VALUE, TAG> key, VALUE value) {
        if (key == ActionFields.YAW) {
            this.yaw = ActionFields.YAW.cast(value);
        }
        if (key == ActionFields.PITCH) {
            this.pitch = ActionFields.PITCH.cast(value);
        }
        if (key == ActionFields.MOTION_VECTOR) {
            this.motion = (Vec3d) value;
        }
        if (key == ActionFields.ENTITY_REG_NAME) {
            this.entityID = (ResourceLocation) value;
        }
        if (key == ActionFields.ENTITY_DATA) {
            this.entityData = (NBTTagCompound) value;
        }
    }

    @Override
    public <VALUE, TAG extends NBTBase> VALUE getValue(ActionField<VALUE, TAG> key) {
        if (key == ActionFields.YAW) {
            return key.cast(yaw);
        }
        if (key == ActionFields.PITCH) {
            return key.cast(pitch);
        }
        if (key == ActionFields.MOTION_VECTOR) {
            return key.cast(motion);
        }
        if (key == ActionFields.ENTITY_REG_NAME) {
            return key.cast(entityID);
        }
        if (key == ActionFields.ENTITY_DATA) {
            return key.cast(entityData);
        }
        return null;
    }


    @Override
    public Collection<ActionField> getFields() {
        return FIELDS;
    }
}
