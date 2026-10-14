package icbm.classic.content.blast;

import com.google.common.collect.ImmutableList;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.missiles.IMissile;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;
import java.util.Collection;

/**
 * Directed blast that drills a hole of {@code depth} blocks, one of the standard 1.12 explosives.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>1.12 facing enum → {@code Direction}, {@code getFrontOffsetX/Y/Z()} → {@code getStepX/Y/Z()}.</li>
 *     <li>{@code exploder.rotationPitch} → {@code Entity#getXRot()} and
 *     {@code exploder.getAdjustedHorizontalFacing()} → {@code Entity#getDirection()} (both are
 *     {@code Direction.fromYRot(yRot)}).</li>
 *     <li>{@code block.isAir(state, world, pos)} → {@code BlockState#isAir()} and
 *     {@code block.getBlockHardness(state, world, pos)} → {@code BlockState#getDestroySpeed(level, pos)}.</li>
 *     <li>{@code block.getExplosionResistance(world, pos, entity, explosion)} → {@code Block#getExplosionResistance()}
 *     (26.3 takes no level/position/source).</li>
 *     <li>Action fields: 1.12 {@code ActionField<VALUE, TAG extends NBTBase>} lost its NBT tag type in 26.3, see
 *     {@link #setValue(ActionField, Object)} and {@link #getFields()}.</li>
 * </ul>
 */
public class BlastBreach extends BlastTNT
{
    //1.12 held a raw ImmutableList<ActionField>, 26.3 typed the field so the wildcard is needed here
    public static final ImmutableList<ActionField<?>> SUPPORTED_FIELDS = ImmutableList.<ActionField<?>>of(ActionFields.HOST_DIRECTION);
    @Setter @Accessors(chain = true)
    private int depth;

    @Setter @Accessors(chain = true)
    private int width;

    @Setter @Accessors(chain = true)
    private float energy;

    @Setter @Accessors(chain = true)
    private float energyDistanceScale;

    @Setter @Accessors(chain = true)
    private float energyCostDistance;

    @Setter @Accessors(chain = true)
    private Direction direction; //TODO recode to be angle based

    /**
     * 1.12: {@code <VALUE, TAG extends NBTBase> void setValue(ActionField<VALUE, TAG> key, VALUE value)}.
     * 26.3 dropped the NBT tag type from {@link ActionField}, so the override follows the ported
     * {@link icbm.classic.api.actions.data.IActionFieldReceiver} signature. See
     * {@code ActionDataBase#setValue} for the shape the rest of the port uses.
     */
    @Override
    public <VALUE> void setValue(ActionField<VALUE> key, VALUE value) {
        if(key == ActionFields.HOST_DIRECTION) {
            direction = ActionFields.HOST_DIRECTION.cast(value);
        }
    }

    /**
     * 1.12 returned a raw {@code Collection<ActionField>}, the ported
     * {@link icbm.classic.api.actions.data.IActionFieldProvider} declares {@code Collection<ActionField<?>>}.
     */
    @Override
    @NonNull
    public Collection<ActionField<?>> getFields() {
        return SUPPORTED_FIELDS;
    }

