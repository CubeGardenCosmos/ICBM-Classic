package icbm.classic.content.blast;

import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.content.entity.EntityLightBeam;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.explosive.ThreadWorkBlast;
import icbm.classic.lib.thread.IThreadWork;
import icbm.classic.lib.thread.LevelSnapshot;
import icbm.classic.lib.thread.WorkerThreadManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used by Exothermic and Endothermic explosions.
 * <p>
 * Runs two collection passes on the S1.09 worker threads. The first one collects the blocks close to the
 * center that become flying blocks, the second one (started a few seconds later) collects everything the
 * beam covers so the subclass can mutate it. 1.12 started a thread per pass and wrote the results straight
 * into the fields of the blast; 26.3 splits the work into a snapshot read on the worker and a result hop
 * back to the server thread, see {@link #startThread(Function, int)} and {@link #runOnServerThread(Runnable)}.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>The 1.12 block state type → {@link BlockState}; {@code world.getBlockState} stays a level read on
 *     the server thread but becomes a {@link LevelSnapshot} read inside the passes.</li>
 *     <li>{@code block.isAir(state, world, pos)} → {@code BlockState#isAir()} and
 *     {@code state.getBlockHardness(world, pos)} → {@code BlockState#getDestroySpeed(level, pos)}, the
 *     hardness test moved to {@link #canMutate(BlockPos)}.</li>
 *     <li>{@code world.setBlockToAir(pos)} → {@code Level#setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)}
 *     and {@code world().createExplosion(exploder, x, y, z, 4F, true)} → {@code Level#explode(...,
 *     Level.ExplosionInteraction.TNT)}, the 1.12 trailing {@code true} was the terrain damaging explosion.</li>
 *     <li>{@code world().spawnEntity(beam)} → {@code Level#addFreshEntity(beam)},
 *     {@code ICBMSounds.X.play(world, x, y, z, volume, pitch, true)} →
 *     {@code Level#playSound(null, x, y, z, ModSounds.X, SoundSource.BLOCKS, volume, pitch)} and
 *     {@code world.setWorldTime(ticks)} → the data driven clock of the level, see {@link #setWorldTime(long)}.</li>
 *     <li>{@code Location#setPos(double, double, double)} on a mutable position floors the values, which is
 *     what {@code BlockPos.MutableBlockPos#set(double, double, double)} does in 26.3.</li>
 * </ul>
 *
 * @author Calclavia
 */
public abstract class BlastBeam extends Blast implements IBlastTickable
{
    protected final Set<EntityFlyingBlock> flyingBlocks = new HashSet<>();
    /** Positions collected by the passes, 1.12 reused the same list for both passes */
    protected final List<BlockPos> blocksToRemove = new ArrayList<>();

    protected EntityLightBeam lightBeam;
    protected float red, green, blue;

    private boolean hasDoneSetup = false;

    private boolean hasStartedFirstThread = false;
    private boolean hasCompletedFirstThread = false;

    private boolean hasGeneratedFlyingBlocks = false;

    private boolean hasStartedSecondThread = false;
    private boolean hasCompetedSecondThread = false;

    private boolean hasEnabledGravityForFlyingBlocks = false;

    private boolean hasPlacedBlocks = false;

    private int secondThreadTimer = 20 * 5;

    public BlastBeam()
    {
    }

    @Override
    protected boolean doExplode(int callCount)
    {
        if (!hasDoneSetup)
        {
            hasDoneSetup = true;

            //Play audio
            //1.12: ICBMSounds.BEAM_CHARGING.play(world, location.x(), location.y(), location.z(), 4.0F, 0.8F, true).
            //The 1.12 distanceDelay flag only affected the client side helper, 26.3 has no such argument
            this.world().playSound(null, location.x(), location.y(), location.z(), ModSounds.BEAM_CHARGING, SoundSource.BLOCKS, 4.0F, 0.8F);

            //Basic explosion
            //TODO remove basic in favor of thread
            //1.12: world().createExplosion(this.exploder, location.x(), location.y(), location.z(), 4F, true)
            this.level().explode(this.exploder, location.x(), location.y(), location.z(), 4F, Level.ExplosionInteraction.TNT);

            //Create beam
            this.lightBeam = new EntityLightBeam(this.world())
                    .setPosition(location)
                    .setColor(this.red, this.green, this.blue);
            this.lightBeam.beamSize = 1;
            this.lightBeam.beamGlowSize = 2;
            this.lightBeam.setTargetBeamProgress(0.1f);
            this.world().addFreshEntity(this.lightBeam); //1.12: world().spawnEntity(this.lightBeam)
        }

        //Start first thread if not already started
        if (!hasStartedFirstThread)
        {
            hasStartedFirstThread = true;
            this.lightBeam.setTargetBeamProgress(0.2f);
            //1.12: WorkerThreadManager.INSTANCE.addWork(getFirstThread())
            startThread(this::getFirstThread, (int) Math.max(5, getBlastRadius() / 10));
        }

        //When first thread is completed start floating blocks and ticking down to start second thread
        if (hasCompletedFirstThread && !hasStartedSecondThread)
        {
            //Spawn flying blocks
            if (!hasGeneratedFlyingBlocks)
            {
                hasGeneratedFlyingBlocks = true;
                this.lightBeam.setTargetBeamProgress(0.5f);

                //Edit blocks and queue spawning
                for (BlockPos blockPos : blocksToRemove)
                {
                    //1.12: FlyingBlock.spawnFlyingBlock(world, pos, e -> e.setGravity(e.getGravity() - 0.01f),
                    //e -> world.setBlockToAir(pos)). The helper also gates on the block hardness and the
                    //flying block config, so the pass does not have to re-check those here
                    FlyingBlock.spawnFlyingBlock(this.world(), blockPos,
                            (entity) -> entity.setProjectileGravity(entity.getProjectileGravity() - 0.01f),
                            (e) -> this.world().setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
                }

                blocksToRemove.clear();
            }

            //Delay second thread start
            if (secondThreadTimer-- <= 0)
            {
                this.lightBeam.setTargetBeamProgress(0.8f);
                hasStartedSecondThread = true;
                //1.12: WorkerThreadManager.INSTANCE.addWork(getSecondThread())
                startThread(this::getSecondThread, (int) getBlastRadius());
            }
        }

        if (hasCompetedSecondThread)
        {
            this.lightBeam.setTargetBeamProgress(1f);
            if (!hasEnabledGravityForFlyingBlocks)
            {
                hasEnabledGravityForFlyingBlocks = true;
                //1.12: entity.setGravity(0.5f). Kept as is: 1.12 never filled this set (BlastAntiGravitational
                //keeps its own), so the callback never ran there either
                flyingBlocks.forEach(entity -> entity.setProjectileGravity(0.5f));
            }

            if (!hasPlacedBlocks)
            {
                hasPlacedBlocks = true;
                //1.12 checked the block hardness while collecting the pass, the worker of the port can not
                //read a level (overview.md §6) so the check is done here, see #canMutate(BlockPos)
                mutateBlocks(blocksToRemove.stream().filter(this::canMutate).toList());
                blocksToRemove.clear();
            }
        }
        return hasPlacedBlocks;
    }

    /**
     * Captures the blocks the pass may read and starts the worker on them.
     * <p>
     * 1.12 handed the live world to the worker task and let it read world data off thread. 26.3 forbids that
     * (overview.md §6), so the area is copied by the server thread first and the worker only reads that
     * {@link LevelSnapshot} (S1.09).
     *
     * @param workFactory builds the task for the captured snapshot, see {@link #getFirstThread(LevelSnapshot)}
     * @param radius      block radius the pass reads, which is also the radius captured into the snapshot
     */
    private void startThread(Function<LevelSnapshot, IThreadWork> workFactory, int radius)
    {
        final LevelSnapshot snapshot = LevelSnapshot.capture(level(), BlockPos.containing(getPosition()), radius);
        WorkerThreadManager.INSTANCE.addWork(workFactory.apply(snapshot), snapshot);
    }

    /**
     * 1.12 {@code getFirstThread()}: collects the blocks that become flying blocks.
     *
     * @param snapshot read only view of the world for the worker, the 1.12 task read the world directly
     * @return task to hand to the {@link WorkerThreadManager}
     */
    protected IThreadWork getFirstThread(LevelSnapshot snapshot)
    {
        return new ThreadWorkBlast(this.toString(), (steps, edits) -> collectFlyingBlocks(snapshot, edits), edits ->
                runOnServerThread(() ->
                {
                    blocksToRemove.addAll(edits);
                    hasGeneratedFlyingBlocks = false;
                    hasCompletedFirstThread = true;
                }));
    }

    /**
     * 1.12 {@code getSecondThread()}: collects every block the mutations of the subclass should look at.
     *
     * @param snapshot read only view of the world for the worker, the 1.12 task read the world directly
     * @return task to hand to the {@link WorkerThreadManager}
     */
    protected IThreadWork getSecondThread(LevelSnapshot snapshot)
    {
        return new ThreadWorkBlast(this.toString(), (steps, edits) -> collectBlocksToMutate(snapshot, edits), edits ->
                runOnServerThread(() ->
                {
                    blocksToRemove.addAll(edits);
                    hasPlacedBlocks = false;
                    hasCompetedSecondThread = true;
                }));
    }

    /**
     * Runs the given action on the server thread.
     * <p>
     * A {@link ThreadWorkBlast} completes on the worker thread, so the two completion callbacks above have to
     * hand the collected positions over before touching the state of this blast. 1.12 wrote the lists straight
     * from the worker, which raced the tick reading them; the port uses the server task queue instead, the
     * same hop {@link icbm.classic.content.blast.threaded.BlastThreaded} does.
     */
    private void runOnServerThread(Runnable action)
    {
        if (world() instanceof ServerLevel serverLevel)
        {
            serverLevel.getServer().execute(action);
        }
    }

    /**
     * 1.12 {@code collectFlyingBlocks(Consumer)}: collects the blocks near the center that will float.
     *
     * @param snapshot read only view of the world for the worker
     * @param edits    acceptor for the positions
     * @return false as the pass finishes in a single run, matching the 1.12 task result
     */
    public boolean collectFlyingBlocks(LevelSnapshot snapshot, Consumer<BlockPos> edits)
    {
        collectBlocks(edits, snapshot, (int) Math.max(5, getBlastRadius() / 10));
        return false;
    }

    /**
     * 1.12 {@code collectBlocksToMutate(Consumer)}: collects every block the beam covers.
     *
     * @param snapshot read only view of the world for the worker
     * @param edits    acceptor for the positions
     * @return false as the pass finishes in a single run, matching the 1.12 task result
     */
    public boolean collectBlocksToMutate(LevelSnapshot snapshot, Consumer<BlockPos> edits)
    {
        collectBlocks(edits, snapshot, (int) getBlastRadius());
        return false;
    }

    //TODO make generic and recycle as a generic collector
    protected void collectBlocks(Consumer<BlockPos> edits, LevelSnapshot snapshot, int r)
    {
        final int radiusSQ = r * r;
        final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        BlastHelpers.forEachPosInRadius(r, (x, y, z) ->
        {
            final double distanceSQ = (x * x + y * y + z * z);
            if (distanceSQ <= radiusSQ)
            {
                //Update position
                blockPos.set(location.x() + x, location.y() + y, location.z() + z); //1.12: blockPos.setPos(double, double, double)

                //Get block
                //1.12 read the live world here, the worker thread may only read the snapshot (overview.md §6)
                final BlockState state = snapshot.getBlockState(blockPos);

                //Validate TODO rework to not access blockstates, instead just collect positions
                if (!state.isAir()) //1.12: !block.isAir(state, world, blockPos)
                {
                    edits.accept(blockPos.immutable()); //1.12: blockPos.toImmutable()
                }
            }
        });
    }

    /**
     * True if the position collected by the second pass should still be mutated.
     * <p>
     * 1.12 tested {@code !isAir && getBlockHardness >= 0} while collecting, which kept unbreakable blocks
     * such as bedrock out of the mutation. That test needs a level the worker may not read, so it runs on the
     * server thread right before {@link #mutateBlocks(List)} and also drops positions removed in the meantime.
     *
     * @param pos collected position
     * @return true to hand the position to the subclass
     */
    private boolean canMutate(BlockPos pos)
    {
        final BlockState state = this.world().getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(this.world(), pos) >= 0;
    }

    protected abstract void mutateBlocks(List<BlockPos> edits);

    /**
     * Changes the time of day of the level this blast runs in.
     * <p>
     * 1.12 wrote the day time of the world directly ({@code setWorldTime}). 26.3 drives the day/night cycle
     * with a data driven clock per dimension, so the same tick value is written into the default clock of the
     * level. Dimensions without a day/night cycle (the default clock is empty) are left alone.
     *
     * @param ticks absolute clock ticks, using the 1.12 day time values (18000 = night, 1200 = day)
     */
    protected void setWorldTime(long ticks)
    {
        if (world() instanceof ServerLevel serverLevel)
        {
            serverLevel.dimensionType().defaultClock().ifPresent(clock ->
                    serverLevel.getServer().clockManager().setTotalTicks(clock, ticks));
        }
    }

    @Override
    protected void onBlastCompleted()
    {
        //1.12: ICBMSounds.POWER_DOWN.play(world, location.x(), location.y(), location.z(), 4.0F, 0.8F, true)
        this.world().playSound(null, location.x(), location.y(), location.z(), ModSounds.POWER_DOWN, SoundSource.BLOCKS, 4.0F, 0.8F);

        if (this.lightBeam != null)
        {
            this.lightBeam.startDeathCycle();
            this.lightBeam.setTargetBeamProgress(0f);
        }
    }

    //================================================
    //=====      1.12 material replacements     ======
    //================================================

    /**
     * 1.12 classified blocks with {@code Material}, which 26.3 removed (porting-guide.md §2.1) in favor of
     * block tags and block identity. The helpers below are the replacement table used by the beam blasts:
     * <ul>
     *     <li>{@code Material.WATER} → {@link Blocks#WATER}, 26.3 has one water block for the still and the
     *     flowing state of the fluid.</li>
     *     <li>{@code Material.LEAVES} → {@link BlockTags#LEAVES}.</li>
     *     <li>{@code Material.VINE} → {@link Blocks#VINE} and {@link Blocks#GLOW_LICHEN}, the blocks of that
     *     group.</li>
     *     <li>{@code Material.PLANTS} → the vanilla plant groups ({@link BlockTags#CROPS},
     *     {@link BlockTags#SAPLINGS}, {@link BlockTags#FLOWERS}, {@link BlockTags#CAVE_VINES}) plus the
     *     single plants 26.3 has no tag for.</li>
     *     <li>{@code Material.ROCK} → the vanilla stone groups ({@link BlockTags#BASE_STONE_OVERWORLD},
     *     {@link BlockTags#BASE_STONE_NETHER}, {@link BlockTags#ORES}, {@link BlockTags#STONE_BRICKS}) plus
     *     the stone family blocks that are in neither of them.</li>
     *     <li>{@code Material.SAND} → {@link BlockTags#SAND} plus gravel and soul sand, which 1.12 grouped
     *     with sand and 26.3 moved into other tags.</li>
     *     <li>{@code Material.GROUND} and {@code Material.GRASS} → {@link BlockTags#DIRT} plus the dirt like
     *     blocks that are in no dirt tag.</li>
     * </ul>
     */

    /** 1.12 {@code blockState.getMaterial() == Material.WATER} */
    protected static boolean isWater(BlockState state)
    {
        return state.is(Blocks.WATER);
    }

    /** 1.12 {@code Material.LEAVES || Material.VINE || Material.PLANTS} */
    protected static boolean isVegetation(BlockState state)
    {
        return state.is(BlockTags.LEAVES) //Material.LEAVES
                || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) //Material.VINE
                || state.is(BlockTags.CROPS) //Material.PLANTS
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CAVE_VINES)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.NETHER_WART)
                || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    /** 1.12 {@code blockState.getMaterial() == Material.ROCK} */
    protected static boolean isRock(BlockState state)
    {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.ORES)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN);
    }

    /** 1.12 {@code blockState.getMaterial() == Material.SAND} */
    protected static boolean isSand(BlockState state)
    {
        return state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SOUL_SAND);
    }

    /** 1.12 {@code blockState.getMaterial() == Material.GROUND || blockState.getMaterial() == Material.GRASS} */
    protected static boolean isGroundOrGrass(BlockState state)
    {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.MYCELIUM);
    }
}
