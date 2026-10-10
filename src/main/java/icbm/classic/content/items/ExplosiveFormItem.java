package icbm.classic.content.items;

import icbm.classic.content.reg.ExplosiveForms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Flat explosive item (missile, grenade, bomb cart, bomblet). 1.12 had one item class per form with the
 * explosive stored in the item metadata; ADR 0002 flattens them into one item per explosive and form.
 * <p>
 * S2.05 only needs the flat items so the ids and the creative tab exist. S4.02 (missile), S4.05 (grenade,
 * bomb cart) and S4.06 (bomblet) replace this class with the real behaviour classes.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class ExplosiveFormItem extends Item
{
    private final Identifier explosiveId;
    private final ExplosiveForms.ExplosiveForm form;

    public ExplosiveFormItem(Identifier explosiveId, ExplosiveForms.ExplosiveForm form, Item.Properties properties)
    {
        super(properties);
        this.explosiveId = explosiveId;
        this.form = form;
    }

    /** Explosive this item represents, see {@code docs/architecture/registry-ids.md} §1. */
    public Identifier getExplosiveId()
    {
        return explosiveId;
    }

    /** Form this item represents (missile, grenade, bomb cart or bomblet). */
    public ExplosiveForms.ExplosiveForm getForm()
    {
        return form;
    }
}
