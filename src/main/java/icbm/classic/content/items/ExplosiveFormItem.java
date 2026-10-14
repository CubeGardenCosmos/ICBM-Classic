package icbm.classic.content.items;

import icbm.classic.api.EnumTier;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.reg.ExplosiveForms;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

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

    /**
     * Appends the explosive tier line 1.12 {@code ItemBlockExplosive#getDetailedInfo} produced for the flat
     * explosive items. Shared by {@link ItemGrenade} and {@link ItemBombCart} (S4.05); call it from
     * {@code appendHoverText}.
     * <p>
     * 1.12: {@code TextFormatting.DARK_RED + LanguageUtility.getLocal("info.misc.tier") + ": " +
     * tierData.getTooltipColor() + tierData.getLocalizedName()} and only when the tier is not
     * {@code EnumTier.NONE}.
     */
    protected void appendExplosiveTier(Consumer<Component> tooltip)
    {
        final IExplosiveData data = ICBMClassicHelpers.getExplosive(explosiveId, true);
        if (data != null && data.getTier() != EnumTier.NONE)
        {
            final EnumTier tier = data.getTier();
            tooltip.accept(Component.translatable("icbmclassic.tooltip.misc.tier").withStyle(ChatFormatting.DARK_RED)
                    .append(": ")
                    .append(Component.translatable("icbmclassic.tier." + tier.getSerializedName()).withStyle(tier.getTooltipColor())));
        }
    }
}
