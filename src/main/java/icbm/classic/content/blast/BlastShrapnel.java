package icbm.classic.content.blast;

import icbm.classic.content.entity.EntityFragments;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Spawns a ring of {@link EntityFragments} on the 6 faces of the blast cube, used by the shrapnel, anvil and
 * fragmentation explosives (the fragment type itself is set by the supplier from {@code ExplosiveInit}).
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code Function<World, EntityFragments>} → {@code Function<Level, EntityFragments>} and
 *     {@code world().isRemote} → the level being a {@link ServerLevel}.</li>
 *     <li>{@code setLocationAndAngles(x, y, z, yaw, pitch)} → {@code Entity#setPos(double, double, double)}
 *     plus {@code setYRot}/{@code setXRot} and the previous rotation fields, which is what the 1.12 method
 *     did (and what the interpolation of the first tick reads).</li>
 *     <li>{@code fragment.posX/posY/posZ} writes → collected into locals and applied with
 *     {@code setPos(double, double, double)}, {@code fragment.motionX/Y/Z} writes →
 *     {@code Entity#setDeltaMovement(double, double, double)}.</li>
 *     <li>{@code MathHelper} → {@code Mth}, {@code world().rand} → {@code Level#getRandom()} and
 *     {@code world().spawnEntity(fragment)} → {@code Level#addFreshEntity(Entity)}.</li>
 * </ul>
 */
public class BlastShrapnel extends Blast
{
    @Setter @Accessors(chain = true)
    private Function<Level, EntityFragments> projectile;

    @Override
    public boolean doExplode(int callCount)
    {
        if (this.world instanceof ServerLevel serverLevel) //1.12: !world().isRemote
        {
            float rotationStep = 360 / this.getBlastRadius();

            // TODO add logic to detect when rotation is blocked by ground and to avoid spawning fragments
            for (int yawIndex = 0; yawIndex < this.getBlastRadius(); yawIndex++)
            {
                // Try to do a 360 explosion on all 6 faces of the cube.
                float rotationYaw = 0.0F + rotationStep * yawIndex;

                // TODO randomize position, velocity y rotation to create a more realistic blast fragmentation effect
                for (int pitchIndex = 0; pitchIndex < this.getBlastRadius(); pitchIndex++)
                {
                    final EntityFragments fragment = projectile.apply(serverLevel);

                    float rotationPitch = 0.0F + rotationStep * pitchIndex;

                    //1.12: fragment.setLocationAndAngles(location.x(), Math.floor(location.y()) + 1.5, location.z(), rotationYaw, rotationPitch)
                    //      which set the position and copied the rotation into the previous rotation fields
                    fragment.setPos(location.x(), Mth.floor(location.y()) + 1.5, location.z()); //TODO fix y-pos to not offset by 1.5
                    fragment.setYRot(rotationYaw);
                    fragment.setXRot(rotationPitch);
                    fragment.yRotO = rotationYaw;
                    fragment.xRotO = rotationPitch;

                    //1.12: fragment.posX -= (MathHelper.cos(rotationYaw / 180.0F * (float) Math.PI) * 0.16F)
                    //      fragment.posY -= 0.10000000149011612D
                    //      fragment.posZ -= (MathHelper.sin(rotationYaw / 180.0F * (float) Math.PI) * 0.16F)
                    //      fragment.setPosition(fragment.posX, fragment.posY, fragment.posZ)
                    final double posX = fragment.getX() - (Mth.cos(rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
                    final double posY = fragment.getY() - 0.10000000149011612D; //TODO figure out why magic number
                    final double posZ = fragment.getZ() - (Mth.sin(rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
                    fragment.setPos(posX, posY, posZ);

                    //1.12: fragment.motionX = (-MathHelper.sin(rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI))
                    //      fragment.motionZ = (MathHelper.cos(rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI))
                    //      fragment.motionY = (-MathHelper.sin(rotationPitch / 180.0F * (float) Math.PI))
                    fragment.setDeltaMovement(
                            -Mth.sin(rotationYaw / 180.0F * (float) Math.PI) * Mth.cos(rotationPitch / 180.0F * (float) Math.PI),
                            -Mth.sin(rotationPitch / 180.0F * (float) Math.PI),
                            Mth.cos(rotationYaw / 180.0F * (float) Math.PI) * Mth.cos(rotationPitch / 180.0F * (float) Math.PI));

                    //1.12: fragment.setArrowHeading(fragment.motionX * world().rand.nextFloat(), fragment.motionY * world().rand.nextFloat(), fragment.motionZ * world().rand.nextFloat(), 0.5f + (0.7f * world().rand.nextFloat()), 1.0F)
                    final Vec3 motion = fragment.getDeltaMovement();
                    fragment.setArrowHeading(motion.x * serverLevel.getRandom().nextFloat(), motion.y * serverLevel.getRandom().nextFloat(), motion.z * serverLevel.getRandom().nextFloat(), 0.5f + (0.7f * serverLevel.getRandom().nextFloat()), 1.0F);

                    serverLevel.addFreshEntity(fragment); //1.12: world().spawnEntity(fragment)
                }
            }
        }
        return false;
    }
}
