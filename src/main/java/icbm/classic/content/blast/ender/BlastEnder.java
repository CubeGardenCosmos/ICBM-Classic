package icbm.classic.content.blast.ender;

import icbm.classic.ICBMClassic;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.content.blast.Blast;
import icbm.classic.lib.transform.vector.Location;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Ender explosive, pulls the entities in range towards the center, then teleports everything inside of the
 * last 4 blocks to the target set by {@link EnderBlastCustomization}.
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>1.12 built the portals, the teleport sound and the huge explosion particle on the client
 *     ({@code world().isRemote}) because the client kept its own copy of the blast through
 *     {@code EntityExplosion}. 26.3 dropped client side blast construction (blast effects are server
 *     driven), so the same particles are sent from the server with {@code ServerLevel#sendParticles} and
 *     {@code ParticleTypes.PORTAL} (1.12 {@code EnumParticleTypes.PORTAL}) /
 *     {@code ParticleTypes.EXPLOSION_EMITTER} (1.12 {@code EnumParticleTypes.EXPLOSION_HUGE}).</li>
 *     <li>{@code world().rand} → {@code Level#getRandom()}, {@code entity.posX/Y/Z} → {@code Entity#getX/Y/Z},
 *     {@code entity.motionX/Y/Z} writes → {@code Entity#setDeltaMovement(double, double, double)},
 *     {@code world().isAirBlock(pos)} → {@code LevelReader#isEmptyBlock(pos)} and {@code pos.up()} →
 *     {@code BlockPos#above()}.</li>
 *     <li>{@code Entity#setPosition}/{@code EntityPlayerMP#connection.setPlayerLocation} → a single
 *     {@code Entity#teleport(TeleportTransition)}, see the teleport comment in {@link #doExplode(int)}.</li>
 *     <li>{@code new EntityEnderman(world())} → {@code new Enderman(EntityTypes.ENDERMAN, world())} and
 *     {@code world().spawnEntity(entity)} → {@code Level#addFreshEntity(entity)}.</li>
 * </ul>
 */
public class BlastEnder extends Blast implements IBlastTickable //TODO handle save/load
{
    public int duration = 20 * 8;

    /** Target the entities are teleported to, null until a target is set or the random search found one */
    @Nullable
    private Vec3 teleportTarget;

    @Nullable
    public Vec3 getTeleportTarget()
    {
        return teleportTarget;
    }

    public void setTeleportTarget(Vec3 teleportTarget)
    {
        this.teleportTarget = teleportTarget;
    }

    @Override
    public boolean doExplode(int callCount) //TODO break into smaller methods
    {
        // 1.12 only ran the portal particles on the client (world().isRemote) because the client built its own
        // copy of the blast. 26.3 has no client side blast (effects are server driven), so the same portals are
        // sent from the server below instead.
        if (this.world instanceof ServerLevel serverLevel)
        {
            int r = (int) (this.getBlastRadius() - ((double) this.callCount / (double) this.duration) * this.getBlastRadius());

            for (int x = -r; x < r; x++)
            {
                for (int z = -r; z < r; z++)
                {
                    for (int y = -r; y < r; y++)
                    {
                        Location targetPosition = location.add(new Pos(x, y, z)); //TODO replace with mutable blockpos

                        double distance = targetPosition.distance(location);

                        if (distance < r && distance > r - 1)
                        {
                            //1.12: !targetPosition.getBlock(world()).isAir(targetPosition.getBlockState(world()), world(), targetPosition.toBlockPos())
                            if (!targetPosition.getBlockState(world()).isAir())
                            {
                                continue;
                            }

                            if (this.world().getRandom().nextFloat() < Math.max(0.001 * r, 0.01))
                            {
                                float velX = (float) ((targetPosition.x() - location.x()) * 0.6);
                                float velY = (float) ((targetPosition.y() - location.y()) * 0.6);
                                float velZ = (float) ((targetPosition.z() - location.z()) * 0.6);

                                //1.12: world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, velX, velY, velZ)
                                //26.3: a count of 0 spawns one particle at the exact position and uses the three
                                //spread distances (1) as the per axis speed multiplier, see BlastNuclear
                                serverLevel.sendParticles(ParticleTypes.PORTAL, targetPosition.x(), targetPosition.y(), targetPosition.z(), 0,
                                        1.0D, 1.0D, 1.0D,
                                        velX, velY, velZ);
                            }
                        }
                    }
                }
            }
        }

        // 1.12 ran this half on the client and the server, the port keeps its own ServerLevel guard because
        // the teleport below needs one (blasts only tick on the server, see Blast#onBlastTick)
        if (this.world instanceof ServerLevel serverLevel)
        {
            int radius = (int) this.getBlastRadius();
            AABB bounds = new AABB(location.x() - radius, location.y() - radius, location.z() - radius, location.x() + radius, location.y() + radius, location.z() + radius); //1.12: new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
            List<Entity> allEntities = world().getEntitiesOfClass(Entity.class, bounds); //1.12: world().getEntitiesWithinAABB(Entity.class, bounds)
            boolean explosionCreated = false;

            for (Entity entity : allEntities)
            {
                if (entity != this.controller)
                {
                    double xDifference = entity.getX() - location.x(); //1.12: entity.posX
                    double yDifference = entity.getY() - location.y();
                    double zDifference = entity.getZ() - location.z();

                    int r = (int) this.getBlastRadius();
                    if (xDifference < 0)
                    {
                        r = (int) -this.getBlastRadius();
                    }

                    double motionX = entity.getDeltaMovement().x - (r - xDifference) * Math.abs(xDifference) * 0.0006; //1.12: entity.motionX -=

                    r = (int) this.getBlastRadius();
                    if (entity.getY() > location.y()) //1.12: entity.posY > location.y()
                    {
                        r = (int) -this.getBlastRadius();
                    }
                    double motionY = entity.getDeltaMovement().y + (r - yDifference) * Math.abs(yDifference) * 0.0011; //1.12: entity.motionY +=

                    r = (int) this.getBlastRadius();
                    if (zDifference < 0)
                    {
                        r = (int) -this.getBlastRadius();
                    }

                    double motionZ = entity.getDeltaMovement().z - (r - zDifference) * Math.abs(zDifference) * 0.0006; //1.12: entity.motionZ -=

                    //1.12 wrote the three motion fields, 26.3 has to write them back through the entity
                    entity.setDeltaMovement(motionX, motionY, motionZ);

                    if (new Pos(entity.getX(), entity.getY(), entity.getZ()).distance(location) < 4)
                    {
                        if (!explosionCreated && callCount % 5 == 0)
                        {
                            //1.12: world().spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, entity.posX, entity.posY, entity.posZ, 0.0D, 0.0D, 0.0D)
                            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, entity.getX(), entity.getY(), entity.getZ(), 0,
                                    1.0D, 1.0D, 1.0D,
                                    0.0D, 0.0D, 0.0D);
                            explosionCreated = true;
                        }

                        try
                        {
                            // If a target doesn't exist, search for a random one within 100 block range
                            if (this.teleportTarget == null)
                            {
                                int checkY = (int) Math.floor(this.controller.getY()); //1.12: (int) Math.floor(this.controller.posY)
                                int checkX = this.world().getRandom().nextInt(300) - 150 + (int) this.controller.getX(); //1.12: this.world().rand.nextInt(300)
                                int checkZ = this.world().getRandom().nextInt(300) - 150 + (int) this.controller.getZ();

                                //Look for space with air gap
                                BlockPos pos;
                                BlockPos pos2;
                                do
                                {
                                    pos = new BlockPos(checkX, checkY, checkZ);
                                    pos2 = pos.above(); //1.12: pos.up()
                                    checkY++;
                                }
                                while (this.world().isEmptyBlock(pos) && !this.world().isEmptyBlock(pos2) && checkY < 254); //1.12: world().isAirBlock(...)

                                this.teleportTarget = new Vec3(checkX + 0.5, checkY + 0.5, checkZ + 0.5);
                            }

                            //1.12: SoundEvents.ENTITY_ENDERMEN_TELEPORT
                            this.world().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F); //1.12: entity.posX, entity.posY, entity.posZ

                            // 1.12 had two paths: players went through
                            // ((EntityPlayerMP) entity).connection.setPlayerLocation(x, y, z, yaw, pitch) and every
                            // other entity through entity.setPosition(x, y, z) (which never changed dimension, the
                            // dimension of the customization is only shown in the tooltip). 26.3 has one absolute
                            // teleport, Entity#teleport(TeleportTransition): a ServerPlayer lands in
                            // ServerPlayer#teleport which for the same dimension calls
                            // ServerConnection#teleport(PositionMoveRotation, relatives) + resetPosition (the 26.3
                            // form of setPlayerLocation), every other entity has its position snapped and a
                            // teleport packet sent to the tracking players. The transition keeps the current
                            // rotation and delta movement and uses no relative flags, so the move is absolute like
                            // 1.12 and TeleportTransition.create(...) (a player respawn helper) does not apply.
                            entity.teleport(new TeleportTransition(
                                    serverLevel,
                                    this.teleportTarget,
                                    entity.getDeltaMovement(),
                                    entity.getYRot(),
                                    entity.getXRot(),
                                    Set.of(),
                                    TeleportTransition.DO_NOTHING));
                        }
                        catch (Exception e)
                        {
                            ICBMClassic.logger().error("Failed to teleport entity to the End.", e);
                        }
                    }
                }
            }
        }

        //1.12: SoundEvents.BLOCK_PORTAL_AMBIENT + world().rand
        this.world().playSound(null, this.location.x(), this.location.y(), this.location.z(), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 2F, world().getRandom().nextFloat() * 0.4F + 0.8F);

        return this.callCount > this.duration;
    }

    @Override
    public void onBlastCompleted()
    {
        super.onBlastCompleted();

        if (!this.world().isClientSide()) //1.12: !this.world().isRemote
        {
            for (int i = 0; i < 8; i++) //TODO check for safe location to spawn
            {
                final Enderman enderman = new Enderman(EntityTypes.ENDERMAN, this.world()); //1.12: new EntityEnderman(world())
                enderman.setPos(this.location.x(), this.location.y(), this.location.z()); //1.12: enderman.setPosition(...)
                this.world().addFreshEntity(enderman); //1.12: this.world().spawnEntity(enderman)
            }
        }
    }

    @Override
    public float getBlastRadius()
    {
        return 20;
    }

    @Override
    public boolean isMovable()
    {
        return true;
    }
}
