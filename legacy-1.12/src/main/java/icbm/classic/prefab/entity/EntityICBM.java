package icbm.classic.prefab.entity;

import icbm.classic.api.data.IWorldPosition;
import icbm.classic.lib.saving.NbtSaveHandler;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Base entity class to be shared by most entities
 * Created by Robin on 1/24/2015.
 */
public abstract class EntityICBM extends Entity implements IWorldPosition
{
    /** Does the entity have HP to take damage. */
    protected boolean hasHealth = false;

    private static final DataParameter<Float> HEALTH = EntityDataManager.<Float>createKey(EntityICBM.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> MAX_HEALTH = EntityDataManager.<Float>createKey(EntityICBM.class, DataSerializers.FLOAT);

    public EntityICBM(World world)
    {
        super(world);
    }

    @Override
    protected void entityInit()
    {
        this.dataManager.register(HEALTH, 1f);
        this.dataManager.register(MAX_HEALTH, 1f);
    }

    public float getHealth()
    {
        return this.dataManager.get(HEALTH);
    }

    public <T> T setHealth(float health)
    {
        this.dataManager.set(HEALTH, MathHelper.clamp(health, 0.0F, this.getMaxHealth()));
        return (T) this;
    }

    public float getMaxHealth()
    {
        return this.dataManager.get(MAX_HEALTH);
    }

    public <T> T  setMaxHealth(float hp) {
        this.dataManager.set(MAX_HEALTH, hp);
        return (T) this;
    }

    public <T> T  initHealth(float hp) {
        this.setMaxHealth(hp);
        this.setHealth(hp);
        return (T) this;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage)
    {
        if (hasHealth && damage > 0 && canDamage(this, source))
        {
            this.setHealth(Math.max(getHealth() - damage, 0));
            if (getHealth() <= 0)
            {
                onDestroyedBy(source, damage);
            }
            return true;
        }
        return false;
    }

    /**
     * Can damage be applied at all to the entity
     *
     * @param entity - entity being attacked
     * @return true if the entity can be damaged
     */
    public boolean canDamage(Entity entity, DamageSource source)
    {
        if (!entity.isEntityInvulnerable(source) && entity.isEntityAlive())
        {
            if (entity instanceof EntityLivingBase)
            {
                if (entity instanceof EntityPlayer)
                {
                    if (((EntityPlayer) entity).capabilities.isCreativeMode)
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }


    /**
     * Called when the entity is killed
     */
    protected void onDestroyedBy(DamageSource source, float damage)
    {
       this.destroy();
    }

    /**
     * Called when the entity expires or
     * is destroyed in some way.
     */
    protected void destroy() {
        this.setDead();
    }

    /**
     * Sets the position based on the bounding box
     */
    protected void alignToBounds()
    {
        this.posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0D;
        this.posY = this.getEntityBoundingBox().minY + (double) this.getYOffset() - (double) this.height;
        this.posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0D;
    }

    /**
     * Gets the predicted position
     *
     * @param t - number of ticks to predicted
     * @return predicted position of the project
     */
    public Vec3d getPredictedPosition(int t)
    {
        return new Vec3d(posX + motionX * t, posY + motionY * t, posZ + motionZ * t);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt)
    {
        SAVE_LOGIC.load(this, nbt);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt)
    {
        SAVE_LOGIC.save(this, nbt);
    }

    private static final NbtSaveHandler<EntityICBM> SAVE_LOGIC = new NbtSaveHandler<EntityICBM>()
        .mainRoot()
        /* */.nodeFloat("health", EntityICBM::getHealth, EntityICBM::setHealth)
        /* */.nodeFloat("health_max", EntityICBM::getMaxHealth, EntityICBM::setMaxHealth)
        .base();

    @Override
    public World world()
    {
        return world;
    }

    @Override
    public double x()
    {
        return posX;
    }

    @Override
    public double y()
    {
        return posY;
    }

    @Override
    public double z()
    {
        return posZ;
    }

    public Pos getVelocity()
    {
        return new Pos(motionX, motionY, motionZ); //TODO make wrapper object
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setVelocity(double xx, double yy, double zz) {
        //ICBMClassic.logger().info("Projectile#setVelocity: {} {} {} from {}", xx, yy, zz, Thread.currentThread().getStackTrace()[2]);

        // Client side only gets 5 decimal places due to packet storing as int then converting back to double using divide by 8000... effectively a float
        setMotionVector(xx, yy, zz);
    }

    public void setMotionVector(double xx, double yy, double zz) {
        this.motionX = xx;
        this.motionY = yy;
        this.motionZ = zz;
    }
}
