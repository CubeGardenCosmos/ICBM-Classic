package icbm.classic.lib.radio;

import icbm.classic.ICBMClassic;
import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.lib.data.BoundBlockPos;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Map based system for radio waves being transmitted in an area
 *
 * 1.12 keyed the maps by an integer dimension id and cleared them from a Forge {@code WorldEvent.Unload}
 * subscriber. 26.3 keys by {@link ResourceKey} and the entry point wires the Fabric level unload event to
 * {@link #onLevelUnload(ResourceKey)} (see docs/architecture/overview.md §7).
 *
 * Created by Dark(DarkGuardsman, Robin) on 4/20/2016.
 */
public final class RadioRegistry
{
    /** Used to indicate the object has full map radio range */
    public static final IBoundBox<BlockPos> INFINITE = new BoundBlockPos(-Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** Level key to radio maps */
    private static final Map<ResourceKey<Level>, RadioMap> RADIO_MAPS = new HashMap();

    public static String EMPTY_HZ = "";


    /**
     * Adds an entity to the map
     *
     * @param receiver - entity
     * @return true if added
     */
    public static boolean add(IRadioReceiver receiver)
    {
        if(receiver == null || receiver.getWorld() == null || receiver.getWorld().isClientSide())
        {
            return false;
        }
        return getRadioMapForDim(receiver.getWorld().dimension()).add(receiver);
    }

    public static boolean addOrUpdate(IRadioReceiver receiver)
    {
        if(receiver == null || receiver.getWorld() == null || receiver.getWorld().isClientSide())
        {
            return false;
        }
        if (!add(receiver))
        {
            RadioMap map = getRadioMapForDim(receiver.getWorld().dimension());
            if (map.receive_to_chunks.containsKey(receiver))
            {
                map.update(receiver);
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Removes an entity from the map
     *
     * @param tile - entity
     * @return true if removed
     */
    public static boolean remove(IRadioReceiver tile)
    {
        if(tile == null || tile.getWorld() == null || tile.getWorld().isClientSide())
        {
            return false;
        }
        final ResourceKey<Level> dimID = tile.getWorld().dimension();
        if (RADIO_MAPS.containsKey(dimID))
        {
            RadioMap map = getRadioMapForDim(dimID);
            return map.remove(tile);
        }
        return false;
    }

    /**
     * Called to send a message over the network
     *
     * @param world level the message is sent in
     * @param sender posting message
     * @param packet containing message
     */
    public static void popMessage(Level world, IRadioSender sender, IRadioMessage packet)
    {
        if(world == null || world.isClientSide()) {
            ICBMClassic.logger().error("RadioRegistry: Invalid world : " + world, new RuntimeException());
            return;
        }
        final ResourceKey<Level> dimID = world.dimension();
        if (RADIO_MAPS.containsKey(dimID))
        {
            RadioMap map = getRadioMapForDim(dimID);
            map.popMessage(sender, packet);
        }
    }

    /**
     * Gets a radio map for the world
     *
     * @param world - should be a valid world that is loaded and has a level key
     * @return existing map, or new map if one does not exist
     */
    public static RadioMap getRadioMapForWorld(Level world)
    {
        if (world != null)
        {
            if (world.isClientSide())
            {
                if (FabricLoader.getInstance().isDevelopmentEnvironment())
                {
                    ICBMClassic.logger().error("RadioRegistry: Radio data can not be requested client side.", new RuntimeException());
                }
                return null;
            }
            return getRadioMapForDim(world.dimension());
        }
        //Only throw an error in dev mode, ignore in normal runtime
        else if (FabricLoader.getInstance().isDevelopmentEnvironment())
        {
            ICBMClassic.logger().error("RadioRegistry: World can not be null when requesting a radio map", new RuntimeException());
        }
        return null;
    }

    /**
     * Gets a radio map for a dimension
     *
     * @param dimID - unique level key
     * @return existing map, or new map if one does not exist
     */
    public static RadioMap getRadioMapForDim(ResourceKey<Level> dimID)
    {
        RadioMap map = RADIO_MAPS.get(dimID);
        if (map == null)
        {
            map = new RadioMap(dimID);
            RADIO_MAPS.put(dimID, map);
        }
        return map;
    }

    /**
     * Drops all receivers tracked for a level. Called from the Fabric level unload event.
     *
     * @param dimID level key that was unloaded
     */
    public static void clear(ResourceKey<Level> dimID)
    {
        final RadioMap map = RADIO_MAPS.remove(dimID);
        if (map != null)
        {
            map.unloadAll();
        }
    }

    /**
     * {@link #clear(ResourceKey)} alias used by the entry point event hook.
     *
     * @param dimID level key that was unloaded
     */
    public static void onLevelUnload(ResourceKey<Level> dimID)
    {
        clear(dimID);
    }

    /** Drops every level map, called when the server stops */
    public static void clearAll()
    {
        for (RadioMap map : RADIO_MAPS.values())
        {
            map.unloadAll();
        }
        RADIO_MAPS.clear();
    }
}
