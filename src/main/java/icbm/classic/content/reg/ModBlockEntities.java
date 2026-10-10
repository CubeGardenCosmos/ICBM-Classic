package icbm.classic.content.reg;

/**
 * Block entity type registration (1.12 {@code GameRegistry.registerTileEntity} calls in {@code BlockReg}).
 * <p>
 * IDs come from registry-ids.md §2; Filled by S3.09 (explosive), S5.01–S5.06 (machines).
 */
public final class ModBlockEntities
{
    private ModBlockEntities() {}

    /** Registers every block entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // TODO(S3.09) explosive (shared by the 23 flat explosive blocks)
        // TODO(S5.02..S5.06) launcher_base, launcher_frame, launcher_connector, launcher_screen,
        //  cruise_launcher, radar_station, emp_tower + emp_tower_fake
    }
}
