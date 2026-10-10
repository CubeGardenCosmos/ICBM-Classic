package icbm.classic.lib.capability.gps;

import icbm.classic.api.caps.IGPSData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Pure logic helpers for GPS data based items (coord cards, laser detonator, radar gun).
 * <p>
 * 1.12 resolved the stored dimension through {@code DimensionManager} and named the world from
 * {@code WorldInfo}. 26.3 has no global level lookup, so the dimension is passed around as an
 * {@link Identifier} (see {@link IGPSData#getWorldId()}) and callers resolve it through the server.
 *
 * @deprecated replaced with the {@code icbmclassic:gps_data} data component (S2.04/S5.08)
 */
@Deprecated
public class GPSDataHelpers {

    public static boolean handlePlayerInteraction(IGPSData gpsData, Player player, BiConsumer<Identifier, Vec3> setter) {
        return handlePlayerInteraction(gpsData, player, true, setter);
    }

    public static boolean handlePlayerInteraction(IGPSData gpsData, Player player, Consumer<Vec3> setter) {
        return handlePlayerInteraction(gpsData, player, false, (w, v) -> setter.accept(v));
    }

    public static boolean handlePlayerInteraction(IGPSData gpsData, Player player, boolean setWorld, BiConsumer<Identifier, Vec3> setter) {
        if (gpsData == null)
        {
            return false;
        }

        final Vec3 position = gpsData.getPosition();
        final Identifier dimension = gpsData.getWorldId();
        if(position == null) {
            player.sendSystemMessage(Component.translatable("chat.launcher.noTargetInTool"));
        }
        else if(setWorld && dimension != null) {
            setter.accept(dimension, position);

            final String x = String.format("%.1f", position.x);
            final String y = String.format("%.1f", position.y);
            final String z = String.format("%.1f", position.z);

            player.sendSystemMessage(Component.translatable("info.icbmclassic:gps.set.all", x, y, z, dimension.toString()));
        }
        else {
            setter.accept(null, position);

            final String x = String.format("%.1f", position.x);
            final String y = String.format("%.1f", position.y);
            final String z = String.format("%.1f", position.z);
            player.sendSystemMessage(Component.translatable("info.icbmclassic:gps.set.pos", x, y, z));
        }
        return true;
    }
}
