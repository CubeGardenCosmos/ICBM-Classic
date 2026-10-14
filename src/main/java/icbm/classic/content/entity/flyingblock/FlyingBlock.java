package icbm.classic.content.entity.flyingblock;

import icbm.classic.config.ConfigFlyingBlocks;
import icbm.classic.config.util.BlockStateConfigListOld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles validation and spawning of {@link EntityFlyingBlock}.
 *
 * <p>26.3 port notes (porting-guide.md §2.1):
 * <ul>
 *     <li>The 1.12 {@code Material} system is gone. {@code Material.FIRE} → {@link BaseFireBlock} (the only
 *     1.12 block using it) and {@code IFluidBlock}/{@code BlockLiquid} → {@link LiquidBlock} (the mapping
 *     {@code BlastBlockHelpers} uses for fluids).</li>
 *     <li>{@code Block#getBlockHardness(World, BlockPos)} → {@code BlockState#getDestroySpeed(Level, BlockPos)},
 *     {@code Block#isAir(state, world, pos)} → {@code BlockState#isAir()}.</li>
 *     <li>{@code World#setBlockState(pos, state, flags)} → {@code Level#setBlock(pos, state, flags)}, the
 *     1.12 flag 2 is {@link Block#UPDATE_CLIENTS}.</li>
 *     <li>{@code World#spawnEntity} → {@code Level#addFreshEntity} and {@code Entity#setPosition} →
 *     {@code Entity#setPos}.</li>
 * </ul>
 */
public class FlyingBlock {

    // Config list controlling if a block is allowed for spawning
    static final BlockStateConfigListOld banAllowList = new BlockStateConfigListOld("[Flying Blocks][Ban/Allow Config]",
        (blockStateConfigList) -> {
            // Mod blacklisted due to https://github.com/BuiltBrokenModding/ICBM-Classic/issues/420
            blockStateConfigList.addMod("dynamictrees");//TODO remove when issue #420 is resolved


            // Load configs
            blockStateConfigList.loadBlockStates(ConfigFlyingBlocks.banAllow.blockStates);
        }
    );

    /**
     * Validates if the given block state is allowed as a flying block
     *
     * @param state to check
     * @return true if allowed
     */
    public static boolean isAllowed(BlockState state) {
        if (!ConfigFlyingBlocks.enabled
            || state == null
            || state.getBlock() instanceof BaseFireBlock // 1.12 checked Material.FIRE
            || state.getBlock() instanceof LiquidBlock // 1.12 checked IFluidBlock / BlockLiquid
        ) {
            return false;
        }

        // Ban List
        if (ConfigFlyingBlocks.banAllow.ban) {
            return !banAllowList.contains(state);
        }

        // Allow List
        return banAllowList.contains(state);
    }

    /**
     * Applies any mutations to the block state before converting to a flying block
     *
     * @param state to mutate
     * @return new state to use
     */
    public static BlockState applyMutations(BlockState state) {
        return state;
    }

    /**
     * Spawns a flying block, will check that block isn't air and isn't unbreakable
     * <p>
     * 1.12 pointed mods at the Forge entity spawn events to prevent this. 26.3 has no cancellable entity
     * spawn event (Fabric's {@code ServerEntityEvents.ENTITY_LOAD} is a notification), so the checks in
     * {@link #isAllowed(BlockState)} are the only gate.
     *
     * @param world to spawn into
     * @param pos   to set
     * @return true if spawned
     */
    public static boolean spawnFlyingBlock(Level world, BlockPos pos,
                                           Consumer<EntityFlyingBlock> preSpawnCallback,
                                           Consumer<EntityFlyingBlock> postSpawnCallback) {
        final BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir() && isAllowed(blockState))
        {
            final float hardness = blockState.getDestroySpeed(world, pos);
            if (hardness >= 0)
            {
                final BlockCaptureData blockCaptureData = new BlockCaptureData(world, pos);
                return spawnFlyingBlock(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    blockCaptureData, preSpawnCallback, postSpawnCallback,
                    () -> world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
                );
            }
        }
        return false;
    }

    /**
     * Spawns a flying block, it is assumed the caller does any pre-checks on block itself.
     * <p>
     * If a mod wishes to prevent this use the spawn events of 26.3, see
     * {@link #spawnFlyingBlock(Level, BlockPos, Consumer, Consumer)}.
     *
     * @param world to spawn into
     * @param x location
     * @param y location
     * @param z location
     * @param blockCaptureData to spawn, can be replaced by other systems and user config
     * @param removeBlock callback to set to air, done after collecting data
     * @return true if spawned
     */
    public static boolean spawnFlyingBlock(Level world, double x, double y, double z,
                                           BlockCaptureData blockCaptureData,
                                           Consumer<EntityFlyingBlock> preSpawnCallback,
                                           Consumer<EntityFlyingBlock> postSpawnCallback,
                                           Supplier<Boolean> removeBlock) {
        if (!isAllowed(blockCaptureData.getBlockState())) {
            return false;
        }

        if(removeBlock != null && !removeBlock.get()) {
            return false;
        }

        // TODO limit per chunk and per world to help reduce lag

        final EntityFlyingBlock flyingBlock = new EntityFlyingBlock(world);
        flyingBlock.setBlockData(blockCaptureData); //TODO allow mutations of state
        flyingBlock.setPos(x, y, z);

        // Pre-spawn data set, needed for extra properties that should be exposed to spawn event
        Optional.ofNullable(preSpawnCallback).ifPresent(f -> f.accept(flyingBlock));

        // 26.3 has no spawn data packet: push the state (which the callback above may have replaced) into
        // the synced data before the entity starts being tracked by clients
        flyingBlock.syncBlockState();

        if (world.addFreshEntity(flyingBlock)) {

            // Post-spawn data set, needed for logic that can't run outside the world
            Optional.ofNullable(postSpawnCallback).ifPresent(f -> f.accept(flyingBlock));

            //TODO add event logging
            return true;
        }
        return false;
    }

    public static void loadFromConfig() {
        banAllowList.reload();
        //TODO load replacements, ensure we store as block -> handler
    }
}
