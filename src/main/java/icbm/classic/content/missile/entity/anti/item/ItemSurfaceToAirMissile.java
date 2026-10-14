package icbm.classic.content.missile.entity.anti.item;

import net.minecraft.world.item.Item;

/**
 * Surface to Air Missile (SAM) / Anti-Ballistic Missile item.
 *
 * Created by Robin Seifert on 11/30/2021.
 */
public class ItemSurfaceToAirMissile extends Item
{
    public ItemSurfaceToAirMissile(Properties properties)
    {
        super(properties.stacksTo(1));
    }
}
