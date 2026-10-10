package icbm.classic.test.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Covers the S2.04 materials and simple items:
 *
 * <ul>
 *     <li>every id from registry-ids.md §3 that this step registers exists in the item registry and the
 *     {@code ModItems} field points at it,</li>
 *     <li>the antidote consumes (food + consumable components) and removes the vanilla effects it lists,</li>
 *     <li>effects outside that list stay (the 26.3 port uses an explicit removal list, see the class doc).</li>
 * </ul>
 */
public class MaterialsGameTest
{
    @GameTest(maxTicks = 40)
    public void materialsAndAntidote(GameTestHelper helper)
    {
        final List<String> ids = List.of(
                "steel_ingot", "copper_ingot", "steel_clump", "steel_plate", "iron_plate",
                "basic_circuit", "advanced_circuit", "elite_circuit", "copper_wire", "gold_wire",
                "sulfur_dust", "saltpeter", "saltpeter_ball", "poison_powder", "antidote",
                "empty_bomblet", "missile_module");

        for (String id : ids)
        {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ICBMConstants.id(id)),
                    "item " + id + " is not registered");
        }

        helper.assertTrue(ModItems.STEEL_INGOT != null && ModItems.STEEL_INGOT == BuiltInRegistries.ITEM.getValue(ICBMConstants.id("steel_ingot")),
                "ModItems.STEEL_INGOT does not point at the registered item");
        helper.assertTrue(ModItems.MISSILE_MODULE != null && ModItems.MISSILE_MODULE == BuiltInRegistries.ITEM.getValue(ICBMConstants.id("missile_module")),
                "ModItems.MISSILE_MODULE does not point at the registered item");

        // the antidote has to be edible (1.12 EnumAction.EAT with a 32 tick duration)
        helper.assertTrue(new ItemStack(ModItems.ANTIDOTE).getItem().components().has(net.minecraft.core.component.DataComponents.CONSUMABLE),
                "the antidote must have the consumable component");

        // a pig, not an undead mob: undead are immune to poison/regeneration, which would hide a broken antidote
        final Pig subject = EntityTypes.PIG.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (subject == null)
        {
            helper.fail("failed to create a pig for the antidote test");
            return;
        }
        subject.setNoAi(true);
        helper.getLevel().addFreshEntity(subject);

        subject.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        subject.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        subject.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        helper.assertTrue(subject.hasEffect(MobEffects.POISON) && subject.hasEffect(MobEffects.WITHER),
                "precondition failed, the subject did not receive the effects");

        new ItemStack(ModItems.ANTIDOTE).finishUsingItem(helper.getLevel(), subject);

        helper.assertTrue(!subject.hasEffect(MobEffects.POISON), "the antidote should remove poison");
        helper.assertTrue(!subject.hasEffect(MobEffects.WITHER), "the antidote should remove wither");
        helper.assertTrue(subject.hasEffect(MobEffects.REGENERATION),
                "the antidote should keep effects that are not on its removal list");

        // a plain material item must not be edible
        helper.assertTrue(!new ItemStack(ModItems.SULFUR_DUST).getItem().components().has(net.minecraft.core.component.DataComponents.CONSUMABLE),
                "a material must not be edible");
        helper.assertTrue(new ItemStack(Items.STONE).getItem() != ModItems.SULFUR_DUST,
                "sanity check, the sulfur item must not be vanilla stone");

        helper.succeed();
    }
}
