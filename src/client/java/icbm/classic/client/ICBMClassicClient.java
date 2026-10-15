package icbm.classic.client;

import icbm.classic.client.content.blocks.launcher.base.gui.GuiLauncherBase;
import icbm.classic.client.content.blocks.launcher.cruise.gui.GuiCruiseLauncher;
import icbm.classic.client.content.blocks.launcher.screen.gui.GuiLauncherScreen;
import icbm.classic.client.content.blocks.radarstation.gui.GuiRadarStation;
import icbm.classic.client.content.blocks.emptower.gui.GuiEMPTower;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.client.network.IcbmClientNetwork;
import icbm.classic.client.particle.ModParticlesClient;
import icbm.classic.client.render.IcbmBlockEntityRenderers;
import icbm.classic.client.render.IcbmEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ICBMClassicClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ICBM-Classic/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("ICBM-Classic client initializing (26.3 port)");
        // 客户端注册顺序见 docs/architecture/overview.md §4–§5，后续步骤按顺序在此追加调用
        IcbmClientNetwork.register();
        IcbmEntityRenderers.register();
        IcbmBlockEntityRenderers.register();
        ModParticlesClient.register();
        MenuScreens.register(ModMenus.LAUNCHER_BASE, GuiLauncherBase::new);
        MenuScreens.register(ModMenus.LAUNCHER_SCREEN, GuiLauncherScreen::new);
        MenuScreens.register(ModMenus.CRUISE_LAUNCHER, GuiCruiseLauncher::new);
        MenuScreens.register(ModMenus.RADAR_STATION, GuiRadarStation::new);
        MenuScreens.register(ModMenus.EMP_TOWER, GuiEMPTower::new);
    }
}
