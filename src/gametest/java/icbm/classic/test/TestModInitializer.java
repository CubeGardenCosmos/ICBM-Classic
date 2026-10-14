package icbm.classic.test;

import icbm.classic.test.projectile.TestProjectile;
import net.fabricmc.api.ModInitializer;

/**
 * Entry point of the {@code icbmclassic-test} mod. Runs while the game is still loading, which is the only
 * window where entity types can be registered into {@code BuiltInRegistries}.
 */
public class TestModInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        TestProjectile.register();
    }
}
