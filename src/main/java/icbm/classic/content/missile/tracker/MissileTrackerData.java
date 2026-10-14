package icbm.classic.content.missile.tracker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.lib.NBTConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Stores missile simulation data for off-chunk / unloaded-chunk missile simulation (S4.03).
 */
public class MissileTrackerData
{
    /**
     * Target position codec supporting both the 1.12 CompoundTag format ({x, y, z})
     * and the 26.3 list format ([x, y, z]).
     */
    public static final Codec<Vec3> TARGET_POS_CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.DOUBLE.fieldOf(NBTConstants.X).forGetter(Vec3::x),
                    Codec.DOUBLE.fieldOf(NBTConstants.Y).forGetter(Vec3::y),
                    Codec.DOUBLE.fieldOf(NBTConstants.Z).forGetter(Vec3::z)
            ).apply(instance, Vec3::new)),
            Vec3.CODEC
    );

    public static final Codec<MissileTrackerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(NBTConstants.TICKS).forGetter(mtd -> mtd.ticksLeftToTarget),
            TARGET_POS_CODEC.fieldOf(NBTConstants.TARGET).forGetter(mtd -> mtd.targetPos),
            CompoundTag.CODEC.fieldOf(NBTConstants.DATA).forGetter(mtd -> mtd.missileData),
            Codec.INT.optionalFieldOf("preLoadChunkTimer", 0).forGetter(mtd -> mtd.preLoadChunkTimer)
    ).apply(instance, MissileTrackerData::new));

    /** Seconds before the missile spawns in the loaded chunk (1 update tick = 1 second) */
    public int preLoadChunkTimer;

    /** Seconds/ticks left before the missile reaches the target area (1 update tick = 1 second) */
    public int ticksLeftToTarget;

    /** Target coordinates */
    public Vec3 targetPos;

    /** Serialized entity data for rebuilding missile on spawn */
    public CompoundTag missileData;

    public MissileTrackerData(int ticksLeftToTarget, Vec3 targetPos, CompoundTag missileData, int preLoadChunkTimer)
    {
        this.ticksLeftToTarget = ticksLeftToTarget;
        this.targetPos = targetPos != null ? targetPos : Vec3.ZERO;
        this.missileData = missileData != null ? missileData : new CompoundTag();
        this.preLoadChunkTimer = preLoadChunkTimer;
    }

    public MissileTrackerData(int ticksLeftToTarget, Vec3 targetPos, CompoundTag missileData)
    {
        this(ticksLeftToTarget, targetPos, missileData, 0);
    }

    public MissileTrackerData(EntityExplosiveMissile missile)
    {
        this.targetPos = missile.getMissileCapability().getTargetData() != null
                ? missile.getMissileCapability().getTargetData().getPosition()
                : missile.position();
        this.missileData = createMissileTag(missile);
        this.preLoadChunkTimer = 0;
        this.ticksLeftToTarget = 0;
    }

    private static CompoundTag createMissileTag(EntityExplosiveMissile missile)
    {
        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, missile.level().registryAccess());
        missile.saveWithoutId(out);
        final CompoundTag tag = out.buildResult();
        tag.putString("id", "icbmclassic:explosive_missile");
        tag.remove("Pos");
        tag.remove("Motion");
        return tag;
    }

    public CompoundTag writeToNBT(CompoundTag nbt)
    {
        nbt.putInt(NBTConstants.TICKS, ticksLeftToTarget);
        final CompoundTag targetTag = new CompoundTag();
        targetTag.putDouble(NBTConstants.X, targetPos.x);
        targetTag.putDouble(NBTConstants.Y, targetPos.y);
        targetTag.putDouble(NBTConstants.Z, targetPos.z);
        nbt.put(NBTConstants.TARGET, targetTag);
        nbt.put(NBTConstants.DATA, missileData.copy());
        if (preLoadChunkTimer > 0)
        {
            nbt.putInt("preLoadChunkTimer", preLoadChunkTimer);
        }
        return nbt;
    }

    public void readFromNBT(CompoundTag nbt)
    {
        this.ticksLeftToTarget = nbt.getIntOr(NBTConstants.TICKS, 0);
        final CompoundTag targetTag = nbt.getCompoundOrEmpty(NBTConstants.TARGET);
        this.targetPos = new Vec3(
                targetTag.getDoubleOr(NBTConstants.X, 0),
                targetTag.getDoubleOr(NBTConstants.Y, 0),
                targetTag.getDoubleOr(NBTConstants.Z, 0)
        );
        this.missileData = nbt.getCompoundOrEmpty(NBTConstants.DATA).copy();
        this.preLoadChunkTimer = nbt.getIntOr("preLoadChunkTimer", 0);
    }
}
