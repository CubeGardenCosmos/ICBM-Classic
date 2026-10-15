package icbm.classic.test.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.ConfigMain;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.gas.ProtectiveArmorHandler;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModEffects;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the gas explosives, the custom effects, the armour protection and the antidote (S3.05):
 *
 * <ul>
 *     <li>chemical gas hands the poison set to an unprotected entity,</li>
 *     <li>an entity in full configured protection is spared,</li>
 *     <li>debilitation applies its effect set,</li>
 *     <li>the smoke explosive spawns the smoke entity through {@code ActionSpawnEntity},</li>
 *     <li>the antidote clears the mod's effects.</li>
 * </ul>
 */
public class GasBlastsGameTest
{
    /** Center of the blast, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Size every blast is forced to, keeps the affected area inside the test structure */
    private static final int TEST_SIZE = 3;

    private static IAction triggerBlast(GameTestHelper helper, String explosiveId)
    {
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id(explosiveId), false);
        final BlockPos pos = helper.absolutePos(CENTER);
        final Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final IActionSource source = new ActionSource(helper.getLevel(), center, new CauseByTimer("test", 0));

        final IAction action = data.create(helper.getLevel(), center.x, center.y, center.z, source, null);
        if (action instanceof IBlastInit init)
        {
            init.setBlastSize(TEST_SIZE);
        }
        action.doAction();
        return action;
    }

    private static Zombie spawnZombie(GameTestHelper helper, double x, double z)
    {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        final Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(x, CENTER.getY() + 1.0D, z));
        // the gas cloud only covers its own radius, a walking zombie can wander out of it and make the
        // result depend on the mob AI; the tests only care about the gas
        zombie.setNoAi(true);
        return zombie;
    }

    private static void resetArmorProtection()
    {
        ProtectiveArmorHandler.loadFromConfig();
        ProtectiveArmorHandler.protectionValues.clearDefaults();
    }

    @GameTest(maxTicks = 800)
    public void chemicalBlastPoisonsZombie(GameTestHelper helper)
    {
        final Zombie zombie = spawnZombie(helper, 4.5D, 4.5D);

        triggerBlast(helper, "chemical");

        // the gas cloud ticks, so the effect lands after the cloud built up
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.hasEffect(ModEffects.TOXIN),
                        "chemical gas did not poison the zombie (removed " + zombie.isRemoved() + ", at "
                                + zombie.position() + ", effects " + zombie.getActiveEffects() + ")"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 800)
    public void chemicalBlastSparesProtectedZombie(GameTestHelper helper)
    {
        try
        {
            // 1.12 resolved the armour ratings from config; the port keeps that list, so the rating for the
            // uniform is registered here the same way the config file would. `require_helmet` is on, so the
            // unprotected zombie (no helmet) still gets poisoned while the armoured one does not.
            ProtectiveArmorHandler.protectionValues.setDefaultMeta(new ItemStack(Items.IRON_HELMET), 1.0F, 1);
            ProtectiveArmorHandler.loadFromConfig();

            final Zombie armored = spawnZombie(helper, 4.5D, 4.5D);
            armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));

            helper.assertTrue(ProtectiveArmorHandler.getProtectionRating(armored)
                            >= ConfigMain.protectiveArmor.minProtectionChemicalGas,
                    "the test setup did not reach the protection threshold, rating was "
                            + ProtectiveArmorHandler.getProtectionRating(armored));

            triggerBlast(helper, "chemical");

            // wait for the cloud to have ticked at least once, then assert nothing was applied
            helper.startSequence()
                    .thenIdle(100)
                    .thenExecute(() -> {
                        try
                        {
                            helper.assertTrue(armored.getActiveEffects().isEmpty(),
                                    "the protected zombie was poisoned anyway, effects: " + armored.getActiveEffects());
                        }
                        finally
                        {
                            resetArmorProtection();
                        }
                    })
                    .thenSucceed();
        }
        catch (RuntimeException | Error failure)
        {
            resetArmorProtection();
            throw failure;
        }
    }

    @GameTest(maxTicks = 800)
    public void debilitationBlastAppliesEffects(GameTestHelper helper)
    {
        final Zombie zombie = spawnZombie(helper, 4.5D, 4.5D);

        triggerBlast(helper, "debilitation");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!zombie.isRemoved() && !zombie.getActiveEffects().isEmpty(),
                        "debilitation gas did not apply any effect to the zombie (removed " + zombie.isRemoved()
                                + ", at " + zombie.position() + ", effects " + zombie.getActiveEffects() + ")"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 800)
    public void smokeBlastSpawnsSmokeEntity(GameTestHelper helper)
    {
        triggerBlast(helper, "smoke");

        // the smoke explosive is an ActionSpawnEntity that creates the registered smoke entity
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!helper.getEntities(ModEntities.SMOKE).isEmpty(),
                        "the smoke explosive did not spawn a smoke entity"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void antidoteClearsModEffects(GameTestHelper helper)
    {
        final Zombie zombie = spawnZombie(helper, 4.5D, 4.5D);
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.TOXIN, 200, 0));
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.RADIATION, 200, 0));
        helper.assertTrue(zombie.hasEffect(ModEffects.TOXIN), "the test could not apply the mod effects");

        final ItemStack antidote = new ItemStack(ModItems.ANTIDOTE);
        antidote.getItem().finishUsingItem(antidote, helper.getLevel(), zombie);

        helper.assertTrue(!zombie.hasEffect(ModEffects.TOXIN) && !zombie.hasEffect(ModEffects.RADIATION),
                "the antidote did not clear the mod effects, effects left: " + zombie.getActiveEffects());
        helper.succeed();
    }
}

