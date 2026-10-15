package icbm.classic.content.blocks.launcher;

import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public abstract class LauncherBaseCapability implements IMissileLauncher, ISavable
{
    private UUID uniqueId;

    @Override
    public UUID getUniqueId()
    {
        if (uniqueId == null)
        {
            uniqueId = UUID.randomUUID();
        }
        return uniqueId;
    }

    private void setUniqueId(UUID uniqueId)
    {
        this.uniqueId = uniqueId;
    }

    @Override
    public void save(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<LauncherBaseCapability> SAVE_LOGIC = new SaveHandler<LauncherBaseCapability>()
            .mainRoot()
            .nodeUUID("uniqueId", LauncherBaseCapability::getUniqueId, LauncherBaseCapability::setUniqueId)
            .base();
}
