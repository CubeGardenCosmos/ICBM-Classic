package icbm.classic.content.blocks.radarstation;

import icbm.classic.content.abm.radar.FireControl;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.math.Prediction;
import icbm.classic.content.abm.math.TrajectoryPredictor;
import icbm.classic.content.abm.math.Vec3d;
import icbm.classic.content.abm.radar.RadarTrack;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.content.abm.radar.TrackTable;
import icbm.classic.content.abm.radar.Vectors;
import icbm.classic.content.abm.radar.RadarDefenseMode;
import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigRadar;
import icbm.classic.content.blocks.radarstation.data.RadarRenderData;
import icbm.classic.content.blocks.radarstation.gui.ContainerRadarStation;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.lib.data.IMachineInfo;
import icbm.classic.lib.energy.EnergyRules;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import icbm.classic.lib.energy.ExposedEnergyStorage;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private RadarDefenseMode defenseMode = RadarDefenseMode.ABM_THEN_EMP;
    private final TrackTable trackTable = new TrackTable();
    private final FireControl fireControl = new FireControl();
    private final RadarRenderData radarRenderData = new RadarRenderData(this);
    public final IcbmEnergyBuffer energyStorage = new IcbmEnergyBuffer(
            () -> ConfigRadar.POWER_CAPACITY, () -> ConfigRadar.POWER_CAPACITY).onChange(this::setChanged);
    private ExposedEnergyStorage exposed;

    public ExposedEnergyStorage getExposedEnergy()
    {
        if (exposed == null) exposed = new ExposedEnergyStorage(energyStorage, this::setChanged);
        return exposed;
    }
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
    public static final PacketCodexTile<TileRadarStation, TileRadarStation> PACKET_DEFENSE_MODE =
            (PacketCodexTile<TileRadarStation, TileRadarStation>) new PacketCodexTile<TileRadarStation, TileRadarStation>(REGISTRY_NAME, "defense_mode", TileRadarStation.class)
                    .fromClient().nodeInt(tile -> tile.defenseMode.ordinal(), (tile, value) -> {
                        if (value >= 0 && value < RadarDefenseMode.values().length)
                            tile.setDefenseMode(RadarDefenseMode.values()[value]);
                    }).onFinished((tile, target, player) -> tile.setChanged());
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
                    .nodeEnum(RadarDefenseMode.class, TileRadarStation::getDefenseMode, TileRadarStation::setDefenseMode)
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
                PACKET_TRIGGER_RANGE, PACKET_DETECTION_RANGE, PACKET_REDSTONE_OUTPUT, PACKET_DEFENSE_MODE);
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
        tile.tickPower();
        tile.serverTick((ServerLevel) level);
        final boolean hasPower = EnergyRules.consumeOrFree(tile.energyStorage, tile.getEnergyCost());
        if (hasPower)
        {
            if (tile.ticks % ConfigRadar.SCAN_TICKS == 0) tile.doScan();
            if (level instanceof ServerLevel serverLevel && (tile.defenseMode == RadarDefenseMode.ABM_ONLY
                    || tile.defenseMode == RadarDefenseMode.ABM_THEN_EMP))
                tile.fireControl.tick(serverLevel, tile);
            if (tile.defenseMode == RadarDefenseMode.EMP_ONLY
                    || tile.defenseMode == RadarDefenseMode.ABM_THEN_EMP)
            {
                // Channel is checked first, then empTarget() picks the track in one pass; the cooldown only
                // advances while a qualifying target exists, as before (perf-audit C5)
                if (!tile.radio.getChannel().equals(RadioRegistry.EMPTY_HZ))
                {
                    final RadarTrack empTarget = tile.empTarget();
                    if (empTarget != null)
                    {
                        if (tile.firingCooldown > 0) tile.firingCooldown--;
                        if (tile.firingCooldown <= 0)
                        {
                            tile.firingCooldown = ConfigRadar.SAM_TICKS;
                            final Level currentLevel = tile.getLevel();
                            final Entity missile = currentLevel == null ? null : currentLevel.getEntity(empTarget.targetId);
                            if (missile instanceof IMissile)
                                RadioRegistry.popMessage(level, tile.radio,
                                        new IncomingMissileMessage(tile.radio.getChannel(), (IMissile) missile, true));
                        }
                    }
                }
            }
        }
        else
        {
            tile.trackTable.clear();
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

    public void tickPower()
    {
        icbm.classic.lib.energy.EnergyItemSlotHelper.dischargeInto(inventory, 0, energyStorage);
    }

    private static boolean isReservedBatteryItem(ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ICBMConstants.id("battery"));
    }

    private void doScan()
    {
        descriptionPacketSender.doNext();
        radarRenderData.clear();
        final Level level = getLevel();
        if (level == null) return;

        final long now = level.getGameTime();
        final Set<UUID> seen = new HashSet<>();
        final List<Entity> entities = RadarRegistry.getAllLivingObjectsWithin(level,
                getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5,
                Math.min(detectionRange, ConfigRadar.MAX_RANGE));
        for (Entity entity : entities)
        {
            if (!isThreat(entity)) continue;
            final IMissile missile = ICBMClassicHelpers.getMissile(entity);
            if (missile == null || missile.getTicksInAir() <= 1) continue;

            final UUID targetId = entity.getUUID();
            seen.add(targetId);
            final Vec3d position = Vectors.from(entity.position());
            final Vec3d velocity = Vectors.from(entity.getDeltaMovement());
            RadarTrack track = trackTable.get(targetId);
            if (track == null)
            {
                track = trackTable.create(targetId, position, velocity, now);
            }
            else
            {
                final long elapsedTicks = Math.max(1, now - track.lastSeenTick);
                track.acceleration = velocity.sub(track.velocity).scale(1.0 / elapsedTicks);
                track.position = position;
                track.velocity = velocity;
                track.lastSeenTick = now;
            }

            final IMissileTarget target = ConfigAbm.USE_MISSILE_TARGET_DATA ? missile.getTargetData() : null;
            final boolean useTarget = target != null && target.isValid() && target.getPosition() != null;
            final Vec3 targetPos = useTarget ? target.getPosition() : null;
            final Prediction prediction = TrajectoryPredictor.predictImpact(position, velocity, track.acceleration,
                    useTarget ? targetPos.y : getBlockPos().getY(), ConfigAbm.PREDICTION_MAX_TICKS);
            if (useTarget)
            {
                // design §5: impact is the missile's own target; ticks still come from extrapolation
                track.predictedImpact = Vectors.from(targetPos);
                track.ticksToImpact = prediction != null ? prediction.ticks() : horizontalTicks(position, velocity, track.predictedImpact);
            }
            else
            {
                track.predictedImpact = prediction == null ? null : prediction.impact();
                track.ticksToImpact = prediction == null ? Integer.MAX_VALUE : prediction.ticks();
            }

            // C1: ENGAGED is sticky only while an interceptor for this target is still in flight. Once the ABM is
            // gone the track falls back to normal classification, so a failed interception can be engaged again
            // (the reengage delay itself is still enforced by RadarTrack#nextEngageTick in FireControl).
            final TrackStatus nextStatus = track.status == TrackStatus.ENGAGED && hasActiveInterceptors(level, targetId)
                    ? TrackStatus.ENGAGED
                    : track.predictedImpact != null && horizontalDistanceSqr(track.predictedImpact) <= (double) triggerRange * triggerRange
                    ? TrackStatus.THREAT
                    : TrackStatus.DETECTED;
            track.setStatus(nextStatus, now);
            trackTable.update(track);
        }

        for (RadarTrack track : trackTable.values())
        {
            if (!seen.contains(track.targetId))
            {
                if (level instanceof ServerLevel serverLevel && EngagementRegistry.wasIntercepted(serverLevel, track.targetId))
                    track.setStatus(TrackStatus.INTERCEPTED, now);
                else
                    trackTable.markLost(track.targetId, now);
            }
        }
        trackTable.purgeLost(now);
        if (!playersUsing.isEmpty()) radarRenderData.update();
    }

    /**
     * True when at least one interceptor ordered at this target is still alive in the level, so the engagement
     * is still running (perf-audit C1). {@link EngagementRegistry#activeCount} prunes interceptors whose entity
     * is gone, so a destroyed/suicide ABM reports a count of zero.
     */
    private static boolean hasActiveInterceptors(Level level, UUID targetId)
    {
        return level instanceof ServerLevel serverLevel
                && EngagementRegistry.activeCount(serverLevel, targetId) > 0;
    }

    /** Design §5 fallback when extrapolation finds no landing: horizontal distance over horizontal speed (min 0.1). */
    private static int horizontalTicks(Vec3d position, Vec3d velocity, Vec3d target)
    {
        final double dx = target.x() - position.x();
        final double dz = target.z() - position.z();
        final double speed = Math.max(Math.hypot(velocity.x(), velocity.z()), 0.1);
        return (int) Math.ceil(Math.hypot(dx, dz) / speed);
    }

    private double horizontalDistanceSqr(Vec3d point)
    {
        final double dx = point.x() - (getBlockPos().getX() + 0.5);
        final double dz = point.z() - (getBlockPos().getZ() + 0.5);
        return dx * dx + dz * dz;
    }

    private List<RadarTrack> orderedThreats()
    {
        return trackTable.threats().stream()
                .sorted(Comparator.comparingInt(track -> track.ticksToImpact))
                .toList();
    }
    /**
     * The incoming track an EMP shot would be fired at, or null when none qualifies.
     *
     * Only the first (smallest {@code ticksToImpact}) track is ever used, so this picks it in one pass instead of
     * sorting a whole list (perf-audit C5). Ties keep the first track in table order, like the stable sort did.
     */
    private RadarTrack empTarget()
    {
        // A qualifying track always has one of these two statuses, the status scan is cheaper than copying the table
        if (!trackTable.hasThreat() && !trackTable.hasEngaged()) return null;
        final boolean fallbackOnly = defenseMode == RadarDefenseMode.ABM_THEN_EMP;
        RadarTrack best = null;
        for (RadarTrack track : trackTable.values())
        {
            if (track.status != TrackStatus.THREAT && track.status != TrackStatus.ENGAGED) continue;
            if (fallbackOnly && track.ticksToImpact > ConfigAbm.EMP_FALLBACK_TICKS) continue;
            if (best == null || track.ticksToImpact < best.ticksToImpact) best = track;
        }
        return best;
    }

    private List<IMissile> incomingThreats()
    {
        final Level level = getLevel();
        if (level == null) return List.of();
        return orderedThreats().stream()
                .map(track -> level.getEntity(track.targetId))
                .filter(IMissile.class::isInstance)
                .map(IMissile.class::cast)
                .toList();
    }

    private List<Entity> detectedThreats()
    {
        final Level level = getLevel();
        if (level == null) return List.of();
        return trackTable.values().stream()
                .filter(track -> track.status == TrackStatus.DETECTED)
                .map(track -> level.getEntity(track.targetId))
                .filter(entity -> entity != null && !entity.isRemoved())
                .sorted(Comparator.comparingDouble(this::distance))
                .toList();
    }

    private double distance(Entity entity)
    {
        return entity.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
    }

    public static boolean isThreat(Entity entity)
    {
        return entity instanceof IMissile && !(entity instanceof EntityAntiBallisticMissile)
                && ICBMClassicHelpers.getExplosive(entity) != null;
    }


    public EnumRadarState getRadarState()
    {
        if (isClient()) return radarVisualState;
        if (!energyStorage.has(getEnergyCost())) return EnumRadarState.OFF;
        // No tracks means neither list below can be non-empty, skip both scans (perf-audit F3)
        if (trackTable.isEmpty()) return EnumRadarState.ON;
        // C6: an ENGAGED track is still a live danger, threats() only holds THREAT so it has to be checked here
        if (trackTable.hasEngaged() || !getIncomingThreats().isEmpty()) return EnumRadarState.DANGER;
        if (!getDetectedThreats().isEmpty()) return EnumRadarState.WARNING;
        return EnumRadarState.ON;
    }

    public int getStrongRedstonePower(Direction side)
    {
        return outputRedstone ? Math.min(15, trackTable.threats().size()
                + (int) trackTable.values().stream().filter(track -> track.status == TrackStatus.ENGAGED).count()) : 0;
    }

    public int getEnergyCost()
    {
        return ConfigRadar.POWER_COST;
    }

    public boolean hasIncomingMissiles()
    {
        // hasEngaged() is a cheap scan and an empty table can not be incoming, both checks short circuit the threat list scan (perf-audit F3)
        return !trackTable.isEmpty()
                && (trackTable.hasEngaged() || !getIncomingThreats().isEmpty());
    }
    public int getDetectionRange() { return detectionRange; }
    public void setDetectionRange(int value) { detectionRange = value; }
    public int getTriggerRange() { return triggerRange; }
    public void setTriggerRange(int value) { triggerRange = value; }
    public RadarDefenseMode getDefenseMode() { return defenseMode; }
    public void setDefenseMode(RadarDefenseMode value) { defenseMode = value; }
    public boolean isOutputRedstone() { return outputRedstone; }
    public void setOutputRedstone(boolean value) { outputRedstone = value; }
    public List<Entity> getDetectedThreats() { return detectedThreats(); }
    public List<IMissile> getIncomingThreats() { return incomingThreats(); }
    public TrackTable getTrackTable() { return trackTable; }
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
        out.putString("defense_mode", defenseMode.name());
    }

    @Override
    protected void loadAdditional(ValueInput in)
    {
        super.loadAdditional(in);
        SAVE_LOGIC.load(this, in);
        energyStorage.setStored(in.getLongOr("energy", 0));
        firingCooldown = in.getIntOr("sam_delay", 0);
        in.getString("defense_mode").ifPresent(value -> {
            try { defenseMode = RadarDefenseMode.valueOf(value); }
            catch (IllegalArgumentException ignored) { defenseMode = RadarDefenseMode.ABM_THEN_EMP; }
        });
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
