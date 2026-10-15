package icbm.classic.content.blocks.launcher.cruise;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.events.LauncherSetTargetEvent;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.content.blocks.launcher.FiringPackage;
import icbm.classic.content.blocks.launcher.cruise.gui.ContainerCruiseLauncher;
import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherNode;
import icbm.classic.content.missile.logic.source.cause.RedstoneCause;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.lib.capability.launcher.CapabilityMissileHolder;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.tile.TickAction;
import icbm.classic.lib.transform.rotation.EulerAngle;
import icbm.classic.lib.transform.vector.Pos;
import icbm.classic.prefab.gui.IPlayerUsing;
import icbm.classic.prefab.inventory.InventorySlot;
import icbm.classic.prefab.inventory.InventoryWithSlots;
import icbm.classic.prefab.tile.IGuiTile;
import icbm.classic.prefab.tile.TileMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Cruise launcher block entity, aiming and firing direct-flight missiles. */
public class TileCruiseLauncher extends TileMachine implements IGuiTile, ILauncherComponent, IMachineInfo, IPlayerUsing
{
    public static final net.minecraft.resources.Identifier REGISTRY_NAME = ICBMConstants.id("cruise_launcher");
    public static final double MISSILE_HOLDER_Y = 2.0;
    private static final int REDSTONE_CHECK_RATE = 40;
    private static final double ROTATION_SPEED = 10.0;

