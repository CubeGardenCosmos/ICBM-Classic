package icbm.classic.content.entity;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.capability.ex.CapabilityExplosiveEntity;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.mixin.MinecartTNTAccessor;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;

/**
 * Bomb cart. A minecart that carries an ICBM explosive instead of the vanilla TNT and runs the matching
 * blast (through {@link CapabilityExplosiveEntity} and {@link ICBMClassicAPI#EX_MINECART_REGISTRY}).
 * <p>
 * Registered as {@code icbmclassic:bomb_cart} ({@code ModEntities.BOMB_CART}), see registry-ids.md §4.
 * <p>
 * Porting notes (porting-guide §2.4):
 * <ul>
 *     <li>1.12 {@code EntityMinecartTNT#explodeCart(double)} → 26.3
 *     {@link MinecartTNT#explode(DamageSource, double)}, {@code killMinecart(DamageSource)} →
 *     {@code VehicleEntity#destroy(ServerLevel, DamageSource)}, {@code onUpdate()} → {@code tick()},
 *     {@code ignite()} → {@link MinecartTNT#primeFuse(DamageSource)} and {@code getDefaultDisplayTile()} →
 *     {@link #getDefaultDisplayBlockState()}.</li>
 *     <li>1.12 reached the private {@code minecartTNTFuse} through an access transformer; 26.3 has no setter
 *     for the fuse, so {@link MinecartTNTAccessor} exposes it (see {@code icbmclassic.mixins.json}).</li>
 *     <li>{@code IEntityAdditionalSpawnData} is gone: 1.12 sent the explosive item stack as spawn data,
 *     26.3 syncs the same stack through {@link SynchedEntityData} ({@link #DATA_RENDER_STACK}) so the client
 *     can pick the display block and the icon. The stack stays the payload of the
 *     {@link CapabilityExplosiveEntity} for save/customizations.</li>
 *     <li>The 1.12 legacy save branch (numeric explosive id at {@code NBTConstants.EXPLOSIVE}) is dropped,
 *     ADR 0002 does not migrate 1.12 saves.</li>
 * </ul>
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class EntityBombCart extends MinecartTNT
{
    /**
     * Stack the cart was built from. 1.12 sent it as the spawn data; it also carries the explosive (flat
     * items, ADR 0002) and replaces the vanilla TNT display block.
     */
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK = SynchedEntityData.defineId(EntityBombCart.class, EntityDataSerializers.ITEM_STACK);

    /** Explosive capability, 1.12 {@code explosive} (holds the stack and the customizations) */
    public final CapabilityExplosiveEntity explosive = new CapabilityExplosiveEntity(this);

    public EntityBombCart(EntityType<EntityBombCart> type, Level level)
    {
        super(type, level);
    }

    /**
     * Spawns a bomb cart with the given explosive stack, 1.12
     * {@code EntityBombCart(World, x, y, z, ItemStack)}.
     */
    public EntityBombCart(Level level, double x, double y, double z, ItemStack itemStack)
    {
        this(ModEntities.BOMB_CART, level);
        // 1.12 EntityMinecart(World, x, y, z) set the position; snapTo also copies it into the old position
        // so the client does not interpolate the cart in from the world origin
        this.snapTo(x, y, z);
        this.setItemStack(itemStack);
    }

    /**
     * Sets the explosive stack of this cart.
     *
     * @param stack - explosive stack
     * @return this
     */
    public EntityBombCart setItemStack(ItemStack stack)
    {
        explosive.setStack(stack);
        // 1.12 sent the stack as spawn data; 26.3 syncs it (see DATA_RENDER_STACK)
        this.entityData.set(DATA_RENDER_STACK, stack.copyWithCount(1));
        return this;
    }

    /** Stack to render/label this cart, 1.12 {@code explosive.toStack()}. */
    public ItemStack renderItemStack()
    {
        return this.entityData.get(DATA_RENDER_STACK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
    }

    /**
     * Ticks the fuse of the explosive content registry, 1.12 {@code EntityBombCart#onUpdate}.
     */
    @Override
    public void tick()
    {
        super.tick();
        if (isPrimed()) // 1.12 isIgnited()
        {
            ICBMClassicAPI.EX_MINECART_REGISTRY.tickFuse(this, getExplosiveData(), getFuse());
        }
    }

    /**
     * 1.12 {@code ignite()}: sets the fuse of the explosive content registry and tells the client to start
     * the fuse effects. 26.3 {@link MinecartTNT#primeFuse(DamageSource)} sets the vanilla 80 tick fuse and
     * does the broadcast + sound, so the fuse is overwritten with the ICBM fuse time afterwards.
     */
    @Override
    public void primeFuse(DamageSource source)
    {
        // 1.12: this.minecartTNTFuse = ICBMClassicAPI.EX_MINECART_REGISTRY.getFuseTime(this, explosive.getExplosiveData())
        super.primeFuse(source);
        if (isPrimed()) // super.primeFuse bails out when the TNT_EXPLODES gamerule is off
        {
            ((MinecartTNTAccessor) this).icbmclassic$setFuse(
                    ICBMClassicAPI.EX_MINECART_REGISTRY.getFuseTime(this, getExplosiveData()));
        }
    }

    /**
     * Runs the blast of the explosive this cart carries, 1.12 {@code EntityBombCart#explodeCart(double)}.
     */
    @Override
    protected void explode(DamageSource source, double speed)
    {
        // TODO(S4.01) build the cause from EntityCause(this) once content/missile/logic/source/cause is ported,
        //  same as EntityExplosive#explode
        explosive.doExplosion(this.getX(), this.getY(), this.getZ(),
                new ActionSource(level(), new Vec3(this.getX(), this.getY(), this.getZ()), null));
        this.discard(); // 1.12 setDead()
    }

    /**
     * 1.12 {@code killMinecart(DamageSource)}: drops the cart item unless the cart was blown up and runs the
     * blast when the cart was burning, exploding or moving fast. 26.3 {@code VehicleEntity#destroy} is the
     * hook for a cart whose health ran out (vanilla primes the cart instead, which is not the 1.12 behaviour).
     */
    @Override
    public void destroy(ServerLevel level, DamageSource source)
    {
        this.discard();
        final double speed = this.getDeltaMovement().horizontalDistanceSqr();

        if (!source.is(DamageTypeTags.IS_EXPLOSION))
        {
            // 1.12 entityDropItem(getCartItem(), 0.0F)
            this.destroy(level, getDropItem());
        }

        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION) || speed >= 0.009999999776482582D)
        {
            this.explode(source, speed);
        }
    }

    /**
     * Item the cart drops, 1.12 {@code getCartItem()}.
     */
    @Override
    protected Item getDropItem()
    {
        final ItemStack stack = explosive.toStack();
        return stack.isEmpty() ? Items.TNT_MINECART : stack.getItem();
    }

    /**
     * Stack shown in the creative pick / by Waila style mods, 1.12 {@code getCartItem()}.
     * <p>
     * 1.12 read it off the explosive capability, which the client filled from the spawn data; 26.3 keeps the
     * capability server side, so the synced {@link #renderItemStack()} is used with the capability as the
     * server side fallback.
     */
    @Override
    public ItemStack getPickResult()
    {
        final ItemStack synced = renderItemStack();
        return synced.isEmpty() ? explosive.toStack() : synced;
    }

    /**
     * Display block of the cart. 1.12 built it from the shared explosive block:
     * {@code blockExplosive.getDefaultState().withProperty(EX_PROP, ex).withProperty(ROTATION_PROP, UP)}; the
     * flat blocks carry the explosive themselves, so the block is looked up by the explosive id.
     */
    @Override
    public BlockState getDefaultDisplayBlockState()
    {
        final Block block = ModBlocks.EXPLOSIVE_BLOCKS.get(getExplosiveData().getRegistryKey());
        if (block instanceof ExplosiveBlock)
        {
            return block.defaultBlockState().setValue(ExplosiveBlock.FACING, Direction.UP);
        }
        return super.getDefaultDisplayBlockState();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        super.readAdditionalSaveData(in);
        SAVE_LOGIC.load(this, in);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        super.addAdditionalSaveData(out);
        SAVE_LOGIC.save(this, out);
    }

    /**
     * 1.12 stored the capability under {@code NBTConstants.EXPLOSIVE}; the port keeps the key and delegates to
     * {@link CapabilityExplosiveEntity#save(ValueOutput)}, which names its own item stack node for the 26.3
     * item stack codec.
     */
    private static final SaveHandler<EntityBombCart> SAVE_LOGIC = new SaveHandler<EntityBombCart>()
        .mainRoot()
        /* */.nodeSavable(NBTConstants.EXPLOSIVE, EntityBombCart::getExplosiveCap)
        .base();

    /** The explosive capability of this entity, 1.12 {@code explosive}. */
    public CapabilityExplosiveEntity getExplosiveCap()
    {
        return explosive;
    }

    /**
     * Explosive this cart carries, resolved from the synced stack so it answers the same on both sides.
     * 1.12 resolved it off the explosive capability ({@code explosive.getExplosiveData()}).
     */
    @NonNull
    public IExplosiveData getExplosiveData()
    {
        final IExplosive data = ICBMClassicHelpers.getExplosive(renderItemStack());
        return data != null ? data.getExplosiveData() : ICBMExplosives.CONDENSED;
    }
}
