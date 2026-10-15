package icbm.classic.content.blocks.launcher.base;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IMissileHolder;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.content.blocks.launcher.FiringPackage;
import icbm.classic.content.blocks.launcher.base.gui.ContainerLaunchBase;
import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherNode;
import icbm.classic.content.entity.EntityPlayerSeat;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.capability.launcher.CapabilityMissileHolder;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.tile.TickAction;
import icbm.classic.prefab.inventory.InventorySlot;
import net.minecraft.network.chat.Component;
import icbm.classic.prefab.inventory.InventoryWithSlots;
import icbm.classic.prefab.gui.IPlayerUsing;
import icbm.classic.prefab.tile.BlockICBM;
import icbm.classic.prefab.tile.TileMachine;
import icbm.classic.prefab.tile.IGuiTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/** Block entity at the base of the launcher network. */
public class TileLauncherBase extends TileMachine implements ILauncherComponent, IMachineInfo, IPlayerUsing, IGuiTile
{
    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> ConfigLauncher.POWER_CAPACITY, () -> ConfigLauncher.POWER_CAPACITY).onChange(this::setChanged);
    public final InventoryWithSlots inventory = new InventoryWithSlots(2)
            .withChangeCallback((slot, stack) -> setChanged())
            .withSlot(new InventorySlot(0, ICBMClassicHelpers::isMissile)
                    .withInsertCheck(stack -> !checkForMissileInBounds())
                    .withChangeCallback(stack -> setChanged()))
            .withSlot(new InventorySlot(1, TileLauncherBase::isReservedBatteryItem));

    public final IMissileHolder missileHolder = new CapabilityMissileHolder(inventory, 0);
    public final LauncherCapability missileLauncher = new LauncherCapability(this);
    public EntityPlayerSeat seat;
    public ItemStack cachedMissileStack = ItemStack.EMPTY;

    private final LauncherNode launcherNode = new LauncherNode(this, true);
    private final List<ServerPlayer> playersUsing = new LinkedList<>();
    private boolean checkMissileCollision = true;
    private boolean hasMissileCollision;
    private boolean networkInitialized;
    private int firingDelay;
    private int lockHeight = 3;
    private int groupId = -1;
    private int groupIndex = -1;
    private Direction seatSide;
    public static final PacketCodexTile<TileLauncherBase, TileLauncherBase> PACKET_FIRING_DELAY =
            (PacketCodexTile<TileLauncherBase, TileLauncherBase>) new PacketCodexTile<TileLauncherBase, TileLauncherBase>(ICBMConstants.id("launcher_base"), "firing_delay", TileLauncherBase.class)
                    .fromClient().nodeInt(TileLauncherBase::getFiringDelay, TileLauncherBase::setFiringDelay);
    public static final PacketCodexTile<TileLauncherBase, TileLauncherBase> PACKET_LOCK_HEIGHT =
            (PacketCodexTile<TileLauncherBase, TileLauncherBase>) new PacketCodexTile<TileLauncherBase, TileLauncherBase>(ICBMConstants.id("launcher_base"), "lock_height", TileLauncherBase.class)
                    .fromClient().nodeInt(TileLauncherBase::getLockHeight, TileLauncherBase::setLockHeight);
    public static final PacketCodexTile<TileLauncherBase, TileLauncherBase> PACKET_GROUP_ID =
            (PacketCodexTile<TileLauncherBase, TileLauncherBase>) new PacketCodexTile<TileLauncherBase, TileLauncherBase>(ICBMConstants.id("launcher_base"), "group_id", TileLauncherBase.class)
                    .fromClient().nodeInt(TileLauncherBase::getGroupId, TileLauncherBase::setGroupId);
    public static final PacketCodexTile<TileLauncherBase, TileLauncherBase> PACKET_GROUP_INDEX =
            (PacketCodexTile<TileLauncherBase, TileLauncherBase>) new PacketCodexTile<TileLauncherBase, TileLauncherBase>(ICBMConstants.id("launcher_base"), "group_index", TileLauncherBase.class)
                    .fromClient().nodeInt(TileLauncherBase::getGroupIndex, TileLauncherBase::setGroupIndex);
    public static final PacketCodexTile<TileLauncherBase, TileLauncherBase> PACKET_SEAT_ROTATION =
            (PacketCodexTile<TileLauncherBase, TileLauncherBase>) new PacketCodexTile<TileLauncherBase, TileLauncherBase>(ICBMConstants.id("launcher_base"), "seat_rotation", TileLauncherBase.class)
                    .fromClient().nodeDirection(TileLauncherBase::getSeatSide, TileLauncherBase::setSeatSide);
    public static void registerCodexes()
    {
        PacketCodexReg.register(PACKET_FIRING_DELAY, PACKET_LOCK_HEIGHT, PACKET_GROUP_ID, PACKET_GROUP_INDEX, PACKET_SEAT_ROTATION);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player)
    {
        return new ContainerLaunchBase(id, playerInventory, this);
    }

    @Override
    public List<ServerPlayer> getPlayersUsing()
    {
        return playersUsing;
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.icbmclassic.launcher_base");
    }
    private FiringPackage firingPackage;

    public TileLauncherBase(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.LAUNCHER_BASE, pos, state);
        tickActions.add(new TickAction(() -> isServer() && firingPackage != null, this::handleFirePackage));
        tickActions.add(new TickAction(3, true, this::updateSeat));
        tickActions.add(inventory);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
        final TileLauncherBase be = (TileLauncherBase) blockEntity;
        if (level.isClientSide())
        {
            be.clientTick();
            return;
        }
        if (!be.networkInitialized)
        {
            be.networkInitialized = true;
            be.launcherNode.connectToTiles();
        }
        be.serverTick((ServerLevel) level);
    }

    private static boolean isReservedBatteryItem(ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    @Override
    public void provideInformation(BiConsumer<String, Object> consumer)
    {
        consumer.accept(NEEDS_POWER, ConfigMain.REQUIRES_POWER);
        consumer.accept(ENERGY_COST_ACTION, getFiringCost());
        consumer.accept("MAX_RANGE", ConfigLauncher.RANGE);
        consumer.accept("INACCURACY_BASE", ConfigLauncher.MIN_INACCURACY);
        consumer.accept("INACCURACY_RANGE", ConfigLauncher.SCALED_INACCURACY_DISTANCE);
        consumer.accept("INACCURACY_LAUNCHERS", ConfigLauncher.SCALED_INACCURACY_LAUNCHERS);
    }

    public Direction getLaunchDirection()
    {
        final BlockState state = getBlockState();
        return state.hasProperty(BlockICBM.ROTATION_PROP) ? state.getValue(BlockICBM.ROTATION_PROP) : Direction.UP;
    }

    public Direction getSeatSide()
    {
        if (seatSide == null)
        {
            seatSide = switch (getLaunchDirection())
            {
                case UP, DOWN -> Direction.NORTH;
                default -> Direction.UP;
            };
        }
        return seatSide;
    }

    public void setSeatSide(Direction side)
    {
        seatSide = side;
        setChanged();
    }

    public float getMissileYaw(boolean render)
    {
        if (render)
        {
            return switch (getLaunchDirection())
            {
                case NORTH -> 0;
                case SOUTH -> -180;
                case WEST -> 90;
                case EAST -> -90;
                default -> 0;
            };
        }
        return switch (getLaunchDirection())
        {
            case NORTH -> -180;
            case SOUTH -> 0;
            case WEST -> -90;
            case EAST -> 90;
            default -> 0;
        };
    }

    public float getMissilePitch(boolean render)
    {
        if (render)
        {
            return switch (getLaunchDirection())
            {
                case UP -> 0;
                case DOWN -> -180;
                default -> -90;
            };
        }
        return switch (getLaunchDirection())
        {
            case UP -> 90;
            case DOWN -> -90;
            default -> 0;
        };
    }

    public int getFiringCost()
    {
        return ConfigLauncher.POWER_COST;
    }

    @Override
    public LauncherNode getNetworkNode()
    {
        return launcherNode;
    }

    @Override
    public boolean isValid()
    {
        return !isRemoved();
    }

    public int getFiringDelay() { return firingDelay; }
    public void setFiringDelay(int firingDelay) { this.firingDelay = firingDelay; setChanged(); }
    public int getLockHeight() { return lockHeight; }
    public void setLockHeight(int lockHeight) { this.lockHeight = lockHeight; setChanged(); }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; setChanged(); }
    public int getGroupIndex() { return groupIndex; }
    public void setGroupIndex(int groupIndex) { this.groupIndex = groupIndex; setChanged(); }
    public FiringPackage getFiringPackage() { return firingPackage; }
    public void setFiringPackage(FiringPackage firingPackage) { this.firingPackage = firingPackage; setChanged(); }
    public InventoryWithSlots getInventory() { return inventory; }

    private void handleFirePackage()
    {
        firingPackage.setCountDown(firingPackage.getCountDown() - 1);
        if (firingPackage.getCountDown() <= 0)
        {
            firingPackage.launch(missileLauncher);
            firingPackage = null;
            setChanged();
        }
    }

    private void updateSeat()
    {
        checkMissileCollision = true;
        final Level level = getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }
        if (!getMissileStack().isEmpty() && seat == null)
        {
            seat = new EntityPlayerSeat(level);
            ((ServerLevel) level).addFreshEntity(seat);
        }
        else if (getMissileStack().isEmpty() && seat != null)
        {
            seat.getPassengers().forEach(Entity::stopRiding);
            seat.discard();
            seat = null;
        }
    }

    public boolean checkForMissileInBounds()
    {
        if (checkMissileCollision)
        {
            checkMissileCollision = false;
            final Level level = getLevel();
            if (level != null)
            {
                final BlockPos pos = getBlockPos();
                final AABB bounds = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 5, pos.getZ() + 1);
                hasMissileCollision = !level.getEntitiesOfClass(EntityMissile.class, bounds).isEmpty();
            }
        }
        return hasMissileCollision;
    }

    public ItemStack getMissileStack()
    {
        if (isClient() && !cachedMissileStack.isEmpty())
        {
            return cachedMissileStack;
        }
        return missileHolder.getMissileStack();
    }

    public boolean tryInsertMissile(Player player, InteractionHand hand, ItemStack heldItem)
    {
        if (getMissileStack().isEmpty() && missileHolder.canSupportMissile(heldItem))
        {
            if (isServer())
            {
                final ItemStack remainder = inventory.insertItem(0, heldItem, false);
                if (!player.getAbilities().instabuild)
                {
                    player.setItemInHand(hand, remainder);
                }
            }
            return true;
        }
        if (player.isShiftKeyDown() && heldItem.isEmpty() && !getMissileStack().isEmpty())
        {
            if (isServer())
            {
                player.setItemInHand(hand, getMissileStack().copy());
                inventory.extractItem(0, 1, false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        launcherNode.onTileRemoved();
        if (getLevel() != null)
        {
            Containers.dropContents(getLevel(), pos, inventory);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved()
    {
        launcherNode.onTileRemoved();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput out)
    {
        super.saveAdditional(out);
        if (firingPackage != null)
        {
            firingPackage.save(out.child("firing_package"));
        }
        out.putLong("energy", energyStorage.getStored());
    }

    @Override
    protected void loadAdditional(ValueInput in)
    {
        super.loadAdditional(in);
        SAVE_LOGIC.load(this, in);
        firingPackage = null;
        in.child("firing_package").ifPresent(input ->
        {
            firingPackage = new FiringPackage(null, null);
            firingPackage.load(input);
        });
        energyStorage.setStored(in.getLongOr("energy", 0));
    }

    private static final SaveHandler<TileLauncherBase> SAVE_LOGIC = new SaveHandler<TileLauncherBase>()
            .mainRoot()
            .nodeInteger("lock_height", TileLauncherBase::getLockHeight, TileLauncherBase::setLockHeight)
            .nodeInteger("group_id", TileLauncherBase::getGroupId, TileLauncherBase::setGroupId)
            .nodeInteger("group_index", TileLauncherBase::getGroupIndex, TileLauncherBase::setGroupIndex)
            .nodeInteger("firing_delay", TileLauncherBase::getFiringDelay, TileLauncherBase::setFiringDelay)
            .nodeSavable("inventory", tile -> tile.inventory)
            .nodeSavable("launcher", tile -> tile.missileLauncher)
            .nodeDirection("seat_side", TileLauncherBase::getSeatSide, TileLauncherBase::setSeatSide)
            .base();
}
