package icbm.classic.content.blast.redmatter.logic;

import icbm.classic.ICBMClassic;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.explosion.IBlastIgnore;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.helpers.BlastBlockHelpers;
import icbm.classic.content.blast.redmatter.DamageSourceRedmatter;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.entity.EntityExplosion;
import icbm.classic.content.entity.flyingblock.BlockCaptureData;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Handles logic of the redmatter tick
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li><b>Threading (S1.09):</b> 1.12 ran this whole class on the server thread. There is no thread anywhere
 *     in the 1.12 redmatter code: {@link RedmatterBlockCollector} is a static helper that only does integer
 *     math over the six faces and never reads the world. The world reads and edits
 *     ({@code Level#clip}, {@code Level#getBlockState}, {@code Level#setBlock}) happen inside
 *     {@link #tick()} right after, which {@code EntityRedmatter#tick} calls on the server thread, exactly like
 *     1.12. The worker framework ({@code LevelSnapshot} + {@code BlockEditQueue}) is therefore not needed
 *     here: nothing runs off thread, so there is nothing to snapshot or queue. The per tick budgets
 *     (raytraces / block edits) that 1.12 used to keep the main thread happy are kept.</li>
 *     <li>1.12 {@code Entity} position/motion reads ({@code posX/Y/Z}, {@code getPositionVector()},
 *     {@code getDistance(entity)}) are the 26.3 {@code Entity#getX/Y/Z()}, {@code Entity#position()} and
 *     {@code Entity#distanceTo(Entity)}.</li>
 *     <li>{@code MathHelper} is {@code Mth} (1.12 {@code MathHelper.sqrt(double)} is {@code Mth.sqrt(float)}),
 *     the 1.12 facing enum is {@link Direction} and the 1.12 player flags
 *     {@code capabilities.isCreativeMode}/{@code capabilities.disableDamage} are
 *     {@code Player#isCreative()}/{@code Abilities.invulnerable}.</li>
 *     <li>{@code world.rand} is {@code Level#getRandom()}; it returns a {@code RandomSource} which is not a
 *     {@code java.util.Random}, so the {@code CalculationHelpers.randFloatRange} calls of 1.12 go through
 *     {@link #randFloatRange(RandomSource, float)} / {@link #randFloatRange(RandomSource, float, float)}
 *     below (same math as {@code Mth.nextFloat}).</li>
 *     <li>Block removal is {@code Level#setBlock(pos, state, Block#UPDATE_ALL)}, which is the 1.12
 *     {@code setBlockState(pos, state, 3)}, and air is {@code BlockState#isAir()}. The 1.12 pair
 *     {@code Blocks.WATER}/{@code Blocks.FLOWING_WATER} is a single water block in 26.3, see
 *     {@link #isWater(BlockState)}.</li>
 *     <li>{@code entity.addVelocity(...) + velocityChanged = true} is {@code Entity#push(...)}, which marks
 *     the velocity for sync. push also drops non finite values, so the 1.12 NaN result of a zero distance
 *     can no longer corrupt the entity motion.</li>
 *     <li>1.12 {@code attackEntityFrom} is {@code Entity#hurtServer(ServerLevel, ...)} and
 *     {@code setDead()} is {@code Entity#discard()}.</li>
 *     <li>1.12 queried the explosive and blast velocity capabilities for the entities being eaten, 26.3 reads
 *     those implementations off the entity instance ({@code IExplosive} for the explosive branch,
 *     {@code EntityRedmatter#capRedmatterPull} for the motion override).</li>
 * </ul>
 */
public class RedmatterLogic
{
    //Host of the logic
    public final EntityRedmatter host;

    /** Blocks destroyed in the current size cycle... cycle can last few ticks to several minutes */
    protected int blockDestroyedThisCycle = 0;
    /** Blocks destroyed this tick, used to limit max world edits */
    protected int blockDestroyedThisTick = 0;
    /** Raytrace lines run to detect blocks this tick, used to limit CPU usage */
    protected int raytracesThisTick = 0;
    /** Cycles run since we removed blocks, used to track starve rate */
    protected int cyclesSinceLastBlockRemoved = -1;
    /** Current scan radius for blocks */
    protected int currentBlockDestroyRadius = -1;

    /** Queue of raytraces to run for searching blocks, this defaults to edge blocks only */
    protected final Queue<BlockPos> rayTraceTargets = new LinkedList<>();

    /**
     * @param host - redmatter entity running this logic
     */
    public RedmatterLogic(EntityRedmatter host)
    {
        this.host = host;
    }

    /**
     * Check how many blocks can be edited per tick
     *
     * @return blocks that can be removed each tick
     */
    public int getBlocksPerTick()
    {
        return ConfigBlast.redmatter.MAX_BLOCKS_EDITS_PER_TICK;
    }

    /** Invoked each tick by the controlling entity */
    public void tick()
    {
        preTick();
        doTick();
        postTick();
    }

    /** Prep cycle to cleanup from last tick */
    protected void preTick()
    {
        raytracesThisTick = 0;
        blockDestroyedThisTick = 0;

        //Init destroy radius based on host's saved value
        if (currentBlockDestroyRadius < 0)
        {
            currentBlockDestroyRadius = (int) Math.floor(host.getBlastSize());
        }
    }

    /** Actual work cycle */
    protected void doTick()
    {
        //Do actions
        detectAndDestroyBlocks();
        doEntityEffects();

        //Play effects
        if (ConfigBlast.redmatter.ENABLE_AUDIO)
        {
            //TODO collapse audio should play near blocks destroyed for better effect
            if (host.level().getRandom().nextInt(8) == 0) //1.12: world.rand.nextInt(8)
            {
                final double playX = host.getX() + randFloatRange(host.level().getRandom(), host.getBlastSize());
                final double playY = host.getY() + randFloatRange(host.level().getRandom(), host.getBlastSize());
                final double playZ = host.getZ() + randFloatRange(host.level().getRandom(), host.getBlastSize());
                final float volume = randFloatRange(host.level().getRandom(), 5, 6);
                final float pitch = randFloatRange(host.level().getRandom(), 0.6f, 1);
                //1.12: ICBMSounds.COLLAPSE.play(world, x, y, z, volume, pitch, true); the trailing flag only
                //reached the client branch of the 1.12 sound helper, this logic is server only
                host.level().playSound(null, playX, playY, playZ, ModSounds.COLLAPSE, SoundSource.BLOCKS, volume, pitch);
            }
            //TODO check if this should play every tick
            host.level().playSound(null, host.getX(), host.getY(), host.getZ(), ModSounds.REDMATTER, SoundSource.BLOCKS, 3.0F,
                    randFloatRange(host.level().getRandom(), -0.8f, 1.2f));
        }
    }

    /** Post work cycle */
    protected void postTick()
    {
        //Decrease block if we don't destroy anything
        if (blockDestroyedThisCycle <= 0)
        {
            decreaseScale();
        }
    }

    /** Handles decreasing the size of the redmatter */
    protected void decreaseScale()
    {
        final float size = host.getBlastSize();

        //We are not removing blocks so the redmatter is starving (has no blocks to remove), decrease size
        if (size <= cyclesSinceLastBlockRemoved)
        {
            //TODO make it optional to remove small redmatters. This way we can leave land marks were old redmatter exist
            //TODO if we leave small redmatters allow players to remove them and/or capture in jars
            if (size <= ConfigBlast.redmatter.MIN_SIZE)
            {
                host.setBlastSize(0);
                host.discard(); //1.12: host.setDead()
                ICBMClassic.logger().info("Redmatter[{}] has starved to death at {} {} {} {}",
                        host.getId(), //1.12: host.getEntityId()
                        host.getX(),
                        host.getY(),
                        host.getZ(),
                        host.level().dimension()); //1.12: host.world.provider.getDimension()
            }
            else
            //Decrease mass
            {
                final float newSize = size < 1 ? size * 0.9f : size * ConfigBlast.redmatter.STARVE_SCALE;
                host.setBlastSize(newSize);
            }
        }
    }

    /** Handles looking for blocks and starting the destroy process */
    protected void detectAndDestroyBlocks()
    {
        //Match blast size it if changes
        if (currentBlockDestroyRadius > host.getBlastMaxSize())
        {
            setCurrentBlockDestroyRadius((int) Math.floor(host.getBlastSize()));
        }

        //Init stage
        if (rayTraceTargets.isEmpty())
        {
            startNextBlockDestroyCycle();
        }

        //Destroy blocks until we are told to stop
        cycleDestroyBlocks();
    }

    /** Detects blocks and removes them */
    protected void cycleDestroyBlocks()
    {
        //Loop targets and trace limit per tick
        final Vec3 center = host.position(); //1.12: host.getPositionVector()
        while (!shouldStopBreakingBlocks() && !rayTraceTargets.isEmpty())
        {
            raytracesThisTick++;
            rayTraceTowardsBlock(center, rayTraceTargets.poll());
        }
    }

    /** Triggers the next block cycle finding all raytrace paths */
    protected void startNextBlockDestroyCycle()
    {
        //Increase size
        if (cyclesSinceLastBlockRemoved > 0)
        {
            cyclesSinceLastBlockRemoved = 0;
            setCurrentBlockDestroyRadius(currentBlockDestroyRadius + 1);  //TODO change scale to number of blocks eaten
        }

        //Ensure we are empty to avoid memory overflow or duplicate data
        rayTraceTargets.clear();

        //Collect positions
        RedmatterBlockCollector.collectBlocksOnWallEdges(currentBlockDestroyRadius, (rx, ry, rz) -> {
            //TODO implement flywheel pattern
            rayTraceTargets.offer(new BlockPos(rx, ry, rz));
        });

        //Randomize ray order
        Collections.shuffle((List<?>) rayTraceTargets);

        //If we didn't destroy anything at this layer expand
        if (blockDestroyedThisCycle <= 0)
        {
            cyclesSinceLastBlockRemoved += 1;
        }
        blockDestroyedThisCycle = 0;
    }

    /**
     * Raytraces towards a target position looking for blocks
     * <p>
     * This will not remove all blocks in the path. Instead it kills the first
     * block it finds allowing for a repeat cycle of block removal in the path
     * over several ticks.
     *
     * @param center - redmatter center, passed in to avoid recreating
     * @param target - block target to trace towards
     */
    protected void rayTraceTowardsBlock(final Vec3 center, final BlockPos target)
    {
        //Build target position
        final double targetX = target.getX() + center.x;
        final double targetY = target.getY() + center.y;
        final double targetZ = target.getZ() + center.z;
        final Vec3 pos = new Vec3(targetX, targetY, targetZ);

        //Raytrace towards block
        //1.12: world.rayTraceBlocks(center, pos, true, false, false) -> 26.3 ClipContext. The three 1.12 flags
        //used here are stopOnLiquid = true and ignoreBlockWithoutBoundingBox = false (the third flag only
        //affected the miss result), which is the Fluid.ANY + Block.OUTLINE mapping of
        //icbm.classic.lib.transform.vector.AbstractPos#rayTraceBlocks
        final BlockHitResult rayTrace = host.level().clip(new ClipContext(center, pos, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, host));
        //26.3 answers with HitResult.Type.MISS instead of a null result when nothing was hit
        if (rayTrace.getType() == HitResult.Type.BLOCK)
        {
            processNextBlock(rayTrace.getBlockPos());
        }
    }

    /**
     * Checks to see if we should stop looping while breaking blocks
     *
     * @return true to stop
     */
    protected boolean shouldStopBreakingBlocks()
    {
        return raytracesThisTick > ConfigBlast.redmatter.DEFAULT_BLOCK_RAYTRACE_PER_TICK
                || blockDestroyedThisTick > getBlocksPerTick()
                || host.isRemoved(); //1.12: host.isDead
    }

    /**
     * Process the next block from looping
     *
     * @param blockPos - blockToEdit
     */
    protected void processNextBlock(BlockPos blockPos)
    {
        //1.12: MathHelper.sqrt(host.getDistanceSqToCenter(blockPos)), the (float) Math.sqrt of the squared
        //distance from the host to the center of the block
        final double dist = (float) Math.sqrt(host.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D));

        //We are looping in a shell orbit around the center
        if (dist < (this.currentBlockDestroyRadius + 1))
        {
            final BlockCaptureData blockCaptureData = new BlockCaptureData(host.level(), blockPos);
            if (shouldRemoveBlock(blockPos, blockCaptureData.getBlockState())) //TODO calculate a pressure or pull force to destroy weaker blocks before stronger blocks
            {
                //TODO handle multi-blocks
                //TODO: render fluid streams moving into hole


                if (host.level().setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) //1.12: world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3)
                {
                    //Freeze fluid blocks to improve pull rate
                    if (isWater(blockCaptureData.getBlockState()))
                    {
                        freezeWaterAround(blockPos);
                    }

                    //Convert a random amount of destroyed blocks into flying blocks for visuals
                    if (canTurnIntoFlyingBlock(blockCaptureData.getBlockState()) && host.level().getRandom().nextFloat() > ConfigBlast.redmatter.CHANCE_FOR_FLYING_BLOCK)
                    {
                        spawnFlyingBlock(blockPos, blockCaptureData);
                    }
                    markBlockRemoved();
                }
            }
        }
    }

    private void freezeWaterAround(BlockPos pos) //TODO convert to map<Block, Action> to allow introducing more effects
    {
        for (Direction side : Direction.values()) //1.12: the facing enum's values()
        {
            final BlockPos blockPos = pos.relative(side); //1.12: pos.add(side.getDirectionVec())
            final BlockState blockState = host.level().getBlockState(blockPos);
            if (isWater(blockState))
            {
                host.level().setBlock(blockPos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL); //TODO turn into fake ice that melts randomly
            }
        }
    }

    /**
     * 1.12 checked the block identity of {@code Blocks.WATER} and {@code Blocks.FLOWING_WATER}. 26.3 has a
     * single water block covering the still and the flowing state, so the 1.12 pair collapses to
     * {@code state.is(Blocks.WATER)} (the same mapping {@code BlastBeam#isWater} uses).
     */
    private static boolean isWater(BlockState state)
    {
        return state.is(Blocks.WATER);
    }

    private void markBlockRemoved()
    {
        blockDestroyedThisCycle++; //Tracks blocks removed over several ticks
        blockDestroyedThisTick++; //Tracks blocks removed in a single tick
        cyclesSinceLastBlockRemoved = 0;
    }

    /**
     * Checks to see if we should remove the block
     *
     * @param blockPos   - position of the block in the world
     * @param blockState - state of the block
     * @return true to remove
     */
    protected boolean shouldRemoveBlock(BlockPos blockPos, BlockState blockState)
    {
        final boolean isFluid = BlastBlockHelpers.isFluid(blockState);
        //Ignore air blocks and unbreakable blocks
        return !blockState.isAir() //1.12: !block.isAir(state, world, pos)
                && (BlastBlockHelpers.isFlowingWater(blockState) || !isFluid && blockState.getDestroySpeed(host.level(), blockPos) >= 0); //1.12: blockState.getBlockHardness(world, pos)

    }

    protected boolean canTurnIntoFlyingBlock(BlockState blockState)
    {
        return ConfigBlast.redmatter.SPAWN_FLYING_BLOCKS && !BlastBlockHelpers.isFluid(blockState);
    }

    protected void spawnFlyingBlock(BlockPos blockPos, BlockCaptureData blockCaptureData)
    {
        // TODO get itemStack, do rng for if stack is complete version or broken version
        FlyingBlock.spawnFlyingBlock(host.level(), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, blockCaptureData, (entity) -> {
            entity.setYawChange(50 * host.level().getRandom().nextFloat()); //TODO why 50? 1.12: entity.yawChange
            entity.setPitchChange(50 * host.level().getRandom().nextFloat()); //1.12: entity.pitchChange
            entity.noPhysics = true; //1.12: entity.noClip = true
        }, this::handleEntities, null);
    }

    private float getEntityImpactRange()
    {
        return host.getBlastSize() * ConfigBlast.redmatter.GRAVITY_SCALE;
    }

    protected void doEntityEffects()
    {
        final float entityRadius = getEntityImpactRange();

        final AABB bounds = new AABB( //1.12: AxisAlignedBB
                host.getX() - entityRadius,
                host.getY() - entityRadius,
                host.getZ() - entityRadius,
                host.getX() + entityRadius,
                host.getY() + entityRadius,
                host.getZ() + entityRadius);

        //Get all entities in the cube area
        //1.12: world.getEntitiesWithinAABB(Entity.class, bounds)
        host.level().getEntitiesOfClass(Entity.class, bounds)
                //Filter down
                .stream().filter(this::shouldHandleEntity)
                //Apply to each
                .forEach(this::handleEntities);
    }

    private boolean shouldHandleEntity(Entity entity)
    {
        //Ignore self
        if (entity == host)
        {
            return false;
        }

        //Ignore players that are in creative mode or can't be harmed TODO may need to check for spectator?
        //1.12: capabilities.isCreativeMode || capabilities.disableDamage
        if (entity instanceof Player player && (player.isCreative() || player.getAbilities().invulnerable))
        {
            return false;
        }

        //Ignore entities that mark themselves are ignorable
        if (entity instanceof IBlastIgnore ignore && ignore.canIgnore(host.blastData)) //TODO remove
        {
            return false;
        }

        return true;
    }

    /**
     * Makes an entity get affected by Red Matter.
     */
    protected void handleEntities(Entity entity)
    {
        //Calculate different from center
        final double xDifference = host.getX() - entity.getX();
        final double yDifference = host.getY() - entity.getY();
        final double zDifference = host.getZ() - entity.getZ();

        final double distance = host.distanceTo(entity); //1.12: host.getDistance(entity)

        moveEntity(entity, xDifference, yDifference, zDifference, distance);
        attackEntity(entity, distance);
    }

    private boolean moveEntity(Entity entity, double xDifference, double yDifference, double zDifference, double distance)
    {
        //Allow overriding default pull logic
        //1.12 queried the blast velocity capability, which only the redmatter entity answered with a real
        //handler (CapRedmatterPull); every other entity got the Forge default answer of false. Reading the
        //handler off the entity instance is the 26.3 replacement for that capability lookup
        if (entity instanceof EntityRedmatter otherRedmatter
                && otherRedmatter.capRedmatterPull.onBlastApplyMotion(host, host.blastData, xDifference, yDifference, zDifference, distance))
        {
            return true;
        }

        final double distanceScale = Math.max(0, 1 - (distance / getEntityImpactRange())); //0.0 to 1.0
        final double pullPower = Math.min(1, distanceScale * host.getBlastSize() * 0.01); //TODO need a max speed

        //Calculate velocity (delta / mag) * power
        final double velX = (xDifference / distance) * pullPower;
        final double velY = (yDifference / distance) * pullPower;
        final double velZ = (zDifference / distance) * pullPower;

        // Gravity Velocity
        //1.12: entity.addVelocity(velX, velY, velZ) + entity.velocityChanged = true, push marks the sync
        entity.push(velX, velY, velZ); //Negative pulls it towards the redmatter

        return true;
    }

    private void attackEntity(Entity entity, double distance)
    {
        //TODO move each section to capability or reg system
        //TODO make config driven, break section out into its own method

        //Handle eating logic
        final double attackRange = Math.max(1, ConfigBlast.redmatter.KILL_SCALE * host.getBlastSize());
        if (distance < attackRange)
        {
            if (entity instanceof EntityRedmatter otherRedmatter && !otherRedmatter.isRemoved()) //1.12: entity instanceof EntityRedmatter && !entity.isDead
            {
                //https://www.wolframalpha.com/input/?i=(4%2F3)pi+*+r%5E3+%3D+(4%2F3)pi+*+a%5E3+%2B+(4%2F3)pi+*+b%5E3

                //We are going to merge both blasts together
                final double selfRad = Math.pow(host.getBlastSize(), 3);
                final double targetRad = Math.pow(otherRedmatter.getBlastSize(), 3);

                final float newRad = (float) Math.cbrt(selfRad + targetRad); //TODO why cube?

                host.setBlastSize(newRad);

                //TODO combine the vectors instead of turning into zero
                host.setDeltaMovement(0, 0, 0); //1.12: host.motionX/Y/Z = 0

                //TODO fire an event when combined (non-cancelable to allow acting on combined result)
                otherRedmatter.discard(); //1.12: entity.setDead()
            }
            else if (entity instanceof EntityExplosion explosionEntity && explosionEntity.getBlast() instanceof IBlast blast)
            {
                blast.clearBlast();
            }
            else if (entity instanceof IExplosive explosive) //1.12: entity.hasCapability(EXPLOSIVE_CAPABILITY, null)
            {
                //TODO(S4) use the ported entity cause once content/missile/logic/source/cause is ported, the
                //1.12 cause carried the host entity
                //TODO provide additional cause information related to what created the redmatter
                final ActionSource actionSource = new ActionSource(host.level(),
                        new Vec3(entity.getX(), entity.getY(), entity.getZ()), null);
                explosive.getExplosiveData().create(host.level(), entity.getX(), entity.getY(), entity.getZ(), actionSource, null).doAction();
                entity.discard(); //1.12: entity.setDead()
            }
            else if (entity instanceof LivingEntity) //1.12: the living entity class
            {
                //1.12: entity.attackEntityFrom(new DamageSourceRedmatter(this), ConfigBlast.redmatter.damage).
                //The logic only runs on the server thread (see the class javadoc)
                if (host.level() instanceof ServerLevel serverLevel)
                {
                    entity.hurtServer(serverLevel, new DamageSourceRedmatter(this), ConfigBlast.redmatter.damage);
                }
            }
            else
            {
                //Kill entity in the center of the ball
                entity.discard(); //1.12: entity.setDead()
                if (entity instanceof EntityFlyingBlock)
                {
                    if (host.getBlastSize() < host.getBlastMaxSize())
                    {
                        host.setBlastSize(host.getBlastSize() + 0.05f); //TODO magic number and config
                    }
                }
            }
        }
    }

    public void setCurrentBlockDestroyRadius(int size)
    {
        this.currentBlockDestroyRadius = (int) Math.max(1, Math.min(size, host.getBlastMaxSize()));
        this.host.setBlastSize(size);
    }

    /**
     * 1.12 used {@code CalculationHelpers.randFloatRange(Random, float)}, the range is {@code -scale} to
     * {@code +scale}.
     */
    private static float randFloatRange(RandomSource rand, float scale)
    {
        return Mth.nextFloat(rand, -scale, scale);
    }

    /**
     * 1.12 used {@code CalculationHelpers.randFloatRange(Random, float, float)}, same math as
     * {@code Mth.nextFloat(RandomSource, min, max)}.
     */
    private static float randFloatRange(RandomSource rand, float min, float max)
    {
        return Mth.nextFloat(rand, min, max);
    }
}
