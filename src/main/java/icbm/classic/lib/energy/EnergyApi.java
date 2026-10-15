package icbm.classic.lib.energy;

import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.emptower.TileEmpTowerFake;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.frame.TileLauncherFrame;
import icbm.classic.content.blocks.launcher.network.LauncherNetwork;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import icbm.classic.ICBMClassic;
import team.reborn.energy.api.EnergyStorage;

/** Integration point for Team Reborn Energy API registrations. */
public final class EnergyApi {
    private EnergyApi() {}

    public static void register() {
        EnergyStorage.ITEM.registerForItems((stack, context) -> BatteryItem.createStorage(context), ModItems.BATTERY);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.getExposedEnergy(), ModBlockEntities.LAUNCHER_BASE);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.getExposedEnergy(), ModBlockEntities.CRUISE_LAUNCHER);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.getExposedEnergy(), ModBlockEntities.LAUNCHER_SCREEN);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.getExposedEnergy(), ModBlockEntities.RADAR_STATION);
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, be, dir) -> {
            if (be instanceof TileEMPTower tower) return tower.getExposedEnergy();
            if (be instanceof TileEmpTowerFake fake) {
                final TileEMPTower host = fake.getHost();
                return host == null ? null : host.getExposedEnergy();
            }
            return null;
        }, ModBlocks.EMP_TOWER);
        EnergyStorage.SIDED.registerForBlockEntity(EnergyApi::networkEnergy, ModBlockEntities.LAUNCHER_FRAME);
        EnergyStorage.SIDED.registerForBlockEntity(EnergyApi::networkEnergy, ModBlockEntities.LAUNCHER_CONNECTOR);
        ICBMClassic.LOGGER.info("Team Reborn Energy integration initialized");
    }

    private static EnergyStorage networkEnergy(TileLauncherFrame frame, net.minecraft.core.Direction direction) {
        final LauncherNetwork network = frame.getNetworkNode().getNetwork();
        if (network == null) return null;
        for (var node : network.getComponents()) {
            if (node.getSelf() instanceof TileLauncherBase || node.getSelf() instanceof TileCruiseLauncher) {
                return new LauncherNetworkEnergy(network);
            }
        }
        return null;
    }
}
