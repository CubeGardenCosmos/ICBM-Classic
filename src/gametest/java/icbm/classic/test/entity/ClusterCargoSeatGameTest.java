package icbm.classic.test.entity;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.content.cargo.RecipeCargoData;
import icbm.classic.content.cargo.RecipeCargoDataDecraft;
import icbm.classic.content.cargo.parachute.EntityParachute;
import icbm.classic.content.cluster.action.ActionCluster;
import icbm.classic.content.cluster.action.ActionDataCluster;
import icbm.classic.content.cluster.bomblet.EntityBombDroplet;
import icbm.classic.content.cluster.bomblet.ItemBombDroplet;
import icbm.classic.content.cluster.missile.RecipeCluster;
import icbm.classic.content.entity.EntityPlayerSeat;
import icbm.classic.content.missile.entity.itemstack.EntityHeldItemMissile;
import icbm.classic.content.missile.entity.itemstack.item.RecipeHeldItemMissile;
import icbm.classic.content.missile.entity.itemstack.item.RecipeHeldItemMissileDecraft;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.data.ExplosiveSettings;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * GameTests for S4.06: cluster missiles, bomblets, cargo drops, held item missiles, seat, and custom recipes.
 */
public class ClusterCargoSeatGameTest
{
    @GameTest(maxTicks = 40)
    public void clusterMissileSpawnsBomblets(GameTestHelper helper)
    {
        final Vec3 spawnPos = helper.absoluteVec(new Vec3(3, 4, 3));
        final IActionSource source = new ActionSource(helper.getLevel(), spawnPos, null);
        final ActionDataCluster data = new ActionDataCluster();
        final ActionCluster action = new ActionCluster(helper.getLevel(), spawnPos, source, data);

        final NonNullList<ItemStack> list = NonNullList.create();
        final ItemStack bomblet = new ItemStack(ModItems.EXPLOSIVE_BOMBLETS.get(ICBMConstants.id("condensed")));
        for (int i = 0; i < 5; i++)
        {
            list.add(bomblet.copy());
        }
        action.setSpawnList(list);

        action.doAction();

        final List<EntityBombDroplet> droplets = helper.getEntities(ModEntities.BOMB_DROPLET);
        helper.assertTrue(droplets.size() >= 5, "expected at least 5 bomblets spawned, got " + droplets.size());
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void parachuteFallsSlowly(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(3, 4, 3);
        final EntityParachute parachute = helper.spawn(ModEntities.PARACHUTE, pos);

        helper.assertTrue(parachute.getProjectileGravity() == 0.01f,
                "expected gravity 0.01, got " + parachute.getProjectileGravity());
        helper.assertTrue(parachute.getAirResistance() == 0.95f,
                "expected air resistance 0.95, got " + parachute.getAirResistance());

        helper.runAfterDelay(5, () -> {
            final Vec3 motion = parachute.getDeltaMovement();
            helper.assertTrue(Math.abs(motion.y) <= 0.5,
                    "parachute fell too fast, motion.y: " + motion.y);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void heldItemMissileDropsPayloadOnImpact(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(3, 2, 3);
        final EntityHeldItemMissile missile = helper.spawn(ModEntities.HELD_ITEM_MISSILE, pos);
        final ItemStack payload = new ItemStack(Items.DIAMOND, 3);
        missile.setHeldItem(payload);

        final BlockHitResult hit = new BlockHitResult(helper.absoluteVec(new Vec3(3, 1, 3)), Direction.UP, helper.absolutePos(pos.below()), false);
        missile.actionOnImpact(hit);

        helper.assertTrue(missile.isRemoved(), "missile should be removed on impact");

        final List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(pos)).inflate(2),
                item -> item.getItem().is(Items.DIAMOND) && item.getItem().getCount() == 3);

        helper.assertTrue(!items.isEmpty(), "payload diamond was not dropped on impact");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void customRecipesMatchAndAssemble(GameTestHelper helper)
    {
        // 1. RecipeCluster
        final RecipeCluster recipeCluster = new RecipeCluster();
        final ItemStack clusterMissile = new ItemStack(ModItems.CLUSTER_MISSILE);
        final ItemStack bomblet1 = new ItemStack(ModItems.EXPLOSIVE_BOMBLETS.get(ICBMConstants.id("condensed")));
        final ItemStack bomblet2 = new ItemStack(ModItems.EXPLOSIVE_BOMBLETS.get(ICBMConstants.id("condensed")));
        final CraftingInput clusterInput = CraftingInput.of(2, 2, List.of(clusterMissile, bomblet1, bomblet2, ItemStack.EMPTY));
        helper.assertTrue(recipeCluster.matches(clusterInput, helper.getLevel()), "RecipeCluster failed to match");
        final ItemStack clusterOutput = recipeCluster.assemble(clusterInput);
        helper.assertTrue(clusterOutput.is(ModItems.CLUSTER_MISSILE), "RecipeCluster output is not cluster missile");
        final ExplosiveSettings settings = clusterOutput.get(ModDataComponents.EXPLOSIVE_SETTINGS);
        helper.assertTrue(settings != null && settings.cluster().isPresent() && settings.cluster().get().count() == 2,
                "RecipeCluster output missing expected count 2, got " + settings);

        // 2. RecipeHeldItemMissile
        final RecipeHeldItemMissile recipeHeld = new RecipeHeldItemMissile();
        final ItemStack heldMissile = new ItemStack(ModItems.HELD_ITEM_MISSILE);
        final ItemStack apple = new ItemStack(Items.APPLE);
        final CraftingInput heldInput = CraftingInput.of(2, 1, List.of(heldMissile, apple));
        helper.assertTrue(recipeHeld.matches(heldInput, helper.getLevel()), "RecipeHeldItemMissile failed to match");
        final ItemStack heldOutput = recipeHeld.assemble(heldInput);
        helper.assertTrue(heldOutput.is(ModItems.HELD_ITEM_MISSILE) && heldOutput.get(ModDataComponents.HELD_ITEM).is(Items.APPLE),
                "RecipeHeldItemMissile output did not hold apple");

        // 3. RecipeHeldItemMissileDecraft
        final RecipeHeldItemMissileDecraft recipeHeldDecraft = new RecipeHeldItemMissileDecraft();
        final CraftingInput heldDecraftInput = CraftingInput.of(1, 1, List.of(heldOutput));
        helper.assertTrue(recipeHeldDecraft.matches(heldDecraftInput, helper.getLevel()), "RecipeHeldItemMissileDecraft failed to match");
        final ItemStack decraftedHeld = recipeHeldDecraft.assemble(heldDecraftInput);
        helper.assertTrue(decraftedHeld.is(Items.APPLE), "RecipeHeldItemMissileDecraft output is not apple");
        final NonNullList<ItemStack> heldRemaining = recipeHeldDecraft.getRemainingItems(heldDecraftInput);
        helper.assertTrue(heldRemaining.get(0).is(ModItems.HELD_ITEM_MISSILE), "RecipeHeldItemMissileDecraft did not leave empty missile");

        // 4. RecipeCargoData
        final RecipeCargoData recipeCargo = new RecipeCargoData();
        final ItemStack parachute = new ItemStack(ModItems.PARACHUTE);
        final ItemStack goldenApple = new ItemStack(Items.GOLDEN_APPLE);
        final CraftingInput cargoInput = CraftingInput.of(2, 1, List.of(parachute, goldenApple));
        helper.assertTrue(recipeCargo.matches(cargoInput, helper.getLevel()), "RecipeCargoData failed to match");
        final ItemStack cargoOutput = recipeCargo.assemble(cargoInput);
        helper.assertTrue(cargoOutput.is(ModItems.PARACHUTE) && cargoOutput.get(ModDataComponents.HELD_ITEM).is(Items.GOLDEN_APPLE),
                "RecipeCargoData output did not hold golden apple");

        // 5. RecipeCargoDataDecraft
        final RecipeCargoDataDecraft recipeCargoDecraft = new RecipeCargoDataDecraft();
        final CraftingInput cargoDecraftInput = CraftingInput.of(1, 1, List.of(cargoOutput));
        helper.assertTrue(recipeCargoDecraft.matches(cargoDecraftInput, helper.getLevel()), "RecipeCargoDataDecraft failed to match");
        final ItemStack decraftedCargo = recipeCargoDecraft.assemble(cargoDecraftInput);
        helper.assertTrue(decraftedCargo.is(Items.GOLDEN_APPLE), "RecipeCargoDataDecraft output is not golden apple");
        final NonNullList<ItemStack> cargoRemaining = recipeCargoDecraft.getRemainingItems(cargoDecraftInput);
        helper.assertTrue(cargoRemaining.get(0).is(ModItems.PARACHUTE), "RecipeCargoDataDecraft did not leave empty parachute");

        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void playerSeatCanBeRidden(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(3, 2, 3);
        final EntityPlayerSeat seat = helper.spawn(ModEntities.MISSILE_SEAT, pos);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.startRiding(seat);
        helper.assertTrue(seat.hasPassenger(player), "player should be riding the seat");

        player.stopRiding();
        helper.assertTrue(!seat.hasPassenger(player), "player should have dismounted the seat");

        helper.succeed();
    }
}
