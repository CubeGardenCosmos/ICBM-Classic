package icbm.classic.content.missile.entity;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.config.ConfigDebug;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 * <p>
 * 1.12 attached this as the {@code IMissile} Forge capability of {@link EntityMissile}. 26.3 has no
 * capability system (porting-guide.md §2.6), so {@link EntityMissile} implements {@link IMissile} and
 * forwards to this class. The 1.12 {@code register()} (CapabilityManager) is deleted.
 */
public class CapabilityMissile implements IMissile, ISavable
{
    private final EntityMissile missile;
    private IMissileTarget targetData;

    private IActionSource firingSource;

    private IMissileFlightLogic flightLogic;
    private boolean doFlight = false;

    public CapabilityMissile(EntityMissile missile)
    {
        this.missile = missile;
    }

    @Override
    public int getTicksInAir()
    {
        return missile.ticksInAir;
    }

    @Override
    public Entity getMissileEntity()
    {
        return missile;
    }

    @Override
    public void setTargetData(IMissileTarget data) {
        this.targetData = data;
    }

    @Override
    public IMissileTarget getTargetData()
    {
        return this.targetData;
    }

    @Override
    public void setFlightLogic(IMissileFlightLogic logic)
    {
        this.missile.syncClient = true;
        this.flightLogic = logic;
    }

    @Override
    public void switchFlightLogic(IMissileFlightLogic logic) {
        if(ConfigDebug.DEBUG_MISSILE_LOGIC) {
            // TODO move to event system
            ICBMClassic.logger().info(this + ": Switching flight logic from '" + getFlightLogic() + "' to '" + logic + "'");
        }
        setFlightLogic(logic);
        triggerFlightLogic();
    }

    protected void triggerFlightLogic() {
        if(flightLogic != null) {
            flightLogic.calculateFlightPath(missile.level(), missile.getX(), missile.getY(), missile.getZ(), getTargetData()); //TODO show in launcher screen with predicted path and time
            flightLogic.start(missile, this);
        }
    }

    @Override
    public IMissileFlightLogic getFlightLogic()
    {
        return this.flightLogic;
    }

    @Override
    public void setMissileSource(IActionSource source)
    {
        this.firingSource = source;
    }

    @Override
    public IActionSource getMissileSource()
    {
        return firingSource;
    }

    @Override
    public void launch()
    {
        //Tell missile to start moving
        this.doFlight = true;
        triggerFlightLogic();

        //Trigger events
        //TODO add generic event
        ICBMClassicAPI.EX_MISSILE_REGISTRY.triggerLaunch(this);

        //Trigger events
        // TODO add an event system here
        RadarRegistry.add(this.missile); //TODO replace with capability and have radar system listen for entity spawn event

        //Play audio
        // 1.12 ICBMSounds.MISSILE_LAUNCH.play(world(), x(), y(), z(), 1F, pitch, true) used SoundCategory.BLOCKS and
        // the client side distance delay. 26.3 plays on the server side through Level#playSound (no delay).
        final Level level = missile.level();
        level.playSound(null, missile.getX(), missile.getY(), missile.getZ(), ModSounds.MISSILE_LAUNCH, SoundSource.BLOCKS,
            1F, (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);


        if (ConfigDebug.DEBUG_MISSILE_LAUNCHES)
        {
            ICBMClassic.logger().info(
                String.format("Missile(%s): Launch triggered of type[%s] starting from '%s' and aimed at `%s`",
                    this.missile.getId(),
                    this.missile.getName().getString(),
                    Optional.ofNullable(this.getMissileSource()).map(Objects::toString).orElse("'null'"),
                    Optional.ofNullable(this.getTargetData()).map(Objects::toString).orElse("'null'")
                )
            );
        }
    }

    @Override
    public Level world()
    {
        return missile.level();
    }

    @Override
    public double z()
    {
        return missile.getZ();
    }

    @Override
    public double x()
    {
        return missile.getX();
    }

    @Override
    public double y()
    {
        return missile.getY();
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<CapabilityMissile> SAVE_LOGIC = new SaveHandler<CapabilityMissile>()
        .addRoot("flags")
        /* */.nodeBoolean("doFlight", CapabilityMissile::canRunFlightLogic, (cap, i) -> cap.doFlight = i)
        .base()
        .mainRoot()
        /* */.nodeBuildableObject("target", () -> ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY, CapabilityMissile::getTargetData, CapabilityMissile::setTargetData)
        /* */.nodeBuildableObject("flight", () -> ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY, CapabilityMissile::getFlightLogic, CapabilityMissile::setFlightLogic)
        // 1.12 used nodeINBTSerializable("source", ...) with the ActionSource implementing INBTSerializable; the
        // 26.3 node is nodeSavable and has to build a new ActionSource on load (ActionSource is the only
        // IActionSource implementation in the mod).
        /* */.nodeSavable("source",
            (CapabilityMissile cap) -> (ISavable) cap.getMissileSource(),
            (cap, source) -> cap.setMissileSource((IActionSource) source),
            ActionSource::new)
        .base();

    public boolean canRunFlightLogic()
    {
        return doFlight;
    }

    @Override
    public int hashCode() {
        return missile != null ? missile.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof CapabilityMissile) {
            return Objects.equals(((CapabilityMissile) other).flightLogic, flightLogic)
                && Objects.equals(((CapabilityMissile) other).targetData, targetData)
                && ((CapabilityMissile) other).doFlight == doFlight;
        }
        return false;
    }
}
