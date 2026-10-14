package icbm.classic.content.blast;

import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

/**
 * Exothermic ("exothermic" explosive) beam blast, burns the area of the beam into a nether like landscape.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code world.setBlockState(pos, state, 3)} → {@code Level#setBlock(pos, state, Block.UPDATE_ALL)},
 *     the 1.12 call without a flag used the same default of 3. {@code world.setBlockToAir(pos)} →
 *     {@code Level#setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)}.</li>
 *     <li>{@code pos.up()/down()} → {@code BlockPos#above()/below()}, {@code world.rand} →
 *     {@code Level#getRandom()} and {@code state.getMaterial()} → the helpers of {@link BlastBeam}.</li>
 *     <li>{@code Blocks.FLOWING_LAVA.getDefaultState()} → {@code Blocks.LAVA.defaultBlockState()} (26.3 has one
 *     lava block, the flowing state is the fluid level of that block) and {@code Blocks.MAGMA} →
 *     {@link Blocks#MAGMA_BLOCK}.</li>
 *     <li>{@code Blocks.FIRE.getDefaultState()} → {@link BaseFireBlock#getState(Level, BlockPos)} and
 *     {@code Blocks.FIRE.canPlaceBlockAt(world, pos)} → {@code BlockState#canSurvive(Level, BlockPos)} on that
 *     state, the same mapping the ported incendiary blast uses. {@code block.isReplaceable(world, pos)} →
 *     {@code BlockState#canBeReplaced()}.</li>
 *     <li>{@code world().getGameRules().getBoolean("doDaylightCycle")} →
 *     {@code ServerLevel#getGameRules()#get(GameRules.ADVANCE_TIME)} and {@code setWorldTime(18000)} →
 *     {@link BlastBeam#setWorldTime(long)}, the 26.3 day/night cycle is a per dimension clock.</li>
 * </ul>
 */
public class BlastExothermic extends BlastBeam
{
    public BlastExothermic()
    {
        this.red = 0.7f;
        this.green = 0.3f;
        this.blue = 0;
    }

    @Override
    protected void mutateBlocks(List<BlockPos> edits)
    {
        final double radius = this.getBlastRadius();
        final double radiusDecay = Math.max(1, radius * 0.3); //TODO config
        final double radiusEnsured = Math.max(1, radius * 0.1); //TODO config
        for (BlockPos targetPosition : edits)
        {
            final double delta_x = location.xi() - targetPosition.getX();
            final double delta_y = location.yi() - targetPosition.getY();
            final double delta_z = location.zi() - targetPosition.getZ();

            final double distance = Math.sqrt(delta_x * delta_x + delta_y * delta_y + delta_z * delta_z);
            final double distanceScale = 1 - (distance / radius);

            final BlockState blockState = this.world().getBlockState(targetPosition);

            //Turn fluids and liquid like blocks to air
            //1.12: blockState.getMaterial() == Material.WATER || block == Blocks.ICE
            if (isWater(blockState) || blockState.is(Blocks.ICE))
            {
                this.world().setBlock(targetPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); //1.12: world.setBlockToAir(targetPosition)
            }

            //Closer to center the better the chance of spawning blocks
            if (distance <= radiusDecay || Math.random() < distanceScale)
            {
                //Destroy plants
                if (isVegetation(blockState)) //1.12: Material.LEAVES || Material.VINE || Material.PLANTS
                {
                    //1.12: !block.isReplaceable(world(), targetPosition) || Blocks.FIRE.canPlaceBlockAt(world(), targetPosition)
                    final BlockState fireState = BaseFireBlock.getState(this.world(), targetPosition);
                    if (!blockState.canBeReplaced() || fireState.canSurvive(this.world(), targetPosition))
                    {
                        this.world().setBlock(targetPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    else
                    {
                        this.world().setBlock(targetPosition, fireState, Block.UPDATE_ALL); //1.12: world.setBlockState(targetPosition, Blocks.FIRE.getDefaultState())
                    }
                }

                //Turn random stone into lava
                else if (isRock(blockState)) //1.12: Material.ROCK
                {
                    //Small chance to turn to lava
                    if (this.world().getRandom().nextFloat() > 0.9) //TODO add config
                    {
                        this.world().setBlock(targetPosition, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL); //1.12: Blocks.FLOWING_LAVA.getDefaultState()
                    }
                    //Coin flip to turn to magma
                    else if (this.world().getRandom().nextBoolean()) //TODO add config
                    {
                        this.world().setBlock(targetPosition.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_ALL); //1.12: targetPosition.down(), Blocks.MAGMA
                    }
                    //Coin flip to turn to netherrack
                    else if (this.world().getRandom().nextBoolean() || distance <= radiusEnsured) //TODO add config
                    {
                        placeNetherrack(this.world(), targetPosition);
                    }
                }

                //Sand replacement
                else if (isSand(blockState)) //1.12: Material.SAND
                {
                    if (this.world().getRandom().nextBoolean()) //TODO add config
                    {
                        this.world().setBlock(targetPosition.below(), Blocks.SOUL_SAND.defaultBlockState(), Block.UPDATE_ALL); //1.12: targetPosition.down()
                    }
                    else
                    {
                        placeNetherrack(this.world(), targetPosition);
                    }
                }

                //Ground replacement
                else if (isGroundOrGrass(blockState)) //1.12: Material.GROUND || Material.GRASS
                {
                    placeNetherrack(this.world(), targetPosition);
                }

                //Randomly place fire TODO move to outside mutate so we always place fire while charging up
                if (Math.random() < distanceScale)
                {
                    tryPlaceFire(this.world(), targetPosition.above(), false); //1.12: targetPosition.up()
                }
            }
        }
    }

    private static void placeNetherrack(Level world, BlockPos pos)
    {
        //1.12: !world.setBlockState(pos, Blocks.NETHERRACK.getDefaultState(), 3)
        if (!world.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL))
        {
            System.out.println("Failed to place netherrack at " + pos);
        }

        //Place fire randomly above netherrack
        tryPlaceFire(world, pos.above(), true); //1.12: pos.up()
    }

    private static void tryPlaceFire(Level world, BlockPos pos, boolean random)
    {
        if (!random || world.getRandom().nextBoolean()) //1.12: world.rand.nextBoolean()
        {
            //Place fire
            final BlockState blockState = world.getBlockState(pos);
            //1.12: blockState.getBlock().isReplaceable(world, pos) && Blocks.FIRE.canPlaceBlockAt(world, pos)
            final BlockState fireState = BaseFireBlock.getState(world, pos);
            if (blockState.canBeReplaced() && fireState.canSurvive(world, pos))
            {
                world.setBlock(pos, fireState, Block.UPDATE_ALL); //1.12: world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 3)
            }
        }
    }

    @Override
    public void onBlastCompleted()
    {
        super.onBlastCompleted();

        //Change time of day
        //1.12: ConfigBlast.ALLOW_DAY_NIGHT && world().getGameRules().getBoolean("doDaylightCycle")
        if (ConfigBlast.ALLOW_DAY_NIGHT && this.world() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.ADVANCE_TIME))
        {
            setWorldTime(18_000); //1.12: world().setWorldTime(18_000)
        }
    }
}
