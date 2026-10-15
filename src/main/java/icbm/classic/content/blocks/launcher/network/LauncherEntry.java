package icbm.classic.content.blocks.launcher.network;

import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.content.blocks.launcher.FiringPackage;
import lombok.Data;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

@Data
public class LauncherEntry {
    private final IMissileLauncher launcher;
    private final BlockEntity host;
    private final Direction side;

    // Internal history; launcher event APIs are the public surface.
    private IActionStatus lastFiringStatus;
    private FiringPackage lastFiringPackage;
    private long lastFiringTime;
}
