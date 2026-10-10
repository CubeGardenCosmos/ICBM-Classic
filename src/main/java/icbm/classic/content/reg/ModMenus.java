package icbm.classic.content.reg;

/**
 * Menu type registration (1.12 {@code IGuiHandler}). The five machines use
 * {@code ExtendedMenuType<T, BlockPos>} (registry-ids.md §5, porting-guide.md §7).
 * <p>
 * Filled by S5.03–S5.06.
 */
public final class ModMenus
{
    private ModMenus() {}

    /** Registers every menu type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // TODO(S5.03..S5.06) launcher_base, launcher_screen, cruise_launcher, radar_station, emp_tower
    }
}
