package icbm.classic.api.actions.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ActionFields {
    /** Size of area from center of action. For a box with size 3, this means it will generate 7x7 area. As size will be 3 up + 1 center + 3 down */
    public static ActionField<Float, NBTTagFloat> AREA_SIZE;

    /** RUNTIME_ONLY: Way to access host entity, usually provided by the entity itself */
    public static ActionField<Entity, NBTBase> HOST_ENTITY;

    /** RUNTIME_ONLY: Position of the host */
    public static ActionField<Vec3d, NBTTagCompound> HOST_POSITION;

    /** RUNTIME_ONLY: Rough direction the host is facing */
    public static ActionField<EnumFacing, NBTTagByte> HOST_DIRECTION;

    /** True if system has impacted something. Usually being entity has impacted ground or another entity. */
    public static ActionField<Boolean, NBTTagByte> IMPACTED;

    /** Position of the current target or aim, or desired target position of an action */
    public static ActionField<Vec3d, NBTTagCompound> TARGET_POSITION;

    /** Motion vector to set */
    public static ActionField<Vec3d, NBTTagCompound> MOTION_VECTOR;

    /** Rotation yaw to set */
    public static ActionField<Float, NBTTagFloat> YAW;

    /** Rotation pitch to set */
    public static ActionField<Float, NBTTagFloat> PITCH;

    /** Entity registry name, for use with {@link net.minecraft.entity.EntityList} */
    public static ActionField<ResourceLocation, NBTTagString> ENTITY_REG_NAME;

    /** Entity save data */
    public static ActionField<NBTTagCompound, NBTTagCompound> ENTITY_DATA;
}
