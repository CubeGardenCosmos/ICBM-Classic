package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.blocks.explosive.ExplosiveBlockEntity;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.emptower.TileEmpTowerFake;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.connector.TileLauncherConnector;
import icbm.classic.content.blocks.launcher.frame.TileLauncherFrame;
import icbm.classic.lib.lookup.IcbmLookups;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity type registration (1.12 {@code GameRegistry.registerTileEntity} calls in {@code BlockReg}).
 * <p>
 * IDs come from registry-ids.md §2; Filled by S3.09 (explosive), S5.01–S5.06 (machines).
 */
public final class ModBlockEntities
{
    private ModBlockEntities() {}

    /** {@code icbmclassic:explosive}, 1.12 {@code TileEntityExplosive}, shared by the 23 flat explosive blocks. */
    public static BlockEntityType<ExplosiveBlockEntity> EXPLOSIVE;

    public static BlockEntityType<TileLauncherBase> LAUNCHER_BASE;
    public static BlockEntityType<TileLauncherFrame> LAUNCHER_FRAME;
    public static BlockEntityType<TileLauncherConnector> LAUNCHER_CONNECTOR;
    public static BlockEntityType<TileLauncherScreen> LAUNCHER_SCREEN;
    public static BlockEntityType<TileCruiseLauncher> CRUISE_LAUNCHER;
    public static BlockEntityType<TileRadarStation> RADAR_STATION;
    public static BlockEntityType<TileEMPTower> EMP_TOWER;
    public static BlockEntityType<TileEmpTowerFake> EMP_TOWER_FAKE;

    /** Registers every block entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // 1.12 registered one tile entity class for the metadata block "explosives"; with the flat blocks
        // (ADR 0002) the single type is bound to all 23 blocks, see registry-ids.md §2
        EXPLOSIVE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("explosive"),
                FabricBlockEntityTypeBuilder.create(ExplosiveBlockEntity::new,
                        ModBlocks.EXPLOSIVE_BLOCKS.values().toArray(Block[]::new)).build());

        LAUNCHER_BASE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("launcher_base"),
                FabricBlockEntityTypeBuilder.create(TileLauncherBase::new, ModBlocks.LAUNCHER_BASE).build());
        LAUNCHER_FRAME = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("launcher_frame"),
                FabricBlockEntityTypeBuilder.create(TileLauncherFrame::new, ModBlocks.LAUNCHER_FRAME).build());
        LAUNCHER_CONNECTOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("launcher_connector"),
                FabricBlockEntityTypeBuilder.create(TileLauncherConnector::new, ModBlocks.LAUNCHER_CONNECTOR).build());
        LAUNCHER_SCREEN = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("launcher_screen"),
                FabricBlockEntityTypeBuilder.create(TileLauncherScreen::new, ModBlocks.LAUNCHER_SCREEN).build());
        CRUISE_LAUNCHER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("cruise_launcher"),
                FabricBlockEntityTypeBuilder.create(TileCruiseLauncher::new, ModBlocks.CRUISE_LAUNCHER).build());
        RADAR_STATION = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("radar_station"),
                FabricBlockEntityTypeBuilder.create(TileRadarStation::new, ModBlocks.RADAR_STATION).build());
        EMP_TOWER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("emp_tower"),
                FabricBlockEntityTypeBuilder.create(TileEMPTower::new, ModBlocks.EMP_TOWER).build());
        EMP_TOWER_FAKE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("emp_tower_fake"),
                FabricBlockEntityTypeBuilder.create(TileEmpTowerFake::new, ModBlocks.EMP_TOWER).build());

        IcbmLookups.MISSILE_LAUNCHER.registerForBlockEntity((be, dir) -> be.missileLauncher, LAUNCHER_BASE);
        IcbmLookups.MISSILE_HOLDER.registerForBlockEntity((be, dir) -> be.missileHolder, LAUNCHER_BASE);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> ContainerStorage.of(be.getInventory(), dir), LAUNCHER_BASE);
        IcbmLookups.MISSILE_LAUNCHER.registerForBlockEntity((be, dir) -> be.launcher, CRUISE_LAUNCHER);
        IcbmLookups.MISSILE_HOLDER.registerForBlockEntity((be, dir) -> be.missileHolder, CRUISE_LAUNCHER);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> ContainerStorage.of(be.inventory, dir), CRUISE_LAUNCHER);
    }
}
