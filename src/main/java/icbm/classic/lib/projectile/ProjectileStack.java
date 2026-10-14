package icbm.classic.lib.projectile;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileStack;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 1.12 bound the type parameter to {@code EntityParachute}. That class is ported with the cargo content
 * (S4.06), so the stack is typed to {@link Entity} until then.
 */
public class ProjectileStack implements IProjectileStack<Entity>, ISavable {

    @Getter @Setter @Accessors(chain = true)
    private IProjectileData projectileData;


    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }


    private static final SaveHandler<ProjectileStack> SAVE_LOGIC = new SaveHandler<ProjectileStack>()
        //Stuck in ground data
        .mainRoot()
        .nodeBuildableObject("data", () -> ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, ProjectileStack::getProjectileData, ProjectileStack::setProjectileData)
        .base();
}
