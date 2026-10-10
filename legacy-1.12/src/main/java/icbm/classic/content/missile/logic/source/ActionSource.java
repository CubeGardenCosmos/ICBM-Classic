package icbm.classic.content.missile.logic.source;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.lib.saving.NbtSaveHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ActionSource implements IActionSource, INBTSerializable<NBTTagCompound> {

    private World world;
    private Vec3d position;
    private IActionCause cause;

    @Override
    public NBTTagCompound serializeNBT() {
        return SAVE_LOGIC.save(this, new NBTTagCompound());
    }

    @Override
    public void deserializeNBT(NBTTagCompound save) {
        SAVE_LOGIC.load(this, save);
    }

    private static final NbtSaveHandler<ActionSource> SAVE_LOGIC = new NbtSaveHandler<ActionSource>()
        .mainRoot()
        /* */.nodeWorldDim("dimension", ActionSource::getWorld, ActionSource::setWorld)
        /* */.nodeVec3d("pos", ActionSource::getPosition, ActionSource::setPosition)
        /* */.nodeBuildableObject("cause", () -> ICBMClassicAPI.ACTION_CAUSE_REGISTRY, ActionSource::getCause, ActionSource::setCause)
        .base();
}
