package icbm.classic.content.cluster.bomblet;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.api.missiles.projectile.ProjectileTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.util.Collection;

@NoArgsConstructor
public class BombletProjectileData implements IProjectileData<EntityBombDroplet>, INBTSerializable<NBTTagCompound> {

    public static final ResourceLocation NAME = new ResourceLocation(ICBMConstants.DOMAIN, "bomblet");
    public static final ImmutableList<MetaTag> TYPES = ImmutableList.of(ProjectileTypes.TYPE_EXPLOSIVE);

    @Setter @Getter @Accessors(chain = true)
    private ItemStack explosiveStack = ItemStack.EMPTY;

    @Nonnull
    public Collection<MetaTag> getTypeTags() {
        return TYPES;
    }

    @Nonnull
    @Override
    public ResourceLocation getRegistryKey() {
        return NAME;
    }

    @Nonnull
    @Override
    public IProjectileDataRegistry getRegistry() {
        return ICBMClassicAPI.PROJECTILE_DATA_REGISTRY;
    }

    @Override
    public EntityBombDroplet newEntity(World world, boolean allowItemPickup) {
        final EntityBombDroplet bombDroplet = new EntityBombDroplet(world);
        bombDroplet.explosive.setStack(explosiveStack);
        return bombDroplet;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound save = new NBTTagCompound();
        save.setTag("explosive", explosiveStack.serializeNBT());
        return save;
    }

    @Override
    public void deserializeNBT(NBTTagCompound save) {
        explosiveStack = new ItemStack(save.getCompoundTag("explosive"));
    }
}
