package icbm.classic.content.blast;

import com.builtbroken.jlib.lang.StringHelpers;
import icbm.classic.ICBMClassic;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.config.ConfigDebug;
import icbm.classic.content.blast.thread.BasicResistanceCallBack;
import icbm.classic.content.blast.thread.IThreadCallBack;
import icbm.classic.content.blast.threaded.BlastThreaded;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.explosive.ThreadWorkBlast;
import icbm.classic.lib.thread.IThreadWork;
import icbm.classic.lib.thread.LevelSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sonic wave blast, ray traces the blocks around the center and then consumes the result over the ticks of an
 * expanding radius, pushing entities away while it runs.
 * <p>
 * Port notes (porting-guide.md §2, docs/architecture/overview.md §6):
 * <ul>
 *     <li>1.12 started one {@code ThreadLargeExplosion} in {@code firstTick()} and read the live world from it.
 *     The port uses {@link BlastThreaded}, which starts the worker on the first tick and never lets it touch the
 *     level: the worker reads the {@link LevelSnapshot} instead (see {@link #doRun(int, Consumer)}).</li>
 *     <li>1.12 {@code world.rand} inside the worker → a local {@code RandomSource}, the level random is owned by
 *     the server thread.</li>
 *     <li>1.12 {@code blockState.getBlockHardness(world, pos)} off thread → the stored destroy speed of the
 *     snapshot state, the 26.3 lookup returns the stored value and does not read the level.</li>
 *     <li>1.12 {@code block == BlockReg.blockExplosive} → {@code block instanceof ExplosiveBlock}, ADR 0002
 *     flattens the single explosive block into one block per explosive.</li>
 *     <li>1.12 {@code world.setBlockToAir(pos)} → {@code Level#setBlock(pos, air, Block.UPDATE_ALL)} (flag 3)
 *     and {@code entity.motionX/Y/Z} writes → {@code Entity#push(double, double, double)}, see
 *     {@link BlastTNT#pushEntities(float, float, PushType)}.</li>
 *     <li>{@code ICBMSounds.SONICWAVE.play(...)} → {@code Level#playSound(...)} with {@link ModSounds#SONICWAVE}.</li>
 * </ul>
 */
public class BlastSonic extends BlastThreaded implements IBlastTickable
{
    private boolean hasShockWave = false;

    /** 1.12 {@code new BasicResistanceCallBack(blast)}: used by the ray pass to price each block it hits */
    private final IThreadCallBack callBack = new BasicResistanceCallBack();

    /**
     * Read only world view for the worker. 1.12 gave the live world to the 1.12 thread class, 26.3 hands the
     * snapshot to {@link IThreadWork#doRun(int, LevelSnapshot)} only, so it is kept for the collection pass.
     * Only ever touched by the worker thread that is running {@link #getWorkerTask()}.
     */
    private LevelSnapshot snapshot;

    public BlastSonic()
    {

    }

    public Blast setShockWave()
    {
        this.hasShockWave = true;
        return this;
    }

    /**
     * Wraps the ported worker task so the snapshot can be read by {@link #doRun(int, Consumer)}.
     */
    @Override
    protected IThreadWork getWorkerTask()
    {
        return new ThreadWorkBlast(toString(), this::doRun, this::onWorkerThreadComplete)
        {
            @Override
            public boolean doRun(int steps, LevelSnapshot snapshot)
            {
                BlastSonic.this.snapshot = snapshot;
                return super.doRun(steps, snapshot);
            }
        };
    }

    /**
     * Ray trace of the 1.12 {@code ThreadLargeExplosion#doRun(World, Location)}: walks a sphere of rays out from
     * the center and keeps every block the ray still has power to break.
     * <p>
     * Runs on a worker thread, only {@link #snapshot} may be read.
     *
     * @param loops - current loop count
     * @param edits - acceptor for the positions the ray broke
     * @return false, the whole sphere is traced in one pass like the 1.12 thread did
     */
    @Override
    public boolean doRun(int loops, Consumer<BlockPos> edits)
    {
        final LevelSnapshot worldSnapshot = this.snapshot;
        long time = System.nanoTime();

        //1.12: new ThreadLargeExplosion(this, (int) this.getBlastRadius(), getBlastRadius() * 2, this.exploder)
        final int radius = (int) this.getBlastRadius();
        final float energy = this.getBlastRadius() * 2;

        //How many steps to go per rotation
        final int steps = (int) Math.ceil(Math.PI * radius);

        //1.12: world.rand.nextFloat() for the power below. A worker must not read the level, so the pass uses
        //      its own random source (this only shifts the random energy jitter of the rays).
        final RandomSource random = RandomSource.create();

        double x;
        double y;
        double z;

        double dx;
        double dy;
        double dz;

        double power;

        double yaw;
        double pitch;

        //1.12: the loop was also gated on the thread kill flag, the ported worker stops through its own stop flag
        for (int phi_n = 0; phi_n < 2 * steps; phi_n++)
        {
            for (int theta_n = 0; theta_n < steps; theta_n++)
            {
                //Calculate power
                power = energy - (energy * random.nextFloat() / 2);

                //Get angles for rotation steps
                yaw = Math.PI * 2 / steps * phi_n;
                pitch = Math.PI / steps * theta_n;

                //Figure out vector to move for trace (cut in half to improve trace skipping blocks)
                dx = Math.sin(pitch) * Math.cos(yaw) * 0.5;
                dy = Math.cos(pitch) * 0.5;
                dz = Math.sin(pitch) * Math.sin(yaw) * 0.5;

                //Reset position to current
                x = location.x();
                y = location.y();
                z = location.z();

                BlockPos prevPos = null;

                //Trace from start to end
                while (location.distance(x, y, z) <= radius && power > 0)
                {
                    //Consume power per loop
                    power -= 0.3F * 0.75F * 5; //TODO why the magic numbers?

                    //Convert double position to int position as block pos
                    final BlockPos blockPos = BlockPos.containing(x, y, z);

                    //Only do action one time per block (not a perfect solution, but solves double hit on the same block in the same line)
                    //1.12 compared the block pos references here, kept as is so the results stay the same
                    if (prevPos != blockPos)
                    {
                        //1.12: position.world().isBlockLoaded(blockPos). The snapshot answers air for positions
                        //      it did not capture, which the two checks below skip like an unloaded chunk would

                        //Get block state and block from position
                        final BlockState state = worldSnapshot.getBlockState(blockPos);
                        final Block block = state.getBlock();

                        //Ignore air blocks && Only break block that can be broken
                        //1.12: !block.isAir(state, world, blockPos) && state.getBlockHardness(world, blockPos) >= 0
                        //26.3: BlockState#getDestroySpeed returns the stored speed without reading the level, so
                        //      the worker can filter unbreakable blocks without the level
                        if (!state.isAir() && state.getDestroySpeed(null, blockPos) >= 0)
                        {
                            //Consume power based on block
                            power -= this.callBack.getResistance(null, location, blockPos, this.exploder, block);

                            //If we still have power, break the block
                            if (power > 0f)
                            {
                                edits.accept(blockPos); //1.12: this.blast.addThreadResult(blockPos)
                            }
                        }
                    }

                    //Note previous block
                    prevPos = blockPos;

                    //Move forward
                    x += dx;
                    y += dy;
                    z += dz;
                }
            }
        }

        if (ConfigDebug.DEBUG_THREADS)
        {
            time = System.nanoTime() - time;
            String timeString = StringHelpers.formatNanoTime(time);
            String msg = "BlastSonic#doRun() -> Completed calculation in [%s] \nBlast: %s\nRadius: %s\nEnergy: %s";
            ICBMClassic.logger().info(String.format(msg, timeString, this, radius, energy));
        }
        return false;
    }

    public void firstTick()
    {
        //TODO remove thread
        //1.12: createAndStartThread(new ThreadLargeExplosion(this, (int) this.getBlastRadius(), getBlastRadius() * 2, this.exploder))
        //26.3: the ported base starts the worker on the first tick, -1 marks a call that is not a ticking call
        super.doExplode(-1);

        //1.12: ICBMSounds.SONICWAVE.play(world, location.x(), location.y(), location.z(), 4.0F, pitch, true)
        this.world().playSound(null, location.x(), location.y(), location.z(), ModSounds.SONICWAVE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.world().getRandom().nextFloat() - this.world().getRandom().nextFloat()) * 0.2F) * 0.7F);
    }

    /**
     * This blast consumes the worker result itself, one batch every 4 ticks, see {@link #doExplode(int)}, so the
     * base class must not apply the positions through the block edit queue (that would remove every collected
     * block at once and mark the blast finished).
     *
     * @param edits sorted positions the worker collected
     */
    @Override
    protected void onWorkerResultsReady(List<BlockPos> edits)
    {
        this.threadResults = edits;
        this.threadComplete = true;
    }

    @Override
    public boolean doExplode(int callCount) //TODO Rewrite this entire method
    {
        if (callCount <= 0)
        {
            firstTick();
        }
        final int radius = Math.max(1, this.callCount); //TODO scale off of size & callcout
        if (callCount % 4 == 0)
        {
            if (isThreadCompleted())
            {
                if (!getThreadResults().isEmpty())
                {
                    final Iterator<BlockPos> it = getThreadResults().iterator();
                    //TODO rewrite this to not be threaded, instead iterate out each tick and remove X number of blocks

                    while (it.hasNext())
                    {
                        final BlockPos targetPosition = it.next();

                        final double distance = location.distance(targetPosition);

                        //Only act on blocks inside the current radius TODO scale radius separate from ticks so we can control block creation
                        if (distance <= radius) //TODO consider making less round?
                        {
                            //Remove
                            it.remove();

                            //Get data
                            final BlockState blockState = world().getBlockState(targetPosition);
                            final Block block = blockState.getBlock();

                            //Only act on movable blocks
                            //1.12: !block.isAir(blockState, world, targetPosition) && blockState.getBlockHardness(world, targetPosition) >= 0
                            if (!blockState.isAir() && blockState.getDestroySpeed(world(), targetPosition) >= 0)
                            {
                                //Trigger explosions
                                if (block instanceof ExplosiveBlock) //1.12: block == BlockReg.blockExplosive
                                {
                                    //TODO(S3.09) trigger the primed explosive once ExplosiveBlockEntity exists
                                }

                                if (this.world().getRandom().nextFloat() < 0.1) {  //TODO add config for chance, increase chance if we fail to spawn a block
                                    FlyingBlock.spawnFlyingBlock(world(), targetPosition, null, null);
                                }
                                //1.12: this.world().setBlockToAir(targetPosition)
                                this.world().setBlock(targetPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                }
                else
                {
                    this.endBlast();
                    if (ConfigDebug.DEBUG_THREADS)
                    {
                        String msg = String.format("BlastSonic#doPostExplode() -> Thread failed to find blocks to edit. Either thread failed or no valid blocks were found in range." +
                                        "\nWorld = %s " +
                                        "\nThread = %s" +
                                        "\nSize = %s" +
                                        "\nPos = %s",
                                world, getWorkerTaskOrNull(), size, location);
                        ICBMClassic.logger().error(msg);
                    }
                }
            }
        }

        final int entityEffectRadius = 2 * this.callCount; //TODO scale to radius
        final AABB bounds = new AABB(
                location.x() - entityEffectRadius, location.y() - entityEffectRadius, location.z() - entityEffectRadius,
                location.x() + entityEffectRadius, location.y() + entityEffectRadius, location.z() + entityEffectRadius);

        //1.12: this.world().getEntitiesWithinAABB(Entity.class, bounds)
        final List<Entity> allEntities = this.world().getEntitiesOfClass(Entity.class, bounds);
        for (Entity entity : allEntities)
        {
            //1.12: !(entity instanceof the 1.12 player class) || !((the 1.12 player class) entity).isCreative()
            if (!(entity instanceof Player player) || !player.isCreative())
            {
                //Get difference
                double xDelta = entity.getX() - location.x();
                double yDelta = entity.getY() - location.y();
                double zDelta = entity.getZ() - location.z();

                //Normalize
                float distance = Mth.sqrt((float) (xDelta * xDelta + yDelta * yDelta + zDelta * zDelta));
                xDelta = xDelta / (double) distance;
                yDelta = yDelta / (double) distance;
                zDelta = zDelta / (double) distance;

                //Scale
                final double scale = Math.max(0, (1 - (distance / getBlastRadius()))) * 3;
                xDelta *= scale;
                yDelta *= scale;
                zDelta *= scale;

                //1.12: entity.motionX += xDelta * this.world().rand.nextFloat() * 0.2
                //      entity.motionY += Math.abs(yDelta * this.world().rand.nextFloat()) * 1
                //      entity.motionZ += zDelta * this.world().rand.nextFloat() * 0.2
                //26.3: Entity#push is the add motion + sync pair (1.12 isAirBorne)
                entity.push(xDelta * this.world().getRandom().nextFloat() * 0.2,
                        Math.abs(yDelta * this.world().getRandom().nextFloat()) * 1,
                        zDelta * this.world().getRandom().nextFloat() * 0.2);
            }
        }

        return this.callCount > this.getBlastRadius();
    }
}
