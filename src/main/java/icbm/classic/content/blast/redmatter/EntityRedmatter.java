package icbm.classic.content.blast.redmatter;

import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.redmatter.caps.BlastRedmatterWrapper;
import icbm.classic.content.blast.redmatter.caps.CapRedmatterPull;
import icbm.classic.content.blast.redmatter.logic.RedmatterLogic;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

/**
 * The black hole style explosive entity, spawned by {@link ActionSpawnRedmatter}.
 * <p>
 * 26.3 port notes (porting-guide.md §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} does not exist. The two values the 1.12 data manager watched
 *     (blast size and max size) stay in {@link SynchedEntityData} as {@link #SIZE_DATA} and
 *     {@link #MAX_SIZE_DATA}; 1.12 already used the data manager for both so nothing moved.</li>
 *     <li>{@code hasCapability}/{@code getCapability} are dropped. 26.3 has no capability system: the
 *     {@link BlastRedmatterWrapper} and {@link CapRedmatterPull} handlers are read straight off the fields
 *     by the code that needs them (see {@code RedmatterLogic#moveEntity} and
 *     {@code RedmatterLogic#shouldHandleEntity}).</li>
 *     <li>{@code setSize(0.98F, 0.98F)} moved to {@code EntityType.Builder#sized} (see
 *     {@link ModEntities}, registration owned by the parent step), {@code noClip} is
 *     {@link Entity#noPhysics} and {@code ticksExisted = 0} is the 26.3 default of a fresh entity.
 *     {@code preventEntitySpawning} has no 26.3 counterpart (entity spawning is data driven) and
 *     {@code ignoreFrustumCheck} is a renderer concern, so the S4.07 renderer owns it.</li>
 *     <li>{@code canTriggerWalking()} no longer exists (blocks decide trampling themselves) and
 *     {@code canBeCollidedWith()} is {@link #isPickable()} (kept as an explicit override to mirror 1.12,
 *     it is the 26.3 default).</li>
 *     <li>1.12 also held a client side size lerp handler used only by the renderer
 *     ({@code RedmatterClientLogic}); 26.3 keeps it as {@link #clientVisualSize} plus
 *     {@link #lerpClientVisualSize(float)}, which the S4.07 renderer calls. No client class is referenced,
 *     so the field exists on both sides and is only ever written on the client.</li>
 * </ul>
 * The renderer (S4.07) reads {@link #getBlastSize()} through the synced data, {@code tickCount} for its
 * animations, {@link #clientVisualSize} for the lerped size and
 * {@code ConfigBlast.redmatter.RENDER_SCALE}/{@code RENDER_COLORED_BEAMS}; it needs no other field from this
 * class.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 4/19/2020.
 */
public class EntityRedmatter extends Entity
{
    public static final String NBT_BLAST_SIZE = "blast_size";
    public static final String NBT_BLAST_SIZE_MAX = "blast_size_max";


    public static final float MAX_SPEED = 0.5f;
    public static final float SPEED_REDUCTION = 0.98f;

    //Acts as an API wrapper for the entity
    public final BlastRedmatterWrapper blastData = new BlastRedmatterWrapper(this);
    public final CapRedmatterPull capRedmatterPull = new CapRedmatterPull(this);

    //Handlers
    public final RedmatterLogic redmatterLogic = new RedmatterLogic(this);

    /**
     * 1.12 {@code RedmatterClientLogic#visualSize}: the smoothly changing size the renderer draws with. It is
     * client side only, the renderer is the only writer (see {@link #lerpClientVisualSize(float)}).
     */
    public float clientVisualSize = 0.0F;

    /**
     * 1.12 {@code RedmatterClientLogic#lerpSize}, triggered from the render once per frame.
     *
     * @param deltaTick percentage of time passed (0.0 - 1.0f)
     */
    public void lerpClientVisualSize(float deltaTick)
    {
        this.clientVisualSize = this.clientVisualSize + deltaTick * (getBlastSize() - this.clientVisualSize);
    }

    /** Actual size of the redmatter */
    private static final EntityDataAccessor<Float> SIZE_DATA = SynchedEntityData.defineId(EntityRedmatter.class, EntityDataSerializers.FLOAT);
    /** Largest possible size of the redmatter */
    private static final EntityDataAccessor<Float> MAX_SIZE_DATA = SynchedEntityData.defineId(EntityRedmatter.class, EntityDataSerializers.FLOAT);

