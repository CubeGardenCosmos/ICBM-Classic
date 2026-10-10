package icbm.classic.client;

import icbm.classic.client.network.IcbmClientNetwork;
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
    }
}
