package icbm.classic.api.actions.data;

import icbm.classic.api.data.meta.MetaTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Registry of the standard action fields.
 * <p>
 * Each field is created by {@link icbm.classic.lib.actions.ActionSystem#setup()}. The save/load functions
 * use the same 1.12 keys (for example {@code "area.size"}) but now write straight through
 * {@link net.minecraft.world.level.storage.ValueOutput} instead of producing an NBT tag.
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public final class ActionFields {
    /** Size of area from center of action. For a box with size 3, this means it will generate 7x7 area. As size will be 3 up + 1 center + 3 down */
    public static ActionField<Float> AREA_SIZE;

    /** RUNTIME_ONLY: Way to access host entity, usually provided by the entity itself */
    public static ActionField<Entity> HOST_ENTITY;

    /** RUNTIME_ONLY: Position of the host */
    public static ActionField<Vec3> HOST_POSITION;

    /** RUNTIME_ONLY: Rough direction the host is facing */
    public static ActionField<Direction> HOST_DIRECTION;

    /** True if system has impacted something. Usually being entity has impacted ground or another entity. */
    public static ActionField<Boolean> IMPACTED;

    /** Position of the current target or aim, or desired target position of an action */
    public static ActionField<Vec3> TARGET_POSITION;

    /** Motion vector to set */
    public static ActionField<Vec3> MOTION_VECTOR;

    /** Rotation yaw to set */
    public static ActionField<Float> YAW;

    /** Rotation pitch to set */
    public static ActionField<Float> PITCH;

    /** Entity registry name, for use with {@link net.minecraft.world.entity.EntityType} */
    public static ActionField<Identifier> ENTITY_REG_NAME;

    /** Entity save data */
    public static ActionField<CompoundTag> ENTITY_DATA;
}
