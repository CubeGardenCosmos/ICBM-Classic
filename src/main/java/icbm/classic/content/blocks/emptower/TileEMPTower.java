package icbm.classic.content.blocks.emptower;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigEmpTower;
import icbm.classic.content.actions.emp.ActionDataEmpArea;
import icbm.classic.content.blocks.emptower.gui.ContainerEMPTower;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.lib.actions.PotentialActionKnown;
import icbm.classic.lib.actions.fields.ActionFieldProvider;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.EnergyRules;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.energy.ExposedEnergyStorage;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.tile.TickAction;
import icbm.classic.lib.tile.TickDoOnce;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/** Logic side of the EMP tower block. */
public class TileEMPTower extends TileMachine implements IGuiTile, IMachineInfo, IPlayerUsing
{
    public static final net.minecraft.resources.Identifier REGISTRY_NAME = ICBMConstants.id("emptower");
    public static final int ROTATION_SPEED = 15;

    public float rotation;
    public float prevRotation;
    protected int cooldownTicks;
    private int range = 60;
    private boolean radioRegistered;

    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> (long) getFiringCost() + (long) getTickingCost() * ConfigEmpTower.ENERGY_COST_TICKING_CAP,
            () -> ConfigEmpTower.ENERGY_INPUT).onChange(this::setChanged);
    private ExposedEnergyStorage exposed;

    public ExposedEnergyStorage getExposedEnergy()
    {
        if (exposed == null) exposed = new ExposedEnergyStorage(energyStorage, this::setChanged);
        return exposed;
    }
    public final InventoryWithSlots inventory = new InventoryWithSlots(1)
            .withChangeCallback((slot, stack) -> setChanged())
            .withSlot(new InventorySlot(0, TileEMPTower::isReservedBatteryItem));
    public final RadioEmpTower radioCap = new RadioEmpTower(this);
    public final PotentialActionKnown empAction = new PotentialActionKnown(ActionDataEmpArea.REG_NAME)
            .withProvider(new ActionFieldProvider().field(icbm.classic.api.actions.data.ActionFields.AREA_SIZE, () -> (float) getRange()));
    private final List<TileEmpTowerFake> subBlocks = new ArrayList<>();
    private final List<ServerPlayer> playersUsing = new LinkedList<>();
    private final TickDoOnce descriptionPacketSender = new TickDoOnce(tick -> PACKET_DESCRIPTION.sendToTracking(this));

    public TileEMPTower(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.EMP_TOWER, pos, state);
        tickActions.add(descriptionPacketSender);
        tickActions.add(new TickAction(3, true, tick -> PACKET_GUI.sendToGuiUsers(this, playersUsing)));
        tickActions.add(new TickAction(20, true,
                tick -> playersUsing.removeIf(player -> !(player.containerMenu instanceof ContainerEMPTower))));
        tickActions.add(inventory);
        tickActions.add(empAction);
        tickActions.add(new TickAction(5, tick -> updateStructure()));
    }

    public static void registerCodexes()
    {
        PacketCodexReg.register(PACKET_DESCRIPTION, PACKET_GUI, PACKET_RADIUS, PACKET_RADIO_HZ,
                PACKET_RADIO_DISABLE, PACKET_FIRE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
        final TileEMPTower tile = (TileEMPTower) blockEntity;
        if (level.isClientSide())
        {
            tile.clientTick();
            tile.animate();
            return;
        }
        if (!tile.radioRegistered)
        {
            tile.radioRegistered = true;
            tile.radioCap.ensureRegistered();
        }
        tile.serverTick((ServerLevel) level);
        EnergyRules.consumeOrFree(tile.energyStorage, tile.getTickingCost());
        if (tile.isReady() && level.hasNeighborSignal(pos)) tile.fire(null);
        tile.animate();
    }

    private void animate()
    {
        prevRotation = rotation;
        if (cooldownTicks > 0) cooldownTicks--;
        else
        {
            rotation += getChargePercentage() * ROTATION_SPEED;
            while (rotation > 180.0F) rotation -= 360.0F;
            while (rotation < -180.0F) rotation += 360.0F;
        }
    }

    private void updateStructure()
    {
        subBlocks.clear();
        final Level level = getLevel();
        if (level == null) return;
        BlockPos above = getBlockPos().above();
        while (level.getBlockState(above).is(getBlockState().getBlock()))
        {
            final BlockEntity entity = level.getBlockEntity(above);
            if (entity instanceof TileEmpTowerFake fake)
            {
                fake.setHost(this);
                subBlocks.add(fake);
            }
            above = above.above();
        }
    }

    private static boolean isReservedBatteryItem(net.minecraft.world.item.ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    public float getChargePercentage()
    {
        return ConfigMain.REQUIRES_POWER ? Math.max(0f, Math.min(1f, energyStorage.getStored() / (float) Math.max(1, getFiringCost()))) : 1f;
    }

    public int getFiringCost()
    {
        return ConfigMain.REQUIRES_POWER ? range * range * ConfigEmpTower.ENERGY_COST_AREA : 0;
    }

    public int getTickingCost()
    {
        return ConfigMain.REQUIRES_POWER ? range * ConfigEmpTower.ENERGY_COST_TICKING : 0;
    }

    public int getMaxRadius()
    {
        return ConfigEmpTower.MAX_BASE_RANGE + subBlocks.size() * ConfigEmpTower.BONUS_RADIUS;
    }

    public int getRange() { return range; }

    public void setRange(int value)
    {
        range = Math.max(1, Math.min(value, getMaxRadius()));
    }

    public boolean fire(icbm.classic.api.actions.cause.IActionCause cause)
    {
        final Level level = getLevel();
        if (level == null || !isReady()) return false;
        final var response = empAction.doAction(level, getBlockPos(), cause);
        if (!response.isType(ActionStatusTypes.GREEN)) return false;
        EnergyRules.consumeOrFree(energyStorage, getFiringCost());
        cooldownTicks = ConfigEmpTower.COOLDOWN;
        setChanged();
        return true;
    }

    public boolean isReady()
    {
        return cooldownTicks <= 0 && energyStorage.has(getFiringCost());
    }

    public int getCooldown() { return cooldownTicks; }
    public int getMaxCooldown() { return ConfigEmpTower.COOLDOWN; }
    public float getCooldownPercentage() { return getMaxCooldown() == 0 ? 1f : 1f - cooldownTicks / (float) getMaxCooldown(); }
    public RadioEmpTower getRadio() { return radioCap; }
    @Override public List<ServerPlayer> getPlayersUsing() { return playersUsing; }

    @Override
    public void provideInformation(BiConsumer<String, Object> consumer)
    {
        consumer.accept(NEEDS_POWER, ConfigMain.REQUIRES_POWER);
        consumer.accept(ENERGY_COST_TICK, getTickingCost());
        consumer.accept(ENERGY_COST_ACTION, getFiringCost());
        consumer.accept(ENERGY_RECEIVE_LIMIT, ConfigEmpTower.ENERGY_INPUT);
        consumer.accept("COOLING_TICKS", getMaxCooldown());
        consumer.accept("MAX_RANGE", getMaxRadius());
    }

    @Override public Component getDisplayName() { return Component.translatable("block.icbmclassic.emp_tower"); }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player)
    {
        return getBlockPos();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
    {
        return new ContainerEMPTower(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        SAVE_LOGIC.save(this, output);
        output.putLong("energy", energyStorage.getStored());
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        SAVE_LOGIC.load(this, input);
        energyStorage.setStored(input.getLongOr("energy", 0));
    }
    @Override
    public void setRemoved()
    {
        radioCap.unregister();
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        final Level level = getLevel();
        if (level != null) net.minecraft.world.Containers.dropContents(level, pos, inventory);
        super.preRemoveSideEffects(pos, state);
    }

    private static final SaveHandler<TileEMPTower> SAVE_LOGIC = new SaveHandler<TileEMPTower>()
            .mainRoot()
            .nodeSavable("inventory", tile -> tile.inventory)
            .nodeSavable("radio", tile -> tile.radioCap)
            .nodeSavable("emp_action", tile -> tile.empAction)
            .nodeInteger("range", TileEMPTower::getRange, TileEMPTower::setRange)
            .nodeInteger("cooldown", tile -> tile.cooldownTicks, (tile, value) -> tile.cooldownTicks = value)
            .nodeFloat("rotation", tile -> tile.rotation, (tile, value) -> tile.rotation = value)
            .nodeFloat("prev_rotation", tile -> tile.prevRotation, (tile, value) -> tile.prevRotation = value)
            .base();

    public static final PacketCodexTile<TileEMPTower, TileEMPTower> PACKET_RADIUS =
            (PacketCodexTile<TileEMPTower, TileEMPTower>) new PacketCodexTile<TileEMPTower, TileEMPTower>(REGISTRY_NAME, "radius", TileEMPTower.class)
                    .fromClient().nodeInt(TileEMPTower::getRange,
                            (tile, value) -> tile.setRange(Math.max(1, Math.min(value, tile.getMaxRadius()))))
                    .onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileEMPTower, RadioEmpTower> PACKET_RADIO_HZ =
            (PacketCodexTile<TileEMPTower, RadioEmpTower>) new PacketCodexTile<TileEMPTower, RadioEmpTower>(REGISTRY_NAME, "radio.frequency", RadioEmpTower.class, tile -> tile.radioCap)
                    .fromClient().nodeString(RadioEmpTower::getChannel, (radio, value) -> {
                        if (value != null && value.length() <= 32) radio.setChannel(value);
                    }).onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileEMPTower, RadioEmpTower> PACKET_RADIO_DISABLE =
            (PacketCodexTile<TileEMPTower, RadioEmpTower>) new PacketCodexTile<TileEMPTower, RadioEmpTower>(REGISTRY_NAME, "radio.disable", RadioEmpTower.class, tile -> tile.radioCap)
                    .fromClient().toggleBoolean(RadioEmpTower::isDisabled, RadioEmpTower::setDisabled)
                    .onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileEMPTower, TileEMPTower> PACKET_GUI =
            (PacketCodexTile<TileEMPTower, TileEMPTower>) new PacketCodexTile<TileEMPTower, TileEMPTower>(REGISTRY_NAME, "gui", TileEMPTower.class)
                    .fromServer().nodeInt(tile -> (int) tile.energyStorage.getStored(), (tile, value) -> tile.energyStorage.setStored(value))
                    .nodeString(tile -> tile.radioCap.getChannel(), (tile, value) -> tile.radioCap.setChannel(value))
                    .nodeBoolean(tile -> tile.radioCap.isDisabled(), (tile, value) -> tile.radioCap.setDisabled(value))
                    .nodeInt(TileEMPTower::getRange, TileEMPTower::setRange);
    public static final PacketCodexTile<TileEMPTower, TileEMPTower> PACKET_FIRE =
            (PacketCodexTile<TileEMPTower, TileEMPTower>) new PacketCodexTile<TileEMPTower, TileEMPTower>(REGISTRY_NAME, "fire", TileEMPTower.class)
                    .fromClient().onFinished((tile, target, player) -> tile.fire(null));
    public static final PacketCodexTile<TileEMPTower, TileEMPTower> PACKET_DESCRIPTION =
            (PacketCodexTile<TileEMPTower, TileEMPTower>) new PacketCodexTile<TileEMPTower, TileEMPTower>(REGISTRY_NAME, "description", TileEMPTower.class)
                    .fromServer().nodeFloat(tile -> tile.rotation, (tile, value) -> tile.rotation = value)
                    .nodeInt(tile -> tile.cooldownTicks, (tile, value) -> tile.cooldownTicks = value);
}