    public EntityRedmatter(EntityType<EntityRedmatter> type, Level level)
    {
        super(type, level);
        // 1.12 called setSize(0.98F, 0.98F) here, 26.3 takes the size from EntityType.Builder#sized (ModEntities)
        // 1.12 set preventEntitySpawning and ignoreFrustumCheck, see the class javadoc for both
        this.noPhysics = true; //1.12: noClip = true
        // 1.12 set ticksExisted = 0, which is the 26.3 default of a fresh entity (tickCount)
    }

    /** 1.12 constructor {@code EntityRedmatter(World)}, kept so actions can spawn the entity. */
    public EntityRedmatter(Level level)
    {
        this(ModEntities.REDMATTER, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add
        // 1.12 registered both values in entityInit()
        builder.define(SIZE_DATA, ConfigBlast.redmatter.DEFAULT_SIZE);
        builder.define(MAX_SIZE_DATA, ConfigBlast.redmatter.MAX_SIZE);
    }

    @Override
    public void tick()
    {
        super.tick(); //1.12: super.onUpdate()

        //Update motion until we hit zero
        final Vec3 motion = this.getDeltaMovement();
        if (motion.x != 0 || motion.y != 0 || motion.z != 0) //TODO replace zero with range check to prevent rounding issues
        {
            reduceMotion();
            correctMotion();
            move(MoverType.SELF, this.getDeltaMovement());
        }

        //Run only if server
        if (!this.level().isClientSide()) //1.12: !world.isRemote
        {
            redmatterLogic.tick();
        }
    }

    //<editor-fold desc="motion handling">
    private void reduceMotion()
    {
        //1.12: motionX *= SPEED_REDUCTION, repeated for Y and Z
        this.setDeltaMovement(this.getDeltaMovement().scale(SPEED_REDUCTION));
    }

    private void correctMotion()
    {
        //TODO see if we can remove the sqrt and if the limit should be in an if-statement
        final Vec3 motion = this.getDeltaMovement();

        //Normalize motion as a speed value
        //1.12 called MathHelper.sqrt(double), which is (float) Math.sqrt(value)
        final float speed = Mth.sqrt((float) (motion.x * motion.x + motion.y * motion.y + motion.z * motion.z));

        //Limit our velocity vector by the updated speed
        final float limitedSpeed = Math.min(speed, MAX_SPEED);

        //1.12 divided the motion by 'speed' and then multiplied by 'limitedSpeed', both steps are kept
        //verbatim so the rounding matches (and a zero speed still produces the 1.12 NaN instead of a clamp)
        this.setDeltaMovement(
                motion.x / speed * limitedSpeed,
                motion.y / speed * limitedSpeed,
                motion.z / speed * limitedSpeed);
    }
    //</editor-fold>

    //<editor-fold desc="saving">
    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    /**
     * 1.12 wrote both keys with a presence check on read only, which is what the {@code SaveNodeFloat}
     * default (keep the field when the key is missing) reproduces.
     * <p>
     * The max key is read into the size field, which is what the 1.12 {@code readEntityFromNBT} did. Kept
     * verbatim, do not fix.
     */
    private static final SaveHandler<EntityRedmatter> SAVE_LOGIC = new SaveHandler<EntityRedmatter>()
        .mainRoot()
        /* */.nodeFloat(NBT_BLAST_SIZE, EntityRedmatter::getBlastSize, EntityRedmatter::setBlastSize)
        /* */.nodeFloat(NBT_BLAST_SIZE_MAX, EntityRedmatter::getBlastMaxSize, EntityRedmatter::setBlastSize)
        .base();
    //</editor-fold>

    //<editor-fold desc="disabled-props">
    // 1.12 canTriggerWalking() no longer exists, blocks decide trampling themselves

    /**
     * 1.12 {@code canBeCollidedWith()} returned false. That is the default of {@link #isPickable()} in
     * 26.3, the override is kept to mirror the 1.12 class.
     */
    @Override
    public boolean isPickable()
    {
        return false;
    }

    /**
     * 1.12 inherited the default {@code Entity#attackEntityFrom}, which only marked the entity as attacked
     * and answered false. 26.3 requires the override as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }
    //</editor-fold>

    public float getBlastSize()
    {
        return this.entityData.get(SIZE_DATA);
    }

    public float getBlastMaxSize()
    {
        return this.entityData.get(MAX_SIZE_DATA);
    }

    public void setBlastSize(float size)
    {
        final float limitedSize = Math.max(ConfigBlast.redmatter.MIN_SIZE, size);
        this.entityData.set(SIZE_DATA, limitedSize);
    }

    public void setBlastMaxSize(float size)
    {
        this.entityData.set(MAX_SIZE_DATA, size);
    }
}
