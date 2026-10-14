package icbm.classic;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.reg.events.MissileFlightLogicRegistryEvent;
import icbm.classic.api.reg.events.MissileTargetRegistryEvent;
import icbm.classic.api.reg.events.ProjectileDataRegistryEvent;
import icbm.classic.command.ICBMCommands;
import icbm.classic.config.ConfigThread;
import icbm.classic.config.core.ConfigManager;
import icbm.classic.content.missile.logic.flight.ArcFlightLogic;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.missile.logic.flight.DirectFlightLogic;
import icbm.classic.content.missile.logic.flight.FollowTargetLogic;
import icbm.classic.content.missile.logic.flight.WarmupFlightLogic;
import icbm.classic.content.missile.logic.flight.move.MoveByFacingLogic;
import icbm.classic.content.missile.logic.flight.move.MoveByVec3Logic;
import icbm.classic.content.missile.logic.flight.move.MoveForTicksLogic;
import icbm.classic.content.missile.logic.targeting.BallisticTargetingData;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.reg.ExplosiveInit;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModCreativeTabs;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.radioactive.RadioactiveHandler;
import icbm.classic.content.reg.ModEffects;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.cargo.balloon.BalloonProjectileData;
import icbm.classic.content.cargo.parachute.ParachuteProjectileData;
import icbm.classic.content.cluster.bomblet.BombletProjectileData;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.content.reg.ModParticles;
import icbm.classic.content.reg.ModRecipes;
import icbm.classic.content.reg.ModSounds;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import icbm.classic.content.missile.tracker.MissileTrackerHandler;
import icbm.classic.lib.actions.ActionSystem;
import icbm.classic.lib.actions.WorkTickingActionHandler;
import icbm.classic.lib.buildable.BuildableObjectRegistry;
import icbm.classic.lib.lookup.IcbmLookups;
import icbm.classic.lib.projectile.ProjectileDataRegistry;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.thread.BlockEditQueue;
import icbm.classic.lib.thread.WorkerThreadManager;
import icbm.classic.lib.world.ProjectileBlockInteraction;
import icbm.classic.network.IcbmNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
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
        // Missile builder registries (S4.01): 1.12 created them in preInit just before ActionSystem.setup()
        // (ICBMClassic#handleMissileTargetRegistry / #handleMissileFlightRegistry), overview §5 item 2.
        setupMissileRegistries();
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
        // Particle types (S4.07): server side content and data packs address the ICBM smoke by id
        ModParticles.register();
        // Custom mob effects (S3.05): the gas blasts and the antidote reference them by holder
        ModEffects.register();
        ModBlocks.register();
        ModItems.register();
        ModRecipes.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModMenus.register();
        ModCreativeTabs.register();
        IcbmNetwork.registerPayloads();

        // Command tree (S3.10): 1.12 registered a CommandEntryPoint("icbm", ICBMCommands.ICBM_COMMAND) with
        // the Forge command registry during pre init; Fabric's CommandRegistrationCallback replaces that
        // event. The command output uses the icbmclassic.command.* keys migrated in S2.10.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ICBMCommands.init(dispatcher));

        // Projectile block interaction table (S3.01): 1.12 registered this next to the other world
        // handlers during preInit (ICBMClassic#preInit).
        ProjectileBlockInteraction.register();

        // Projectile data registry (S3.01). 1.12 created the registry during preInit and locked it once the
        // content systems had registered their own projectile types (ICBMClassic#handleProjectileDataRegistry);
        // content steps insert their registrations before the lock.
        final ProjectileDataRegistry projectileDataRegistry = new ProjectileDataRegistry();
        ICBMClassicAPI.PROJECTILE_DATA_REGISTRY = projectileDataRegistry;
        ProjectileDataRegistryEvent.EVENT.register(registry -> {
            registry.register(BombletProjectileData.NAME, BombletProjectileData::new);
            registry.register(ParachuteProjectileData.NAME, ParachuteProjectileData::new);
            registry.register(BalloonProjectileData.NAME, BalloonProjectileData::new);

            for (Item bomblet : ModItems.EXPLOSIVE_BOMBLETS.values()) {
                registry.registerItemStackConversation(new ItemStack(bomblet), stack ->
                        new BombletProjectileData().setExplosiveStack(stack.copy()));
            }
            registry.registerItemStackConversation(new ItemStack(ModItems.EMPTY_BOMBLET), stack ->
                    new BombletProjectileData().setExplosiveStack(stack.copy()));

            registry.registerItemStackConversation(new ItemStack(ModItems.PARACHUTE), stack -> {
                final ParachuteProjectileData data = new ParachuteProjectileData();
                final ItemStack held = stack.getOrDefault(ModDataComponents.HELD_ITEM, ItemStack.EMPTY);
                data.setHeldItem(held.copy());
                return data;
            });

            registry.registerItemStackConversation(new ItemStack(ModItems.BALLOON), stack -> {
                final BalloonProjectileData data = new BalloonProjectileData();
                final ItemStack held = stack.getOrDefault(ModDataComponents.HELD_ITEM, ItemStack.EMPTY);
                data.setHeldItem(held.copy());
                return data;
            });
        });
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

        // The radioactive swap table reads block tags (vegetation check), so it is built once the static
        // registry tags are bound (1.12 rebuilt it on the config reload event)
        CommonLifecycleEvents.TAGS_LOADED.register((registryAccess, client) -> RadioactiveHandler.setup());

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

        // Missile tracker simulates missiles across unloaded chunks (S4.03, docs/architecture/overview.md §7).
        ServerLevelEvents.LOAD.register((server, level) -> MissileTrackerHandler.onLevelLoad(level));
        ServerLevelEvents.UNLOAD.register((server, level) -> MissileTrackerHandler.onLevelUnload(level));
        ServerTickEvents.END_LEVEL_TICK.register(MissileTrackerHandler::onLevelTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MissileTrackerHandler.clearAll());

        LOGGER.info("ICBM-Classic initializing (26.3 port)");
    }

    /**
     * Builds the missile builder registries and locks them.
     * <p>
     * 1.12 did this in preInit ({@code handleMissileTargetRegistry} / {@code handleMissileFlightRegistry}).
     * The registries have to exist before any content asks for them, so they are built here with the other
     * builder registries (overview.md §5 item 2). {@code BallisticFlightLogicOld} is not registered: the class
     * is dropped with the 1.12 save compatibility (ADR 0002).
     */
    private static void setupMissileRegistries() {
        final BuildableObjectRegistry<IMissileTarget> targetRegistry = new BuildableObjectRegistry<>("TARGET_DATA", "missile.target");
        ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY = targetRegistry;

        // Default types
        targetRegistry.register(BasicTargetData.REG_NAME, BasicTargetData::new);
        targetRegistry.register(BallisticTargetingData.REG_NAME, BallisticTargetingData::new);
        // 1.12 reserved the SAM target name with a builder answering null; SAMTargetData is ported in S4.04
        targetRegistry.register(ICBMConstants.id("anti.missile"), () -> null); //Can't be restored from save but reserving name

        //Fire registry event
        MissileTargetRegistryEvent.EVENT.invoker().onRegisterTargets(targetRegistry);

        //Lock to prevent late registry
        targetRegistry.lock();

        final BuildableObjectRegistry<IMissileFlightLogic> flightRegistry = new BuildableObjectRegistry<>("FLIGHT_LOGIC", "missile.flight");
        ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY = flightRegistry;

        // Register defaults
        flightRegistry.register(DirectFlightLogic.REG_NAME, DirectFlightLogic::new);
        flightRegistry.register(ArcFlightLogic.REG_NAME, ArcFlightLogic::new);
        flightRegistry.register(WarmupFlightLogic.REG_NAME, WarmupFlightLogic::new);
        flightRegistry.register(MoveByFacingLogic.REG_NAME, MoveByFacingLogic::new);
        flightRegistry.register(MoveByVec3Logic.REG_NAME, MoveByVec3Logic::new);
        flightRegistry.register(MoveForTicksLogic.REG_NAME, MoveForTicksLogic::new);
        flightRegistry.register(DeadFlightLogic.REG_NAME, DeadFlightLogic::new);
        flightRegistry.register(FollowTargetLogic.REG_NAME, FollowTargetLogic::new);

        //Fire registry event
        MissileFlightLogicRegistryEvent.EVENT.invoker().onRegisterFlightLogic(flightRegistry);

        //Lock to prevent late registry
        flightRegistry.lock();
    }
}
