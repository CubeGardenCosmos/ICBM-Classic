package icbm.classic.lib.energy;

import icbm.classic.config.ConfigMain;

/**
 * 能量规则门面。容量与消耗常量来自 config 各配置类，本类只提供规则判断与扣电辅助。
 */
public final class EnergyRules {
    private EnergyRules() {}

    /** ConfigMain.REQUIRES_POWER=false 时所有机器视为满电、不消耗（ADR 0003/energy-fe §2 全局开关）。 */
    public static boolean ignoresPower() {
        return !ConfigMain.REQUIRES_POWER;
    }

    /** 需要电力时从缓冲扣除；不需要电力时总是成功且不扣除。 */
    public static boolean consumeOrFree(IcbmEnergyBuffer buffer, long amount) {
        if (ignoresPower()) {
            return true;
        }
        return buffer.consume(amount);
    }
}
