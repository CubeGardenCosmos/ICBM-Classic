package icbm.classic.lib.transform;

import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * S1.01：{@link PosDistanceSorter} 的排序顺序（纯数学，不依赖世界）。
 */
class PosDistanceSorterTest {

    @Test
    void sortsBySquaredDistance() {
        final List<BlockPos> points = new ArrayList<>(List.of(
            new BlockPos(5, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(3, 0, 0),
            new BlockPos(2, 0, 0)));

        points.sort(new PosDistanceSorter(new Pos(0, 0, 0), false, PosDistanceSorter.Sort.SQ));

        assertEquals(List.of(
            new BlockPos(1, 0, 0),
            new BlockPos(2, 0, 0),
            new BlockPos(3, 0, 0),
            new BlockPos(5, 0, 0)), points);
    }

    @Test
    void sortsByManhattanDistance() {
        final List<BlockPos> points = new ArrayList<>(List.of(
            new BlockPos(3, 0, 0),
            new BlockPos(2, 0, 2),
            new BlockPos(1, 0, 0)));

        points.sort(new PosDistanceSorter(new Pos(0, 0, 0), false, PosDistanceSorter.Sort.MANHATTEN));

        assertEquals(List.of(
            new BlockPos(1, 0, 0),
            new BlockPos(3, 0, 0),
            new BlockPos(2, 0, 2)), points);
    }

    @Test
    void sortsByYWhenEnabled() {
        final List<BlockPos> points = new ArrayList<>(List.of(
            new BlockPos(0, 5, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 3, 0)));

        points.sort(new PosDistanceSorter(new Pos(0, 0, 0), true, PosDistanceSorter.Sort.SQRT));

        assertEquals(List.of(
            new BlockPos(0, 1, 0),
            new BlockPos(0, 3, 0),
            new BlockPos(0, 5, 0)), points);
    }
}
