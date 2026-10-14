package icbm.classic.content.blast;

import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Endothermic ("endothermic" explosive) beam blast, freezes the area of the beam into an ice and snow
 * landscape.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code world.setBlockState(pos, state, 3)} → {@code Level#setBlock(pos, state, Block.UPDATE_ALL)},
 *     {@code world.setBlockToAir(pos)} → {@code Level#setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)}
 *     and the 1.12 call without flags used the same default of 3.</li>
 *     <li>{@code pos.up()/down()} → {@code BlockPos#above()/below()}, {@code world.rand} →
 *     {@code Level#getRandom()} and {@code state.getMaterial()} → the helpers of {@link BlastBeam}.</li>
 *     <li>{@code Blocks.MAGMA} → {@link Blocks#MAGMA_BLOCK}, {@code Blocks.SNOW_LAYER} → {@link Blocks#SNOW}
 *     with {@code BlockSnow.LAYERS} → {@link SnowLayerBlock#LAYERS} and {@code Blocks.SNOW} →
 *     {@link Blocks#SNOW_BLOCK}.</li>
 *     <li>1.12 had a settled and a flowing lava block, 26.3 has one lava block with a fluid level, see the
 *     comment at the lava branch. {@code BlockLiquid.LEVEL} → {@link LiquidBlock#LEVEL}.</li>
 *     <li>{@code blockStateUnder.isSideSolid(world, pos.down(), 1.12 up facing)} →
 *     {@code BlockState#isFaceSturdy(level, pos, Direction.UP)} and
 *     {@code Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos)} → {@code BlockState#canSurvive(level, pos)}.</li>
 *     <li>{@code world().getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB)} →
 *     {@code Level#getEntitiesOfClass(LivingEntity.class, AABB)}, the same entity mapping the ported mutation
 *     blast uses. {@code entity.addPotionEffect(new PotionEffect(...))} →
 *     {@code Entity#addEffect(new MobEffectInstance(holder, duration, amplifier))}.</li>
 *     <li>{@code world().getGameRules().getBoolean("doDaylightCycle")} →
 *     {@code ServerLevel#getGameRules()#get(GameRules.ADVANCE_TIME)} and {@code setWorldTime(1200)} →
 *     {@link BlastBeam#setWorldTime(long)}, the 26.3 day/night cycle is a per dimension clock.</li>
 * </ul>
 */
public class BlastEndothermic extends BlastBeam
{
    public BlastEndothermic()
    {
        this.red = 0f;
        this.green = 0.3f;
        this.blue = 0.7f;
    }

    @Override
    protected void mutateBlocks(List<BlockPos> edits)
    {
        final double radius = this.getBlastRadius();
        final double radiusDecay = Math.max(1, radius * 0.3); //TODO config
        for (BlockPos targetPosition : edits)
        {
            final double delta_x = location.xi() - targetPosition.getX();
            final double delta_y = location.yi() - targetPosition.getY();
            final double delta_z = location.zi() - targetPosition.getZ();

            final double distance = Math.sqrt(delta_x * delta_x + delta_y * delta_y + delta_z * delta_z);
            final double distanceScale = 1 - (distance / radius);

            final BlockState blockState = this.world().getBlockState(targetPosition);

            //Closer to center the better the chance of spawning blocks
            if (distance <= radiusDecay || Math.random() < distanceScale)
            {
                //Turn fluids and liquid like blocks to air
                if (isWater(blockState)) //1.12: blockState.getMaterial() == Material.WATER
                {
                    this.world().setBlock(targetPosition, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL); //1.12: world.setBlockState(targetPosition, Blocks.ICE.getDefaultState(), 3)
                }
                else if (blockState.is(Blocks.FIRE)) //1.12: blockState.getBlock() == Blocks.FIRE
                {
                    this.world().setBlock(targetPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); //1.12: world.setBlockToAir(targetPosition)
                }
                else if (blockState.is(Blocks.LAVA)) //1.12: blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.FLOWING_LAVA
                {
                    //1.12 had a settled lava block and a flowing one. 26.3 has one lava block that carries the
                    //fluid level, level 0 is the source and matches the settled block of 1.12
                    if (blockState.getValue(LiquidBlock.LEVEL) == 0)
                    {
                        this.world().setBlock(targetPosition, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL); //1.12: world.setBlockState(targetPosition, Blocks.OBSIDIAN.getDefaultState())
                    }
                    else
                    {
                        //1.12: Math.min(8, Math.max(1, blockState.getValue(BlockLiquid.LEVEL) / 2)) layers of snow
                        final int level = Math.min(8, Math.max(1, blockState.getValue(LiquidBlock.LEVEL) / 2));
                        this.world().setBlock(targetPosition, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, level), Block.UPDATE_ALL);
                    }
                }
                else if (blockState.is(Blocks.MAGMA_BLOCK)) //1.12: blockState.getBlock() == Blocks.MAGMA
                {
                    this.world().setBlock(targetPosition, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
                else if (blockState.is(Blocks.NETHERRACK)) //1.12: blockState.getBlock() == Blocks.NETHERRACK
                {
                    this.world().setBlock(targetPosition, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }
                else if (blockState.is(Blocks.SOUL_SAND)) //1.12: blockState.getBlock() == Blocks.SOUL_SAND
                {
                    if (this.world().getRandom().nextBoolean()) //1.12: world.rand.nextBoolean()
                    {
                        this.world().setBlock(targetPosition, Blocks.SAND.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    else
                    {
                        this.world().setBlock(targetPosition, Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }

                //Ground replacement
                else if (isGroundOrGrass(blockState)) //1.12: Material.GROUND || Material.GRASS
                {
                    if (this.world().getRandom().nextBoolean()) //1.12: world.rand.nextBoolean()
                    {
                        this.world().setBlock(targetPosition, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    else
                    {
                        this.world().setBlock(targetPosition, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_ALL); //1.12: Blocks.SNOW.getDefaultState()
                    }
                }

                //Randomly place fire TODO move to outside mutate so we always place snow while charging up
                if (Math.random() < distanceScale)
                {
                    tryPlaceSnow(this.world(), targetPosition.above(), false); //1.12: targetPosition.up()
                }
            }
        }
    }

    private static void tryPlaceSnow(Level world, BlockPos pos, boolean random)
    {
        if (!random || world.getRandom().nextBoolean()) //1.12: world.rand.nextBoolean()
        {
            //Place snow
            final BlockState blockState = world.getBlockState(pos);
            final BlockState blockStateUnder = world.getBlockState(pos.below()); //1.12: pos.down()
            //1.12: blockState.getBlock().isReplaceable(world, pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos)
            //      && blockStateUnder.isSideSolid(world, pos.down(), 1.12 up facing)
            if (blockState.canBeReplaced()
                    && Blocks.SNOW.defaultBlockState().canSurvive(world, pos)
                    && blockStateUnder.isFaceSturdy(world, pos.below(), Direction.UP))
            {
                //1.12: Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, 1 + world.rand.nextInt(7))
                world.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + world.getRandom().nextInt(7)), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onBlastCompleted()
    {
        super.onBlastCompleted();

        //Freeze all nearby entities.
        //1.12: world().getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(location ± getBlastRadius()))
        final List<LivingEntity> livingEntities = world().getEntitiesOfClass(LivingEntity.class, new AABB(location.x() - getBlastRadius(), location.y() - getBlastRadius(), location.z() - getBlastRadius(), location.x() + getBlastRadius(), location.y() + getBlastRadius(), location.z() + getBlastRadius()));

        if (!livingEntities.isEmpty())
        {
            for (LivingEntity entity : livingEntities)
            {
                if (entity.isAlive()) //1.12: entity.isEntityAlive()
                {
                    //entity.addEffect(new CustomPotionEffect(PoisonFrostBite.INSTANCE, 60 * 20, 1, null));
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 10 * 20, 2)); //1.12: entity.addPotionEffect(new PotionEffect(MobEffects.POISON, 10 * 20, 2))
                    entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120 * 20, 2)); //1.12: entity.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 120 * 20, 2))
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 4)); //1.12: entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120 * 20, 4))
                }
            }
        }

        //Change to time
        //1.12: ConfigBlast.ALLOW_DAY_NIGHT && world().getGameRules().getBoolean("doDaylightCycle")
        if (ConfigBlast.ALLOW_DAY_NIGHT && this.world() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.ADVANCE_TIME))
        {
            setWorldTime(1200); //1.12: world().setWorldTime(1200)
        }
    }
}
