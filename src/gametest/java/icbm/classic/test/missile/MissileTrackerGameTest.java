package icbm.classic.test.missile;

import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.missile.tracker.MissileTrackerData;
import icbm.classic.content.missile.tracker.MissileTrackerHandler;
import icbm.classic.content.missile.tracker.MissileTrackerWorld;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Game tests for off-chunk missile simulation and missile tracker SavedData (S4.03).
 */
public class MissileTrackerGameTest
{
    /**
     * Asserts that simulateMissile removes an eligible explosive missile entity from the world
     * and records it into the level's MissileTrackerWorld SavedData.
     */
    @GameTest(maxTicks = 40)
    public void simulateMissileRemovesEntityAndEntersSavedData(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(level, true);
        tracker.destroy(level);

        final BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        final BlockPos target = helper.absolutePos(new BlockPos(50, 2, 50));

        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, level);
        missile.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);

        final Item missileItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        missile.explosive.setStack(new ItemStack(missileItem));
        missile.initHealth(missile.getMaxHealth());
        missile.setTargetData(new BasicTargetData(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D));

        level.addFreshEntity(missile);
        helper.assertTrue(!missile.isRemoved(), "missile entity must be in world before simulation");

        final boolean queued = MissileTrackerHandler.simulateMissile(missile);
        helper.assertTrue(queued, "simulateMissile must return true for eligible missile");
        helper.assertTrue(missile.isRemoved(), "missile entity must be discarded after entering simulation");

        helper.assertTrue(tracker.missileList.size() == 1,
                "tracker should have exactly 1 missile in simulation queue, had " + tracker.missileList.size());
        final MissileTrackerData data = tracker.missileList.getFirst();
        helper.assertTrue(data.ticksLeftToTarget > 0, "ticksLeftToTarget should be positive");
        helper.assertTrue(Math.abs(data.targetPos.x - (target.getX() + 0.5D)) < 0.001D, "target x should match");
        helper.assertTrue(Math.abs(data.targetPos.z - (target.getZ() + 0.5D)) < 0.001D, "target z should match");

        tracker.destroy(level);
        helper.succeed();
    }

    /**
     * Asserts that a simulated missile whose ticks expire re-enters the world near its target coordinates
     * at simulation enter height with DeadFlightLogic.
     */
    @GameTest(maxTicks = 40)
    public void simulatedMissileRespawnsAtDestinationWhenTicksExpire(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(level, true);
        tracker.destroy(level);

        final BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        final BlockPos target = helper.absolutePos(new BlockPos(10, 2, 10));

        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, level);
        missile.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);

        final Item missileItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        missile.explosive.setStack(new ItemStack(missileItem));
        missile.initHealth(missile.getMaxHealth());
        missile.setTargetData(new BasicTargetData(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D));

        level.addFreshEntity(missile);
        final boolean queued = MissileTrackerHandler.simulateMissile(missile);
        helper.assertTrue(queued, "simulateMissile should succeed");

        helper.assertTrue(tracker.missileList.size() == 1, "must have 1 simulated missile");
        final MissileTrackerData data = tracker.missileList.getFirst();

        // Simulate missile reaching target
        data.ticksLeftToTarget = 0;

        // First tracker update moves missile to spawn queue and forces chunks
        tracker.runSimulationTick(level);
        helper.assertTrue(tracker.missileList.isEmpty(), "missileList should be empty after reaching target");
        helper.assertTrue(tracker.missileSpawnList.size() == 1, "missileSpawnList should hold the missile");

        // Advance preload countdown to 1
        tracker.missileSpawnList.getFirst().preLoadChunkTimer = 1;

        // Second tracker update spawns missile into destination
        tracker.runSimulationTick(level);
        helper.assertTrue(tracker.missileSpawnList.isEmpty(), "missileSpawnList should be empty after spawning");

        // Query spawned missile near target coordinates
        final AABB box = new AABB(
                target.getX() - 5, ConfigMissile.SIMULATION_ENTER_HEIGHT - 5, target.getZ() - 5,
                target.getX() + 5, ConfigMissile.SIMULATION_ENTER_HEIGHT + 5, target.getZ() + 5
        );
        final List<EntityExplosiveMissile> spawnedMissiles = level.getEntitiesOfClass(EntityExplosiveMissile.class, box);
        helper.assertTrue(!spawnedMissiles.isEmpty(), "a missile should have spawned near target at simulation enter height");

        final EntityExplosiveMissile spawned = spawnedMissiles.getFirst();
        helper.assertTrue(spawned.getType() == ModEntities.EXPLOSIVE_MISSILE, "spawned entity should be explosive missile");
        helper.assertTrue(Math.abs(spawned.getX() - (target.getX() + 0.5D)) < 0.1D, "spawned x should match target x");
        helper.assertTrue(Math.abs(spawned.getY() - ConfigMissile.SIMULATION_ENTER_HEIGHT) < 0.1D, "spawned y should match simulation enter height");
        helper.assertTrue(Math.abs(spawned.getZ() - (target.getZ() + 0.5D)) < 0.1D, "spawned z should match target z");
        helper.assertTrue(spawned.getFlightLogic() instanceof DeadFlightLogic, "spawned missile should use DeadFlightLogic");
        helper.assertTrue(ICBMExplosives.CONDENSED.getRegistryKey().equals(spawned.getExplosiveData().getRegistryKey()),
                "spawned missile should retain condensed explosive payload");

        // Clean up spawned missile and forced chunks
        spawned.discard();
        tracker.destroy(level);
        helper.succeed();
    }

    /**
     * Asserts that MissileTrackerData and MissileTrackerWorld survive Codec serialization and deserialization.
     */
    @GameTest(maxTicks = 40)
    public void missileTrackerDataAndWorldSurviveCodecRoundTrip(GameTestHelper helper)
    {
        final CompoundTag missileTag = new CompoundTag();
        missileTag.putString("id", "icbmclassic:explosive_missile");
        missileTag.putString("customKey", "roundTripValue");

        final Vec3 target = new Vec3(123.5, 64.0, -456.5);
        final MissileTrackerData originalData = new MissileTrackerData(45, target, missileTag, 2);

        // 1. MissileTrackerData Codec round trip
        final Tag encodedDataTag = MissileTrackerData.CODEC.encodeStart(NbtOps.INSTANCE, originalData)
                .getOrThrow(msg -> new AssertionError("Failed to encode MissileTrackerData: " + msg));
        helper.assertTrue(encodedDataTag instanceof CompoundTag, "encoded data should be CompoundTag");

        final MissileTrackerData decodedData = MissileTrackerData.CODEC.parse(NbtOps.INSTANCE, encodedDataTag)
                .getOrThrow(msg -> new AssertionError("Failed to decode MissileTrackerData: " + msg));

        helper.assertTrue(decodedData.ticksLeftToTarget == 45, "ticksLeftToTarget must match");
        helper.assertTrue(decodedData.targetPos.equals(target), "targetPos must match");
        helper.assertTrue("roundTripValue".equals(decodedData.missileData.getStringOr("customKey", "")), "missileData payload must match");
        helper.assertTrue(decodedData.preLoadChunkTimer == 2, "preLoadChunkTimer must match");

        // 2. MissileTrackerWorld Codec round trip
        final ChunkPos chunkPos = new ChunkPos(7, -28);
        final MissileTrackerWorld.TrackedChunk trackedChunk = new MissileTrackerWorld.TrackedChunk(chunkPos, 50);
        final MissileTrackerWorld originalWorld = new MissileTrackerWorld(
                List.of(originalData),
                List.of(new MissileTrackerData(0, target, missileTag.copy(), 4)),
                List.of(trackedChunk)
        );

        final Tag encodedWorldTag = MissileTrackerWorld.CODEC.encodeStart(NbtOps.INSTANCE, originalWorld)
                .getOrThrow(msg -> new AssertionError("Failed to encode MissileTrackerWorld: " + msg));

        final MissileTrackerWorld decodedWorld = MissileTrackerWorld.CODEC.parse(NbtOps.INSTANCE, encodedWorldTag)
                .getOrThrow(msg -> new AssertionError("Failed to decode MissileTrackerWorld: " + msg));

        helper.assertTrue(decodedWorld.missileList.size() == 1, "missileList size must match");
        helper.assertTrue(decodedWorld.missileList.getFirst().ticksLeftToTarget == 45, "decoded missile ticks must match");
        helper.assertTrue(decodedWorld.missileSpawnList.size() == 1, "missileSpawnList size must match");
        helper.assertTrue(decodedWorld.missileSpawnList.getFirst().preLoadChunkTimer == 4, "decoded spawn preLoadChunkTimer must match");
        helper.assertTrue(decodedWorld.trackedChunks.size() == 1, "trackedChunks size must match");
        helper.assertTrue(decodedWorld.trackedChunks.getFirst().chunkPos.equals(chunkPos), "chunkPos must match");
        helper.assertTrue(decodedWorld.trackedChunks.getFirst().timeLeft == 50, "chunk timeLeft must match");

        helper.succeed();
    }
}
