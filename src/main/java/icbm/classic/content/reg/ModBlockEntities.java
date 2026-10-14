package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.blocks.explosive.ExplosiveBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity type registration (1.12 {@code GameRegistry.registerTileEntity} calls in {@code BlockReg}).
 * <p>
 * IDs come from registry-ids.md §2; Filled by S3.09 (explosive), S5.01–S5.06 (machines).
 */
public final class ModBlockEntities
{
    private ModBlockEntities() {}

    /** {@code icbmclassic:explosive}, 1.12 {@code TileEntityExplosive}, shared by the 23 flat explosive blocks. */
    public static BlockEntityType<ExplosiveBlockEntity> EXPLOSIVE;

    /** Registers every block entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // 1.12 registered one tile entity class for the metadata block "explosives"; with the flat blocks
        // (ADR 0002) the single type is bound to all 23 blocks, see registry-ids.md §2
        EXPLOSIVE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ICBMConstants.id("explosive"),
                FabricBlockEntityTypeBuilder.create(ExplosiveBlockEntity::new,
                        ModBlocks.EXPLOSIVE_BLOCKS.values().toArray(Block[]::new)).build());

        // TODO(S5.02..S5.06) launcher_base, launcher_frame, launcher_connector, launcher_screen,
        //  cruise_launcher, radar_station, emp_tower + emp_tower_fake
    }
}
