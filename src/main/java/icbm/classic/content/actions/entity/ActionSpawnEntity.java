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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import java.util.List;
import java.util.UUID;

/**
 * Spawns an entity at the action position, optionally setting its motion, rotation and save data. Used by the
 * smoke explosive (1.12 {@code ExplosiveInit} built it with {@code ActionFields.ENTITY_REG_NAME = smoke}).
 * <p>
 * 26.3 port notes (1.12 -> 26.3):
 * <ul>
 *     <li>{@code EntityList.createEntityByIDFromName(entityID, world)} -> the flat entity type registry
 *     ({@code BuiltInRegistries.ENTITY_TYPE}, a DefaultedRegistry so {@code containsKey} is needed before
 *     {@code getValue}) plus {@code EntityType#create(Level, EntitySpawnReason)}. The reason
 *     {@code TRIGGERED} mirrors "spawned by content".</li>
 *     <li>{@code entity.setPosition(x, y, z)} -> {@code Entity#setPos(double, double, double)}.</li>
 *     <li>{@code ((EntityProjectile) entity).setMotionVector(...)} / {@code entity.motionX = ...}: both the
 *     1.12 {@code EntityProjectile} path and the plain entity path only wrote the motion vector ({@code
 *     EntityICBM#setMotionVector} set motionX/Y/Z), 26.3 has one {@code Entity#setDeltaMovement} for both.</li>
 *     <li>{@code rotationYaw}/{@code prevRotationYaw} -> {@code setYRot}; {@code prevYRot} no longer exists in
 *     26.3 (rotation interpolation works off {@code yRotO}), so only the current rotation is set.
 *     {@code rotationYawHead}/{@code renderYawOffset} -> {@code setYHeadRot}/{@code setYBodyRot}.</li>
 *     <li>{@code MathHelper.wrapDegrees} -> {@link Mth#wrapDegrees(float)}, {@code world.rand} ->
 *     {@code Level#getRandom()}.</li>
 *     <li>{@code entity.writeToNBT(new tag)} + {@code merge} + {@code readFromNBT} -> {@code Entity#save(ValueOutput)}
 *     through {@link TagValueOutput}, {@link CompoundTag#merge(CompoundTag)}, then {@code Entity#load(ValueInput)}
 *     through {@link TagValueInput}; the UUID is restored afterwards exactly like 1.12.</li>
 *     <li>{@code world.spawnEntity(entity)} -> {@code ServerLevel#addFreshEntity(Entity)} (both answer false
 *     when the entity could not be added).</li>
 * </ul>
 * <b>Registration:</b> {@code ActionSystem.setup()} must register {@code ACTION_ENTITY_SPAWN}
 * ({@code icbmclassic:entity.spawn}) with an {@link icbm.classic.lib.actions.ActionDataGeneric} that creates
 * this action and lists {@link #FIELDS} (the 1.12 {@code ActionSystem} line is shown in the report). That file
 * is owned by the coordinating agent.
 */
public class ActionSpawnEntity extends ActionBase {

    //1.12 held a raw ImmutableList<ActionField>, 26.3 typed the field so the wildcard is needed here
    public static final List<ActionField<?>> FIELDS = ImmutableList.<ActionField<?>>of(
        ActionFields.YAW, ActionFields.PITCH, ActionFields.MOTION_VECTOR,
        ActionFields.ENTITY_REG_NAME, ActionFields.ENTITY_DATA);

    private Identifier entityID;
    private CompoundTag entityData;
    private Vec3 motion;
    private Float yaw;
    private Float pitch;

    public ActionSpawnEntity(Level world, Vec3 position, IActionSource source, IActionData actionData) {
        super(world, position, source, actionData);
    }

    @NonNull
    @Override
    public IActionStatus doAction() {
        if (entityID == null) {
            return new MissingFieldStatus().setSource("ActionSpawnEntity").setField("entityId");
        }
        //1.12: EntityList.createEntityByIDFromName(entityID, getWorld())
        final EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.containsKey(entityID) ? BuiltInRegistries.ENTITY_TYPE.getValue(entityID) : null;
        final Entity entity = entityType != null ? entityType.create(getWorld(), EntitySpawnReason.TRIGGERED) : null;
        if (entity == null) {
            return new MissingFieldStatus().setSource("ActionSpawnEntity#doAction()").setField("entityInstance");
        }

        entity.setPos(getPosition().x, getPosition().y, getPosition().z);
        if (motion != null) {
            entity.setDeltaMovement(motion.x, motion.y, motion.z);
        }

        // Apply yaw
        if (yaw != null) {
            entity.setYRot(yaw);
        } else {
            entity.setYRot(Mth.wrapDegrees(getWorld().getRandom().nextFloat() * 360.0F));
        }
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setYHeadRot(entity.getYRot());
            livingEntity.setYBodyRot(entity.getYRot());
        }

        // Apply pitch
        if (pitch != null) {
            entity.setXRot(pitch);
        } else {
            //1.12 bug kept on purpose: this branch wrote the yaw fields again instead of the pitch
            entity.setYRot(Mth.wrapDegrees(getWorld().getRandom().nextFloat() * 360.0F));
        }

        // Handle data loading
        if (entityData != null) {
            final UUID uuid = entity.getUUID();

            //1.12: wrote the entity to a fresh compound tag, merged entityData over it, then read it back into the entity
            final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, getWorld().registryAccess());
            entity.save(out);
            final CompoundTag mergedData = out.buildResult();
            mergedData.merge(entityData);
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, getWorld().registryAccess(), mergedData));

            entity.setUUID(uuid);
        }

        //1.12: getWorld().spawnEntity(entity)
        if (!(getWorld() instanceof ServerLevel serverLevel) || !serverLevel.addFreshEntity(entity)) {
            return ActionResponses.ENTITY_SPAWN_FAILED;
        }
        return ActionResponses.COMPLETED;
    }

    @Override
    public <VALUE> void setValue(ActionField<VALUE> key, VALUE value) {
        if (key == ActionFields.YAW) {
            this.yaw = ActionFields.YAW.cast(value);
        }
        if (key == ActionFields.PITCH) {
            this.pitch = ActionFields.PITCH.cast(value);
        }
        if (key == ActionFields.MOTION_VECTOR) {
            this.motion = (Vec3) value;
        }
        if (key == ActionFields.ENTITY_REG_NAME) {
            this.entityID = (Identifier) value;
        }
        if (key == ActionFields.ENTITY_DATA) {
            this.entityData = (CompoundTag) value;
        }
    }

    @Override
    public <VALUE> VALUE getValue(ActionField<VALUE> key) {
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
    public List<ActionField<?>> getFields() {
        return FIELDS;
    }
}
