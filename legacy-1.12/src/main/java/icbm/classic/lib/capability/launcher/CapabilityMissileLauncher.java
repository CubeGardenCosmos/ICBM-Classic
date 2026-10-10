package icbm.classic.lib.capability.launcher;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.content.blocks.launcher.status.LauncherStatus;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class CapabilityMissileLauncher implements IMissileLauncher
{
    public static void register()
    {
        CapabilityManager.INSTANCE.register(IMissileLauncher.class, new Capability.IStorage<IMissileLauncher>()
        {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<IMissileLauncher> capability, IMissileLauncher instance, EnumFacing side)
            {
                return null;
            }

            @Override
            public void readNBT(Capability<IMissileLauncher> capability, IMissileLauncher instance, EnumFacing side, NBTBase nbt)
            {

            }
        },
            CapabilityMissileLauncher::new);
    }

    @Override
    public UUID getUniqueId() {
        return null;
    }

    @Override
    public IActionStatus getStatus() {
        return LauncherStatus.ERROR_GENERIC;
    }

    @Override
    public IActionStatus preCheckLaunch(IMissileTarget target, @Nullable IActionCause cause) {
        return LauncherStatus.ERROR_GENERIC;
    }

    @Override
    public IActionStatus launch(ILauncherSolution firingSolution, @Nullable IActionCause cause, boolean simulate) {
        return LauncherStatus.ERROR_GENERIC;
    }
}
