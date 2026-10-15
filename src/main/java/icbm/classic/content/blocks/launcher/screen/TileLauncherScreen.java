package icbm.classic.content.blocks.launcher.screen;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.events.LauncherSetTargetEvent;
import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import icbm.classic.content.blocks.launcher.LauncherSolution;
import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherEntry;
import icbm.classic.content.blocks.launcher.network.LauncherNode;
import icbm.classic.content.blocks.launcher.screen.gui.ContainerLaunchScreen;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.energy.ExposedEnergyStorage;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.tile.TickAction;
import icbm.classic.prefab.gui.IPlayerUsing;
import icbm.classic.prefab.inventory.InventorySlot;
import icbm.classic.prefab.inventory.InventoryWithSlots;
import icbm.classic.prefab.tile.IGuiTile;
import icbm.classic.prefab.tile.TileMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/** Block entity of the launcher network screen. */
public class TileLauncherScreen extends TileMachine implements ILauncherComponent, IMachineInfo, IPlayerUsing, IGuiTile
{
    public static final net.minecraft.resources.Identifier REGISTRY_NAME = ICBMConstants.id("launcher_screen");

    private Vec3 target = Vec3.ZERO;
    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> ConfigLauncher.POWER_CAPACITY, () -> ConfigLauncher.POWER_CAPACITY).onChange(this::setChanged);
    private ExposedEnergyStorage exposed;

    public ExposedEnergyStorage getExposedEnergy()
    {
        if (exposed == null) exposed = new ExposedEnergyStorage(energyStorage, this::setChanged);
        return exposed;
    }
    public final InventoryWithSlots inventory = new InventoryWithSlots(1)
            .withChangeCallback((slot, stack) -> setChanged())
            .withSlot(new InventorySlot(0, TileLauncherScreen::isReservedBatteryItem));
    private final LauncherNode launcherNode = new LauncherNode(this, false);
    public final RadioScreen radioCap = new RadioScreen(this);
    private float launcherInaccuracy;
    private boolean refreshStatus;
    private boolean radioRegistered;
    private final List<ServerPlayer> playersUsing = new LinkedList<>();
    private int screenTicks;

    public TileLauncherScreen(BlockPos pos, BlockState state)
    {
        super(icbm.classic.content.reg.ModBlockEntities.LAUNCHER_SCREEN, pos, state);
        tickActions.add(new TickAction(3, true, (tile) -> PACKET_GUI.sendToGuiUsers(this, playersUsing)));
        tickActions.add(new TickAction(20, true, (tile) -> playersUsing.removeIf(player -> !(player.containerMenu instanceof ContainerLaunchScreen))));
        tickActions.add(inventory);
    }

    private static boolean isReservedBatteryItem(ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLauncherScreen tile)
    {
        if (level.isClientSide())
        {
            tile.clientTick();
            return;
        }
        tile.screenTicks++;
        if (!tile.radioRegistered)
        {
            tile.radioRegistered = true;
            tile.radioCap.ensureRegistered();
            tile.getNetworkNode().connectToTiles();
        }
        tile.tickPower();
        tile.serverTick((ServerLevel) level);
        if (tile.screenTicks % 5 == 0 || tile.refreshStatus)
        {
            tile.refreshStatus = false;
            final List<LauncherEntry> launchers = tile.getLaunchersInGroup();
            tile.launcherInaccuracy = launchers.stream()
                    .map(LauncherEntry::getLauncher)
                    .map(launcher -> launcher.getInaccuracy(tile.getTarget(), launchers.size()))
                    .max(Float::compareTo).orElse(0f);
        }
        if (tile.screenTicks % 3 == 0 && level.hasNeighborSignal(pos))
        {
            tile.fireAllLaunchers(false);
        }
    }

    public void tickPower()
    {
        icbm.classic.lib.energy.EnergyItemSlotHelper.dischargeInto(inventory, 0, energyStorage);
    }

    @Override
    public LauncherNode getNetworkNode()
    {
        return launcherNode;
    }

    @Override
    public void provideInformation(BiConsumer<String, Object> consumer)
    {
        consumer.accept(NEEDS_POWER, ConfigMain.REQUIRES_POWER);
    }

    public Vec3 getTarget()
    {
        return target;
    }

    public void setTarget(Vec3 target)
    {
        final Level level = getLevel();
        if (target == null || level == null || !level.getWorldBorder().isWithinBounds(target))
        {
            return;
        }
        final LauncherSetTargetEvent event = new LauncherSetTargetEvent(level, getBlockPos(), target);
        if (LauncherSetTargetEvent.EVENT.invoker().onSetTarget(event))
        {
            this.target = event.target == null ? Vec3.ZERO : event.target;
            setChanged();
            refreshStatus = true;
        }
    }

    public List<LauncherEntry> getLaunchersInGroup()
    {
        return launcherNode.getNetwork() == null ? List.of() : launcherNode.getNetwork().getLaunchers(-1);
    }

    public IActionStatus preCheck(IMissileLauncher launcher)
    {
        return launcher.preCheckLaunch(new BasicTargetData(getTarget()), createCause());
    }

    private IActionCause createCause()
    {
        return new BlockScreenCause(getLevel(), getBlockPos(), getBlockState());
    }

    public boolean fireAllLaunchers(boolean simulate)
    {
        refreshStatus = true;
        if (launcherNode.getNetwork() == null)
        {
            return false;
        }
        final List<LauncherEntry> launchers = getLaunchersInGroup();
        if (launchers.isEmpty())
        {
            return false;
        }
        final IMissileTarget targetData = new BasicTargetData(getTarget());
        final ILauncherSolution solution = new LauncherSolution(targetData, -1, launchers.size());
        return launcherNode.getNetwork().launch(solution, createCause(), simulate)
                .filter(entry -> !entry.getLastFiringStatus().isBlocking()).count() > 0;
    }

    public boolean canLaunch()
    {
        return !getLaunchersInGroup().isEmpty();
    }

    public Component getStatusTranslation()
    {
        if (launcherNode.getNetwork() == null)
        {
            return LauncherLangs.TRANSLATION_ERROR_NO_NETWORK;
        }
        if (getLaunchersInGroup().isEmpty())
        {
            return LauncherLangs.TRANSLATION_ERROR_NO_LAUNCHER;
        }
        return LauncherLangs.TRANSLATION_READY;
    }

    public float getLauncherInaccuracy()
    {
        return launcherInaccuracy;
    }

    public List<ServerPlayer> getPlayersUsing()
    {
        return playersUsing;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
    {
        return new ContainerLaunchScreen(id, inventory, this);
    }
    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.icbmclassic.launcher_screen");
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        SAVE_LOGIC.save(this, output);
        output.putFloat("launcher_inaccuracy", launcherInaccuracy);
        output.putLong("energy", energyStorage.getStored());
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        SAVE_LOGIC.load(this, input);
        launcherInaccuracy = input.getFloatOr("launcher_inaccuracy", 0f);
        energyStorage.setStored(input.getLongOr("energy", 0));
    }

    @Override
    public void setRemoved()
    {
        radioCap.unregister();
        launcherNode.onTileRemoved();
        super.setRemoved();
    }

    public static final PacketCodexTile<TileLauncherScreen, TileLauncherScreen> PACKET_GUI = (PacketCodexTile<TileLauncherScreen, TileLauncherScreen>) new PacketCodexTile<TileLauncherScreen, TileLauncherScreen>(REGISTRY_NAME, "description", TileLauncherScreen.class)
            .fromServer()
            .nodeVec3(TileLauncherScreen::getTarget, TileLauncherScreen::setTarget)
            .nodeString(tile -> tile.radioCap.getChannel(), (tile, channel) -> tile.radioCap.setChannel(channel))
            .nodeBoolean(tile -> tile.radioCap.isDisabled(), (tile, disabled) -> tile.radioCap.setDisabled(disabled))
            .nodeFloat(TileLauncherScreen::getLauncherInaccuracy, (tile, inaccuracy) -> tile.launcherInaccuracy = inaccuracy);

    public static final PacketCodexTile<TileLauncherScreen, RadioScreen> PACKET_RADIO_HZ = (PacketCodexTile<TileLauncherScreen, RadioScreen>) new PacketCodexTile<TileLauncherScreen, RadioScreen>(REGISTRY_NAME, "radio.frequency", RadioScreen.class, tile -> tile.radioCap)
            .fromClient()
            .nodeString(RadioScreen::getChannel, (radio, channel) -> {
                if (channel != null && channel.length() <= 32)
                {
                    radio.setChannel(channel);
                }
            })
            .onFinished((tile, radio, player) -> tile.setChanged());

    public static final PacketCodexTile<TileLauncherScreen, RadioScreen> PACKET_RADIO_DISABLE = (PacketCodexTile<TileLauncherScreen, RadioScreen>) new PacketCodexTile<TileLauncherScreen, RadioScreen>(REGISTRY_NAME, "radio.disable", RadioScreen.class, tile -> tile.radioCap)
            .fromClient()
            .toggleBoolean(RadioScreen::isDisabled, RadioScreen::setDisabled)
            .onFinished((tile, radio, player) -> tile.setChanged());

    public static final PacketCodexTile<TileLauncherScreen, TileLauncherScreen> PACKET_TARGET = (PacketCodexTile<TileLauncherScreen, TileLauncherScreen>) new PacketCodexTile<TileLauncherScreen, TileLauncherScreen>(REGISTRY_NAME, "target", TileLauncherScreen.class)
            .fromClient()
            .nodeVec3(TileLauncherScreen::getTarget, (tile, value) -> {
                final Level level = tile.getLevel();
                if (level != null && value != null && level.getWorldBorder().isWithinBounds(value))
                {
                    tile.setTarget(value);
                }
            })
            .onFinished((tile, target, player) -> tile.setChanged());

    public static final PacketCodexTile<TileLauncherScreen, TileLauncherScreen> PACKET_LAUNCH = (PacketCodexTile<TileLauncherScreen, TileLauncherScreen>) new PacketCodexTile<TileLauncherScreen, TileLauncherScreen>(REGISTRY_NAME, "launch", TileLauncherScreen.class)
            .fromClient()
            .onFinished((tile, target, player) -> tile.fireAllLaunchers(false));

    public static void registerCodexes()
    {
        PacketCodexReg.register(PACKET_RADIO_HZ, PACKET_RADIO_DISABLE, PACKET_TARGET, PACKET_GUI, PACKET_LAUNCH);
    }

    private static final SaveHandler<TileLauncherScreen> SAVE_LOGIC = new SaveHandler<TileLauncherScreen>()
            .mainRoot()
            .nodeSavable("radio", tile -> tile.radioCap)
            .nodeSavable("inventory", tile -> tile.inventory)
            .nodeVec3("target", TileLauncherScreen::getTarget, (tile, value) -> tile.target = value)
            .base();
}
