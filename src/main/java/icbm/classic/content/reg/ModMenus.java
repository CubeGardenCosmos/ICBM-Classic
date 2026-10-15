package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.blocks.launcher.base.gui.ContainerLaunchBase;
import icbm.classic.content.blocks.launcher.screen.gui.ContainerLaunchScreen;
import icbm.classic.content.blocks.launcher.cruise.gui.ContainerCruiseLauncher;
import icbm.classic.content.blocks.radarstation.gui.ContainerRadarStation;
import icbm.classic.content.blocks.emptower.gui.ContainerEMPTower;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Menu type registration (1.12 {@code IGuiHandler}). */
public final class ModMenus
{
    public static ExtendedMenuType<ContainerLaunchBase, BlockPos> LAUNCHER_BASE;
    public static ExtendedMenuType<ContainerLaunchScreen, BlockPos> LAUNCHER_SCREEN;
    public static ExtendedMenuType<ContainerCruiseLauncher, BlockPos> CRUISE_LAUNCHER;
    public static ExtendedMenuType<ContainerRadarStation, BlockPos> RADAR_STATION;
    public static ExtendedMenuType<ContainerEMPTower, BlockPos> EMP_TOWER;

    private ModMenus() {}

    /** Registers the menus completed by the machine port steps. */
    public static void register()
    {
        LAUNCHER_BASE = Registry.register(BuiltInRegistries.MENU, ICBMConstants.id("launcher_base"),
                new ExtendedMenuType<>(ContainerLaunchBase::new, BlockPos.STREAM_CODEC));
        LAUNCHER_SCREEN = Registry.register(BuiltInRegistries.MENU, ICBMConstants.id("launcher_screen"),
                new ExtendedMenuType<>(ContainerLaunchScreen::new, BlockPos.STREAM_CODEC));
        CRUISE_LAUNCHER = Registry.register(BuiltInRegistries.MENU, ICBMConstants.id("cruise_launcher"),
                new ExtendedMenuType<>(ContainerCruiseLauncher::new, BlockPos.STREAM_CODEC));
        RADAR_STATION = Registry.register(BuiltInRegistries.MENU, ICBMConstants.id("radar_station"),
                new ExtendedMenuType<>(ContainerRadarStation::new, BlockPos.STREAM_CODEC));
        EMP_TOWER = Registry.register(BuiltInRegistries.MENU, ICBMConstants.id("emp_tower"),
                new ExtendedMenuType<>(ContainerEMPTower::new, BlockPos.STREAM_CODEC));
    }
}
