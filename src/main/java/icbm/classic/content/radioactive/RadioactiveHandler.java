package icbm.classic.content.radioactive;

import icbm.classic.ICBMClassic;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.config.util.BlockStateConfigList;
import icbm.classic.config.util.RegistryLookup;
import icbm.classic.content.blast.BlastBeam;
import icbm.classic.content.reg.ModBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Table of the block swaps done by the radioactive fallout of a nuclear blast
 * (1.12 {@code icbm.classic.content.radioactive.RadioactiveHandler}).
 * <p>
 * The table holds the 1.12 default rows (stone to radioactive stone, the dirt family to radioactive dirt,
 * every plant/leaf/vine to air) and then the user entries of
 * {@code ConfigBlast.nuclear.radiationReplacements} on top of them.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>table keys are {@code namespace:path} strings instead of the 1.12 registry names
 *     ({@link RegistryLookup#keyOf(Block)}); the table itself is the same
 *     {@link BlockStateConfigList.BlockChanceOut} and the config entries are the 1.12
 *     {@code domain:resource@meta} / {@code =} syntax.</li>
 *     <li>the 1.12 loop over {@code ForgeRegistries.BLOCKS} with {@code Material} checks is replaced by
 *     {@link RegistryLookup#blockKeys()} plus
 *     {@link BlastBeam#isVegetation(BlockState)}, the ported material table of the beam
 *     blasts. That table is tag based, so the rows can only be built once the vanilla block tags are
 *     loaded: 1.12 could build them during init, the port has to build them from a tag/server start hook
 *     (see {@link #setup()}).</li>
 *     <li>1.12 {@code Blocks.GRASS_PATH} is {@link Blocks#DIRT_PATH} in 26.3 and {@code Blocks.GRASS} is
 *     {@link Blocks#GRASS_BLOCK}; 1.12 read the two radioactive states from the one metadata block, the port
 *     reads the two flat blocks ({@link ModBlocks#RADIOACTIVE_DIRT}, {@link ModBlocks#RADIOACTIVE_STONE}).</li>
 *     <li>1.12 reloaded the table from the Forge config change event; the 26.3 config is read once while
 *     starting up, {@link #loadFromConfig()} stays for that call and for tests.</li>
 * </ul>
 */
public class RadioactiveHandler
{
    /** 1.12 {@code RadioactiveHandler.radioactiveBlockSwaps} */
    public static final BlockStateConfigList.BlockChanceOut radioactiveBlockSwaps = new BlockStateConfigList.BlockChanceOut("[RadioactiveReplacements]", (configList) -> {
        configList.setDefault(RegistryLookup.keyOf(Blocks.STONE), new BlockReplacementData().setChance(0.2f).setBlockState(ModBlocks.RADIOACTIVE_STONE.defaultBlockState()), 0);

        final BlockState radDirt = ModBlocks.RADIOACTIVE_DIRT.defaultBlockState();
        configList.setDefault(RegistryLookup.keyOf(Blocks.DIRT), new BlockReplacementData().setChance(0.2f).setBlockState(radDirt), 0);
        configList.setDefault(RegistryLookup.keyOf(Blocks.FARMLAND), new BlockReplacementData().setChance(0.2f).setBlockState(radDirt), 0);
        configList.setDefault(RegistryLookup.keyOf(Blocks.DIRT_PATH), new BlockReplacementData().setChance(0.2f).setBlockState(radDirt), 0);
        configList.setDefault(RegistryLookup.keyOf(Blocks.GRASS_BLOCK), new BlockReplacementData().setChance(0.2f).setBlockState(radDirt), 0);
        configList.setDefault(RegistryLookup.keyOf(Blocks.MYCELIUM), new BlockReplacementData().setChance(0.2f).setBlockState(radDirt), 0);

        if (!BlastBeam.isVegetation(Blocks.OAK_LEAVES.defaultBlockState()))
        {
            //The vanilla block tags are only bound once the data pack of the server is loaded, running this
            //before that would silently drop every plant/leaf/vine row.
            ICBMClassic.logger().warn("RadioactiveHandler: the vanilla block tags are not loaded, the plant rows "
                    + "of the rot table are missing. Run setup() from a tag/server start hook "
                    + "(CommonLifecycleEvents.TAGS_LOADED or ServerLifecycleEvents.SERVER_STARTING).");
        }

        for (Identifier blockKey : RegistryLookup.blockKeys())
        {
            final Block block = RegistryLookup.getBlock(blockKey.toString());
            //1.12: material == Material.PLANTS || material == Material.LEAVES || material == Material.VINE
            if (block != null && BlastBeam.isVegetation(block.defaultBlockState()))
            {
                configList.setDefault(blockKey.toString(), new BlockReplacementData().setBlockState(Blocks.AIR.defaultBlockState()), 0);
            }
        }

        configList.load("icbmclassic/blast.json#nuclear.radioactive_replacements.list", ConfigBlast.nuclear.radiationReplacements.blockStates);
    });

    /**
     * 1.12 {@code RadioactiveHandler.loadFromConfig()}. The callback above reads
     * {@link ModBlocks#RADIOACTIVE_DIRT}/{@link ModBlocks#RADIOACTIVE_STONE} and evaluates block tags, so this
     * has to run after {@code ModBlocks#register()} and after the vanilla block tags are bound.
     */
    public static void loadFromConfig() {
        radioactiveBlockSwaps.reload();
    }

    /**
     * 1.12 {@code RadioactiveHandler.setup()}, called from the mod start up. 1.12 could run it during init
     * because its material checks were always available; 26.3 replaced them with block tags, which only exist
     * after the server data pack is loaded, so the port wires this to
     * {@code CommonLifecycleEvents.TAGS_LOADED} (or {@code ServerLifecycleEvents.SERVER_STARTING}, which
     * fires after the tags of the world data are applied).
     */
    public static void setup() {
        loadFromConfig();
    }
}
