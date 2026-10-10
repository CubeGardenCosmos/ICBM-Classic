package icbm.classic.content.reg;

/**
 * Entity type registration. Renamed from the 1.12 {@code EntityReg} (porting-guide.md §3).
 * <p>
 * 1.12 used {@code EntityEntryBuilder} with a tracking range in blocks; 26.3 uses
 * {@code EntityType.Builder#clientTrackingRange} in <b>chunks</b> — the converted values are listed in
 * registry-ids.md §4. Filled by S3.01/S3.03 (blast entities) and S4 (missiles, grenade, bomb cart).
 */
public final class ModEntities
{
    private ModEntities() {}

    /** Registers every entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // TODO(S3.01/S3.03) fragment, flying_block, primed_explosive, explosion_holder, light_beam, smoke
        // TODO(S4) missiles, grenade, bomb_cart, bomblet, missile_seat, redmatter, parachute, balloon
    }
}
