package icbm.classic.content.blast.threaded;

import icbm.classic.ICBMClassic;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.helpers.BlastBlockHelpers;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.transform.vector.Location;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * Thermobaric explosive, the smaller sibling of {@link BlastNuclear}: same worker based raycast and cloud,
 * but only the block destruction and the entity damage (no radioactive block swaps or mutation).
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>The 1.12 thread read the live level. The worker now reads the level snapshot captured by
 *     {@link BlastThreaded#doExplode(int)} into {@link BlastThreaded#snapshot}; {@link #doRun(int, Consumer)}
 *     may only read that inherited field and never the world, including its random (1.12 {@code world.rand} →
 *     the {@link #random} seeded from the level random on the server thread in {@link #setupBlast()}).</li>
 *     <li>{@code world.isBlockLoaded(pos)} is dropped: the snapshot reports unloaded chunks as air, which
 *     the air check below already skips (the legacy check also skipped the trace step and could spin).</li>
 *     <li>{@code state.getBlockHardness(world, pos)} → {@code BlockBehaviour#defaultDestroyTime()}, the same
 *     value the state caches ({@code state.getDestroySpeed(level, pos)} needs a level the worker can not
 *     touch). {@code block.getExplosionResistance(world, pos, entity, explosion)} →
 *     {@code Block#getExplosionResistance()} and {@code state.getMaterial().isLiquid()} →
 *     {@link BlastBlockHelpers#isFluid(BlockState)}.</li>
 *     <li>{@code MathHelper.sqrt} → {@link Mth#sqrt(float)}, {@code EnumParticleTypes.SMOKE_NORMAL} →
 *     {@code ParticleTypes.SMOKE} sent through {@code ServerLevel#sendParticles} and the 1.12
 *     {@code ICBMSounds.EXPLOSION.play(...)} → {@code Level#playSound} with {@code ModSounds.EXPLOSION}.</li>
 * </ul>
 */
public class BlastThermobaric extends BlastThreaded {
    private double energy;

    /**
     * Random used by the worker, 1.12 {@code world.rand}. The level random belongs to the server thread and
     * can not be read off thread, so the blast owns one seeded from it in {@link #setupBlast()}.
     */
    private RandomSource random = RandomSource.create();

    public BlastThermobaric() {
    }

    public BlastThermobaric setEnergy(double energy) {
        this.energy = energy;
        return this;
    }

    @Override
    public boolean doRun(int loops, Consumer<BlockPos> edits) {
        //How many steps to go per rotation
        final int steps = (int) Math.ceil(Math.PI * this.getBlastRadius());

        double x;
        double y;
        double z;

        double dx;
        double dy;
        double dz;

        double power;

        double yaw;
        double pitch;

        final int lineDensityScale = 2;
        for (int yawSlices = 0; yawSlices < lineDensityScale * steps; yawSlices++) {
            for (int pitchSlice = 0; pitchSlice < steps; pitchSlice++) {
                //Calculate power
                power = this.energy - (this.energy * random.nextFloat() / 2); //1.12: world.rand.nextFloat()

                //Get angles for rotation steps
                yaw = (Math.PI / steps) * yawSlices;
                pitch = (Math.PI / steps) * pitchSlice;

                //Figure out vector to move for trace (cut in half to improve trace skipping blocks)
                dx = sin(pitch) * cos(yaw) * 0.5;
                dy = cos(pitch) * 0.5;
                dz = sin(pitch) * sin(yaw) * 0.5;

                //Reset position to current
                x = this.x();
                y = this.y();
                z = this.z();

                BlockPos prevPos = null;

                //Trace from start to end
                while (location.distance(x, y, z) <= this.getBlastRadius() && power > 0) //TODO replace distance check with SQ version
                {
                    //Consume power per loop
                    power -= 0.3F * 0.75F * 5; //TODO why the magic numbers?

                    //Convert double position to int position as block pos
                    //1.12: new BlockPos(Math.floor(x), Math.floor(y), Math.floor(z))
                    final BlockPos blockPos = BlockPos.containing(x, y, z);

                    //Only do action one time per block (not a perfect solution, but solves double hit on the same block in the same line)
                    if (!Objects.equals(prevPos, blockPos)) {
                        //1.12: if (!world.isBlockLoaded(blockPos)) continue; //TODO: find better fix for non main thread loading
                        //The snapshot (BlastThreaded#snapshot, LevelSnapshot) reports chunks that were not
                        //loaded as air, which the air check below skips, so no load check is needed.

                        //Get block state from the snapshot instead of the world
                        final BlockState state = this.snapshot.getBlockState(blockPos);

                        //Ignore air blocks
                        if (!state.isAir()) { //1.12: block.isAir(state, world, blockPos)
                            //Consume power based on block
                            power -= getResistance(blockPos, state);

                            //Only break block that can be broken && If we still have power. Then break the block
                            if (state.getBlock().defaultDestroyTime() >= 0 && power > 0f) { //1.12: state.getBlockHardness(world, blockPos) >= 0
                                edits.accept(blockPos);
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
        return false;
    }

    public float getResistance(BlockPos pos, BlockState state) {
        if (BlastBlockHelpers.isFluid(state)) { //1.12: state.getMaterial().isLiquid()
            return 0.25f;
        } else {
            //1.12: block.getExplosionResistance(world, pos, getExplosivePlacedBy(), this), which 26.3
            //shortened to Block#getExplosionResistance() (the default is the same blockResistance / 5)
            return state.getBlock().getExplosionResistance();
        }
    }


    @Override
    public boolean setupBlast() {
        super.setupBlast();
        if (this.world() != null) {
            //1.12: world.rand, seeded on the server thread so the worker has its own source (the worker must
            //not read level.getRandom())
            random = RandomSource.create(world().getRandom().nextLong());
            if (world() instanceof ServerLevel serverLevel) {
                // Spawn nuclear cloud.
                for (int y = 0; y < 26; y++) {
                    int r = 4;

                    if (y < 8) {
                        r = Math.max(Math.min((8 - y) * 2, 10), 4);
                    } else if (y > 15) {
                        r = Math.max(Math.min((y - 15) * 2, 15), 5);
                    }

                    for (int x = -r; x < r; x++) {
                        for (int z = -r; z < r; z++) {
                            double distance = Mth.sqrt(x * x + z * z); //1.12: MathHelper.sqrt(double)

                            if (r > distance && r - 3 < distance) {
                                Location spawnPosition = location.add(new Pos(x * 2, (y - 2) * 2, z * 2));
                                float xDiff = (float) (spawnPosition.x() - location.x());
                                float zDiff = (float) (spawnPosition.z() - location.z());
                                //1.12: world.spawnParticle(SMOKE_NORMAL, x, y, z, xSpeed, ySpeed, zSpeed) sent a
                                //single particle at the exact position with the given per axis speed. 26.3
                                //ServerLevel#sendParticles with a count of 0 does the same when the speed is
                                //given per axis (ClientPacketListener#handleParticleEvent), so the spread (the
                                //three distances) stays 1 and the 1.12 speeds are passed as the axis speeds.
                                serverLevel.sendParticles(ParticleTypes.SMOKE, spawnPosition.x(), spawnPosition.y(), spawnPosition.z(), 0,
                                    1.0D, 1.0D, 1.0D,
                                    xDiff * 0.3 * world().getRandom().nextFloat(), -world().getRandom().nextFloat(), zDiff * 0.3 * world().getRandom().nextFloat()); //(float) (distance / this.getRadius()) * oldWorld().rand.nextFloat(), 0, //0, 8F, 1.2F);
                            }
                        }
                    }
                }
            }

            this.doDamageEntities((float)ConfigBlast.thermobaric.entityDamageScale, (float) (this.energy * ConfigBlast.thermobaric.entityDamageMultiplier));

            //1.12: ICBMSounds.EXPLOSION.play(world, x, y, z, 7.0F, pitch, true), the server side of the helper
            //ignored the distance delay and called Level#playSound (SoundCategory.BLOCKS)
            world().playSound(null, this.location.x(), this.location.y(), this.location.z(), ModSounds.EXPLOSION, SoundSource.BLOCKS, 7.0F, (1.0F + (this.world().getRandom().nextFloat() - this.world().getRandom().nextFloat()) * 0.2F) * 0.7F);
        }

        return true;
    }

    @Override
    public boolean doExplode(int callCount) {
        super.doExplode(callCount);
        int r = this.callCount;

        //1.12 sent this ring from the client copy of the blast (world().isRemote). Client side blast objects
        //are gone in the port (all blast effects are server driven, see EntityExplosion), so the ring is sent
        //by the server instead. Note the 1.12 value: callCount only advances for ticking blasts
        //(Blast#onBlastTick), this blast is threaded so r stays 0 and the loop body never runs, exactly like
        //it did in 1.12 where this branch was only reachable from a client blast.
        if (world() instanceof ServerLevel serverLevel) {
            for (int x = -r; x < r; x++) {
                for (int z = -r; z < r; z++) {
                    double distance = Mth.sqrt(x * x + z * z); //1.12: MathHelper.sqrt(double)

                    if (distance < r && distance > r - 1) {
                        Location targetPosition = this.location.add(new Pos(x, 0, z));

                        if (world().getRandom().nextFloat() < Math.max(0.001 * r, 0.05)) {
                            //1.12: world.spawnParticle(SMOKE_NORMAL, x, y, z, 0, 0, 0), a particle standing
                            //still at the position (count 0 keeps the position exact and the speed 0)
                            serverLevel.sendParticles(ParticleTypes.SMOKE, targetPosition.x(), targetPosition.y(), targetPosition.z(), 0,
                                1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D); //5F, 1F);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onBlastCompleted() {
        super.onBlastCompleted();
        if (world() != null && !world().isClientSide()) { //1.12: !world().isRemote
            try {
                //Attack entities with concussion wave
                this.doDamageEntities((float)ConfigBlast.thermobaric.entityDamageScale, (float) (this.energy * ConfigBlast.thermobaric.entityDamageMultiplier));

                //Play audio
                //1.12: ICBMSounds.EXPLOSION.play(world, x, y, z, 10.0F, pitch, true)
                world().playSound(null, this.location.x(), this.location.y(), this.location.z(), ModSounds.EXPLOSION, SoundSource.BLOCKS, 10.0F, (1.0F + (this.world().getRandom().nextFloat() - this.world().getRandom().nextFloat()) * 0.2F) * 0.7F);

            } catch (Exception e) {
                String msg = String.format("BlastThermobaric#doPostExplode() ->  Unexpected error while running post detonation code " +
                        "\nWorld = %s " +
                        "\nThread = %s" +
                        "\nSize = %s" +
                        "\nPos = %s",
                    world, getWorkerTaskOrNull(), size, location); //1.12: getThread()
                ICBMClassic.logger().error(msg, e);
            }
        }
    }
}
