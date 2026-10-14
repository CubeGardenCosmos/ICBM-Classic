package icbm.classic.test.entity;

import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the flying block entity (S3.03):
 *
 * <ul>
 *     <li>a block captured by {@link FlyingBlock#spawnFlyingBlock(net.minecraft.world.level.Level, BlockPos, java.util.function.Consumer, java.util.function.Consumer)}
 *     is put back into the world when the entity lands and the entity is removed,</li>
 *     <li>the save/load of the entity (carried block state, source stack and gravity) survives a round
 *     trip, including the block state that is synced for the client renderer (S4.07).</li>
 * </ul>
 */
public class FlyingBlockGameTest {

    /** Relative y of the floor the flying block lands on */
    private static final int FLOOR_Y = 0;
    /** Relative x/z the block and the flying block are placed at */
    private static final int BLOCK_X = 2;
    private static final int BLOCK_Z = 2;
    /** Relative y the captured block is taken from, the flying block spawns here */
    private static final int BLOCK_Y = 2;

    @GameTest(maxTicks = 200)
    public void flyingBlockPlacesBlockWhenItLands(GameTestHelper helper) {
        // floor for the flying block to fall onto
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(x, FLOOR_Y, z, Blocks.STONE);
            }
        }

        final BlockPos sourcePos = new BlockPos(BLOCK_X, BLOCK_Y, BLOCK_Z);
        helper.setBlock(sourcePos, Blocks.GOLD_BLOCK);

        // 1.12 spawnFlyingBlock captures the block, removes it and spawns the entity above the position
        final EntityFlyingBlock[] spawned = new EntityFlyingBlock[1];
        helper.assertTrue(FlyingBlock.spawnFlyingBlock(helper.getLevel(), helper.absolutePos(sourcePos), null,
                        entity -> spawned[0] = entity),
                "the flying block was not spawned for the gold block");
        final EntityFlyingBlock entity = spawned[0];

        // the captured state is pushed into the synced data (1.12 sent it with the spawn data)
        helper.assertTrue(entity.getBlockState() == Blocks.GOLD_BLOCK.defaultBlockState(),
                "the spawned entity does not carry the captured block state, synced as " + entity.getBlockState());
        helper.assertTrue(helper.getBlockState(sourcePos).isAir(),
                "the captured block should have been removed from the world");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity.isRemoved(),
                        "the flying block did not land, still at " + entity.position()))
                // one tick so the level has dropped the removed entity from its entity lookup
                .thenIdle(1)
                .thenExecute(() ->
                {
                    // 1.12 places the block at the position calculated for the impact, which for a fall onto
                    // the floor is the block above the floor (the captured block is not put back where it
                    // came from) and drops the source stack when that fails
                    helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(BLOCK_X, FLOOR_Y + 1, BLOCK_Z));
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(BLOCK_X, FLOOR_Y, BLOCK_Z));
                    helper.assertTrue(helper.getBlockState(sourcePos).isAir(),
                            "the flying block put its block back at the captured position instead of the impact");
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityFlyingBlock.class,
                                    entity.getBoundingBox().inflate(8.0D)).isEmpty(),
                            "the flying block entity should have been removed after placing its block");
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void flyingBlockSurvivesSaveRoundTrip(GameTestHelper helper) {
        final EntityFlyingBlock entity = helper.spawn(ModEntities.FLYING_BLOCK, new Vec3(BLOCK_X + 0.5D, BLOCK_Y + 0.5D, BLOCK_Z + 0.5D));
        entity.getBlockData().setBlockState(Blocks.GOLD_BLOCK.defaultBlockState());
        entity.getBlockData().setSourceStack(new ItemStack(Items.GOLD_BLOCK));
        entity.setProjectileGravity(0.25F);

        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        entity.saveWithoutId(output);
        final CompoundTag tag = output.buildResult();

        final EntityFlyingBlock loaded = ModEntities.FLYING_BLOCK.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loaded != null, "failed to create a copy of the flying block");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

        helper.assertTrue(loaded.getBlockData().getBlockState() == Blocks.GOLD_BLOCK.defaultBlockState(),
                "the carried block state did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.getBlockState() == Blocks.GOLD_BLOCK.defaultBlockState(),
                "the synced block state was not restored from the save, saved as " + tag);
        helper.assertTrue(loaded.getBlockData().getSourceStack().is(Items.GOLD_BLOCK),
                "the source stack did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.getProjectileGravity() == 0.25F,
                "the gravity did not survive the save, saved as " + tag);

        loaded.discard();
        entity.discard();
        helper.succeed();
    }
}
