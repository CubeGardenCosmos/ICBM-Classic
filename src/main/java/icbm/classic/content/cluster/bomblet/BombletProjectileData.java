package icbm.classic.content.cluster.bomblet;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.api.missiles.projectile.ProjectileTypes;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collection;

@NoArgsConstructor
public class BombletProjectileData implements IProjectileData<EntityBombDroplet>, ISavable
{
    public static final Identifier NAME = ICBMConstants.id("bomblet");
    public static final ImmutableList<MetaTag> TYPES = ImmutableList.of(ProjectileTypes.TYPE_EXPLOSIVE);

    @Setter @Getter @Accessors(chain = true)
    private ItemStack explosiveStack = ItemStack.EMPTY;

    @Override
    public Collection<MetaTag> getTypeTags()
    {
        return TYPES;
    }

    @Override
    public Identifier getRegistryKey()
    {
        return NAME;
    }

    @Override
    public IProjectileDataRegistry getRegistry()
    {
        return ICBMClassicAPI.PROJECTILE_DATA_REGISTRY;
    }

    @Override
    public EntityBombDroplet newEntity(Level world, boolean allowItemPickup)
    {
        final EntityBombDroplet bombDroplet = new EntityBombDroplet(world);
        bombDroplet.setExplosiveStack(explosiveStack);
        return bombDroplet;
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

    private static final SaveHandler<BombletProjectileData> SAVE_LOGIC = new SaveHandler<BombletProjectileData>()
            .mainRoot()
            .nodeItemStack("explosive", BombletProjectileData::getExplosiveStack, BombletProjectileData::setExplosiveStack)
            .base();
}
