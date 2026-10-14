package icbm.classic.content.blast;

import icbm.classic.content.reg.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Incendiary blast, spreads fire along the same edge rays the TNT style blast uses.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code block.isAir(state, world, pos)} → {@code BlockState#isAir()} and
 *     {@code block.isReplaceable(world, pos)} → {@code BlockState#canBeReplaced()}.</li>
 *     <li>{@code Blocks.FIRE.getDefaultState()} → {@code BaseFireBlock.getState(level, pos)} which picks the
 *     standing/side/soul fire state, {@code Blocks.FIRE.canPlaceBlockAt(world, pos)} →
 *     {@code BlockState#canSurvive(level, pos)} on that state.</li>
 *     <li>{@code world.setBlockState(pos, state, 3)} → {@code Level#setBlock(pos, state, Block.UPDATE_ALL)}
 *     (3 is {@code UPDATE_NEIGHBORS | UPDATE_CLIENTS}) and {@code world.setBlockToAir(pos)} →
 *     {@code Level#setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)}.</li>
 *     <li>{@code new BlockPos(double, double, double)} → {@code BlockPos.containing(double, double, double)},
 *     {@code world().rand} → {@code Level#getRandom()} and {@code block.getExplosionResistance(...)} →
 *     {@code Block#getExplosionResistance()}.</li>
 *     <li>{@code ICBMSounds.EXPLOSION_FIRE.play(world, x, y, z, volume, pitch, true)} →
 *     {@code Level#playSound(null, x, y, z, ModSounds.EXPLOSION_FIRE, SoundSource.BLOCKS, volume, pitch)},
 *     the 1.12 {@code distanceDelay} only applied to the client side playback.</li>
 * </ul>
 */
public class BlastFire extends Blast
{
    public BlastFire()
    {
    }

    @Override
    public boolean doExplode(int callCount)
    {
        if (this.world instanceof ServerLevel serverLevel) //1.12: !this.world().isRemote
        {
            int radius = (int) this.getBlastRadius();

            for (int x = 0; x < radius; ++x) //TODO replace with edge-raytracer
            {
                for (int y = 0; y < radius; ++y)
                {
                    for (int z = 0; z < radius; ++z)
                    {
                        if (x == 0 || x == radius - 1 || y == 0 || y == radius - 1 || z == 0 || z == radius - 1)
                        {
                            double xStep = x / (radius - 1.0F) * 2.0F - 1.0F;
                            double yStep = y / (radius - 1.0F) * 2.0F - 1.0F;
                            double zStep = z / (radius - 1.0F) * 2.0F - 1.0F;
                            double diagonalDistance = Math.sqrt(xStep * xStep + yStep * yStep + zStep * zStep);

                            xStep /= diagonalDistance;
                            yStep /= diagonalDistance;
                            zStep /= diagonalDistance;

                            //1.12: world().rand.nextFloat(), Math.random() below is intentional and world independent
                            float energy = radius * (0.7F + serverLevel.getRandom().nextFloat() * 0.6F);
                            double posX = location.x();
                            double posY = location.y();
                            double posZ = location.z();

                            for (float stepAmount = 0.3F; energy > 0.0F; energy -= stepAmount * 0.75F)
                            {
                                //1.12: new BlockPos(double, double, double)
                                BlockPos targetPosition = BlockPos.containing(posX, posY, posZ);
                                double distanceFromCenter = location.distance(targetPosition);
                                BlockState blockState = serverLevel.getBlockState(targetPosition); //1.12: world().getBlockState(...)
                                Block block = blockState.getBlock();

                                if (!blockState.isAir()) //1.12: !block.isAir(blockState, world, targetPosition)
                                {
                                    //1.12: block.getExplosionResistance(world(), targetPosition, this.exploder, this)
                                    //26.3: Block#getExplosionResistance() no longer takes level/position/source
                                    energy -= (block.getExplosionResistance() + 0.3F) * stepAmount;
                                }

                                if (energy > 0.0F)
                                {
                                    // Set fire by chance and distance
                                    double chance = radius - (Math.random() * distanceFromCenter);

                                    if (chance > distanceFromCenter * 0.55)
                                    {
                                        //1.12: block.isReplaceable(world(), targetPosition) || block.isAir(blockState, world(), targetPosition)
                                        boolean canReplace = blockState.canBeReplaced() || blockState.isAir();

                                        //1.12: canReplace && Blocks.FIRE.canPlaceBlockAt(world(), targetPosition)
                                        //26.3: BaseFireBlock.getState picks the fire state, canSurvive checks the placement
                                        final BlockState fireState = BaseFireBlock.getState(serverLevel, targetPosition);
                                        if (canReplace && fireState.canSurvive(serverLevel, targetPosition))
                                        {
                                            //1.12: world.setBlockState(targetPosition, Blocks.FIRE.getDefaultState(), 3)
                                            serverLevel.setBlock(targetPosition, fireState, Block.UPDATE_ALL);
                                        }
                                        else if (blockState.is(Blocks.ICE)) //1.12: block == Blocks.ICE
                                        {
                                            //1.12: world.setBlockToAir(targetPosition)
                                            serverLevel.setBlock(targetPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                                        }
                                    }
                                }

                                posX += xStep * stepAmount;
                                posY += yStep * stepAmount;
                                posZ += zStep * stepAmount;
                            }
                        }
                    }
                }
            }
        }

        //1.12: ICBMSounds.EXPLOSION_FIRE.play(world, x + 0.5D, y + 0.5D, z + 0.5D, 4.0F, pitch, true)
        this.world().playSound(null, location.x() + 0.5D, location.y() + 0.5D, location.z() + 0.5D, ModSounds.EXPLOSION_FIRE, SoundSource.BLOCKS, 4.0F, (1.0F + (world().getRandom().nextFloat() - world().getRandom().nextFloat()) * 0.2F) * 1F);
        return true;
    }
}
