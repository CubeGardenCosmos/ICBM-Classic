package icbm.classic.api;

import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.caps.IGPSData;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.capability.ex.CapabilityExplosiveStack;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
    public static IGPSData getGPSData(ItemStack stack) {
        // TODO(S2.04/S5.08) read the icbmclassic:gps_data data component (GpsData(Optional<GlobalPos>)).
        //  The component is registered by ModDataComponents (S2.01/S2.04), so this returns null until then.
        //  1.12 used the Forge GPS_CAPABILITY.
        return null;
    }
}
