package icbm.classic.test.projectile;

import icbm.classic.lib.projectile.EntityProjectile;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import org.jspecify.annotations.NonNull;

/**
 * Minimal {@link EntityProjectile} used by the game tests: no damage type, no health, no gravity, and it
 * records every impact so the test can assert that the base class collision handling ran.
 */
public class TestProjectile extends EntityProjectile<TestProjectile> {

    /** Entity type of the test projectile, assigned by {@link #register()} during mod init */
    public static EntityType<TestProjectile> TYPE;

    /** Set by {@link #onImpact(HitResult)} */
    public boolean impacted;

    /**
     * Registers the entity type into the {@code icbmclassic-test} namespace. Called from
     * {@code icbm.classic.test.TestModInitializer} while the registries are still writable.
     */
    public static void register() {
        if (TYPE != null) {
            return;
        }
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("icbmclassic-test", "test_projectile"));
        TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
            EntityType.Builder.<TestProjectile>of(TestProjectile::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(20)
                .build(key));
    }

    public TestProjectile(EntityType<? extends TestProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onImpact(@NonNull HitResult hit) {
        this.impacted = true;
        super.onImpact(hit);
    }

    /** Straight flight keeps the test independent of the gravity constant */
    @Override
    protected float getProjectileGravity() {
        return 0F;
    }

    /** The test projectile has no health, so damage from {@link #onImpactEntity} is ignored */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}
