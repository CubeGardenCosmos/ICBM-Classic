package icbm.classic.content.missile.tracker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.api.events.MissileChunkEvent;
import icbm.classic.config.ConfigDebug;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * World saved data for tracking and simulating missiles outside loaded chunks (S4.03).
 */
public class MissileTrackerWorld extends SavedData
{
    /** Unload cooldown in simulation seconds/update ticks (60 seconds) */
    public static final int UNLOAD_CHUNK_COOLDOWN = 60;

    /** Wait time in seconds/update ticks before spawning missile in a force-loaded chunk (5 seconds) */
    public static final int PRE_LOAD_CHUNK_TIMER = 5;

    /**
     * Represents a chunk forced for missile arrival and the remaining time it should stay loaded.
     */
    public static final class TrackedChunk
    {
        public static final Codec<TrackedChunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ChunkPos.CODEC.fieldOf("chunk").forGetter(TrackedChunk::getChunkPos),
                Codec.INT.fieldOf("timeLeft").forGetter(TrackedChunk::getTimeLeft)
        ).apply(instance, TrackedChunk::new));

        public final ChunkPos chunkPos;
        public int timeLeft;

        public TrackedChunk(ChunkPos chunkPos, int timeLeft)
        {
            this.chunkPos = chunkPos;
            this.timeLeft = timeLeft;
        }

        public ChunkPos getChunkPos()
        {
            return chunkPos;
        }

        public int getTimeLeft()
        {
            return timeLeft;
        }
    }

    public static final Codec<MissileTrackerWorld> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(MissileTrackerData.CODEC).optionalFieldOf(NBTConstants.MISSILES, List.of()).forGetter(w -> w.missileList),
            Codec.list(MissileTrackerData.CODEC).optionalFieldOf(NBTConstants.SPAWNS, List.of()).forGetter(w -> w.missileSpawnList),
            Codec.list(TrackedChunk.CODEC).optionalFieldOf("chunks", List.of()).forGetter(w -> w.trackedChunks)
    ).apply(instance, MissileTrackerWorld::new));

    public static final SavedDataType<MissileTrackerWorld> TYPE = new SavedDataType<>(
            ICBMConstants.id("missile_tracker"),
            MissileTrackerWorld::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    /** Stores missiles currently being simulated in flight */
    public final LinkedList<MissileTrackerData> missileList;

    /** Stores missiles awaiting spawn at target area */
    public final LinkedList<MissileTrackerData> missileSpawnList;

    /** Stores currently forced chunks with remaining hold time */
    public final LinkedList<TrackedChunk> trackedChunks;

    /** Tick counter for reducing simulation updates to 1/second (20 ticks) */
    private int ticks = 0;

    public MissileTrackerWorld()
    {
        this(List.of(), List.of(), List.of());
    }

    public MissileTrackerWorld(List<MissileTrackerData> missiles, List<MissileTrackerData> spawns, List<TrackedChunk> chunks)
    {
        this.missileList = new LinkedList<>(missiles);
        this.missileSpawnList = new LinkedList<>(spawns);
        this.trackedChunks = new LinkedList<>(chunks);
    }

    /**
     * Enters the missile into the simulation queue and removes its entity from the world.
     */
    public void simulateMissile(EntityExplosiveMissile missile)
    {
        if (ConfigDebug.DEBUG_MISSILE_TRACKER)
        {
            ICBMClassic.LOGGER.info("MissileTracker[{}]: Simulating missile: {}",
                    missile.level().dimension().identifier(),
                    missile
            );
        }

        if (!missile.level().isClientSide() && missile.getMissileCapability().getTargetData() != null)
        {
            // Clear flight logic once missile enters data simulation
            missile.getMissileCapability().setFlightLogic(null);

            final MissileTrackerData mtd = new MissileTrackerData(missile);

            // Calculate distance to target
            final double dx = missile.getMissileCapability().getTargetData().getX() - missile.getX();
            final double dz = missile.getMissileCapability().getTargetData().getZ() - missile.getZ();
            final double dst = Math.sqrt(dx * dx + dz * dz);

            // Calculate duration in simulation seconds (1 second per update tick)
            mtd.ticksLeftToTarget = (int) Math.round(dst / ConfigMissile.SIMULATION_FLIGHT_SPEED);
            missileList.add(mtd);

            // Remove entity from the level
            missile.discard();

            this.setDirty();
        }
    }

    /**
     * Called on each server level tick. Runs simulation logic every 20 ticks (1 second).
     */
    public void onLevelTick(final ServerLevel world)
    {
        if (ticks++ >= 20)
        {
            ticks = 0;
            runSimulationTick(world);
        }
    }

    /**
     * Executes one second / 20-tick interval of tracker simulation. Exposed for testing.
     */
    public void runSimulationTick(final ServerLevel world)
    {
        int mIndex = 0;
        final Iterator<MissileTrackerData> missileIterator = missileList.iterator();
        while (missileIterator.hasNext())
        {
            final MissileTrackerData missile = missileIterator.next();
            if (missile.ticksLeftToTarget <= 0)
            {
                // Force target chunk and 4 orthogonal neighbors
                final int chunkX = Mth.floor(missile.targetPos.x) >> 4;
                final int chunkZ = Mth.floor(missile.targetPos.z) >> 4;

                forceChunk(world, new ChunkPos(chunkX, chunkZ));
                forceChunk(world, new ChunkPos(chunkX + 1, chunkZ));
                forceChunk(world, new ChunkPos(chunkX - 1, chunkZ));
                forceChunk(world, new ChunkPos(chunkX, chunkZ + 1));
                forceChunk(world, new ChunkPos(chunkX, chunkZ - 1));

                missile.preLoadChunkTimer = PRE_LOAD_CHUNK_TIMER;
                missileSpawnList.add(missile);
                missileIterator.remove();
            }
            else
            {
                missile.ticksLeftToTarget--;
                if (ConfigDebug.DEBUG_MISSILE_TRACKER)
                {
                    ICBMClassic.LOGGER.info("MissileTracker Missile [{}]: Simulation ticks left: {}", mIndex, missile.ticksLeftToTarget);
                }
            }
            mIndex++;
        }

        // Tick forced chunks and unforce expired ones
        final Iterator<TrackedChunk> chunkIterator = trackedChunks.iterator();
        while (chunkIterator.hasNext())
        {
            final TrackedChunk chunk = chunkIterator.next();
            final int waitTime = chunk.timeLeft - 1;
            if (waitTime <= 0)
            {
                final MissileChunkEvent.Unload unloadEvent = new MissileChunkEvent.Unload(world.dimension().identifier(), chunk.chunkPos, waitTime);
                final boolean allowUnload = MissileChunkEvent.Unload.EVENT.invoker().onUnload(unloadEvent);
                if (allowUnload)
                {
                    world.setChunkForced(chunk.chunkPos.x(), chunk.chunkPos.z(), false);
                    chunkIterator.remove();
                }
                else
                {
                    // Listener extended wait time
                    chunk.timeLeft = unloadEvent.timeLeft;
                }
            }
            else
            {
                chunk.timeLeft = waitTime;
            }
        }

        // Tick spawn queue and re-enter world when countdown reaches 0
        final Iterator<MissileTrackerData> spawnIterator = missileSpawnList.iterator();
        while (spawnIterator.hasNext())
        {
            final MissileTrackerData mtd = spawnIterator.next();
            mtd.preLoadChunkTimer--;
            if (mtd.preLoadChunkTimer <= 0)
            {
                spawnMissileOnDestination(world, mtd);
                spawnIterator.remove();
            }
        }

        this.setDirty();
    }

    private void forceChunk(ServerLevel world, ChunkPos chunkPos)
    {
        for (TrackedChunk tracked : trackedChunks)
        {
            if (tracked.chunkPos.equals(chunkPos))
            {
                tracked.timeLeft = UNLOAD_CHUNK_COOLDOWN;
                return;
            }
        }

        final MissileChunkEvent.Load loadEvent = new MissileChunkEvent.Load(world.dimension().identifier(), chunkPos, UNLOAD_CHUNK_COOLDOWN);
        final boolean allowLoad = MissileChunkEvent.Load.EVENT.invoker().onLoad(loadEvent);
        if (allowLoad)
        {
            trackedChunks.add(new TrackedChunk(chunkPos, loadEvent.timeLeft));
            world.setChunkForced(chunkPos.x(), chunkPos.z(), true);
        }
    }

    private void spawnMissileOnDestination(final ServerLevel world, MissileTrackerData mtd)
    {
        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, world);
        missile.load(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), mtd.missileData));
        missile.snapTo(mtd.targetPos.x, ConfigMissile.SIMULATION_ENTER_HEIGHT, mtd.targetPos.z, 0.0F, 90.0F);
        missile.setDeltaMovement(0, -ConfigMissile.SIMULATION_ENTER_SPEED, 0);

        if (missile.getMissileCapability().getFlightLogic() == null)
        {
            missile.getMissileCapability().setFlightLogic(new DeadFlightLogic(100));
        }

        missile.getMissileCapability().launch();
        world.addFreshEntity(missile);

        if (ConfigDebug.DEBUG_MISSILE_TRACKER)
        {
            ICBMClassic.LOGGER.info("MissileTracker[{}]: Missile spawned by missile tracker: {}",
                    world.dimension().identifier(),
                    missile
            );
        }
    }

    public void destroy(ServerLevel world)
    {
        if (world != null)
        {
            for (TrackedChunk chunk : trackedChunks)
            {
                try
                {
                    world.setChunkForced(chunk.chunkPos.x(), chunk.chunkPos.z(), false);
                }
                catch (Exception e)
                {
                    ICBMClassic.LOGGER.error("Failed to unforce chunk {} on destroy: {}", chunk.chunkPos, e.getMessage());
                }
            }
        }
        destroy();
    }

    public void destroy()
    {
        this.missileList.clear();
        this.missileSpawnList.clear();
        this.trackedChunks.clear();
    }
}
