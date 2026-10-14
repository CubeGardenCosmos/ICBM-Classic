package icbm.classic.content.entity.flyingblock;

import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.config.ConfigFlyingBlocks;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.projectile.EntityProjectile;
import icbm.classic.lib.saving.SaveHandler;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Similiar to {@code net.minecraft.world.entity.item.FallingBlockEntity} but specifically as a projectile to
 * be spawned by {@code icbm.classic.content.blast.redmatter.EntityRedmatter} (S3.06) and other sources.
 *
 * Normally created by {@link FlyingBlock} handling logic
 *
 * <p>26.3 port notes (porting-guide.md §2.2/§2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone. 1.12 sent the captured block data as a compound tag,
 *     the gravity, and the two spin values when the entity spawned; 26.3 syncs the block state with
 *     {@link EntityDataSerializers#BLOCK_STATE} and the three floats as {@link SynchedEntityData} values
 *     ({@link #DATA_BLOCK_STATE}, {@link #DATA_GRAVITY}, {@link #DATA_YAW_CHANGE}, {@link #DATA_PITCH_CHANGE}).
 *     The block state is pushed into the synced data in {@link #setBlockData(BlockCaptureData)} and by
 *     {@link FlyingBlock#spawnFlyingBlock} before the entity is added, which is the 1.12 "state at spawn
 *     time" semantic; the server side logic keeps reading the authoritative
 *     {@link BlockCaptureData#getBlockState()}.</li>
 *     <li>{@code setSize(0.98F, 0.98F)} moved to {@code EntityType.Builder#sized} ({@code ModEntities}),
 *     {@code preventEntitySpawning} has no 26.3 counterpart (entity spawning is data driven, there is no
 *     entity flag) and {@code isImmuneToFire} is {@link #fireImmune()}. {@code canBePushed()} maps to
 *     {@link #isPushable()}, {@code canBeCollidedWith()} to {@link #isPickable()}, and
 *     {@code canTriggerWalking()} no longer exists (blocks decide trampling themselves).</li>
 *     <li>{@code getGravity()} is final on {@link Entity} in 26.3 (it answers from the entity attributes),
 *     so the 1.12 field is exposed through {@link #getProjectileGravity()}/{@link #setProjectileGravity(float)},
 *     the hook {@link EntityProjectile} uses for the per tick pull towards the ground.</li>
 *     <li>{@code world.mayPlace(block, pos, skipCollisionCheck, side, placer)} and
 *     {@code Item#onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ)} do not exist in 26.3, see
 *     {@link #tryPlacement(BlockPos, BlockHitResult)} for the replacement.</li>
 *     <li>{@code EntityFlyingBlock(World)} kept as {@link #EntityFlyingBlock(Level)} (the entity type aware
 *     constructor is used by {@code EntityType.Builder}).</li>
 * </ul>
 */
public class EntityFlyingBlock extends EntityProjectile<EntityFlyingBlock> {

    /**
     * Block the entity is carrying, 1.12 sent it with {@code IEntityAdditionalSpawnData#writeSpawnData}.
     * The client uses this to render the block (S4.07).
     */
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(EntityFlyingBlock.class, EntityDataSerializers.BLOCK_STATE);

    /** 1.12 sent the gravity of the entity with the spawn data */
    private static final EntityDataAccessor<Float> DATA_GRAVITY = SynchedEntityData.defineId(EntityFlyingBlock.class, EntityDataSerializers.FLOAT);

    /** 1.12 sent the yaw spin of the entity with the spawn data */
    private static final EntityDataAccessor<Float> DATA_YAW_CHANGE = SynchedEntityData.defineId(EntityFlyingBlock.class, EntityDataSerializers.FLOAT);

    /** 1.12 sent the pitch spin of the entity with the spawn data */
    private static final EntityDataAccessor<Float> DATA_PITCH_CHANGE = SynchedEntityData.defineId(EntityFlyingBlock.class, EntityDataSerializers.FLOAT);

    /** Data about the block we are replicating */
    private BlockCaptureData blockData;

    public EntityFlyingBlock(EntityType<EntityFlyingBlock> type, Level level) {
        super(type, level);
        // 1.12 set ticksExisted = 0, which is the 26.3 default (tickCount) of a fresh entity.
        // 1.12 set preventEntitySpawning, see the class javadoc. setSize and isImmuneToFire are handled by
        // ModEntities / fireImmune()
        this.inGroundKillTime = ICBMConstants.TICKS_SEC;
    }

    /** 1.12 {@code EntityFlyingBlock(World)}, used by {@link FlyingBlock} to spawn the entity. */
    public EntityFlyingBlock(Level level) {
        this(ModEntities.FLYING_BLOCK, level);
    }

    public void restoreGravity() {
        setProjectileGravity(DEFAULT_GRAVITY);
    }

    public BlockCaptureData getBlockData() {
        if (this.blockData == null) {
            this.blockData = new BlockCaptureData();
        }
        return this.blockData;
    }

    /** 1.12 used a package private setter ({@code @Setter(AccessLevel.PACKAGE)}), {@link FlyingBlock} is in this package. */
    void setBlockData(BlockCaptureData blockData) {
        this.blockData = blockData;
        this.syncBlockState();
    }

    /**
     * Pushes the carried block state into the synced data. 1.12 only ever sent the state once, with the
     * spawn data, so this mirrors that (a later mutation of {@link #getBlockData()} does not reach clients).
     */
    void syncBlockState() {
        this.entityData.set(DATA_BLOCK_STATE, getBlockData().getBlockState());
    }

    /**
     * Block state the entity carries, synced for the client renderer (S4.07). The server side logic uses
     * {@link #getBlockData()}{@code .getBlockState()}, see {@link #syncBlockState()}.
     */
    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    /** Amount of gravity applied each tick, 1.12 exposed this as a settable gravity field. */
    @Override
    public float getProjectileGravity() {
        return this.entityData.get(DATA_GRAVITY);
    }

    public void setProjectileGravity(float gravity) {
        this.entityData.set(DATA_GRAVITY, gravity);
    }

    /** Spin added to the yaw each tick, 1.12 exposed this as a public field that callers wrote to. */
    public float getYawChange() {
        return this.entityData.get(DATA_YAW_CHANGE);
    }

    public void setYawChange(float yawChange) {
        this.entityData.set(DATA_YAW_CHANGE, yawChange);
    }

    /** Spin added to the pitch each tick, 1.12 exposed this as a public field that callers wrote to. */
    public float getPitchChange() {
        return this.entityData.get(DATA_PITCH_CHANGE);
    }

    public void setPitchChange(float pitchChange) {
        this.entityData.set(DATA_PITCH_CHANGE, pitchChange);
    }

    /** 1.12 returned a raw string, 26.3 entity names are components (debug label, not player facing). */
    @NonNull
    @Override
    public Component getName() {
        return Component.literal("Flying Block [" + getBlockData().getBlockState() + ", " + hashCode() + "]");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
        builder.define(DATA_GRAVITY, DEFAULT_GRAVITY);
        builder.define(DATA_YAW_CHANGE, 0F);
        builder.define(DATA_PITCH_CHANGE, 0F);
    }

    @Override
    public void tick() {
        super.tick();

        //Animation
        if (this.getYawChange() > 0) {
            this.setYRot(this.getYRot() + this.getYawChange());
            this.setYawChange(this.getYawChange() - 2);
        }

        if (this.getPitchChange() > 0) {
            this.setXRot(this.getXRot() + this.getPitchChange());
            this.setPitchChange(this.getPitchChange() - 2);
        }
    }

    @Override
    protected boolean shouldExpire() {
        return super.shouldExpire() || this.getY() > 400;
    }

    @Override
    protected void destroy() {
        this.placeBlockIntoWorld(this.blockPosition(), new BlockHitResult(this.position(), Direction.UP, this.blockPosition(), false));
        this.discard();
    }

    @Override
    protected boolean shouldCollideWith(Entity entity) {
        return super.shouldCollideWith(entity) && !(entity instanceof EntityFlyingBlock);
    }

    @Override
    protected void onImpact(@NonNull HitResult impactLocation) {
        //Grace period to get away from the ground
        if (ticksInAir < 5) return;

        if (impactLocation instanceof BlockHitResult blockHit) {
            this.placeBlockIntoWorld(BlockPos.containing(impactLocation.getLocation()), blockHit);
        } else {
            // TODO spawn fragments based on block in some cases (wood material -> wood fragments)
            // TODO if entity is dead, continue moving under previous speed minus some for impact
            dropSourceStack(getBlockData().getSourceStack());
        }
    }

    public void placeBlockIntoWorld(BlockPos pos, BlockHitResult hit) {
        this.discard();

        if (!this.level().isClientSide()) {
            if (isServer() && !tryPlacement(pos, hit)) {
                dropSourceStack(getBlockData().getSourceStack());
            }
        }
    }

    protected boolean tryPlacement(BlockPos pos, BlockHitResult hit) {

        // Attempt to place using item, as it will better handle unique rules
        // 1.12 read the blockData field directly here, which throws for an entity that was spawned without
        // capture data; the lazy getter the rest of the class uses is kept for the port
        final ItemStack sourceStack = getBlockData().getSourceStack();
        if (!sourceStack.isEmpty()) {

            // Use fake player to handle TE placement or other special placement rules
            // 1.12 asked FakePlayerFactory.getMinecraft((WorldServer) world) for the shared fake player of
            // the server; Fabric API ships the same concept as FakePlayer#get(ServerLevel), and this path
            // only runs server side (see placeBlockIntoWorld)
            final FakePlayer player = FakePlayer.get((ServerLevel) this.level()); //TODO attempt to get actual player who create the source of this entity
            player.setItemInHand(InteractionHand.MAIN_HAND, sourceStack);

            //TODO pull entityYaw to get placement direction. This way furnace places facing the same way as it renders.
            // 1.12 Item#onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ) -> 26.3
            // ItemStack#useOn(UseOnContext). The clicked position and the side are the ones 1.12 handed to
            // the item: the position calculated for the impact and the side of the ray trace.
            final InteractionResult result = sourceStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit.getLocation(), hit.getDirection(), pos, false)));

            // cleanup fake
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            if (result == InteractionResult.SUCCESS) {
                return true;
            }
        }

        // Backup plan if we can't place via item, this will break in some cases TODO implement a config to disable blocks or change placement rules
        final BlockState blockState = this.getBlockData().getBlockState();
        // 1.12 World#mayPlace(block, pos, skipCollisionCheck = true, UP, this) ignored the collision box of
        // the placed block and only checked "block at the position is not the same block" plus "the block at
        // the position can be replaced". 26.3 answers the second half with BlockState#canBeReplaced (the
        // material replaceable flag of 1.12). Block#canSurvive is not checked here, 1.12 let the block get
        // removed by the neighbour updates the setBlock flags below trigger.
        final BlockState currentState = this.level().getBlockState(pos);
        if (currentState.getBlock() != blockState.getBlock() && currentState.canBeReplaced()) {

            // 1.12 flag 11 = UPDATE_NEIGHBORS(1) | UPDATE_CLIENTS(2) | UPDATE_IMMEDIATE(8), the same value
            // BlockItem#placeBlock uses in 26.3. Level#destroyBlock is not used, 1.12 replaced the block
            // without dropping the existing one.
            if (!this.level().setBlock(pos, blockState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE)) return false;

            if (this.getBlockData().getBlockEntityData() != null) {
                final BlockEntity blockEntity = this.level().getBlockEntity(pos);

                if (blockEntity != null) {
                    // 1.12 tile entity write, saveWithId adds the "id" the check below compares
                    final TagValueOutput save = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level().registryAccess());
                    blockEntity.saveWithId(save);
                    final CompoundTag currentSave = save.buildResult();

                    // Ensure these are the same tile saves, otherwise we can corrupt a block badly
                    if (Objects.equals(currentSave.getString("id").orElse(null), this.getBlockData().getBlockEntityData().getString("id").orElse(null))) {
                        currentSave.merge(this.getBlockData().getBlockEntityData());
                        currentSave.putInt("x", pos.getX());
                        currentSave.putInt("y", pos.getY());
                        currentSave.putInt("z", pos.getZ());
                        // 1.12 tile entity read / markDirty; the coordinates and the id just written are
                        // ignored by loadCustomOnly, they are kept to mirror the 1.12 save shape
                        blockEntity.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, this.level().registryAccess(), currentSave));
                        blockEntity.setChanged();
                    }
                }
            }

            // Play placement audio, RIP headphone users when redmatter spams :D
            final SoundType soundtype = blockState.getSoundType();
            this.level().playSound(null, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

            return true;
        }
        return false;
    }

    protected void dropSourceStack(ItemStack itemStack) {
        // TODO implement this.shouldDropItem && this.world.getGameRules().getBoolean("doEntityDrops")
        if (itemStack != null && !itemStack.isEmpty()) {
            final ItemEntity entityItem = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack);
            if (!this.level().addFreshEntity(entityItem)) {
                ICBMClassic.logger().error("EntityFlyingBlock: Failed to drop source stack '{}' at dim[{}] pos[{}]", itemStack, this.level().dimension().identifier(), this.blockPosition());
            }
        }
    }

    @Override
    protected DamageSource getImpactDamageSource(Entity entityHit, float velocity, HitResult hit) {
        // 1.12 used DamageSource.FALLING_BLOCK, a plain source without an entity. 26.3 builds the source
        // from the vanilla falling_block damage type (DamageTypes.FALLING_BLOCK), which keeps the 1.12
        // "fallingBlock" death message, through the same helper the other ICBM damage sources use.
        return ModDamageTypes.source(this.level(), DamageTypes.FALLING_BLOCK);
    }

    @Override
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit) {
        return Mth.ceil(velocity * ConfigFlyingBlocks.damageScale);
    }

    /**
     * 1.12 EntityFlyingBlock had no health (the projectile {@code hasHealth} flag is false for it), so
     * impact damage and player hits were ignored. 26.3 requires the {@link Entity#hurtServer} override.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    /** 1.12 {@code isImmuneToFire = true} */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Returns true if other Entities should be prevented from moving through this Entity. */
    @Override
    public boolean isPushable() {
        return true;
    }

    /** 1.12 {@code canBeCollidedWith()} returned true, that is the 26.3 default for pickable entities. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        // 1.12 EntityFlyingBlock#writeEntityToNBT called super first, EntityProjectile holds the ground data
        // and the kill times
        super.addAdditionalSaveData(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        // 1.12 EntityFlyingBlock#readEntityFromNBT called super first
        super.readAdditionalSaveData(in);
        SAVE_LOGIC.load(this, in);
        // 26.3 has no spawn data packet, so a restored state has to be pushed into the synced data for
        // clients that start tracking the entity after it was loaded
        this.syncBlockState();
    }

    private static final SaveHandler<EntityFlyingBlock> SAVE_LOGIC = new SaveHandler<EntityFlyingBlock>()
        //Stuck in ground data
        .mainRoot()
        .nodeFloat("gravity", EntityFlyingBlock::getProjectileGravity, EntityFlyingBlock::setProjectileGravity)
        .nodeSavable("mimic_block", EntityFlyingBlock::getBlockData, EntityFlyingBlock::setBlockData, BlockCaptureData::new)
        .base();
}
