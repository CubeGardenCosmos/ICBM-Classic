package icbm.classic.lib.transform.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S1.01：Pos/Point 的纯数学行为（加减乘、距离、归一化）。
 * 不依赖世界与注册表。
 */
class PosVectorMathTest {

    @Test
    void posAddsSubtractsAndMultipliesComponentWise() {
        final Pos a = new Pos(1, 2, 3);
        final Pos b = new Pos(4, 5, 6);

        final Pos sum = a.add(b);
        assertEquals(5.0, sum.x(), 1e-9);
        assertEquals(7.0, sum.y(), 1e-9);
        assertEquals(9.0, sum.z(), 1e-9);

        final Pos difference = b.sub(a);
        assertEquals(3.0, difference.x(), 1e-9);
        assertEquals(3.0, difference.y(), 1e-9);
        assertEquals(3.0, difference.z(), 1e-9);

        final Pos product = a.multiply(b);
        assertEquals(4.0, product.x(), 1e-9);
        assertEquals(10.0, product.y(), 1e-9);
        assertEquals(18.0, product.z(), 1e-9);

        final Pos scaled = a.multiply(2);
        assertEquals(2.0, scaled.x(), 1e-9);
        assertEquals(4.0, scaled.y(), 1e-9);
        assertEquals(6.0, scaled.z(), 1e-9);
    }

    @Test
    void posDistanceMatchesPythagoras() {
        final Pos origin = new Pos(0, 0, 0);
        assertEquals(5.0, origin.distance(new Pos(3, 4, 0)), 1e-9);
        assertEquals(5.0, origin.distance(3, 4, 0), 1e-9);
        assertEquals(0.0, origin.distance(origin), 1e-9);
        assertEquals(Math.sqrt(3.0), new Pos(1, 1, 1).magnitude(), 1e-9);
    }

    @Test
    void posFloorAndMagnitude() {
        final Pos pos = new Pos(1.75, -2.25, 3.5);
        assertEquals(1.0, pos.floor().x(), 1e-9);
        assertEquals(-3.0, pos.floor().y(), 1e-9);
        assertEquals(3.0, pos.floor().z(), 1e-9);
        assertEquals(Math.sqrt(1 + 1), new Pos(1, 1, 0).magnitude(), 1e-9);
    }

    @Test
    void pointMathAndNormalize() {
        final Point point = new Point(1, 2);
        final Point sum = point.add(2, 3);
        assertEquals(3.0, sum.x(), 1e-9);
        assertEquals(5.0, sum.y(), 1e-9);

        assertEquals(Math.sqrt(13.0), new Point(2, 3).magnitude(), 1e-9);
        assertEquals(1.0, new Point(1, 0).distance(new Point(2, 0)), 1e-9);
        assertEquals(5.0, new Point(0, 0).distance(new Point(3, 4)), 1e-9);

        final Point normalized = new Point(3, 4).normalize();
        assertEquals(0.6, normalized.x(), 1e-9);
        assertEquals(0.8, normalized.y(), 1e-9);

        assertTrue(new Point(0, 0).isZero());
        assertFalse(new Point(0, 1).isZero());
    }
}
