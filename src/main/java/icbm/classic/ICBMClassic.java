package icbm.classic;

import icbm.classic.config.core.ConfigManager;
import icbm.classic.network.IcbmNetwork;
import net.fabricmc.api.ModInitializer;
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
        IcbmNetwork.registerPayloads();

        LOGGER.info("ICBM-Classic initializing (26.3 port)");
    }
}
