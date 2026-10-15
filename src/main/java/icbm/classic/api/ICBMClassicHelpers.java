package icbm.classic.api;

import icbm.classic.api.caps.IExplosive;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.api.caps.IGPSData;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.capability.ex.CapabilityExplosiveStack;
import icbm.classic.lib.data.GpsData;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 * <p>
 * The 1.12 capability lookups were replaced in 26.3: entities/blocks register with
 * {@link IcbmLookups} (or implement the interface directly), so every capability presence/access call
 * became an {@code instanceof}, a lookup query or an explicit deferral where the replacement is a data
 * component that is not registered yet.
 */
public final class ICBMClassicHelpers
{

    /**
     * Called to get explosive
     *
     * @param name - registry name of the explosive
     * @return explosive desired, or default TNT
     */
    public static IExplosiveData getExplosive(String name, boolean returnNull)
    {
        // 1.12 built a resource location from the name -> 26.3 Identifier.parse(name)
        return getExplosive(Identifier.parse(name), returnNull);
    }

    /**
     * Called to get explosive
     *
     * @param name - registry name of the explosive
     * @return explosive desired, or default TNT
     */
    public static IExplosiveData getExplosive(Identifier name, boolean returnNull)
    {
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(name);
        if (data != null)
        {
            return data;
        }
        System.out.println("ICBMClassicAPI: Error - Failed to locate explosive for Name[" + name + "] this may cause unexpected logic");
        return returnNull ? null : ICBMExplosives.CONDENSED;
    }

    /**
     * Checks if the entity is a missile
     *
     * @param entity
     * @return
     */
    public static boolean isMissile(Entity entity)
    {
        return entity != null && getMissile(entity) != null;
    }

    public static boolean isMissile(ItemStack stack)
    {
        return !stack.isEmpty() && IcbmLookups.MISSILE_STACK.find(stack, null) != null;
    }

    /**
     * Helper to get the {@link IMissile} of an entity.
     * <p>
     * 1.12 read the missile capability; 26.3 first checks if the entity implements {@link IMissile} and
     * otherwise asks the {@code icbmclassic:missile} lookup so third party entities can still be missiles.
     *
     * @param entity to pull from
     * @return missile or null if not a missile
     */
    public static IMissile getMissile(Entity entity)
    {
        if (entity == null)
        {
            return null;
        }
        return entity instanceof IMissile missile ? missile : IcbmLookups.MISSILE.find(entity, null);
    }

    public static boolean isExplosive(Entity entity)
    {
        return entity instanceof IExplosive;
    }

    public static IExplosive getExplosive(Entity entity)
    {
        return entity instanceof IExplosive explosive ? explosive : null;
    }

    public static boolean isLauncher(BlockEntity blockEntity, Direction side)
    {
        return getLauncher(blockEntity, side) != null;
    }

    public static IMissileLauncher getLauncher(BlockEntity blockEntity, Direction side)
    {
        if (blockEntity == null || blockEntity.getLevel() == null)
        {
            return null;
        }
        return IcbmLookups.MISSILE_LAUNCHER.find(blockEntity.getLevel(), blockEntity.getBlockPos(), side);
    }

    /**
     * Gets the {@link IExplosive} of an item.
     * <p>
     * 1.12 read the {@code EXPLOSIVE_CAPABILITY} of the stack. 26.3 items are flat: the explosive id is a
     * field of the item class and the optional {@code icbmclassic:explosive_settings} data component
     * carries customizations.
     *
     * @param stack to read
     * @return explosive or null if the item has none
     */
    public static IExplosive getExplosive(ItemStack stack)
    {
        // TODO(S4/S5) the flat missile, grenade and bomb cart items carry their own explosive id, see
        //  CapabilityExplosiveStack#fromStack
        return CapabilityExplosiveStack.fromStack(stack);
    }

    /**
     * Gets the {@link IGPSData} of an item (coord cards, laser detonator, radar gun).
     *
     * @param stack to access
     * @return data if present, or null otherwise
     */
    public static IGPSData getGPSData(ItemStack stack)
    {
        return stack.get(ModDataComponents.GPS_DATA) == null ? null : new GpsDataView(stack);
    }

    /** Compatibility view over the immutable GPS component; the component remains the sole storage. */
    private static final class GpsDataView implements IGPSData
    {
        private final ItemStack stack;

        private GpsDataView(ItemStack stack)
        {
            this.stack = stack;
        }

        private GlobalPos position()
        {
            return stack.getOrDefault(ModDataComponents.GPS_DATA, GpsData.EMPTY).pos().orElse(null);
        }

        @Override
        public void setPosition(net.minecraft.world.phys.Vec3 position)
        {
            if (position == null)
            {
                stack.remove(ModDataComponents.GPS_DATA);
                return;
            }
            GlobalPos current = position();
            stack.set(ModDataComponents.GPS_DATA, new GpsData(GlobalPos.of(
                    current == null ? net.minecraft.world.level.Level.OVERWORLD : current.dimension(),
                    BlockPos.containing(position))));
        }

        @Override
        public void setWorld(net.minecraft.world.level.Level level)
        {
            setWorld(level == null ? null : level.dimension().identifier());
        }

        @Override
        public void setWorld(Identifier dimension)
        {
            GlobalPos current = position();
            if (dimension == null || current == null)
            {
                stack.remove(ModDataComponents.GPS_DATA);
                return;
            }
            stack.set(ModDataComponents.GPS_DATA, new GpsData(GlobalPos.of(
                    ResourceKey.create(Registries.DIMENSION, dimension), current.pos())));
        }

        @Override
        public net.minecraft.world.phys.Vec3 getPosition()
        {
            GlobalPos current = position();
            return current == null ? null : net.minecraft.world.phys.Vec3.atLowerCornerOf(current.pos());
        }

        @Override
        public Identifier getWorldId()
        {
            GlobalPos current = position();
            return current == null ? null : current.dimension().identifier();
        }
    }
}
