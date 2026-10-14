package icbm.classic.content.blast;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.transform.region.Cube;
import icbm.classic.lib.transform.vector.Pos;
import icbm.classic.network.BlockExplosionFxPayload;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Custom version of Minecraft TNT style blast for use
 * in basic explosives.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE/EXPLOSION_LARGE)} →
 *     {@code ServerLevel#sendParticles} with {@code ParticleTypes.EXPLOSION_EMITTER}/{@code ParticleTypes.EXPLOSION},
 *     the same pair the 26.3 vanilla explosion uses (the 1.12 trailing particle argument is gone).</li>
 *     <li>{@code blockState.getMaterial() != Material.AIR} → {@code !BlockState#isAir()} and
 *     {@code block.getExplosionResistance(world, pos, entity, explosion)} → {@code Block#getExplosionResistance()}
 *     (26.3 takes no level/position/source).</li>
 *     <li>{@code block.canDropFromExplosion(this)} + {@code block.dropBlockAsItemWithChance(...)} +
 *     {@code block.onBlockExploded(...)} → {@code BlockState#onExplosionHit(...)}; the 1/blastRadius drop chance
 *     lives in the loot table, see {@link #getBlockInteraction()}.</li>
 *     <li>{@code PacketSpawnBlockExplosion.sendToAllClients(world, ...)} → {@link BlockExplosionFxPayload}.</li>
 *     <li>{@code entity.addVelocity(...)} → {@code Entity#push(double, double, double)}, which is the 26.3
 *     "add to motion and sync it" pair (1.12 {@code isAirBorne}).</li>
 *     <li>{@code MathHelper.floor} → {@code Mth.floor} and {@code world.rand} → {@code Level#getRandom()}.</li>
 * </ul>
 */
public class BlastTNT extends Blast
{
    private PushType pushType = PushType.NO_PUSH;
    /** Do destroy items */
    private boolean destroyItem = false;

    /** Amount of damage to do to an entity */
    @Setter @Accessors(chain = true)
    public float damageToEntities = 10F;

    /** Number of rays (or steps) to use for blast per axis (x, y, z) */
    public int raysPerAxis = 16;

    public BlastTNT()
    {
        super();
    }

    /**
     * Sets push type, defaults to damage entity
     *
     * @param type
     * @return this
     */
    public BlastTNT setPushType(PushType type)
    {
        this.pushType = type;
        return this;
    }

    /**
     * Sets items to be destroyed
     *
     * @return this
     */
    public BlastTNT setDestroyItems()
    {
        this.destroyItem = true;
        return this;
    }

    @Override
    protected void clientRunBlast()
    {

    }

    @Override
    public boolean doExplode(int callCount)
    {
        calculateDamage(); //TODO add listener(s) to control block break and placement

        if (world instanceof ServerLevel serverLevel) //1.12: world instanceof WorldServer
        {
            if (this.size >= 2.0F)
            {
                //1.12: worldServer.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, false, this.x, this.y, this.z, 1, 1.0D, 0.0D, 0.0D, 1)
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1, 1.0D, 0.0D, 0.0D, 0.0D);
            }
            else
            {
                //1.12: worldServer.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, false, this.x, this.y, this.z, 1, 1.0D, 0.0D, 0.0D, 1)
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1, 1.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        //1.12: SoundEvents.ENTITY_GENERIC_EXPLODE + world().rand
        this.world().playSound(null, this.location.x(), this.location.y(), this.location.z(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.world().getRandom().nextFloat() - this.world().getRandom().nextFloat()) * 0.2F) * 0.7F);

        //TODO collect entities before applying effects, this way event can override
        if (this.pushType == PushType.NO_PUSH)
        {
            this.doDamageEntities(this.getBlastRadius(), damageToEntities, this.destroyItem);
        }
        else
        {
            this.pushEntities(12, this.getBlastRadius() * 4, this.pushType);
        }

        // 1.12: MinecraftForge.EVENT_BUS.post(new ExplosionEvent.Detonate(world, this, new ArrayList())) which
        // let listeners block the entity damage before it ran. Forge events are gone in the port and the ported
        // blast API has no detonate event yet, the matching Forge explosion start event was dropped the same way
        // in Blast#doAction. The entity damage above therefore always runs now.

        //Destroy blocks
        doDestroyBlocks(); //TODO add listener(s) to control block break and placement
        return true;
    }

    /**
     * Pre-calculates all blocks to be destroyed
     */
    protected void calculateDamage() //TODO thread
    {
        //TODO fix, alg seems to be making non-semetric shapes in harder blocks.
        //      TnT vanilla will do 3x3
        //      Condensed will do a 2x4 shaft with a few offshoots but doesn't scale correctly
        if (this.world instanceof ServerLevel serverLevel) //1.12: !this.world().isRemote
        {
            for (int xs = 0; xs < this.raysPerAxis; ++xs)
            {
                for (int ys = 0; ys < this.raysPerAxis; ++ys)
                {
                    for (int zs = 0; zs < this.raysPerAxis; ++zs)
                    {
                        //Only run on edges of the cube
                        if (xs == 0 || xs == this.raysPerAxis - 1 || ys == 0 || ys == this.raysPerAxis - 1 || zs == 0 || zs == this.raysPerAxis - 1)
                        {
                            //Delta distance
                            double xStep = xs / (this.raysPerAxis - 1.0F) * 2.0F - 1.0F;
                            double yStep = ys / (this.raysPerAxis - 1.0F) * 2.0F - 1.0F;
                            double zStep = zs / (this.raysPerAxis - 1.0F) * 2.0F - 1.0F;

                            //Distance
                            final double diagonalDistance = Math.sqrt(xStep * xStep + yStep * yStep + zStep * zStep);

                            //normalize
                            xStep /= diagonalDistance;
                            yStep /= diagonalDistance;
                            zStep /= diagonalDistance;

                            //Get energy
                            //1.12: this.world().rand.nextFloat()
                            float radialEnergy = this.getBlastRadius() * (0.7F + serverLevel.getRandom().nextFloat() * 0.6F);

                            //Get starting point for ray
                            double x = this.location.x();
                            double y = this.location.y();
                            double z = this.location.z();

                            for (float step = 0.3F; radialEnergy > 0.0F; radialEnergy -= step * 0.75F)
                            {
                                //Convert position to int
                                //1.12: MathHelper.floor(...)
                                int xi = Mth.floor(x);
                                int yi = Mth.floor(y);
                                int zi = Mth.floor(z);

                                //Get block
                                BlockPos blockPos = new BlockPos(xi, yi, zi);
                                BlockState blockState = serverLevel.getBlockState(blockPos);
                                Block block = blockState.getBlock();

                                //Only act on non-air blocks
                                if (!blockState.isAir()) //1.12: blockState.getMaterial() != Material.AIR
                                {
                                    //Decrease energy based on resistance
                                    //1.12: block.getExplosionResistance(this.world(), blockPos, this.exploder, this)
                                    //26.3: Block#getExplosionResistance() no longer takes level/position/source
                                    radialEnergy -= (block.getExplosionResistance() + 0.3F) * step;

                                    //Track blocks to destroy
                                    if (radialEnergy > 0.0F)
                                    {
                                        if (!getAffectedBlockPositions().contains(blockPos))
                                        {
                                            getAffectedBlockPositions().add(blockPos);
                                        }
                                    }
                                }

                                //Iterate location
                                x += xStep * step;
                                y += yStep * step;
                                z += zStep * step;
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * Removes the blocks from the world
     */
    protected void doDestroyBlocks() //TODO convert to change action
    {
        if (this.world instanceof ServerLevel serverLevel) //1.12: !this.world().isRemote
        {
            for (BlockPos blockDestroyedPos : getAffectedBlockPositions()) //TODO convert block positions to block edits to track prev and current blocks
            {
                //Get block
                final BlockState blockState = serverLevel.getBlockState(blockDestroyedPos);

                ///Generate effect TODO send a single packet with a list of block pos, this will do a 80% reduction in packet byte data
                //1.12: PacketSpawnBlockExplosion.sendToAllClients(world(), x(), y(), z(), getBlastRadius(), blockDestroyedPos)
                BlockExplosionFxPayload.sendToAllClients(serverLevel, x(), y(), z(), getBlastRadius(), blockDestroyedPos);

                //Only edit block if not air
                if (!blockState.isAir()) //1.12: blockState.getMaterial() != Material.AIR
                {
                    try
                    {
                        //Do drops and break the block
                        //1.12: if (blockState.getBlock().canDropFromExplosion(this)) {
                        //          blockState.getBlock().dropBlockAsItemWithChance(world, pos, state, 1.0F / getBlastRadius(), 0);
                        //      }
                        //      blockState.getBlock().onBlockExploded(world, pos, this);
                        //26.3 folds drop + removal into BlockState#onExplosionHit, which checks
                        //BlockBehaviour#dropFromExplosion and drops through the block loot table.
                        blockState.onExplosionHit(serverLevel, blockDestroyedPos, this, (stack, pos) -> Block.popResource(serverLevel, pos, stack));
                    }
                    catch (Exception e)
                    {
                        //1.12 printed the stack trace here, the port logs with the mod logger
                        ICBMClassic.LOGGER.error("BlastTNT failed to destroy block " + blockDestroyedPos + " at " + location, e);
                    }
                }
            }
        }
    }

    /**
     * The 1.12 blast dropped 1/blastRadius of the block loot
     * ({@code dropBlockAsItemWithChance(world, pos, state, 1.0F / getBlastRadius(), 0)}), the same chance
     * vanilla TNT uses. 26.3 moves that chance into the block loot table
     * ({@code minecraft:survives_explosion}) which only receives the explosion radius when the interaction
     * type is {@code DESTROY_WITH_DECAY}, so the blast reports that instead of the base {@code DESTROY}.
     * Only {@link BlockState#onExplosionHit} reads this value, the blast still edits the world itself.
     */
    @Override
    public Explosion.BlockInteraction getBlockInteraction()
    {
        return Explosion.BlockInteraction.DESTROY_WITH_DECAY;
    }

    public void pushEntities(float radius, float force, PushType type) //TODO convert to delay action
    {
        // Step 2: Damage all entities
        Pos minCoord = location.toPos();
        minCoord = minCoord.add(-radius - 1);
        Pos maxCoord = location.toPos();
        maxCoord = maxCoord.add(radius + 1);

        Cube region = new Cube(minCoord, maxCoord);
        //1.12: world.getEntitiesWithinAABB(Entity.class, region.getAABB())
        List<Entity> entities = world.getEntitiesOfClass(Entity.class, region.getAABB());

        for (Entity entity : entities)
        {
            //1.12: entity.getDistance(location.x(), location.y(), location.z())
            double distanceScale = entity.position().distanceTo(new Vec3(location.x(), location.y(), location.z())) / radius;

            if (distanceScale <= 1.0D)
            {
                //Get delta
                double xDifference = entity.getX() - location.x();
                double yDifference = entity.getY() - location.y();
                double zDifference = entity.getZ() - location.z();

                //Get magnitude
                //1.12: MathHelper.sqrt(double) is (float) Math.sqrt(value)
                double mag = (float) Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference);

                //Normalize difference
                xDifference /= mag;
                yDifference /= mag;
                zDifference /= mag;

                //1.12: entity was an instance of the 1.12 player class
                if (type == PushType.ATTRACT)
                {
                    double modifier = distanceScale * force * (entity instanceof Player ? 0.5 : 1);
                    //1.12: entity.addVelocity(-xDifference * modifier, -yDifference * modifier, -zDifference * modifier)
                    entity.push(-xDifference * modifier, -yDifference * modifier, -zDifference * modifier);
                }
                else if (type == PushType.REPEL)
                {
                    double modifier = (1.0D - distanceScale) * force * (entity instanceof Player ? 0.5 : 1);
                    //1.12: entity.addVelocity(xDifference * modifier, yDifference * modifier, zDifference * modifier)
                    entity.push(xDifference * modifier, yDifference * modifier, zDifference * modifier);
                }
            }
        }
    }

    @Override
    public void load(ValueInput in) //1.12: load(nbt compound)
    {
        super.load(in);
        this.pushType = PushType.values()[in.getIntOr(NBTConstants.PUSH_TYPE, 0)]; //1.12: nbt.getInteger
        this.destroyItem = in.getBooleanOr(NBTConstants.DESTROY_ITEM, false); //1.12: nbt.getBoolean
    }

    @Override
    public void save(ValueOutput out) //1.12: save(nbt compound)
    {
        super.save(out);
        out.putInt(NBTConstants.PUSH_TYPE, this.pushType.ordinal()); //1.12: nbt.setInteger
        out.putBoolean(NBTConstants.DESTROY_ITEM, this.destroyItem); //1.12: nbt.setBoolean
    }

    public static enum PushType
    {
        NO_PUSH, ATTRACT, REPEL;
    }
}
