package icbm.classic.content.actions.emp;

import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.config.util.BlockStateConfigList;

/**
 * Config list of the block replacements applied by {@link ActionEmpArea}.
 * <p>
 * 26.3 port notes (1.12 -> 26.3):
 * <ul>
 *     <li>The 1.12 {@code BlockStateConfigList.BlockChanceOut} was ported in
 *     {@code config/util/BlockStateConfigList.java} together with {@code BlockReplacementData}.</li>
 *     <li>1.12 labelled the entries with a Forge config path
 *     ({@code "icbmclassic/blast/emp/replacements/list"}); 26.3 reads the JSON config
 *     {@code config/icbmclassic/blast.json}, so the source label is the equivalent section path.
 *     {@link #setup()} has to run after {@link icbm.classic.config.core.ConfigManager#loadAll()} and after the
 *     block registry is frozen.</li>
 * </ul>
 * 1.12 source: {@code legacy-1.12/src/main/java/icbm/classic/content/actions/emp/EmpHandler.java}.
 */
public class EmpHandler {
    public static final BlockStateConfigList.BlockChanceOut empBlockSwaps = new BlockStateConfigList.BlockChanceOut("[EmpReplacements]", (configList) -> {
        configList.load("icbmclassic/blast.json#emp.replacements.list", ConfigBlast.emp.replacements.blockStates);
    });

    public static void loadFromConfig() {
        empBlockSwaps.reload();
    }

    public static void setup() {
        loadFromConfig();
    }
}