    private Vec3 target = Vec3.ZERO;
    protected final EulerAngle aim = new EulerAngle(0, 0, 0);
    protected final EulerAngle currentAim = new EulerAngle(0, 0, 0);
    protected long lastRotationUpdate = System.nanoTime();
    protected double deltaTime;
    public ItemStack cachedMissileStack = ItemStack.EMPTY;
    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> ConfigLauncher.POWER_CAPACITY, () -> ConfigLauncher.POWER_CAPACITY).onChange(this::setChanged);
    public final InventoryWithSlots inventory = new InventoryWithSlots(2)
            .withChangeCallback((slot, stack) -> setChanged())
            .withSlot(new InventorySlot(0, ICBMClassicHelpers::isMissile))
            .withSlot(new InventorySlot(1, TileCruiseLauncher::isReservedBatteryItem));
    public final CapabilityMissileHolder missileHolder = new CapabilityMissileHolder(inventory, 0);
    public final CLauncherCapability launcher = new CLauncherCapability(this);
    public final RadioCruise radio = new RadioCruise(this);
    private final LauncherNode launcherNode = new LauncherNode(this, true);
    private final List<ServerPlayer> playersUsing = new LinkedList<>();
    private FiringPackage firingPackage;
    private int serverTicks;
    private boolean radioRegistered;

    public static final PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher> PACKET_GUI =
            (PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>) new PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>(REGISTRY_NAME, "gui", TileCruiseLauncher.class)
                    .fromServer()
                    .nodeVec3(TileCruiseLauncher::getTarget, TileCruiseLauncher::setTarget)
                    .nodeString(tile -> tile.radio.getChannel(), (tile, value) -> tile.radio.setChannel(value))
                    .nodeBoolean(tile -> tile.radio.isDisabled(), (tile, value) -> tile.radio.setDisabled(value))
                    .nodeDouble(tile -> tile.currentAim.yaw(), (tile, value) -> tile.currentAim.setYaw(value))
                    .nodeDouble(tile -> tile.currentAim.pitch(), (tile, value) -> tile.currentAim.setPitch(value));

    public static final PacketCodexTile<TileCruiseLauncher, RadioCruise> PACKET_RADIO_HZ =
            (PacketCodexTile<TileCruiseLauncher, RadioCruise>) new PacketCodexTile<TileCruiseLauncher, RadioCruise>(REGISTRY_NAME, "radio.frequency", RadioCruise.class, tile -> tile.radio)
                    .fromClient()
                    .nodeString(RadioCruise::getChannel, (radio, value) -> {
                        if (value != null && value.length() <= 32) radio.setChannel(value);
                    })
                    .onFinished((tile, radio, player) -> tile.setChanged());

    public static final PacketCodexTile<TileCruiseLauncher, RadioCruise> PACKET_RADIO_DISABLE =
            (PacketCodexTile<TileCruiseLauncher, RadioCruise>) new PacketCodexTile<TileCruiseLauncher, RadioCruise>(REGISTRY_NAME, "radio.disable", RadioCruise.class, tile -> tile.radio)
                    .fromClient().toggleBoolean(RadioCruise::isDisabled, RadioCruise::setDisabled)
                    .onFinished((tile, radio, player) -> tile.setChanged());

    public static final PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher> PACKET_TARGET =
            (PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>) new PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>(REGISTRY_NAME, "target", TileCruiseLauncher.class)
                    .fromClient().nodeVec3(TileCruiseLauncher::getTarget, (tile, value) -> {
                        final Level level = tile.getLevel();
                        if (value != null && level != null && level.getWorldBorder().isWithinBounds(value)) tile.setTarget(value);
                    }).onFinished((tile, target, player) -> tile.setChanged());

    public static final PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher> PACKET_LAUNCH =
            (PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>) new PacketCodexTile<TileCruiseLauncher, TileCruiseLauncher>(REGISTRY_NAME, "launch", TileCruiseLauncher.class)
                    .fromClient().onFinished((tile, target, player) -> {
                        tile.launcher.launch(new BasicTargetData(tile.getTarget()), new icbm.classic.content.missile.logic.source.cause.EntityCause(player), false);
                        tile.setChanged();
                    });

    public static void registerCodexes()
    {
        PacketCodexReg.register(PACKET_GUI, PACKET_RADIO_HZ, PACKET_RADIO_DISABLE, PACKET_TARGET, PACKET_LAUNCH);
    }

    public TileCruiseLauncher(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.CRUISE_LAUNCHER, pos, state);
        tickActions.add(new TickAction(3, true, tile -> PACKET_GUI.sendToGuiUsers(this, playersUsing)));
        tickActions.add(new TickAction(20, true, tile -> playersUsing.removeIf(player -> !(player.containerMenu instanceof ContainerCruiseLauncher))));
        tickActions.add(inventory);
    }

    private static boolean isReservedBatteryItem(ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity)
    {
        final TileCruiseLauncher tile = (TileCruiseLauncher) blockEntity;
        if (level.isClientSide())
        {
            tile.clientTick();
            return;
        }
        if (!tile.radioRegistered)
        {
            tile.radioRegistered = true;
            tile.radio.ensureRegistered();
            tile.launcherNode.connectToTiles();
        }
        tile.serverTicks++;
        tile.serverTick((ServerLevel) level);
        tile.deltaTime = Math.min(1.0, Math.max(0.0, (System.nanoTime() - tile.lastRotationUpdate) / 100000000.0));
        tile.lastRotationUpdate = System.nanoTime();
        tile.currentAim.moveTowards(tile.aim, ROTATION_SPEED, tile.deltaTime).clampTo360();
        if (tile.serverTicks % REDSTONE_CHECK_RATE == 0)
        {
            for (Direction side : Direction.values())
            {
                if (level.getSignal(pos.relative(side), side) > 1)
                {
                    tile.firingPackage = new FiringPackage(new BasicTargetData(tile.getTarget()),
                            new RedstoneCause(level, pos, state, side), 0);
                }
            }
        }
        if (tile.firingPackage != null && tile.isAimed())
        {
            tile.firingPackage.setCountDown(tile.firingPackage.getCountDown() - 1);
            if (tile.firingPackage.getCountDown() <= 0)
            {
                tile.firingPackage.launch(tile.launcher);
                tile.firingPackage = null;
                tile.setChanged();
            }
        }
    }

    @Override
    public LauncherNode getNetworkNode()
    {
        return launcherNode;
    }

    @Override
    public void provideInformation(BiConsumer<String, Object> consumer)
    {
        consumer.accept("NEEDS_POWER", ConfigMain.REQUIRES_POWER);
        consumer.accept("ENERGY_COST_ACTION", getFiringCost());
    }

    public int getFiringCost()
    {
        return ConfigLauncher.POWER_COST;
    }

    public Vec3 getTarget()
    {
        return target;
    }

    public void setTarget(Vec3 value)
    {
        if (value == null || Objects.equals(value, target)) return;
        final Level level = getLevel();
        if (level == null || level.isClientSide())
        {
            target = value;
        }
        else
        {
            final LauncherSetTargetEvent event = new LauncherSetTargetEvent(level, getBlockPos(), value);
            if (LauncherSetTargetEvent.EVENT.invoker().onSetTarget(event))
            {
                target = event.target == null ? Vec3.ZERO : event.target;
                setChanged();
            }
        }
        updateAimAngle();
    }

    public boolean isAimed()
    {
        return currentAim.isWithin(aim, 0.01);
    }

    /** Current client-visible turret yaw, in degrees, for the S5.07 block entity renderer. */
    public double getCurrentYaw()
    {
        return currentAim.yaw();
    }

    /** Current client-visible turret pitch, in degrees, for the S5.07 block entity renderer. */
    public double getCurrentPitch()
    {
        return currentAim.pitch();
    }
    private void updateAimAngle()
    {
        if (hasTarget())
        {
            final Vec3 center = new Vec3(getBlockPos().getX() + 0.5, getBlockPos().getY() + MISSILE_HOLDER_Y,
                    getBlockPos().getZ() + 0.5);
            aim.set(new Pos(center).toEulerAngle(target).clampTo360());
            aim.setYaw(EulerAngle.clampPos360(aim.yaw()));
        }
        else
        {
            aim.set(0, 0, 0);
        }
    }

    private boolean hasTarget()
    {
        return target != null && !target.equals(Vec3.ZERO);
    }

    public boolean canSpawnMissileWithNoCollision()
    {
        final Level level = getLevel();
        if (level == null) return false;
        final BlockPos origin = getBlockPos();
        for (int y = 1; y <= 2; y++)
        {
            for (int x = -1; x < 2; x++)
            {
                for (int z = -1; z < 2; z++)
                {
                    if (!level.getBlockState(origin.offset(x, y, z)).isAir()) return false;
                }
            }
        }
        return true;
    }

    public boolean isTooClose(Vec3 position)
    {
        return Vec3.atCenterOf(getBlockPos()).distanceTo(position) < 20;
    }

    public ItemStack getMissileStack()
    {
        return isClient() && !cachedMissileStack.isEmpty() ? cachedMissileStack : missileHolder.getMissileStack();
    }

    public FiringPackage getFiringPackage()
    {
        return firingPackage;
    }

    public void setFiringPackage(FiringPackage firingPackage)
    {
        this.firingPackage = firingPackage;
        setChanged();
    }
    public CapabilityMissileHolder getMissileHolder()
    {
        return missileHolder;
    }

    public List<ServerPlayer> getPlayersUsing()
    {
        return playersUsing;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player)
    {
        return new ContainerCruiseLauncher(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.icbmclassic.cruise_launcher");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        launcherNode.onTileRemoved();
        final Level level = getLevel();
        if (level != null) Containers.dropContents(level, pos, inventory);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved()
    {
        radio.unregister();
        launcherNode.onTileRemoved();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        SAVE_LOGIC.save(this, output);
        output.putLong("energy", energyStorage.getStored());
        currentAim.save(output.child("current_aim"));
        if (firingPackage != null) firingPackage.save(output.child("firing_package"));
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        SAVE_LOGIC.load(this, input);
        energyStorage.setStored(input.getLongOr("energy", 0));
        firingPackage = null;
        input.child("firing_package").ifPresent(child -> {
            firingPackage = new FiringPackage(null, null);
            firingPackage.load(child);
        });
        input.child("current_aim").ifPresent(currentAim::readFrom);
        cachedMissileStack = inventory.getItem(0);
        updateAimAngle();
        currentAim.set(aim);
    }

    private static final SaveHandler<TileCruiseLauncher> SAVE_LOGIC = new SaveHandler<TileCruiseLauncher>()
            .mainRoot()
            .nodeSavable("inventory", tile -> tile.inventory)
            .nodeSavable("radio", tile -> tile.radio)
            .nodeVec3("target", TileCruiseLauncher::getTarget, (tile, value) -> tile.target = value)
            .base();
}
