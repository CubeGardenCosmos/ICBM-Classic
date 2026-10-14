package icbm.classic.content.actions.emp;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.*;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Action data of the EMP explosive. Creates an {@link ActionEmpArea} sized from the {@code area.size} field
 * (falling back to {@link ConfigBlast#emp}{@code .scale}).
 * <p>
 * 26.3 port notes (1.12 -> 26.3):
 * <ul>
 *     <li>1.12 built the registry key from the mod domain plus {@code "emp.area"}; 26.3 uses
 *     {@link ICBMConstants#id(String)}.</li>
 *     <li>{@code new Vec3d(x, y, z)} → {@code new Vec3(x, y, z)}; {@code getTypeTags()} now returns a
 *     {@code Collection<MetaTag>} instead of being un-annotated.</li>
 * </ul>
 * 1.12 source: {@code legacy-1.12/src/main/java/icbm/classic/content/actions/emp/ActionDataEmpArea.java}.
 */
public class ActionDataEmpArea implements IActionData {
    public static final ImmutableList<MetaTag> ACTION_TAGS = ImmutableList.of(BlockActionTypes.BLOCK_EDIT, EntityActionTypes.ENTITY_EDIT, ActionTypes.DESTRUCTIVE, WorldActionTypes.WORLD_AREA, EntityActionTypes.ENTITY_AREA);
    public static final Identifier REG_NAME = ICBMConstants.id("emp.area");
    /** @deprecated This is temp until registered */
    public static IActionData INSTANCE = new ActionDataEmpArea();

    @NonNull
    public Collection<MetaTag> getTypeTags() {
        return ACTION_TAGS;
    }

    @NonNull
    @Override
    public IAction create(Level world, double x, double y, double z, @NonNull IActionSource source, @Nullable IActionFieldProvider fieldAccessor) {
        final ActionEmpArea actionEmpArea = new ActionEmpArea(world, new Vec3(x, y, z), source, this);
        if(fieldAccessor != null && fieldAccessor.hasField(ActionFields.AREA_SIZE)) {
            actionEmpArea.setSize((int)Math.floor(fieldAccessor.getValue(ActionFields.AREA_SIZE))); //TODO allow changing x, y, z size
        }
        else {
            actionEmpArea.setSize((int)Math.floor(ConfigBlast.emp.scale));
        }
        return actionEmpArea;
    }

    @Override
    public List<ActionField<?>> getSupportedFields() {
        return ActionEmpArea.SUPPORTED_FIELDS;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }
}
