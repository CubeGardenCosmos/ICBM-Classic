package icbm.classic.content.blocks.explosive;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.tile.IRotatable;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.actions.fields.ActionFieldProvider;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Explosive block entity, 1.12 {@code TileEntityExplosive}.
 * <p>
 * 1.12 held a {@code CapabilityExplosiveStack} built from the item stack the block was placed with; that
 * capability carried the explosive id (item metadata) and the customizations (ender target, ...). 26.3 items
 * are flat (ADR 0002), so this block entity only keeps the customization data of
 * {@code icbmclassic:explosive_settings} - the explosive id is the block ({@link ExplosiveBlock#getExplosiveId()}).
 * The component is copied from the item on placement through {@link #applyImplicitComponents} /
 * {@link #collectImplicitComponents} ({@code BlockItem#place} calls
 * {@code BlockEntity#applyComponentsFromItemStack} before {@code setPlacedBy}) and saved under the 1.12 NBT
 * key {@link NBTConstants#EXPLOSIVE_STACK}.
 * <p>
 * 1.12 never saved the customization list ({@code CapabilityExplosiveStack} wrote the stack, and
 * {@code toStack} had a "TODO save customizations"); the port keeps the key and saves the settings component
 * itself so the ender target survives a save/load (the S3.09 acceptance asks for that round trip).
 * <p>
 * 1.12 {@code TileEntityExplosive} did not tick (no {@code ITickable#update}), so 26.3 needs no
 * {@code EntityBlock#getTicker}; the fuse is the primed explosive entity's job.
 * <p>
 * 1.12 also overrode {@code getUpdatePacket}/{@code getUpdateTag}/{@code onDataPacket} to send the explosive
 * capability to the client. The 26.3 block entity has no client visible state (the block model only depends
 * on the facing and the primed explosive entity syncs what it renders), so there is nothing to send and no
 * update packet is needed.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 1/6/2019.
 */
public class ExplosiveBlockEntity extends BlockEntity implements IRotatable
{
    /** 1.12 {@code TileEntityExplosive.NBT_EXPLOSIVE_STACK}, kept as the save key of the 26.3 settings. */
    public static final String NBT_EXPLOSIVE_STACK = NBTConstants.EXPLOSIVE_STACK;

    /**
     * Is the tile currently exploding
     * <p>
     * 1.12 did not save this flag and neither does the port.
     */
    public boolean hasBeenTriggered = false;

    /** 1.12 {@code capabilityExplosive} held the placed item stack; 26.3 holds its customization component. */
    private ExplosiveSettings settings = ExplosiveSettings.EMPTY;

    public ExplosiveBlockEntity(BlockPos pos, BlockState state)
    {
        // 1.12 was created by BlockExplosive#createNewTileEntity, 26.3 by ExplosiveBlock#newBlockEntity
        super(ModBlockEntities.EXPLOSIVE, pos, state);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // customization data
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Customization of the placed explosive, empty when the item carried none. */
    public ExplosiveSettings getSettings()
    {
        return settings;
    }

    /** Replaces the customization, {@code null} resets it. 1.12 {@code capabilityExplosive.addCustomization}. */
    public void setSettings(ExplosiveSettings settings)
    {
        this.settings = settings == null ? ExplosiveSettings.EMPTY : settings;
        setChanged();
    }

    /**
     * Reads the customization of the item the block was placed with, 1.12
     * {@code onBlockPlacedBy} {@code explosive.capabilityExplosive = new CapabilityExplosiveStack(itemStack)}.
     * <p>
     * Reading the component through this getter marks it as implicit ({@code BlockEntity#applyComponents}
     * forgets every component the getter was asked for), so it is not duplicated in the block entity
     * component map; {@link #saveAdditional} saves the value itself under the 1.12 key instead.
     */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components)
    {
        final ExplosiveSettings placed = components.get(ModDataComponents.EXPLOSIVE_SETTINGS);
        if (placed != null)
        {
            this.settings = placed;
        }
    }

    /**
     * Hands the customization back to the item, 1.12 {@code BlockExplosive#getPickBlock} rebuilt the block
     * item from the actual state (the flat item is the block now, so only the component is left to carry).
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components)
    {
        if (!ExplosiveSettings.EMPTY.equals(settings))
        {
            components.set(ModDataComponents.EXPLOSIVE_SETTINGS, settings);
        }
    }

    /** 1.12 {@code writeToNBT} wrote {@code capabilityExplosive.toStack()} under {@code explosive_stack}. */
    @Override
    protected void saveAdditional(ValueOutput out)
    {
        out.store(NBT_EXPLOSIVE_STACK, ExplosiveSettings.CODEC, settings);
    }

    /** 1.12 {@code readFromNBT} read the stack back from {@code explosive_stack}. */
    @Override
    protected void loadAdditional(ValueInput in)
    {
        settings = in.read(NBT_EXPLOSIVE_STACK, ExplosiveSettings.CODEC).orElse(ExplosiveSettings.EMPTY);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // explosive
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Explosive of the block this entity belongs to, 1.12 read it off the placed item stack. */
    @Nullable
    public Identifier getExplosiveId()
    {
        return getBlockState().getBlock() instanceof ExplosiveBlock explosive ? explosive.getExplosiveId() : null;
    }

    /** Registry data of {@link #getExplosiveId()}, 1.12 {@code capabilityExplosive.getExplosiveData()}. */
    public IExplosiveData getExplosiveData()
    {
        final Identifier id = getExplosiveId();
        return id == null ? ICBMExplosives.CONDENSED : ICBMClassicHelpers.getExplosive(id, false);
    }

    /**
     * 1.12 {@code TileEntityExplosive#trigger}: detonate this block once. Breaching explosives skip the primed
     * entity and run their blast straight away, everything else spawns a primed explosive that carries the
     * fuse of the explosive content registry.
     *
     * @param setFire sets the primed explosive on fire, used by the placement fire/lava scan
     */
    public void trigger(boolean setFire)
    {
        if (!hasBeenTriggered)
        {
            hasBeenTriggered = true;

            final IExplosiveData explosiveData = getExplosiveData();
            final ItemStack stack = buildExplosiveStack();
            final double x = worldPosition.getX() + 0.5D;
            final double y = worldPosition.getY() + 0.5D;
            final double z = worldPosition.getZ() + 0.5D;

            // TODO handle this better in 1.13+ as this is a temp work around for direction being funky for breach
            if (explosiveData == ICBMExplosives.BREACHING)
            {
                //1.12: new ActionSource(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), null)
                final IActionSource source = new ActionSource(level, new Vec3(x, y, z), null);
                final Direction direction = this.getDirection().getOpposite();
                explosiveData
                        .create(level, x, y, z, source, new ActionFieldProvider().field(ActionFields.HOST_DIRECTION, () -> direction))
                        .doAction();
            }
            else
            {
                //1.12: new EntityExplosive(world, new Pos(pos).add(0.5), getDirection(), capabilityExplosive.toStack())
                final EntityExplosive entityExplosive = new EntityExplosive(level, new Pos(x, y, z), getDirection(),
                        getExplosiveId(), stack);
                //TODO check for tick rate, trigger directly if tick is less than 3

                if (setFire)
                {
                    //1.12: entityExplosive.setFire(100) (seconds)
                    entityExplosive.igniteForSeconds(100);
                }

                level.addFreshEntity(entityExplosive); //1.12: world.spawnEntity(entityExplosive)
            }
            level.removeBlock(worldPosition, false); //1.12: world.setBlockToAir(pos)

            //1.12 logged the stack it took off the capability (capabilityExplosive.toStack())
            ICBMClassic.logger().info("TileEntityExplosive: Triggered ITEM{" + stack + "] "
                    + getExplosiveId() + " at location " + worldPosition);
        }
    }

    /**
     * The stack 1.12 handed to the primed explosive ({@code capabilityExplosive.toStack()}): the block item
     * with the customization component of this block entity.
     */
    private ItemStack buildExplosiveStack()
    {
        final ItemStack stack = new ItemStack(getBlockState().getBlock());
        if (!ExplosiveSettings.EMPTY.equals(settings))
        {
            stack.set(ModDataComponents.EXPLOSIVE_SETTINGS, settings);
        }
        return stack;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // rotation (1.12 TileEntityExplosive implemented IRotatable through the block metadata)
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Direction getDirection()
    {
        //1.12: the facing came from the block metadata, which held the rotation ordinal
        final BlockState state = getBlockState();
        return state.hasProperty(ExplosiveBlock.FACING) ? state.getValue(ExplosiveBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void setDirection(Direction facingDirection)
    {
        final Level level = getLevel();
        if (level != null)
        {
            //1.12: state.withProperty(BlockExplosive.ROTATION_PROP, facingDirection) + world.setBlockState(pos, state, 2)
            final BlockState state = getBlockState();
            if (state.hasProperty(ExplosiveBlock.FACING))
            {
                level.setBlock(worldPosition, state.setValue(ExplosiveBlock.FACING, facingDirection), Block.UPDATE_CLIENTS);
            }
        }
    }
}
