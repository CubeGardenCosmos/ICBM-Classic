package icbm.classic.content.items;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Antidote. An edible item that removes the gas effects.
 * <p>
 * 1.12 implemented this with {@code onItemUseFinish} (32 tick {@code EnumAction.EAT}, no nutrition) and
 * called {@code EntityLivingBase#clearActivePotions()}, which clears <b>every</b> active effect. 26.3
 * expresses the use behaviour with the food/consumable components and the effect removal in
 * {@code finishUsingItem} (S2.04 step 2).
 * <p>
 * Difference to 1.12: the removal list is explicit (vanilla poison effects) so the ICBM effects can be
 * added by S3.05; 1.12 removed every effect including the beneficial ones.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class ItemAntidote extends Item
{
    /** 1.12: {@code super.onItemUseFinish} cleared the potion effects, {@code getMaxItemUseDuration} was 32 ticks. */
    public static final FoodProperties FOOD = new FoodProperties.Builder().alwaysEdible().build();

    public static final Consumable CONSUMABLE = Consumable.builder()
            .consumeSeconds(32 / (float) 20)
            .animation(ItemUseAnimation.EAT)
            .sound(SoundEvents.GENERIC_EAT)
            .hasConsumeParticles(true)
            .build();

    /** Effects removed on consumption. */
    private static final List<Holder<MobEffect>> REMOVED_EFFECTS = List.of(MobEffects.POISON, MobEffects.WITHER);
    // TODO(S3.05) add the ICBM chemical/contagious effects to REMOVED_EFFECTS once ModEffects registers them

    public ItemAntidote(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (!level.isClientSide())
        {
            REMOVED_EFFECTS.forEach(entity::removeEffect);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
