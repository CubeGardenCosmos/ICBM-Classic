package icbm.classic.content.items;

import icbm.classic.content.reg.ModEffects;
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

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * Antidote. An edible item that removes the gas effects.
 * <p>
 * 1.12 implemented this with {@code onItemUseFinish} (32 tick {@code EnumAction.EAT}, no nutrition) and
 * called {@code EntityLivingBase#clearActivePotions()}, which clears <b>every</b> active effect. 26.3
 * expresses the use behaviour with the food/consumable components and the effect removal in
 * {@code finishUsingItem} (S2.04 step 2). S3.05 fills the removal list with the effects the gas blasts
 * and the custom poison effects hand out.
 * <p>
 * Difference to 1.12: the removal list is explicit. 1.12 removed every effect including the beneficial
 * ones (the item's own tooltip was "Cures all effects"), the port removes the mod's gas effects plus the
 * vanilla effects the 1.12 gas blasts applied, so a gas victim is cured the same way while effects the
 * antidote has nothing to do with (e.g. regeneration) survive. See S2.04/S3.05 evidence for the record.
 * <p>
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

    /**
     * Vanilla effects removed on consumption.
     * <p>
     * POISON and WITHER came from the S2.04 list (the 1.12 item cured every effect). The remaining entries
     * are the effects the 1.12 gas blasts applied to an unprotected entity, and are still applied by the
     * 26.3 gas blasts (the custom poison effects re-apply their old set, S3.05):
     * <ul>
     *     <li>{@code Poison} (chemical 30s, contagious 45s, debilitation 18s)</li>
     *     <li>{@code Nausea} (chemical 30s)</li>
     *     <li>{@code Hunger} (chemical and contagious 30s)</li>
     *     <li>{@code Weakness} (chemical and contagious 35s)</li>
     *     <li>{@code MiningFatigue} (chemical and contagious 60s, debilitation 60s)</li>
     *     <li>{@code Blindness} (contagious 15s)</li>
     *     <li>{@code Slowness} (debilitation 60s, amplifier 2)</li>
     * </ul>
     * Keep in sync with {@code content/blast/gas/**} when those effects change.
     */
    private static final List<Holder<MobEffect>> REMOVED_VANILLA_EFFECTS = List.of(
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.NAUSEA,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.MINING_FATIGUE,
            MobEffects.BLINDNESS,
            MobEffects.SLOWNESS);

    public ItemAntidote(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (!level.isClientSide())
        {
            REMOVED_VANILLA_EFFECTS.forEach(entity::removeEffect);

            // ModEffects hands out its holders inside register(), so they can still be absent while this
            // class is loaded. Resolve them per use instead of capturing them in a static list.
            removeModEffect(entity, ModEffects.TOXIN);
            removeModEffect(entity, ModEffects.VIRUS);
            removeModEffect(entity, ModEffects.FROST_BITE);
            removeModEffect(entity, ModEffects.RADIATION);
        }
        // 1.12 shrank the stack after clearing the effects, skipping creative mode; the consumable
        // component does the same (it does not consume the stack for an entity with infinite materials)
        return super.finishUsingItem(stack, level, entity);
    }

    private static void removeModEffect(LivingEntity entity, @Nullable Holder<MobEffect> effect)
    {
        if (effect != null)
        {
            entity.removeEffect(effect);
        }
    }
}
