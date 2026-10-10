package icbm.classic.content.blocks.explosive;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Explosive block. 1.12 had one block with an explosive metadata value and a block entity holding the
 * explosive capability; ADR 0002 flattens it into one block per explosive (23), all sharing the
 * {@code ExplosiveBlockEntity} added in S3.09.
 * <p>
 * S2.05 only needs the flat block so the creative tab and the ids exist; the fuse, interaction,
 * placement and block entity logic is added by S3.09, which keeps the
 * {@code (Identifier explosiveId, Properties)} constructor.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class ExplosiveBlock extends Block
{
    private final Identifier explosiveId;

    public ExplosiveBlock(Identifier explosiveId, BlockBehaviour.Properties properties)
    {
        super(properties);
        this.explosiveId = explosiveId;
    }

    /** Explosive this block represents, see {@code docs/architecture/registry-ids.md} §1. */
    public Identifier getExplosiveId()
    {
        return explosiveId;
    }
}
