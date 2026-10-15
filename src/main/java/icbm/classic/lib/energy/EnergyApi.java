package icbm.classic.lib.energy;

import icbm.classic.ICBMClassic;

/** Integration point for Team Reborn Energy API registrations. */
public final class EnergyApi {
    private EnergyApi() {}

    public static void register() {
        ICBMClassic.LOGGER.info("Team Reborn Energy integration initialized");
    }
}
