package icbm.classic.api.caps;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Access to GPS data of an item.
 * <p>
 * 1.12 exposed this as a Forge capability ({@code ICBMClassicAPI.GPS_CAPABILITY}) resolved through
 * {@code DimensionManager}. 26.3 has no global level lookup and no capability system: the data is stored
 * in the {@code icbmclassic:gps_data} data component ({@code GpsData(Optional<GlobalPos> pos)},
 * registered in S2.01/S2.04) and this interface stays as the pure read/write view used by
 * {@link icbm.classic.lib.capability.gps.GPSDataHelpers}.
 * <p>
 * A dimension is therefore identified by its {@link Identifier} (see {@link Level#dimension()}) and the
 * caller resolves it through the server when a {@link Level} is needed (this is why
 * {@code getWorld()} from 1.12 is gone).
 *
 * @deprecated replaced with the {@code icbmclassic:gps_data} data component (S2.04/S5.08)
 */
@Deprecated
public interface IGPSData {

    /**
     * Sets the stored location
     *
     * @param position to set, can be set to null to clear
     */
    void setPosition(@Nullable Vec3 position);

    /**
     * Sets the dimension of the stored location
     *
     * @param level to set, can be set to null to clear
     */
    default void setWorld(Level level) {
        setWorld(level == null ? null : level.dimension().identifier());
    }

    /**
     * Sets the world
     *
     * @param dimension to set, can be set to null to clear
     */
    void setWorld(@Nullable Identifier dimension);

    /**
     * Gets the position component of the GPS data
     *
     * @return position
     */
    @Nullable
    Vec3 getPosition();

    /**
     * Gets the stored dimension id
     *
     * @return id
     */
    @Nullable
    Identifier getWorldId();
}
