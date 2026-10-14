package icbm.classic.content.blast.threaded;

import icbm.classic.ICBMClassic;
import icbm.classic.config.ConfigDebug;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.BlastHelpers;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.reg.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Antimatter explosive, clears a large sphere of blocks (including the unbreakable ones when configured) and
 * damages entities twice, at the start and at the end of the blast.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>The 1.12 worker only collected positions, the world work (sort, fluid/falling block pull out and the
 *     two step block removal) ran on the server thread through {@code addScheduledTask}. The port keeps that
 *     split: {@link #doRun(int, Consumer)} collects on the worker, {@link #onWorkerResultsReady(List)} runs on
 *     the server thread.</li>
 *     <li>{@code Block#setBlockState(pos, state, flags)} → {@code Level#setBlock(pos, state, flags)} with the
 *     same flags ({@code 2} → {@code Block.UPDATE_CLIENTS}, {@code 3} → {@code Block.UPDATE_ALL}).</li>
 *     <li>{@code state.getBlockHardness(world, pos)} → {@code BlockState#getDestroySpeed(level, pos)} (this
 *     code runs on the server thread, so the level can be passed), {@code Entity#setDead()} →
 *     {@code Entity#discard()}, {@code world.rand} → {@link #random} (the worker can not read the level
 *     random, the server thread seeds it in {@link #setupBlast()}) and {@code MathHelper} helpers are not
 *     used here.</li>
 *     <li>{@code state.getMaterial() == Material.WATER} → {@code state.is(Blocks.WATER)} (26.3 has one water
 *     block for the still and the flowing state), {@code BlockFalling} →
 *     {@link FallingBlock}, {@code EnumParticleTypes} is not used here.</li>
 *     <li>{@code ICBMSounds.ANTIMATTER.play(world, x, y, z, 7F, pitch, true)} → {@code Level#playSound} with
 *     {@code ModSounds.ANTIMATTER}: the 1.12 helper ignored the distance delay on the server and used the
 *     block sound category.</li>
 *     <li>The redmatter check refers to {@link EntityRedmatter}, the entity of the {@code icbmclassic:redmatter}
 *     explosive (same step, registered by {@code ModEntities.REDMATTER}).</li>
 * </ul>
 */
public class BlastAntimatter extends BlastThreaded
{
    private final BlockState replaceState = Blocks.AIR.defaultBlockState();

    /**
     * Random used by the worker, 1.12 {@code world().rand}. The level random belongs to the server thread and
     * can not be read off thread, so the blast owns one seeded from it in {@link #setupBlast()}.
     */
    private RandomSource random = RandomSource.create();

    @Override
    public boolean setupBlast()
    {
        super.setupBlast();
        //1.12: world().rand, seeded on the server thread so the worker has its own source (the worker must not
        //read level.getRandom())
        random = RandomSource.create(world().getRandom().nextLong());
        //1.12: ICBMSounds.ANTIMATTER.play(world, x, y, z, 7F, pitch, true)
        world().playSound(null, this.location.x(), this.location.y(), this.location.z(), ModSounds.ANTIMATTER, SoundSource.BLOCKS, 7F, (float) (this.world().getRandom().nextFloat() * 0.1 + 0.9F));
        return this.doDamageEntities(this.getBlastRadius() * 2, ConfigBlast.antimatter.damage); //TODO config for radius
    }

    @Override
    public void destroyBlock(BlockPos blockPos)
    {
        final BlockState blockState = world().getBlockState(blockPos);
        if (!blockState.isAir()) //1.12: !blockState.getBlock().isAir(blockState, world, blockPos)
        {
            //1.12: blockState.getBlockHardness(world, blockPos) >= 0 || ConfigBlast.antimatter.damageUnbreakable
            //26.3 renames the hardness read to BlockState#getDestroySpeed(BlockGetter, BlockPos); this runs on
            //the server thread (BlockEditQueue#tick), so the level is available.
            if (blockState.getDestroySpeed(world(), blockPos) >= 0 || ConfigBlast.antimatter.damageUnbreakable)
            {
                //1.12: world.setBlockState(blockPos, replaceState, ConfigBlast.BLAST_DO_BLOCKUPDATES ? 3 : 2)
                world().setBlock(blockPos, replaceState, ConfigBlast.BLAST_DO_BLOCKUPDATES ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public boolean doRun(int loops, Consumer<BlockPos> edits)
    {
        long time = System.currentTimeMillis();
        BlastHelpers.forEachPosInRadius(this.getBlastRadius(), (x, y, z) -> {
            if (shouldEditPos(x, y, z))
            {
                edits.accept(new BlockPos(xi() + x, yi() + y, zi() + z));
            }
        });
        time = System.currentTimeMillis() - time;
        if(ConfigDebug.DEBUG_EXPLOSIVES) {
            ICBMClassic.logger().info("Antimatter: thread calculations ran in " + time + "ms");
        }
        return false;
    }

    @Override
    protected void onWorkerResultsReady(List<BlockPos> edits)
    {
        //TODO have threads take a copy of chunks for use before running
        //      OR replace thread system with an entity that ticks over time to collect data
        //1.12 opened with `world instanceof WorldServer`, this callback only runs for a server level
        //(BlastThreaded#onWorkerThreadComplete) and the sort it also did there is done by that method.

        long time = System.currentTimeMillis();

        //Pull out fluids and falling blocks to prevent lag issues
        final List<BlockPos> removeFirst = edits.stream()
                .filter(blockPos -> world().isLoaded(blockPos)) //1.12: world.isBlockLoaded(blockPos)
                .filter(this::isFluid)
                .collect(Collectors.toList());

        time = System.currentTimeMillis() - time;
        if(ConfigDebug.DEBUG_EXPLOSIVES) {
            ICBMClassic.logger().info("Antimatter: filtering first edits ran in " + time + "ms");
        }

        //Schedule edits to run in the world
        scheduledTask(removeFirst, edits);
    }

    private void scheduledTask(List<BlockPos> removeFirst, List<BlockPos> edits) {

        long time = System.currentTimeMillis();

        //Remove any blocks that could cause issues when queued
        //1.12: world.setBlockState(blockPos, replaceState, 2)
        removeFirst.forEach(blockPos -> world().setBlock(blockPos, replaceState, Block.UPDATE_CLIENTS));

        time = System.currentTimeMillis() - time;
        if(ConfigDebug.DEBUG_EXPLOSIVES) {
            ICBMClassic.logger().info("Antimatter: remove#first blocks ran in " + time + "ms,  edits:" + removeFirst.size());
        }

        time = System.currentTimeMillis();

        //Queue edits, even the ones from the previous
        //1.12: BlockEditHandler.queue(world, edits, this::destroyBlock), the ported BlockEditQueue replaces it
        //and runs onPostThreadJoinWorld() once the last edit is in the world
        super.onWorkerResultsReady(edits);

        time = System.currentTimeMillis() - time;
        if(ConfigDebug.DEBUG_EXPLOSIVES) {
            ICBMClassic.logger().info("Antimatter: remove#second ran in " + time + "ms,  edits:" + edits.size());
        }

        //Notify blast we have entered world again
        //1.12 called onPostThreadJoinWorld() here, right after queueing. The port hands it to the queue as
        //the completion callback, which is how BlastThreaded finishes every threaded blast.
    }

    protected boolean isFluid(BlockPos blockPos)
    {
        BlockState state = world().getBlockState(blockPos);
        //1.12: state.getMaterial() == Material.WATER || state.getBlock() instanceof BlockFalling
        return state.is(Blocks.WATER) || state.getBlock() instanceof FallingBlock;
    }

    protected boolean shouldEditPos(int x, int y, int z)
    {
        final double distSQ = x * x + y * y + z * z;
        final double blastSQ = this.getBlastRadius() * this.getBlastRadius();

        final int featherEdge = (int) Math.floor(blastSQ * 0.05f);
        final int delta = (int) Math.floor(blastSQ - distSQ);

        if (delta < featherEdge)
        {
            final double p2 = 1 - (delta / (double) featherEdge);
            return random.nextFloat() > p2; //1.12: world().rand.nextFloat()
        }
        return true;
    }

    @Override
    public void onBlastCompleted()
    {
        super.onBlastCompleted();
        this.doDamageEntities(this.getBlastRadius() * 2, ConfigBlast.antimatter.damage); //TODO config for radius
    }

    @Override
    protected boolean doDamageEntities(float radius, float power, boolean destroyItem) {

        if (!ConfigBlast.antimatter.damageOnRedmatterKill)
        {
            final List<Entity> allEntities = getEntities(radius * 2); //TODO config for radius
            final long killed = allEntities.stream().filter(e -> e instanceof EntityRedmatter).peek(this::onDamageEntity).count();
            if(killed > 0) {
                return false;
            }
            return this.doDamageEntities(allEntities, radius, power, destroyItem);
        }
        return super.doDamageEntities(radius, power, destroyItem);
    }

    @Override
    protected boolean onDamageEntity(Entity entity)
    {
        if (entity instanceof EntityRedmatter)
        {
            //TODO implement a proper event instead of BlastCancel
            entity.discard(); //1.12: entity.setDead()
            return true;
        }

        return false;
    }
}
