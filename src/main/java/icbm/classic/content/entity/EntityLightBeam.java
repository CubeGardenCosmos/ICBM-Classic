package icbm.classic.content.entity;

import com.builtbroken.jlib.data.vector.IPos3D;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Beam shot into the sky by the beam style blasts (1.12 {@code beam.light} → 26.3
 * {@code icbmclassic:light_beam}, {@link ModEntities#LIGHT_BEAM}).
 * <p>
 * The beam is only a visual, the blast drives it through {@link #setTargetBeamProgress(float)},
 * {@link #setActualBeamProgress(float)} and {@link #startDeathCycle()} while the client slowly grows or
 * shrinks the rendered radius towards the synced progress.
 * <p>
 * Porting notes (porting-guide.md §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone; the colour and size values 1.12 sent with
 *     {@code writeSpawnData} are {@link SynchedEntityData} values written by
 *     {@link #updateDataBeforeSync()}, mirroring the vanilla hook used for data that has to be correct in
 *     the spawn packet. The 1.12 {@code DataParameter<Float> BEAM_PROGRESS} stays synced, which keeps the
 *     client side animation identical.</li>
 *     <li>{@code onUpdate} → {@link #tick()}, {@code setDead()} → {@link #discard()} and
 *     {@code ticksExisted} → {@link #tickCount}. 1.12 never called {@code super.onUpdate()}, so
 *     {@link Entity#tick()} is not called here either.</li>
 *     <li>{@code preventEntitySpawning} has no 26.3 counterpart (spawning is data driven) and
 *     {@code ignoreFrustumCheck}/{@code getRenderBoundingBox()} are renderer concerns in 26.3
 *     ({@code EntityRenderer#shouldRender}, S4.07), so both are dropped.</li>
 *     <li>{@code setSize(1F, 1F)} moved to {@code EntityType.Builder#sized}; {@code canBeCollidedWith()}
 *     maps to {@link #isPickable()} and {@code canBePushed()} to {@link #isPushable()};
 *     {@code canTriggerWalking()} no longer exists (blocks decide trampling themselves).</li>
 *     <li>1.12 {@code readEntityFromNBT}/{@code writeEntityToNBT} were empty, so a beam lost its render
 *     values on a world reload. 26.3 entity saves do not carry {@link SynchedEntityData} either, so the
 *     legacy fields are stored by {@link #SAVE_LOGIC} under their field names and restored into the same
 *     fields, which are the ones pushed to the client.</li>
 * </ul>
 *
 * @author Calclavia
 */
public class EntityLightBeam extends Entity
{
    //Render color
    public float red = 1;
    public float green = 0;
    public float blue = 0;

    //Render Size
    public float beamSize = 0.5f;
    public float beamGlowSize = 1f;

    //Client state
    public float clientBeamProgress = 0;

    //State
    public boolean deathCycle = false;
    public float targetBeamProgress = 1;
    public float beamGrowthRate = 0.05f;

    //Data
    /**
     * 1.12 registered this with {@code -1F} in {@code entityInit}. The {@code DATA_} accessors hold the
     * values 1.12 sent as spawn data ({@code writeSpawnData}/{@code readSpawnData}).
     */
    private static final EntityDataAccessor<Float> BEAM_PROGRESS = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RED = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GREEN = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BLUE = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_SIZE = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_GLOW_SIZE = SynchedEntityData.defineId(EntityLightBeam.class, EntityDataSerializers.FLOAT);

    public EntityLightBeam(EntityType<EntityLightBeam> type, Level level)
    {
        super(type, level);
        // 1.12 called setSize(1F, 1F) here; 26.3 takes the size from EntityType.Builder#sized when the
        // content registers its entity type (registry-ids.md §4)
    }

    /**
     * 1.12 constructor {@code EntityLightBeam(World)}, kept so the blasts spawning the beam do not have to
     * change shape (see {@code BlastBeam}).
     */
    public EntityLightBeam(Level level)
    {
        this(ModEntities.LIGHT_BEAM, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add. The defaults have to
        // match the field initializers above, field initializers run after the super constructor (which is
        // what calls this method) and can not be read here.
        builder.define(BEAM_PROGRESS, -1f);
        builder.define(DATA_RED, 1f);
        builder.define(DATA_GREEN, 0f);
        builder.define(DATA_BLUE, 0f);
        builder.define(DATA_BEAM_SIZE, 0.5f);
        builder.define(DATA_BEAM_GLOW_SIZE, 1f);
    }

    /**
     * Called by {@code ServerEntity} right before the synced data is packed (the spawn pairing data and
     * every data update). 1.12 shipped the render values once in the spawn packet through
     * {@code IEntityAdditionalSpawnData#writeSpawnData}; pushing the legacy fields here keeps the field
     * writes done by the blasts (e.g. {@code lightBeam.beamSize = 1}) working without adding setters.
     */
    @Override
    public void updateDataBeforeSync()
    {
        this.entityData.set(DATA_RED, this.red);
        this.entityData.set(DATA_GREEN, this.green);
        this.entityData.set(DATA_BLUE, this.blue);
        this.entityData.set(DATA_BEAM_SIZE, this.beamSize);
        this.entityData.set(DATA_BEAM_GLOW_SIZE, this.beamGlowSize);
    }

    /**
     * Copies the synced render values back into the legacy public fields, so the renderer (S4.07) can read
     * them exactly like it did in 1.12.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);

        if (DATA_RED.equals(key))
        {
            this.red = this.entityData.get(DATA_RED);
        }
        else if (DATA_GREEN.equals(key))
        {
            this.green = this.entityData.get(DATA_GREEN);
        }
        else if (DATA_BLUE.equals(key))
        {
            this.blue = this.entityData.get(DATA_BLUE);
        }
        else if (DATA_BEAM_SIZE.equals(key))
        {
            this.beamSize = this.entityData.get(DATA_BEAM_SIZE);
        }
        else if (DATA_BEAM_GLOW_SIZE.equals(key))
        {
            this.beamGlowSize = this.entityData.get(DATA_BEAM_GLOW_SIZE);
        }
    }

    /**
     * 1.12 inherited {@code Entity#attackEntityFrom}; the beam has no health and can not be collided with,
     * so it never took damage. 26.3 requires the override as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    /**
     * 1.12 {@code canBeCollidedWith()} returned false, which is the 26.3 default of {@link #isPickable()}.
     */
    @Override
    public boolean isPickable()
    {
        return false;
    }

    /**
     * 1.12 {@code canBePushed()} returned false, preventing entities from pushing the beam around.
     */
    @Override
    public boolean isPushable()
    {
        return false;
    }

    public EntityLightBeam setPosition(IPos3D position)
    {
        this.setPos(position.x(), position.y(), position.z());
        return this;
    }

    public EntityLightBeam setColor(float red, float green, float blue)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
        return this;
    }

    public void startDeathCycle()
    {
        deathCycle = true;
    }

    public void setTargetBeamProgress(float value)
    {
        targetBeamProgress = value;
    }

    public void setActualBeamProgress(float value)
    {
        this.entityData.set(BEAM_PROGRESS, Math.min(1, Math.max(0, value)));
    }

    public float getBeamProgress()
    {
        return this.entityData.get(BEAM_PROGRESS);
    }

    /** Called to update the entity's position/logic. */
    @Override
    public void tick()
    {
        // 1.12 EntityLightBeam#onUpdate never called super.onUpdate(). Entity#tick is only Entity#baseTick
        // (fire, fluid, portal, leash, below world), so it is skipped for the same reason.

        //Grow beam slowly
        if (getBeamProgress() < targetBeamProgress)
        {
            setActualBeamProgress(Math.min(targetBeamProgress, getBeamProgress() + beamGrowthRate));
        }
        //Decrease size slowly
        else if(getBeamProgress() > targetBeamProgress)
        {
            setActualBeamProgress(Math.max(targetBeamProgress, getBeamProgress() - beamGrowthRate));
        }

        //Kill off beam when animation finishes
        if (deathCycle && Math.abs(getBeamProgress() - targetBeamProgress) <= 0.01)
        {
            this.discard();
        }
        //Safety in case the beam is never killed
        else if (tickCount > 20 * 60 * 5) //ticks per second * seconds * mins = 5 mins
        {
            this.discard();
        }
    }

    /** (abstract) Protected helper method to read subclass entity data from save. */
    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    /** (abstract) Protected helper method to write subclass entity data to save. */
    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    private static final SaveHandler<EntityLightBeam> SAVE_LOGIC = new SaveHandler<EntityLightBeam>()
        .mainRoot()
        .nodeFloat("red", (e) -> e.red, (e, i) -> e.red = i)
        .nodeFloat("green", (e) -> e.green, (e, i) -> e.green = i)
        .nodeFloat("blue", (e) -> e.blue, (e, i) -> e.blue = i)
        .nodeFloat("beamSize", (e) -> e.beamSize, (e, i) -> e.beamSize = i)
        .nodeFloat("beamGlowSize", (e) -> e.beamGlowSize, (e, i) -> e.beamGlowSize = i)
        .nodeFloat("beamProgress", (e) -> e.getBeamProgress(), (e, i) -> e.entityData.set(BEAM_PROGRESS, i))
        .nodeFloat("targetBeamProgress", (e) -> e.targetBeamProgress, (e, i) -> e.targetBeamProgress = i)
        .nodeFloat("beamGrowthRate", (e) -> e.beamGrowthRate, (e, i) -> e.beamGrowthRate = i)
        .nodeBoolean("deathCycle", (e) -> e.deathCycle, (e, i) -> e.deathCycle = i)
        .base();
}
