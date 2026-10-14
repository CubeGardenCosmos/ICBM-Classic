package icbm.classic.content.entity;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.tile.IRotatable;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.capability.emp.CapabilityEmpKill;
import icbm.classic.lib.capability.ex.CapabilityExplosiveEntity;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Primed explosive block. Spawned when an explosive block is triggered, counts its fuse down and then
 * runs the blast of the held explosive through the registry.
 * <p>
 * Registered as {@code icbmclassic:primed_explosive} ({@code ModEntities.PRIMED_EXPLOSIVE}), see
 * registry-ids.md §4.
 * <p>
 * Porting notes (porting-guide §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone: the fuse, the facing and the explosive id are
 *     {@link SynchedEntityData} values ({@link #DATA_FUSE}, {@link #DATA_FACING},
 *     {@link #DATA_EXPLOSIVE_ID}). 1.12 sent the explosive item stack as spawn data, but 26.3 keys
 *     explosives by {@link Identifier} (ADR 0002), so the id is synced instead and the stack (customizations)
 *     stays server side in {@link #capabilityExplosive}.</li>
 *     <li>1.12 pulled the explosive off the item stack through the Forge explosive capability. 26.3 has no
 *     capability system, so this entity implements {@link IExplosive} itself - the counterpart documented
 *     on {@link icbm.classic.lib.lookup.IcbmLookups} and consumed by
 *     {@link ICBMClassicHelpers#getExplosive(Entity)} -, while {@link #capabilityExplosive} remains the item
 *     stack/customization payload (legacy {@code capabilityExplosive}).</li>
 *     <li>{@code preventEntitySpawning} and {@code canTriggerWalking()} have no 26.3 counterpart and
 *     {@code setSize(0.98F, 0.98F)} moved to {@code EntityType.Builder#sized} (see {@code ModEntities}).</li>
 *     <li>{@code canBeCollidedWith()}/{@code canBePushed()} map to {@link #isPickable()}/{@link #isPushable()},
 *     which answer false by default in 26.3, so both are overridden to keep the 1.12 values.</li>
 * </ul>
 */
public class EntityExplosive extends Entity implements IRotatable, IExplosive, IEMPReceiver
{
    /**
     * Registry key of the explosive this entity runs.
     * <p>
     * 1.12 serialized the explosive capability (item stack) through
     * {@code IEntityAdditionalSpawnData#writeSpawnData}; the stack itself is server side save data now, so
     * only the id the client needs to label/render the entity is synced.
     */
    private static final EntityDataAccessor<String> DATA_EXPLOSIVE_ID = SynchedEntityData.defineId(EntityExplosive.class, EntityDataSerializers.STRING);

    /** Fuse counter, 1.12 sent it as the first field of the spawn data. */
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(EntityExplosive.class, EntityDataSerializers.INT);

    /** Placement facing, 1.12 sent it as the enum ordinal of the spawn data. */
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(EntityExplosive.class, EntityDataSerializers.DIRECTION);

    /**
     * 1.12 {@code fuse} value meaning "not initialized yet": the first tick asks the explosive registry for
     * the configured fuse time of the explosive.
     */
    public static final int FUSE_NOT_SET = -1;

    //Capabilities
    // 1.12 exposed both through getCapability/hasCapability; 26.3 has no capability system, so the entity
    // implements IExplosive/IEMPReceiver and forwards to these (see the class javadoc).
    public final IEMPReceiver capabilityEMP = new CapabilityEmpKill<>(this);
    public final CapabilityExplosiveEntity capabilityExplosive = new CapabilityExplosiveEntity(this);

    /**
     * Constructor used by {@code EntityType} ({@code ModEntities.PRIMED_EXPLOSIVE}).
     */
    public EntityExplosive(EntityType<EntityExplosive> type, Level level)
    {
        super(type, level);
        // 1.12 set preventEntitySpawning = true (dropped, no 26.3 counterpart) and setSize(0.98F, 0.98F),
        // which is EntityType.Builder#sized in 26.3 (see ModEntities)
    }

    /**
     * Spawns a primed explosive for the given explosive, 1.12 {@code EntityExplosive(World, Pos, facing enum, ItemStack)}.
     * <p>
     * 1.12 read the explosive from the item stack, 26.3 takes the {@link Identifier} of the explosive instead
     * (ADR 0002); the optional stack (customizations) is handled by
     * {@link #EntityExplosive(Level, Pos, Direction, Identifier, ItemStack)}.
     *
     * @param level       level to place the entity in
     * @param position    position of the explosive block, the entity throws itself up from here
     * @param orientation facing the explosive block was placed with
     * @param explosiveId registry key of the explosive, see {@code ICBMExplosives}
     */
    public EntityExplosive(Level level, Pos position, Direction orientation, Identifier explosiveId)
    {
        this(ModEntities.PRIMED_EXPLOSIVE, level);
        this.setPos(position.x(), position.y(), position.z());

        // 1.12 used Math.random(); 26.3 takes the level random (porting-guide §2.4)
        final float var8 = (float) (this.level().getRandom().nextFloat() * Math.PI * 2.0D);
        this.setDeltaMovement(-(float) Math.sin(var8) * 0.02F, 0.20000000298023224D, -(float) Math.cos(var8) * 0.02F);

        // 1.12 assigned prevPosX/Y/Z right after setPosition; 26.3 keeps the previous position in
        // xo/yo/zo + xOld/yOld/zOld, which setOldPos copies over (setPos alone would leave them at 0 and
        // the client would interpolate the entity in from the world origin)
        this.setOldPos();

        this.setDirection(orientation);
        this.setExplosiveId(explosiveId);
    }

    /**
     * Same as {@link #EntityExplosive(Level, Pos, Direction, Identifier)} but also carries the stack the
     * explosive block held, 1.12 {@code capabilityExplosive.setStack(stack)}.
     *
     * @param stack stack the explosive was placed with, holds the customization data
     */
    public EntityExplosive(Level level, Pos position, Direction orientation, Identifier explosiveId, ItemStack stack)
    {
        this(level, position, orientation, explosiveId);
        this.capabilityExplosive.setStack(stack);

        // 1.12 had no id argument, it read the explosive off the stack (the capability resolved the stack's
        // explosive). The flat explosive items that let ICBMClassicHelpers#getExplosive(ItemStack) answer a
        // stack land in S2.05/S3.09, until then this stays a fallback for a missing id
        if (explosiveId == null)
        {
            final IExplosive fromStack = ICBMClassicHelpers.getExplosive(stack);
            if (fromStack != null)
            {
                this.setExplosiveId(fromStack.getExplosiveData().getRegistryKey());
            }
        }
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick()
    {
        // 1.12 copied posX/Y/Z into prevPosX/Y/Z at the top of onUpdate; 26.3 runs Entity#commonTick before
        // this method, which copies the position into xo/yo/zo (setOldPosAndRot), so nothing is needed here

        // 1.12 wrote the motionX/Y/Z fields, 26.3 keeps them in the delta movement. move() applies the
        // collision response to the motion (Entity#move pushes it back through setDeltaMovement), which is
        // what 1.12 did to its motion fields, so the ground damping below reads the motion back after move
        final Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.95D, motion.y - 0.045D, motion.z * 0.95D);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.onGround())
        {
            final Vec3 groundMotion = this.getDeltaMovement();
            this.setDeltaMovement(groundMotion.x * 0.699999988079071D, groundMotion.y * -0.5D, groundMotion.z * 0.699999988079071D);
        }

        //Init fuse
        if (getFuse() == FUSE_NOT_SET)
        {
            // 1.12 passed the numeric registry id here; numeric ids are gone (ADR 0002) so the Identifier key is used
            this.setFuse(ICBMClassicAPI.EX_BLOCK_REGISTRY.getFuseTime(level(), getX(), getY(), getZ(), getExplosiveData().getRegistryKey()));
        }

        //Tick fuse to render effects
        ICBMClassicAPI.EX_BLOCK_REGISTRY.tickFuse(level(), getX(), getY(), getZ(), getFuse(), getExplosiveData().getRegistryKey());

        //Tick fuse
        // 1.12 post decremented the counter inside the check (if (this.fuse-- < 1)). The counter is only read
        // by the check, so testing first and decrementing after triggers the blast on the same tick without
        // writing the synced value of an entity that explode() just removed.
        final boolean fuseExpired = getFuse() < 1;
        this.setFuse(getFuse() - 1);
        if (fuseExpired)
        {
            this.explode();
        }

        super.tick();
    }

    /**
     * Runs the blast of the held explosive and removes this entity, 1.12 {@code explode()}.
     */
    public void explode()
    {
        // 1.12: getExplosiveCap().doExplosion(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D,
        //           new ActionSource(world, new Vec3d(posX, posY, posZ), new EntityCause(this)));
        // The capability ran ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(id) ->
        // IExplosiveData#create(level, x, y, z, source, null) -> IAction#doAction().
        // CapabilityExplosiveEntity#doExplosion is not used to keep that chain running on the held
        // explosive: its getExplosiveData() resolves the id from its item stack and
        // ICBMClassicHelpers#getExplosive(ItemStack) is still a S2.05 placeholder answering null, so it would
        // always fall back to condensed. The synced id of this entity is resolved through the registry instead.
        if (!level().isClientSide())
        {
            // TODO(S4) build the cause from EntityCause(this) once content/missile/logic/source/cause is ported,
            //  same as EntityExplosion#constructBlast
            final IActionSource source = new ActionSource(level(), new Vec3(getX(), getY(), getZ()), null);
            getExplosiveData().create(level(), getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D, source, null).doAction();
        }
        this.discard(); // 1.12 setDead()
    }

    /**
     * 1.12 {@code readEntityFromNBT}: fuse + explosive stack.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    /**
     * 1.12 {@code writeEntityToNBT}: fuse + explosive stack.
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    /**
     * 1.12 wrote the fuse with {@code nbt.setByte(NBTConstants.FUSE, (byte) this.fuse)} and read it back with
     * {@code getByte}, which truncated every configured fuse time above 127 ticks (several explosives use
     * more, see {@code ConfigFuseTimes}). The port keeps the legacy keys and stores the counter as an int so
     * the configured value survives a save.
     * <p>
     * The capability payload keeps the legacy key: {@link CapabilityExplosiveEntity#save(ValueOutput)} names
     * its own node, because the 26.3 item stack codec needs a named node, so the stack ends up one level below
     * the legacy {@code explosive_stack} compound.
     */
    private static final SaveHandler<EntityExplosive> SAVE_LOGIC = new SaveHandler<EntityExplosive>()
        .mainRoot()
        /* */.nodeInteger(NBTConstants.FUSE, EntityExplosive::getFuse, EntityExplosive::setFuse)
        /* */.nodeIdentifier(NBTConstants.EXPLOSIVE_ID, EntityExplosive::getExplosiveId, EntityExplosive::setExplosiveId)
        /* */.nodeSavable(NBTConstants.EXPLOSIVE_STACK, EntityExplosive::getExplosiveCap)
        .base();

    /**
     * 1.12 {@code entityInit} was empty; 26.3 requires the synced data to be declared here.
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add
        builder.define(DATA_EXPLOSIVE_ID, "");
        builder.define(DATA_FUSE, FUSE_NOT_SET);
        builder.define(DATA_FACING, Direction.NORTH);
    }

    /**
     * 1.12 inherited {@code Entity#attackEntityFrom}; the block has no health so it never took damage and the
     * attack only marked the motion dirty. 26.3 requires the override as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    /**
     * 1.12 {@code canBeCollidedWith()} returned true.
     * <p>
     * 26.3 {@link Entity#isPickable()} answers false by default, so the value is spelled out here.
     */
    @Override
    public boolean isPickable()
    {
        return true;
    }

    /**
     * 1.12 {@code canBePushed()} returned true.
     * <p>
     * 26.3 {@link Entity#isPushable()} answers false by default, so the value is spelled out here.
     */
    @Override
    public boolean isPushable()
    {
        return true;
    }

    @Override
    public Direction getDirection()
    {
        // 1.12 repaired a null facing to NORTH on read, the synced value can not be null and defaults to NORTH
        return this.entityData.get(DATA_FACING);
    }

    @Override
    public void setDirection(Direction facingDirection)
    {
        // 1.12 stored the facing as given (null included) and repaired it to NORTH in getDirection(); a
        // synced value can not be null, so null becomes the NORTH default here
        this.entityData.set(DATA_FACING, facingDirection != null ? facingDirection : Direction.NORTH);
    }

    /**
     * Registry key of the explosive this entity runs, null while none was set.
     * <p>
     * 1.12 answered this off the explosive capability ({@code getExplosiveCap().getExplosiveData()}).
     */
    @Nullable
    public Identifier getExplosiveId()
    {
        final String id = this.entityData.get(DATA_EXPLOSIVE_ID);
        return id.isEmpty() ? null : Identifier.tryParse(id);
    }

    /**
     * Sets the explosive this entity runs, 1.12 {@code capabilityExplosive.setStack(stack)} where the stack
     * carried the explosive.
     */
    public void setExplosiveId(@Nullable Identifier explosiveId)
    {
        this.entityData.set(DATA_EXPLOSIVE_ID, explosiveId != null ? explosiveId.toString() : "");
    }

    /** Fuse counter in ticks, {@link #FUSE_NOT_SET} until the first tick initializes it. 1.12 {@code fuse}. */
    public int getFuse()
    {
        return this.entityData.get(DATA_FUSE);
    }

    /** Sets the fuse counter, see {@link #getFuse()}. */
    public void setFuse(int fuse)
    {
        this.entityData.set(DATA_FUSE, fuse);
    }

    /** The explosive capability of this entity, 1.12 {@code getExplosiveCap()}. */
    public CapabilityExplosiveEntity getExplosiveCap()
    {
        return capabilityExplosive;
    }

    /**
     * Gets the explosive this entity runs.
     * <p>
     * 1.12 asked {@code getExplosiveCap().getExplosiveData()} (resolved from the item stack) and fell back to
     * condensed when that answered nothing. The port resolves the synced id through the registry, the
     * capability stack route needs the flat explosive items (S2.05/S3.09), see the class javadoc.
     */
    @NonNull
    @Override
    public IExplosiveData getExplosiveData()
    {
        final Identifier id = getExplosiveId();
        if (id != null)
        {
            final IExplosiveData data = ICBMClassicHelpers.getExplosive(id, true);
            if (data != null)
            {
                return data;
            }
        }
        return ICBMExplosives.CONDENSED;
    }

    @Override
    public void applyCustomizations(IBlast blast)
    {
        capabilityExplosive.applyCustomizations(blast);
    }

    @Override
    public void addCustomization(IExplosiveCustomization customization)
    {
        capabilityExplosive.addCustomization(customization);
    }

    @Nullable
    @Override
    public ItemStack toStack()
    {
        return capabilityExplosive.toStack();
    }

    /**
     * Called when the entity is defused, 1.12 dropped the stored explosive stack and killed the entity.
     * <p>
     * 1.12 reached this through {@code ICBMClassicHelpers.getExplosive(entity)} + {@code onDefuse()}, which
     * is this entity in 26.3 (it implements {@link IExplosive}), so the call is forwarded to the capability
     * that owns the stack.
     */
    @Override
    public void onDefuse()
    {
        capabilityExplosive.onDefuse();
    }

    /**
     * 1.12 exposed {@code capabilityEMP} through {@code getCapability(CapabilityEMP.EMP, ...)}; 26.3 finds
     * entity EMP receivers through the {@code icbmclassic:emp_receiver} entity lookup
     * ({@link icbm.classic.lib.lookup.IcbmLookups#EMP_RECEIVER_ENTITY}), see the interface javadoc.
     */
    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
    {
        return capabilityEMP.applyEmpAction(level, x, y, z, emp_blast, power, doAction);
    }

    /**
     * 1.12 returned "Explosive[ex]" as a raw string; 26.3 entity names are components (debug label, not
     * player facing text).
     */
    @Override
    public Component getName()
    {
        // 1.12 answered plain "Explosive" when no data was available; getExplosiveData() answers condensed
        // for that case in 26.3, so the label always carries a registry key
        return Component.literal("Explosive[" + getExplosiveData().getRegistryKey() + "]");
    }
}
