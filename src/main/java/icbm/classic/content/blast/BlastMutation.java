package icbm.classic.content.blast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Mutation blast, converts the living entities in range (pig → zombified piglin,
 * villager → zombie villager).
 * <p>
 * Port notes (porting-guide.md §2):
 * <ul>
 *     <li>{@code world.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB)} →
 *     {@code Level#getEntitiesOfClass(LivingEntity.class, AABB)}.</li>
 *     <li>The 1.12 code built the converted entity by hand ({@code new EntityPigZombie(world)} +
 *     {@code setPosition} + {@code setDead} + {@code world.spawnEntity}); 26.3 has the vanilla conversion
 *     ({@code Mob#convertTo(EntityType, ConversionParams, AfterConversion)}) which does the same steps and
 *     spawns the new mob. {@code preventEntitySpawning = true} maps to {@code Mob#setPersistenceRequired()}
 *     (the flag the vanilla conversions set), {@code setForgeProfession(...)} maps to
 *     {@code ZombieVillager#setVillagerData(VillagerData)}. The vanilla villager conversion also plays the
 *     zombie infect level event, that call is left out here to keep the "sound disabled" behaviour of this
 *     explosive (see {@link #playExplodeSound()}).</li>
 * </ul>
 */
public class BlastMutation extends Blast
{
    @Override
    public boolean doExplode(int callCount)
    {
        if (this.world instanceof ServerLevel serverLevel) //1.12: !this.world().isRemote
        {
            //1.12: new AxisAlignedBB(location ± getBlastRadius())
            final AABB bounds = new AABB(location.x() - this.getBlastRadius(), location.y() - this.getBlastRadius(), location.z() - this.getBlastRadius(), location.x() + this.getBlastRadius(), location.y() + this.getBlastRadius(), location.z() + this.getBlastRadius());
            //1.12: world().getEntitiesWithinAABB(EntityLiving.class, bounds)
            final List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(LivingEntity.class, bounds);

            for (LivingEntity entity : entitiesNearby)
            {
                applyMutationEffect(entity);
            }
        }
        return false;
    }

    /**
     * 1.12: {@code applyMutationEffect(final EntityLivingBase entity)}.
     *
     * @param entity to convert, does nothing when the entity has no mutation
     * @return true if the entity was converted
     */
    public static boolean applyMutationEffect(final LivingEntity entity)
    {
        if (entity instanceof Pig pig)
        {
            //1.12: new EntityPigZombie(entity.world) + preventEntitySpawning + setPosition + entity.setDead() + spawnEntity
            final ZombifiedPiglin newEntity = pig.convertTo(EntityTypes.ZOMBIFIED_PIGLIN, ConversionParams.single(pig, false, true), ZombifiedPiglin::setPersistenceRequired);
            return newEntity != null;
        }
        else if (entity instanceof Villager villager)
        {
            //1.12: new EntityZombieVillager(entity.world) + preventEntitySpawning + setPosition
            //      + newEntity.setForgeProfession(villager.getProfessionForge()) + entity.setDead() + spawnEntity
            final ZombieVillager newEntity = villager.convertTo(EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(villager, true, true), zombie ->
            {
                zombie.setVillagerData(villager.getVillagerData()); //1.12: setForgeProfession(...)
                zombie.setPersistenceRequired(); //1.12: preventEntitySpawning = true
            });
            return newEntity != null;
        }
        return false;
    }

    @Override //disable the sound for this explosive
    protected void playExplodeSound()
    {
    }
}
