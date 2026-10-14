package icbm.classic.content.cluster.action;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.EntityActionTypes;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.lib.saving.ISavable;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class ActionDataCluster implements IActionData, ISavable
{
    public static final Identifier REG_NAME = ICBMConstants.id("entity.cluster.spawning");
    private static final ImmutableList<MetaTag> TAGS = ImmutableList.of(EntityActionTypes.ENTITY_CREATION);

    @Getter
    private final NonNullList<ItemStack> clusterSpawnEntries = NonNullList.create();

    @NonNull
    @Override
    public IAction create(Level world, double x, double y, double z, @NonNull IActionSource source, @Nullable IActionFieldProvider fieldAccessor)
    {
        final ActionCluster cluster = new ActionCluster(world, new Vec3(x, y, z), source, this);
        cluster.setSpawnList(clusterSpawnEntries);
        if (fieldAccessor != null && fieldAccessor.hasField(ActionFields.HOST_ENTITY))
        {
            final Entity host = fieldAccessor.getValue(ActionFields.HOST_ENTITY);
            if (host != null)
            {
                cluster.setSourcePitch(host.getXRot());
                cluster.setSourceYaw(host.getYRot());
            }
        }
        return cluster;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }

    @NonNull
    @Override
    public Collection<MetaTag> getTypeTags()
    {
        return TAGS;
    }

    @Override
    public void save(ValueOutput out)
    {
        out.store("clusterSpawnEntries", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(clusterSpawnEntries));
    }

    @Override
    public void load(ValueInput in)
    {
        clusterSpawnEntries.clear();
        in.read("clusterSpawnEntries", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(clusterSpawnEntries::addAll);
    }
}
