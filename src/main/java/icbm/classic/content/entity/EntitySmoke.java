package icbm.classic.content.entity;

import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.core.particles.ParticleTypes;
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
 * Entity that spawns smoke from it's position
 * <p>
 * Spawned by the smoke explosive (1.12 {@code smoke} → 26.3 {@code icbmclassic:smoke},
 * {@link ModEntities#SMOKE}) through the spawn entity action, which only places it and lets the default
 * colour and lifetime produce the cloud.
 * <p>
 * Porting notes (porting-guide.md §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone; the colour and lifetime 1.12 sent with
 *     {@code writeSpawnData} are {@link SynchedEntityData} values written by
 *     {@link #updateDataBeforeSync()} (the vanilla hook for values that have to be correct in the spawn
 *     packet), which keeps the legacy public fields working as the server side source.</li>
 *     <li>{@code onUpdate} → {@link #tick()}, {@code setDead()} → {@link #discard()} and
 *     {@code ticksExisted} → {@link #tickCount}. 1.12 never called {@code super.onUpdate()}, so
 *     {@link Entity#tick()} is not called here either.</li>
 *     <li>{@code world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, ...)} → the server side
 *     {@link ServerLevel#sendParticles} with {@code ParticleTypes.SMOKE}; 1.12 also ran this on the client
 *     (client side {@code World#spawnParticle} spawned a local copy), which is intentionally not repeated
 *     as 26.3 renders the broadcast for every player including the one hosting the level.</li>
 *     <li>{@code preventEntitySpawning} has no 26.3 counterpart (spawning is data driven) and
 *     {@code ignoreFrustumCheck} is a renderer concern ({@code EntityRenderer#shouldRender}, S4.07).
 *     {@code setSize(1F, 1F)} moved to {@code EntityType.Builder#sized} and {@code canBeCollidedWith()}/
 *     {@code canBePushed()} map to {@link #isPickable()}/{@link #isPushable()};
 *     {@code canTriggerWalking()} no longer exists (blocks decide trampling themselves).</li>
 *     <li>1.12 wrote {@code height = 0.1F} and {@code width = 0.1F} right after {@code setSize(1F, 1F)}. The
 *     collision box had already been built as 1x1 and 26.3 takes both from {@code EntityType.Builder#sized}
 *     (1x1, see {@code ModEntities}), so the overwrite is not reproduced.</li>
 * </ul>
 */
public class EntitySmoke extends Entity
{
    //Render color
    public float red = 1;
    public float green = 0;
    public float blue = 0;
    public int ticksToLive = 100;

    //Data
    /**
     * Values 1.12 sent as spawn data ({@code writeSpawnData}/{@code readSpawnData}).
     */
    private static final EntityDataAccessor<Float> DATA_RED = SynchedEntityData.defineId(EntitySmoke.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GREEN = SynchedEntityData.defineId(EntitySmoke.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BLUE = SynchedEntityData.defineId(EntitySmoke.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TICKS_TO_LIVE = SynchedEntityData.defineId(EntitySmoke.class, EntityDataSerializers.INT);

    public EntitySmoke(EntityType<EntitySmoke> type, Level level)
    {
        super(type, level);
        // 1.12 called setSize(1F, 1F) and then overwrote height/width with 0.1F, see the class javadoc.
        // 26.3 takes the size from EntityType.Builder#sized when the content registers its entity type
        // (registry-ids.md §4)
    }

    /**
     * 1.12 constructor {@code EntitySmoke(World)}, kept so content spawning the cloud does not have to
     * change shape.
     */
    public EntitySmoke(Level level)
    {
        this(ModEntities.SMOKE, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add. The defaults have to
        // match the field initializers above, field initializers run after the super constructor (which is
        // what calls this method) and can not be read here.
        builder.define(DATA_RED, 1f);
        builder.define(DATA_GREEN, 0f);
        builder.define(DATA_BLUE, 0f);
        builder.define(DATA_TICKS_TO_LIVE, 100);
    }

    /**
     * Called by {@code ServerEntity} right before the synced data is packed (the spawn pairing data and
     * every data update). 1.12 shipped these values once in the spawn packet through
     * {@code IEntityAdditionalSpawnData#writeSpawnData}; pushing the legacy fields here keeps the 1.12 style
     * field writes (and the save round trip below) working without adding setters.
     */
    @Override
    public void updateDataBeforeSync()
    {
        this.entityData.set(DATA_RED, this.red);
        this.entityData.set(DATA_GREEN, this.green);
        this.entityData.set(DATA_BLUE, this.blue);
        this.entityData.set(DATA_TICKS_TO_LIVE, this.ticksToLive);
    }

    /**
     * Copies the synced render values back into the legacy public fields, so the renderer (S4.07) and the
     * client side tick can read them exactly like they did in 1.12.
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
        else if (DATA_TICKS_TO_LIVE.equals(key))
        {
            this.ticksToLive = this.entityData.get(DATA_TICKS_TO_LIVE);
        }
    }

    /**
     * 1.12 inherited {@code Entity#attackEntityFrom}; the cloud has no health and can not be collided with,
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
     * 1.12 {@code canBePushed()} returned false, preventing entities from pushing the cloud around.
     */
    @Override
    public boolean isPushable()
    {
        return false;
    }

    public EntitySmoke setColor(float red, float green, float blue)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
        return this;
    }

    /** Called to update the entity's position/logic. */
    @Override
    public void tick()
    {
        // 1.12 EntitySmoke#onUpdate never called super.onUpdate(). Entity#tick is only Entity#baseTick
        // (fire, fluid, portal, leash, below world), so it is skipped for the same reason.

        //Safety in case the beam is never killed
        if (tickCount > ticksToLive)
        {
            this.discard();
        }

        // TODO scale with age with mid life having most, early low and later trailing off
        if (this.level() instanceof ServerLevel serverLevel)
        {
            final int spawnCount = 3 + this.level().getRandom().nextInt(4);
            for (int i = 0; i < spawnCount; i++)
            {
                // 1.12 World#spawnParticle(type, x, y, z, xSpeed, ySpeed, zSpeed) spawned a single particle
                // at the exact position moving with the given velocity. The 26.3 sendParticles with a count
                // of 0 keeps the position exact and derives the per axis velocity as speed * spread
                // (ClientPacketListener#handleParticleEvent), so the spread is 1 and the 1.12 velocities are
                // passed as the per axis speeds.
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.1f, getZ(), 0,
                    1.0D, 1.0D, 1.0D,
                    0.05f * this.level().getRandom().nextFloat() - 0.05f * this.level().getRandom().nextFloat(),
                    0.1f,
                    0.05f * this.level().getRandom().nextFloat() - 0.05f * this.level().getRandom().nextFloat());
            }
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

    /**
     * 1.12 copy-paste bug kept on purpose (golden rule: do not fix 1.12 bugs): the "green" node saves and
     * loads {@code red} and the third node is named "red" again, so only {@code red} and {@code ticksToLive}
     * ever survive a save. Blue was never declared at all.
     */
    private static final SaveHandler<EntitySmoke> SAVE_LOGIC = new SaveHandler<EntitySmoke>()
        .mainRoot()
        .nodeInteger("ticksToLive", (e) -> e.ticksToLive, (e, i) -> e.ticksToLive = i)
        .addRoot("color")
        /* */.nodeFloat("red", (e) -> e.red, (e, i) -> e.red = i)
        /* */.nodeFloat("green", (e) -> e.red, (e, i) -> e.red = i)
        /* */.nodeFloat("red", (e) -> e.red, (e, i) -> e.red = i)
        .base();
}
