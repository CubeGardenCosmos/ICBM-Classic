package icbm.classic.test.energy;

import icbm.classic.config.ConfigMain;
import icbm.classic.lib.energy.ExposedEnergyStorage;
import icbm.classic.lib.energy.IcbmEnergyBuffer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import team.reborn.energy.api.EnergyStorage;

public class EnergyTransactionGameTest {
    @GameTest(maxTicks = 10)
    public void exposedStorageIsTransactionalAndInsertOnly(GameTestHelper helper) {
        boolean requiresPower = ConfigMain.REQUIRES_POWER;
        ConfigMain.REQUIRES_POWER = true;
        try {
            IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 1_000L, () -> 1_000L);
            EnergyStorage storage = new ExposedEnergyStorage(buffer, () -> {});

            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.insert(500, transaction) == 500, "insert must accept 500 FE");
            }
            helper.assertTrue(buffer.getStored() == 0, "closing an uncommitted transaction must roll back inserted energy");

            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.insert(500, transaction) == 500, "committed insert must accept 500 FE");
                transaction.commit();
            }
            helper.assertTrue(buffer.getStored() == 500, "committed insert must persist 500 FE");
            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.extract(500, transaction) == 0, "extraction must always be rejected");
            }

            ConfigMain.REQUIRES_POWER = false;
            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.insert(500, transaction) == 0,
                        "insert must be rejected when power is not required");
                transaction.commit();
            }
            helper.succeed();
        } finally {
            ConfigMain.REQUIRES_POWER = requiresPower;
        }
    }
}
