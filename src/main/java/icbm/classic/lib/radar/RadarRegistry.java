package icbm.classic.lib.radar;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.transform.region.Cube;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Map based system for tracking objects using a radar devices. Only works server side to prevent unwanted data from stacking up.
 *
 * <p>1.12 keyed its maps by an integer dimension id and cleared them from Forge {@code @SubscribeEvent} handlers.
 * 26.3 keys by {@link ResourceKey} and the entry point wires the Fabric entity/chunk/level events to
 * {@link #add(Entity)}, {@link #remove(Entity)}, {@link #onLevelTick(ServerLevel)},
 * {@link #onChunkUnload(ServerLevel, LevelChunk)} and {@link #clear(ResourceKey)}.</p>
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/5/2016.
 */
public final class RadarRegistry
{
    //TODO add client side version for mini-map like systems
    //TODO add per machine tracking map that uses line of sight so hills can block it's view. (Visible Area Cache in other words)

    /**
     * Filter applied before an entity is tracked. 1.12 only fed missiles into the radar map from
     * {@code content/missile/CapabilityMissile} (S4 not ported yet), so 26.3 keeps a hook here.
     *
     * <p>S4 will change the default to only accept entities implementing {@code IMissile}.</p>
     */
    public static Predicate<Entity> entityFilter = entity -> true;

    /** Level key to radar map */
    private static final Map<ResourceKey<Level>, RadarMap> RADAR_MAPS = new HashMap();

    /**
     * Adds an entity to the radar map
     *
     * @param entity - entity
     * @return true if added
     */
    public static boolean add(Entity entity)
    {
        if (entity != null && entityFilter.test(entity) && !entity.isRemoved() && entity.level() != null && !entity.level().isClientSide())
        {
            RadarMap map = getRadarMapForWorld(entity.level());
            return map != null && getRadarMapForWorld(entity.level()).add(entity);
        }
        return false;
    }

    /**
     * Removes an entity from the radar map
     *
     * @param entity - entity
     * @return true if removed
     */
    public static boolean remove(Entity entity)
    {
        if (entity != null && !entity.isRemoved() && entity.level() != null)
        {
            RadarMap map = getRadarMapForWorld(entity.level());
            return map != null ? getRadarMapForWorld(entity.level()).remove(entity) : false;
        }
        return false;
    }

    /**
     * Gets a radar map for the world
     *
     * @param world - should be a valid world that is loaded and has a dimension key
     * @return existing map, or new map if one does not exist
     */
    public static RadarMap getRadarMapForWorld(Level world)
    {
        if (world != null)
        {
            if (world.isClientSide())
            {
                if (FabricLoader.getInstance().isDevelopmentEnvironment())
                {
                    ICBMClassic.logger().error("RadarRegistry: Radar data can not be requested client side.", new RuntimeException());
                }
                return null;
            }
            return getRadarMapForDim(world.dimension());
        }
        //Only throw an error in dev mode, ignore in normal runtime
        else if (FabricLoader.getInstance().isDevelopmentEnvironment())
        {
            ICBMClassic.logger().error("RadarRegistry: World can not be null when requesting a radar map", new RuntimeException());
        }
        return null;
    }

    /**
     * Gets a radar map for a dimension
     *
     * @param dimID - unique dimension key
     * @return existing map, or new map if one does not exist
     */
    public static RadarMap getRadarMapForDim(ResourceKey<Level> dimID)
    {
        RadarMap map = RADAR_MAPS.get(dimID);
        if (map == null)
        {
            map = new RadarMap(dimID);
            RADAR_MAPS.put(dimID, map);
        }
        return map;
    }

    /**
     * Grabs all living radar objects within range 2D
     *
     * @param world to check
     * @param x center
     * @param y center
     * @param z center
     * @param distance in x & z axis
     * @return list, never null
     */
    public static List<Entity> getAllLivingObjectsWithin(Level world, double x, double y, double z, double distance)
    {
        //1.12 hardcoded the map height constant. 26.3 worlds span -64..320 (overworld); this keeps the old
        //0..255 band, so contacts below 0 or above 255 are missed (documented limitation, see S1.06 evidence).
        return getAllLivingObjectsWithin(world, new Cube(x - distance, 0, z - distance, x + distance, ICBMClassic.MAP_HEIGHT, z + distance));
    }

    /**
     * Grabs all living radar objects within range
     *
     * @param world
     * @param cube  - area to search for contacts
     * @return list, never null
     */
    public static List<Entity> getAllLivingObjectsWithin(Level world, Cube cube)
    {
        // TODO recode to take filter as input to reduce output list
        // TODO recode to use a consumer pattern, if so ignore filter and let consumer be the filter

        final List<Entity> list = new ArrayList<>();
        if (RADAR_MAPS.containsKey(world.dimension()))
        {
            final RadarMap map = getRadarMapForWorld(world);
            if (map != null)
            {
                final List<RadarEntity> objects = map.getRadarObjects(cube, true);
                for (RadarEntity object : objects)
                {
                    if (object != null && object.isValid())
                    {
                        Entity entity = ((RadarEntity) object).entity;
                        if (entity != null && !entity.isRemoved())
                        {
                            list.add(entity);
                        }
                    }
                }
            }
            else if (world.isClientSide() && FabricLoader.getInstance().isDevelopmentEnvironment())
            {
                ICBMClassic.logger().error("RadarRegistry: Radar data can not be requested client side.", new RuntimeException());
            }
        }
        return list;
    }

    /**
     * 1.12 {@code chunkUnload(ChunkEvent.Unload)} handler; wired from {@code ServerChunkEvents.CHUNK_UNLOAD}.
     *
     * @param level level the chunk belonged to
     * @param chunk chunk being unloaded
     */
    public static void onChunkUnload(ServerLevel level, LevelChunk chunk)
    {
        if (level != null && chunk != null)
        {
            final ResourceKey<Level> dimID = level.dimension();
            if (RADAR_MAPS.containsKey(dimID))
            {
                getRadarMapForDim(dimID).remove(chunk.getPos());
            }
        }
    }

    /**
     * 1.12 {@code worldUpdateTick(TickEvent.WorldTickEvent)} handler; wired from
     * {@code ServerTickEvents.END_LEVEL_TICK}.
     *
     * @param level level that finished ticking
     */
    public static void onLevelTick(ServerLevel level)
    {
        if (level != null)
        {
            final ResourceKey<Level> dimID = level.dimension();
            if (RADAR_MAPS.containsKey(dimID))
            {
                RadarMap map = getRadarMapForDim(dimID);
                if (map.isEmpty())
                {
                    RADAR_MAPS.remove(dimID);
                }
                else
                {
                    map.update();
                }
            }
        }
    }

    /**
     * 1.12 {@code worldUnload(WorldEvent.Unload)} handler; wired from {@code ServerLevelEvents.UNLOAD}.
     *
     * @param dimID level key that was unloaded
     */
    public static void clear(ResourceKey<Level> dimID)
    {
        final RadarMap map = RADAR_MAPS.remove(dimID);
        if (map != null)
        {
            map.unloadAll();
        }
    }

    /** Drops every level map, called when the server stops */
    public static void clearAll()
    {
        for (RadarMap map : RADAR_MAPS.values())
        {
            map.unloadAll();
        }
        RADAR_MAPS.clear();
    }
}
