package icbm.classic.content.entity;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.explosion.IBlastMovable;
import icbm.classic.api.explosion.IBlastRestore;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.ConfigDebug;
import icbm.classic.content.blast.Blast;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.saving.SaveNode;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The Entity handler responsible for entity explosions.
 * <p>
 * Registered as {@code icbmclassic:explosion_holder} ({@code ModEntities.EXPLOSION_HOLDER}), see
 * registry-ids.md §4. 1.12 spawned this entity from {@code Blast#doAction} for every blast that ticks so
 * the blast could live in the world (movable blasts such as redmatter are pushed around through it).
 * <p>
 * Porting notes (porting-guide §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone; the explosive registry key and the blast y offset
 *     are sent as {@link SynchedEntityData} values instead (see {@link #DATA_EXPLOSIVE_ID} and
 *     {@link #DATA_BLAST_Y_OFFSET}). 1.12 rebuilt a client copy of the blast from that spawn data and ran
 *     {@code Blast#clientRunBlast}; client side blast construction is dropped in 26.3 (blast effects are
 *     server driven), so the client copy reaches this class without a blast and the legacy guard in
 *     {@link #tick()} drops it on its first client tick. The synced values stay available while it lives.</li>
 *     <li>{@code preventEntitySpawning} has no 26.3 counterpart (entity spawning is data driven, there is
 *     no entity flag) and {@code ignoreFrustumCheck} is a renderer concern now handled by
 *     {@code EntityRenderer#shouldRender} (renderer is S4.07), so both are dropped.</li>
 *     <li>{@code noClip} is {@link Entity#noPhysics} and {@code setSize(0.98F, 0.98F)} moved to
 *     {@code EntityType.Builder#sized} (see {@code ModEntities}). {@code canBeCollidedWith()} maps to
 *     {@link #isPickable()} (kept as an explicit override to mirror 1.12, it is the 26.3 default) and
 *     {@code canTriggerWalking()} no longer exists (blocks decide trampling themselves).</li>
 * </ul>
 *
 * @author Calclavia
 */
@Deprecated //TODO replace all usage with more focused entities per explosive
public class EntityExplosion extends Entity
{
    /**
     * 1.12 sent {@code blast.getActionData().getRegistryKey()} through
     * {@code IEntityAdditionalSpawnData#writeSpawnData}; 26.3 has no extra spawn data so it is synced.
     */
    private static final EntityDataAccessor<String> DATA_EXPLOSIVE_ID = SynchedEntityData.defineId(EntityExplosion.class, EntityDataSerializers.STRING);

    /**
     * The {@code blastYOffset} that 1.12 sent next to the registry key, as a double.
     * <p>
     * 26.3's {@link EntityDataSerializers} has no DOUBLE entry, so the closest faithful mapping is FLOAT.
     * The value is only ever a block offset ({@code blast y + 1}, or {@code 0} for movable blasts) and is
     * consumed as an offset of a block position, so the lost tail of the double is not observable.
     */
    private static final EntityDataAccessor<Float> DATA_BLAST_Y_OFFSET = SynchedEntityData.defineId(EntityExplosion.class, EntityDataSerializers.FLOAT);

    @Getter
    private IAction blast;

    public EntityExplosion(EntityType<EntityExplosion> type, Level level)
    {
        super(type, level);
        // 1.12 set noClip, see the class javadoc for preventEntitySpawning/ignoreFrustumCheck/setSize
        this.noPhysics = true;
        // 1.12 set ticksExisted = 0, which is the 26.3 default (tickCount) of a fresh entity
    }

    /** Called by {@link Blast#doAction()} to spawn a ticking holder for the blast (1.12 {@code EntityExplosion(Blast)}). */
    public EntityExplosion(Level level, Blast blast)
    {
        this(ModEntities.EXPLOSION_HOLDER, level);
        this.setBlast(blast);
        if (ConfigDebug.DEBUG_EXPLOSIVES)
        {
            ICBMClassic.logger().info("EntityExplosion#new({}) Created new blast controller entity", blast);
        }
    }

    /** 1.12 returned "Explosion[blast]" as a raw string; 26.3 entity names are components (debug label, not player facing). */
    @Override
    public Component getName()
    {
        return Component.literal("Explosion[" + blast + "]");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add
        builder.define(DATA_EXPLOSIVE_ID, "");
        builder.define(DATA_BLAST_Y_OFFSET, 0F);
    }

    /**
     * 1.12 inherited {@code Entity#attackEntityFrom}; the holder has no health and can not be collided with,
     * so it never took damage. 26.3 requires the override as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     * <p>
     * 1.12 {@code canBeCollidedWith()} returned false; that is the default of {@link #isPickable()} in 26.3.
     */
    @Override
    public boolean isPickable()
    {
        return false;
    }

    /** Called to update the entity's position/logic. */
    @Override
    public void tick()
    {
        // 1.12 EntityExplosion#onUpdate never called super.onUpdate(). Entity#tick is only Entity#baseTick
        // (fire, fluid, portal, leash, below world), so it is skipped for the same reason.

        if (!(this.getBlast() instanceof IBlastTickable) || ((IBlastTickable) this.getBlast()).getEntity() != this || ((IBlastTickable) this.getBlast()).isCompleted())
        {
            this.discard();
            return;
        }

        final Vec3 motion = this.getDeltaMovement();
        if (this.getBlast() instanceof IBlastMovable && (motion.x != 0 || motion.y != 0 || motion.z != 0))
        {
            //Slow entity down
            double motionX = motion.x * .98D;
            double motionY = motion.y * .98D;
            double motionZ = motion.z * .98D;

            //Normalize (1.12 called MathHelper.sqrt(double); 26.3 only has Mth.sqrt(float))
            float speed = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
            motionX /= speed;
            motionY /= speed;
            motionZ /= speed;

            //Apply Speed
            speed = Math.min(speed, 0.5f);
            motionX *= speed;
            motionY *= speed;
            motionZ *= speed;

            //Write the decayed/normalized motion back (1.12 kept it in the motionX/Y/Z fields)
            this.setDeltaMovement(motionX, motionY, motionZ);

            //Move box
            this.setBoundingBox(this.getBoundingBox().move(motionX, motionY, motionZ));

            //Reset position based on box. setPos would rebuild the box from the new position, which would
            //repeat the half height offset of the box center every tick, so only the position is written.
            final Vec3 center = this.getBoundingBox().getCenter();
            this.setPosRaw(center.x, center.y, center.z);

            //Update blast
            ((IBlastMovable) this.getBlast()).onPositionUpdate(center.x, center.y + this.getBlastYOffset(), center.z);
        }

        // 1.12 passed ticksExisted, which is tickCount in 26.3 (increased before the entity is ticked)
        if (this.getBlast() instanceof IBlastTickable && ((IBlastTickable) this.getBlast()).onBlastTick(this.tickCount))
        {
            this.discard();
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement)
    {
        //Remove default movement
    }

    /** (abstract) Protected helper method to read subclass entity data from save. */
    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        // 1.12 wrapped the whole restore in a try/catch and only logged the error, keeping the entity alive
        try
        {
            SAVE_LOGIC.load(this, in);
        }
        catch (Exception e)
        {
            ICBMClassic.logger().error("EntityExplosion: Unexpected error restoring save state of explosion entity!", e);
        }
    }

    /** (abstract) Protected helper method to write subclass entity data to save. */
    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    /**
     * 1.12 wrote the blast's own save data ({@link IBlastRestore#save(ValueOutput)}) into a {@code blast}
     * compound and read it back after the y offset: 1) read the id, 2) build the blast with it,
     * 3) let the blast restore itself. {@link #SAVE_LOGIC} keeps that order (a declarative sub root would
     * be loaded before the main root and build the blast with an unread offset).
     */
    private static class SaveBlast extends SaveNode<EntityExplosion>
    {
        SaveBlast()
        {
            super(NBTConstants.BLAST);
        }

        @Override
        public void save(EntityExplosion objectToSave, ValueOutput out)
        {
            if (objectToSave.getBlast() != null) //TODO add save/load mechanic to bypass need for ex data
            {
                //Save explosive data
                final ValueOutput blastSave = out.child(getSaveKey());
                if (objectToSave.getBlast() instanceof IBlastRestore)
                {
                    ((IBlastRestore) objectToSave.getBlast()).save(blastSave);
                }
                blastSave.putString(NBTConstants.EX_ID, objectToSave.getBlast().getActionData().getRegistryKey().toString());
            }
        }

        @Override
        public void load(EntityExplosion objectToLoad, ValueInput in)
        {
            in.child(getSaveKey()).ifPresent(blastSave ->
            {
                if (objectToLoad.getBlast() == null)
                {
                    final Optional<String> exId = blastSave.getString(NBTConstants.EX_ID);
                    if (exId.isPresent())
                    {
                        objectToLoad.constructBlast(exId.get(), objectToLoad.getBlastYOffset());
                    }
                    else
                    {
                        ICBMClassic.logger().error("EntityExplosion: Failed to read save state for explosion!");
                        objectToLoad.discard();
                    }
                }

                if (objectToLoad.getBlast() instanceof IBlastRestore)
                {
                    ((IBlastRestore) objectToLoad.getBlast()).load(blastSave);
                }
            });
        }
    }

    private static final SaveHandler<EntityExplosion> SAVE_LOGIC = new SaveHandler<EntityExplosion>()
        .mainRoot()
        // 1.12 only wrote this (and the blast compound) when the entity had a blast, so answer null then
        /* */.nodeDouble(NBTConstants.BLAST_POS_Y, ex -> ex.getBlast() != null ? ex.getBlastYOffset() : null, EntityExplosion::setBlastYOffset)
        /* */.node(new SaveBlast())
        .base();

    public void setBlast(Blast blast)
    {
        this.blast = blast;
        if (blast != null)
        {
            blast.setEntityController(this);
            this.setPos(blast.location.x(), !blast.isMovable() ? -1 : blast.y(), blast.location.z());
            this.setBlastYOffset(blast.isMovable() ? 0 : blast.y() + 1);
            syncExplosiveId(blast);
        }
    }

    /**
     * Pushes the explosive registry key that 1.12 sent as spawn data into {@link #DATA_EXPLOSIVE_ID}.
     * <p>
     * 1.12 only read the action data when writing the spawn/save data, and {@code Blast#getActionData()}
     * is only filled by {@code IExplosiveData#create} (a hand built blast has none), so the sync is
     * skipped instead of failing while the holder is built.
     */
    private void syncExplosiveId(IAction blast)
    {
        final IActionData actionData = blast.getActionData();
        if (actionData != null)
        {
            this.entityData.set(DATA_EXPLOSIVE_ID, actionData.getRegistryKey().toString());
        }
    }

    /** The offset applied to the entity's y position to get the blast's y position, 1.12 sent it with the spawn data. */
    public double getBlastYOffset()
    {
        return this.entityData.get(DATA_BLAST_Y_OFFSET);
    }

    private void setBlastYOffset(double blastYOffset)
    {
        this.entityData.set(DATA_BLAST_Y_OFFSET, (float) blastYOffset);
    }

    /** Registry key of the explosive the holder runs, see {@link #DATA_EXPLOSIVE_ID}. */
    public String getExplosiveId()
    {
        return this.entityData.get(DATA_EXPLOSIVE_ID);
    }

    /**
     * Constructs a blast based on the parameters and sets the blast field to that value
     */
    private void constructBlast(String exId, double yOffset)
    {
        final Identifier id = Identifier.parse(exId);
        final IExplosiveData exData = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(id, true);

        if (exData == null)
        {
            ICBMClassic.logger().error("EntityExplosion: Failed to locate explosive with id '{}'!", id);
            this.discard();
            return;
        }

        final double posX = this.getX();
        final double posY = this.getY();
        final double posZ = this.getZ();
        //TODO(S4) use content/missile/logic/source/cause/EntityCause(this) once it is ported (registered in S4);
        // the legacy cause carried this entity + TODO provide additional cause information such as fire, lighter, player, etc
        final ActionSource actionSource = new ActionSource(level(), new Vec3(posX, posY + yOffset, posZ), null);
        IAction blast = exData.create(level(), posX, posY + yOffset, posZ, actionSource, null);

        if (blast instanceof IBlastInit)
        {
            ((IBlastInit) blast).setEntityController(this);
            blast = ((IBlastInit) blast).buildBlast();
        }

        this.blast = blast;
        if (blast != null)
        {
            syncExplosiveId(blast);
        }
    }
}
