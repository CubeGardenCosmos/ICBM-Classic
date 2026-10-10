package icbm.classic.content.missile.logic.source;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.lib.saving.SaveHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ActionSource implements IActionSource {

    private Level world;
    private Vec3 position;
    private IActionCause cause;

    /** Dimension the source was created in. Restored from save so a caller with server access can resolve the level. */
    private Identifier dimensionKey;

    public ActionSource(Level world, Vec3 position, IActionCause cause) {
        this.world = world;
        this.position = position;
        this.cause = cause;
        this.dimensionKey = world != null ? world.dimension().identifier() : null;
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<ActionSource> SAVE_LOGIC = new SaveHandler<ActionSource>()
        .mainRoot()
        /* */.nodeWorldDim("dimension", ActionSource::getWorld, ActionSource::setDimensionKey)
        /* */.nodeVec3("pos", ActionSource::getPosition, ActionSource::setPosition)
        /* */.nodeBuildableObject("cause", () -> ICBMClassicAPI.ACTION_CAUSE_REGISTRY, ActionSource::getCause, ActionSource::setCause)
        .base();
}
