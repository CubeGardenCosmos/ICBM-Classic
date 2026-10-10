package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.data.GpsData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;

/**
 * Data components replacing the 1.12 item NBT keys (registry-ids.md §6).
 * <p>
 * The battery is deliberately absent: S6 uses the Team Reborn Energy item component instead
 * (energy-fe.md §5).
 */
public final class ModDataComponents
{
    /** Customization of an explosive (cluster count, blast scale overrides, ender target). */
    public static DataComponentType<ExplosiveSettings> EXPLOSIVE_SETTINGS;
    /** Stored target of GPS items (remote detonator, laser detonator, radar gun, coord cards). */
    public static DataComponentType<GpsData> GPS_DATA;
    /** Radio channel of the remote detonator. */
    public static DataComponentType<String> RADIO_CHANNEL;
    /** Payload of the held item missile. */
    public static DataComponentType<ItemStack> HELD_ITEM;

    private ModDataComponents() {}

    public static void register()
    {
        EXPLOSIVE_SETTINGS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ICBMConstants.id("explosive_settings"),
                DataComponentType.<ExplosiveSettings>builder()
                        .persistent(ExplosiveSettings.CODEC)
                        .networkSynchronized(ExplosiveSettings.STREAM_CODEC)
                        .build());

        GPS_DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ICBMConstants.id("gps_data"),
                DataComponentType.<GpsData>builder()
                        .persistent(GpsData.CODEC)
                        .networkSynchronized(GpsData.STREAM_CODEC)
                        .build());

        RADIO_CHANNEL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ICBMConstants.id("radio_channel"),
                DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build());

        HELD_ITEM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ICBMConstants.id("held_item"),
                DataComponentType.<ItemStack>builder()
                        .persistent(ItemStack.OPTIONAL_CODEC)
                        .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
                        .build());
    }
}
