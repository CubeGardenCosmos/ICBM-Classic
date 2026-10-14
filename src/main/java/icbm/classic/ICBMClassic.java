package icbm.classic;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.reg.events.ProjectileDataRegistryEvent;
import icbm.classic.config.ConfigThread;
import icbm.classic.config.core.ConfigManager;
import icbm.classic.content.reg.ExplosiveInit;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModCreativeTabs;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.actions.ActionSystem;
import icbm.classic.lib.actions.WorkTickingActionHandler;
import icbm.classic.lib.lookup.IcbmLookups;
import icbm.classic.lib.projectile.ProjectileDataRegistry;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.thread.BlockEditQueue;
import icbm.classic.lib.thread.WorkerThreadManager;
import icbm.classic.lib.world.ProjectileBlockInteraction;
import icbm.classic.network.IcbmNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ICBMClassic implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ICBM-Classic");

    /** 1.12 世界高度常量（地图高度），原为 ICBMClassic.MAP_HEIGHT；lib/transform 的旧工具用它做高度上限判断 */
    public static final int MAP_HEIGHT = 255;

    /** 兼容 1.12 调用点 ICBMClassic.logger() */
    public static Logger logger() { return LOGGER; }

    @Override
    public void onInitialize() {
        // 初始化顺序见 docs/architecture/overview.md §5，后续步骤按顺序在此追加调用
        ConfigManager.loadAll();
        // Action system registries (conditions, causes, statuses, actions, potentials), overview §5 item 2.
        ActionSystem.setup();
        // API lookups replacing the 1.12 Forge capabilities + data attachments (S1.10, porting-guide §2.6).
        IcbmLookups.init();
        // Explosive data must be registered and locked before the content that reads it (S2.02).
        ExplosiveInit.init();
        // Registration order is fixed by docs/architecture/overview.md §5: sounds and data components
        // first (items and blocks reference both while being constructed), then content, then the tab.
        ModSounds.register();
        ModDataComponents.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModMenus.register();
        ModCreativeTabs.register();
        IcbmNetwork.registerPayloads();

        // Projectile block interaction table (S3.01): 1.12 registered this next to the other world
        // handlers during preInit (ICBMClassic#preInit).
        ProjectileBlockInteraction.register();

        // Projectile data registry (S3.01). 1.12 created the registry during preInit and locked it once the
        // content systems had registered their own projectile types (ICBMClassic#handleProjectileDataRegistry);
        // content steps insert their registrations before the lock.
        final ProjectileDataRegistry projectileDataRegistry = new ProjectileDataRegistry();
        ICBMClassicAPI.PROJECTILE_DATA_REGISTRY = projectileDataRegistry;
        // 1.12 registered the vanilla defaults late (postInit, "needs to lock late as we need content to
        // register some types"). 26.3 has a second reason: ItemStack can not be created while the mod is
        // still initializing (porting-guide.md §9 note 5), so the vanilla conversions, the registry event
        // and the lock run on the first server start.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!projectileDataRegistry.isLocked()) {
                projectileDataRegistry.registerVanillaDefaults();
                ProjectileDataRegistryEvent.EVENT.invoker().onRegisterProjectileData(projectileDataRegistry);
                projectileDataRegistry.lock();
            }
        });

        // Drop per-level radio state when a level unloads, and everything when the server stops
        // (docs/architecture/overview.md §5/§7).
        ServerLevelEvents.UNLOAD.register((server, level) -> RadioRegistry.onLevelUnload(level.dimension()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RadioRegistry.clearAll());

        // Radar registry mirrors the 1.12 Forge subscribers with Fabric lifecycle events (S1.06).
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> RadarRegistry.add(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> RadarRegistry.remove(entity));
        ServerTickEvents.END_LEVEL_TICK.register(RadarRegistry::onLevelTick);
        ServerChunkEvents.CHUNK_UNLOAD.register(RadarRegistry::onChunkUnload);
        ServerLevelEvents.UNLOAD.register((server, level) -> RadarRegistry.clear(level.dimension()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RadarRegistry.clearAll());

        // Threaded blast framework (S1.09, docs/architecture/overview.md §6): worker threads read a
        // LevelSnapshot, BlockEditQueue applies their edits on the server thread with a per tick budget.
        WorkerThreadManager.INSTANCE = new WorkerThreadManager(ConfigThread.THREAD_COUNT);
        WorkerThreadManager.INSTANCE.startThreads();
        ServerTickEvents.END_LEVEL_TICK.register(BlockEditQueue::tick);
        ServerLevelEvents.UNLOAD.register((server, level) -> BlockEditQueue.clear(level.dimension()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BlockEditQueue.clearAll());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (WorkerThreadManager.INSTANCE != null) {
                WorkerThreadManager.INSTANCE.killThreads();
            }
        });

        // Active blast tracking (S3.02): 1.12 was a Forge @Mod.EventBusSubscriber on WorldEvent.Unload
        // (WorkTickingActionHandler#worldUnload). A level unload clears the blasts of that level, a server
        // stop clears everything left over.
        ServerLevelEvents.UNLOAD.register((server, level) -> WorkTickingActionHandler.onLevelUnload(level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorkTickingActionHandler.clearAll());

        LOGGER.info("ICBM-Classic initializing (26.3 port)");
    }
}
