package icbm.classic.content.blocks.radarstation;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigRadar;
import icbm.classic.content.blocks.radarstation.data.RadarRenderData;
import icbm.classic.content.blocks.radarstation.gui.ContainerRadarStation;
import icbm.classic.content.missile.entity.anti.EntitySurfaceToAirMissile;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.EnergyRules;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.IncomingMissileMessage;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

public class TileRadarStation extends TileMachine implements IMachineInfo, IGuiTile, IPlayerUsing
{
    public static final net.minecraft.resources.Identifier REGISTRY_NAME = ICBMConstants.id("radarstation");
    public static final String TRANSLATION_TOOLTIP_RANGE = "icbmclassic.gui.radar.range";
    public static final String TRANSLATION_TOOLTIP_RANGE_SHIFT = "icbmclassic.gui.radar.range.shift";
    public static final String TRANSLATION_TOOLTIP_REDSTONE_OFF = "icbmclassic.gui.radar.redstone.off";
    public static final String TRANSLATION_TOOLTIP_REDSTONE_ON = "icbmclassic.gui.radar.redstone.on";

    private int detectionRange = 100;
    private int triggerRange = 50;
    private boolean outputRedstone = true;
    private final List<Entity> detectedThreats = new ArrayList<>();
    private final List<IMissile> incomingThreats = new ArrayList<>();
    private final RadarRenderData radarRenderData = new RadarRenderData(this);
    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> ConfigRadar.POWER_CAPACITY, () -> ConfigRadar.POWER_CAPACITY).onChange(this::setChanged);
    public final InventoryWithSlots inventory = new InventoryWithSlots(1)
            .withChangeCallback((slot, stack) -> setChanged())
            .withSlot(new InventorySlot(0, TileRadarStation::isReservedBatteryItem));
    private final RadioRadar radio = new RadioRadar(this);
    private EnumRadarState radarVisualState = EnumRadarState.OFF;
    private EnumRadarState preRadarVisualState = EnumRadarState.OFF;
    private final TickDoOnce descriptionPacketSender = new TickDoOnce(tick -> PACKET_DESCRIPTION.sendToTracking(this));
    private final List<ServerPlayer> playersUsing = new LinkedList<>();
    private int firingCooldown;
    private boolean radioRegistered;

    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_DESCRIPTION =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "description", TileRadarStation.class)
                    .fromServer()
                    .nodeEnum(EnumRadarState.class, tile -> tile.radarVisualState, (tile, value) -> tile.radarVisualState = value);
    public static final PacketCodexTile<TileRadarStation, RadioRadar> PACKET_RADIO_HZ =
            (PacketCodexTile<TileRadarStation, RadioRadar>) new PacketCodexTile<TileRadarStation, RadioRadar>(REGISTRY_NAME, "radio.frequency", RadioRadar.class, tile -> tile.radio)
                    .fromClient().nodeString(RadioRadar::getChannel, (radio, value) -> {
                        if (value != null && value.length() <= 32) radio.setChannel(value);
                    }).onFinished((tile, radio, player) -> tile.setChanged());
    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_TRIGGER_RANGE =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "range.trigger", TileRadarStation.class)
                    .fromClient().nodeInt(TileRadarStation::getTriggerRange, (tile, value) -> tile.setTriggerRange(Math.max(1, Math.min(ConfigRadar.MAX_RANGE, value))))
                    .onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_DETECTION_RANGE =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "range.detection", TileRadarStation.class)
                    .fromClient().nodeInt(TileRadarStation::getDetectionRange, (tile, value) -> tile.setDetectionRange(Math.max(1, Math.min(ConfigRadar.MAX_RANGE, value))))
                    .onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_REDSTONE_OUTPUT =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "redstone.output", TileRadarStation.class)
                    .fromClient().toggleBoolean(TileRadarStation::isOutputRedstone, TileRadarStation::setOutputRedstone)
                    .onFinished((tile, target, player) -> tile.setChanged());
    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_GUI =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "gui", TileRadarStation.class)
                    .fromServer()
                    .nodeInt(TileRadarStation::getTriggerRange, TileRadarStation::setTriggerRange)
                    .nodeInt(TileRadarStation::getDetectionRange, TileRadarStation::setDetectionRange)
                    .nodeString(tile -> tile.radio.getChannel(), (tile, value) -> tile.radio.setChannel(value))
                    .nodeBoolean(tile -> tile.radio.isDisabled(), (tile, value) -> tile.radio.setDisabled(value))
                    .nodeBoolean(TileRadarStation::isOutputRedstone, TileRadarStation::setOutputRedstone)
                    .node(tile -> tile.radarRenderData.getDots(), (tile, dots) -> tile.radarRenderData.setDots(dots),
                            RadarRenderData::encodeDots, RadarRenderData::decodeDots);

    public static final PacketCodexTile<TileRadarStation, RadioRadar> PACKET_RADIO_DISABLE =
            (PacketCodexTile<TileRadarStation, RadioRadar>) new PacketCodexTile<TileRadarStation, RadioRadar>(REGISTRY_NAME, "radio.disable", RadioRadar.class, tile -> tile.radio)
                    .fromClient().toggleBoolean(RadioRadar::isDisabled, RadioRadar::setDisabled)
                    .onFinished((tile, radio, player) -> tile.setChanged());
    public TileRadarStation(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.RADAR_STATION, pos, state);
        tickActions.add(descriptionPacketSender);
        tickActions.add(new TickAction(3, true, tick -> PACKET_GUI.sendToGuiUsers(this, playersUsing)));
        tickActions.add(new TickAction(20, true,
                tick -> playersUsing.removeIf(player -> !(player.containerMenu instanceof ContainerRadarStation))));
        tickActions.add(inventory);
    }

    public static void registerCodexes()
    {
        PacketCodexReg.register(PACKET_DESCRIPTION, PACKET_GUI, PACKET_RADIO_HZ, PACKET_RADIO_DISABLE,
                PACKET_TRIGGER_RANGE, PACKET_DETECTION_RANGE, PACKET_REDSTONE_OUTPUT);
    }


    public static void tick(Level level, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity)
    {
        final TileRadarStation tile = (TileRadarStation) blockEntity;
        if (level.isClientSide())
        {
            tile.clientTick();
            return;
        }
        if (!tile.radioRegistered)
        {
            tile.radioRegistered = true;
            tile.radio.ensureRegistered();
        }
        tile.serverTick((ServerLevel) level);
        final boolean hasPower = EnergyRules.consumeOrFree(tile.energyStorage, tile.getEnergyCost());
        if (hasPower)
        {
            if (tile.ticks % ConfigRadar.SCAN_TICKS == 0) tile.doScan();
            if (!tile.radio.getChannel().equals(RadioRegistry.EMPTY_HZ) && !tile.incomingThreats.isEmpty())
            {
                if (tile.firingCooldown > 0) tile.firingCooldown--;
                if (tile.firingCooldown <= 0)
                {
                    tile.firingCooldown = ConfigRadar.SAM_TICKS;
                    RadioRegistry.popMessage(level, tile.radio,
                            new IncomingMissileMessage(tile.radio.getChannel(), tile.incomingThreats.get(0), true));
                }
            }
        }
        else
        {
            tile.incomingThreats.clear();
            tile.detectedThreats.clear();
        }

        final boolean shouldBeOn = hasPower && tile.hasIncomingMissiles();
        if (level.getBlockState(pos).getValue(BlockRadarStation.REDSTONE_PROPERTY) != shouldBeOn)
        {
            level.setBlock(pos, level.getBlockState(pos).setValue(BlockRadarStation.REDSTONE_PROPERTY, shouldBeOn), Block.UPDATE_ALL);
            for (Direction direction : Direction.values())
            {
                final BlockPos targetPos = pos.relative(direction);
                level.updateNeighborsAt(targetPos, state.getBlock(), Orientation.fromIndex(0));
            }
        }

        if (!level.isClientSide()) tile.radarVisualState = tile.getRadarState();
        if (tile.preRadarVisualState != tile.radarVisualState)
        {
            tile.setChanged();
            final BlockState visualState = level.getBlockState(pos).setValue(BlockRadarStation.RADAR_STATE, tile.radarVisualState);
            level.setBlock(pos, visualState, Block.UPDATE_ALL);
            tile.preRadarVisualState = tile.radarVisualState;
        }
    }

    private static boolean isReservedBatteryItem(ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    private void doScan()
    {
        incomingThreats.clear();
        detectedThreats.clear();
        descriptionPacketSender.doNext();
        radarRenderData.clear();
        final Level level = getLevel();
        if (level == null) return;
        final List<Entity> entities = RadarRegistry.getAllLivingObjectsWithin(level,
                getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5,
                Math.min(detectionRange, ConfigRadar.MAX_RANGE));
        for (Entity entity : entities)
        {
            if (isThreat(entity))
            {
                final IMissile missile = ICBMClassicHelpers.getMissile(entity);
                if (missile != null && missile.getTicksInAir() > 1)
                {
                    if (isMissileGoingToHit(missile)) incomingThreats.add(missile);
                    else detectedThreats.add(entity);
                }
            }
        }
        final Comparator<Entity> distance = Comparator.comparingDouble(this::distance);
        incomingThreats.sort((a, b) -> distance.compare(a.getMissileEntity(), b.getMissileEntity()));
        detectedThreats.sort(distance);
        if (!playersUsing.isEmpty()) radarRenderData.update();
    }

    private double distance(Entity entity)
    {
        return entity.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
    }

    public static boolean isThreat(Entity entity)
    {
        return entity instanceof IMissile && !(entity instanceof EntitySurfaceToAirMissile)
                && ICBMClassicHelpers.getExplosive(entity) != null;
    }

    public boolean isMissileGoingToHit(IMissile missile)
    {
        if (missile == null || missile.getMissileEntity() == null || missile.getMissileEntity().isRemoved()) return false;
        final Entity entity = missile.getMissileEntity();
        final Vec3 current = new Vec3(missile.x(), missile.y(), missile.z());
        final Vec3 radar = new Vec3(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return current.add(entity.getDeltaMovement()).distanceTo(radar) < current.distanceTo(radar);
    }

    public EnumRadarState getRadarState()
    {
        if (isClient()) return radarVisualState;
        if (!energyStorage.has(getEnergyCost())) return EnumRadarState.OFF;
        if (!incomingThreats.isEmpty()) return EnumRadarState.DANGER;
        if (!detectedThreats.isEmpty()) return EnumRadarState.WARNING;
        return EnumRadarState.ON;
    }

    public int getStrongRedstonePower(Direction side)
    {
        return outputRedstone ? Math.min(15, incomingThreats.size()) : 0;
    }

    public int getEnergyCost()
    {
        return ConfigRadar.POWER_COST;
    }

    public boolean hasIncomingMissiles()
    {
        return !incomingThreats.isEmpty();
    }

    public int getDetectionRange() { return detectionRange; }
    public void setDetectionRange(int value) { detectionRange = value; }
    public int getTriggerRange() { return triggerRange; }
    public void setTriggerRange(int value) { triggerRange = value; }
    public boolean isOutputRedstone() { return outputRedstone; }
    public void setOutputRedstone(boolean value) { outputRedstone = value; }
    public List<Entity> getDetectedThreats() { return detectedThreats; }
    public List<IMissile> getIncomingThreats() { return incomingThreats; }
    public RadarRenderData getRadarRenderData() { return radarRenderData; }
    public RadioRadar getRadio() { return radio; }
    @Override public List<ServerPlayer> getPlayersUsing() { return playersUsing; }

    @Override
    public void provideInformation(BiConsumer<String, Object> consumer)
    {
        consumer.accept(NEEDS_POWER, ConfigMain.REQUIRES_POWER);
        consumer.accept(ENERGY_COST_TICK, getEnergyCost());
        consumer.accept("MAX_RANGE", ConfigRadar.MAX_RANGE);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.icbmclassic.radar_station"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
    {
        return new ContainerRadarStation(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        final Level level = getLevel();
        if (level != null) net.minecraft.world.Containers.dropContents(level, pos, inventory);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved()
    {
        radio.unregister();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput out)
    {
        super.saveAdditional(out);
        SAVE_LOGIC.save(this, out);
        out.putLong("energy", energyStorage.getStored());
        out.putInt("sam_delay", firingCooldown);
    }

    @Override
    protected void loadAdditional(ValueInput in)
    {
        super.loadAdditional(in);
        SAVE_LOGIC.load(this, in);
        energyStorage.setStored(in.getLongOr("energy", 0));
        firingCooldown = in.getIntOr("sam_delay", 0);
    }

    private static final SaveHandler<TileRadarStation> SAVE_LOGIC = new SaveHandler<TileRadarStation>()
            .mainRoot()
            .nodeSavable("inventory", tile -> tile.inventory)
            .nodeSavable("radio", tile -> tile.radio)
            .nodeBoolean("emitAll", TileRadarStation::isOutputRedstone, TileRadarStation::setOutputRedstone)
            .nodeInteger("detection_range", TileRadarStation::getDetectionRange, TileRadarStation::setDetectionRange)
            .nodeInteger("safetyRadius", TileRadarStation::getTriggerRange, TileRadarStation::setTriggerRange)
            .base();
}