    @Override
    protected void calculateDamage()
    {
        //Turn into normal TNT if invalid
        if (depth <= 0)
        {
            super.calculateDamage();
        }
        //TODO add some smoke and block particles for wow effect of a breaching a building
        else if (this.world instanceof ServerLevel serverLevel) //1.12: !this.world().isRemote
        {
            if(direction == null) {
                //Guess direction from entity rotation
                if (this.exploder != null) {
                    // TODO move this logic to projectile and provide via ActionField.HOST_DIRECTION
                    //1.12: exploder.rotationPitch, exploder instanceof EntityMissile (ported in S4, see isMissile)
                    if (this.exploder.getXRot() > 45) {
                        direction = isMissile(this.exploder) ? Direction.UP : Direction.DOWN;
                    } else if (this.exploder.getXRot() < -45) {
                        direction = isMissile(this.exploder) ? Direction.DOWN : Direction.UP;
                    } else {
                        //1.12: exploder.getAdjustedHorizontalFacing()
                        direction = this.exploder.getDirection();

                        // fixes explosion going backwards when the missile flies east or west.
                        if (direction == Direction.EAST || direction == Direction.WEST) {
                            direction = direction.getOpposite();
                        }
                    }
                }
                else {
                    direction = Direction.DOWN;
                }
            }

            //Loop with and height in direction
            for (int h = -width; h <= width; h++)
            {
                for (int w = -width; w <= width; w++)
                {
                    //Reset energy per line
                    float energyRemaining = this.energy - (this.energy * (h + w) * energyDistanceScale);

                    //TODO convert magic numbers into defined logic

                    //Loop depth
                    for (int depthIndex = 0; depthIndex < this.depth && energyRemaining > 0; depthIndex++)
                    {
                        //1.12: direction.getFrontOffsetX/Y/Z(), this.xi()/yi()/zi() come from IPos3D
                        int x = this.xi() + direction.getStepX() * depthIndex;
                        int y = this.yi() + direction.getStepY() * depthIndex;
                        int z = this.zi() + direction.getStepZ() * depthIndex;

                        if (direction == Direction.DOWN || direction == Direction.UP)
                        {
                            x += h;
                            z += w;
                        }
                        else if (direction == Direction.EAST || direction == Direction.WEST)
                        {
                            y += h;
                            z += w;
                        }
                        else if (direction == Direction.NORTH || direction == Direction.SOUTH)
                        {
                            y += h;
                            x += w;
                        }
                        else
                        {
                            return;
                        }

                        //Get block
                        final BlockPos pos = new BlockPos(x, y, z);
                        final BlockState state = serverLevel.getBlockState(pos); //1.12: world.getBlockState(pos)

                        if (!state.isAir()) //1.12: !block.isAir(state, world(), pos)
                        {
                            // Stop at unbreakable
                            //1.12: block.getBlockHardness(state, world, pos)
                            if(state.getDestroySpeed(serverLevel, pos) < 0) {
                                break;
                            }

                            //blockResistance=R*3.... aka hardness
                            //explosiveResistance=blockResistance / 5
                            //R = planks(5) -> 15 -> 3
                            //R = stone(10) -> 30 -> 10
                            //ICBM concrete -> 28
                            //ICBM compact concrete -> 380
                            //ICBM reinforced concrete -> 2800
                            //R = obby(2000) -> 6000 -> 1200
                            //R = unbreakable(6M) -> fuck that


                            //1.12: block.getExplosionResistance(world(), pos, this.exploder, this)
                            //26.3: Block#getExplosionResistance() no longer takes level/position/source
                            final float cost = state.getBlock().getExplosionResistance();
                            if (cost < energyRemaining)
                            {
                                energyRemaining -= cost;
                                getAffectedBlockPositions().add(pos);
                            }


                        }
                        else {
                            energyRemaining *= (1 - energyCostDistance);
                        }
                    }
                }
            }

            //Play some audio
            //1.12: SoundEvents.ENTITY_GENERIC_EXPLODE + world().rand
            this.world().playSound(null, location.x(), location.y(), location.z(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, (1.0F + (world().getRandom().nextFloat() - world().getRandom().nextFloat()) * 0.2F) * 0.7F);
        }
    }

    /**
     * 1.12: {@code this.exploder instanceof EntityMissile}.
     * <p>
     * TODO(S4) the missile entity ({@code icbm.classic.content.missile.entity.EntityMissile}) arrives in S4.
     * 1.12 only attached the missile data as a Forge capability, 26.3 has the missile entity implement
     * {@link IMissile} directly, so the interface check is the ported equivalent of the class check.
     */
    private static boolean isMissile(Entity entity)
    {
        return entity instanceof IMissile;
    }
}
