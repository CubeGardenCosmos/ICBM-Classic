package icbm.classic.lib.transform.region;

import com.builtbroken.jlib.data.vector.IPos3D;
import icbm.classic.lib.transform.vector.Point;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S1.01：区域形状（Cube/Rectangle/Triangle）的包含与边界判定，纯数学不依赖世界。
 */
class CubeRegionTest {

    private static final double DELTA = 1e-9;

    @Test
    void cubeIsValidAndHasExpectedSize() {
        final Cube cube = new Cube(0, 0, 0, 10, 10, 10);
        assertTrue(cube.isValid());
        assertEquals(11.0, cube.getSizeX(), DELTA);
        assertEquals(11.0, cube.getSizeY(), DELTA);
        assertEquals(11.0, cube.getSizeZ(), DELTA);
        assertEquals(1331.0, cube.getVolume(), DELTA);
        assertEquals(11.0, cube.radius(), DELTA);
    }

    @Test
    void cubeEmptyAndFullStatics() {
        assertFalse(Cube.EMPTY.isValid());
        assertTrue(Cube.FULL.isValid());
    }

    @Test
    void cubeContainsOnlyInteriorPoints() {
        final Cube cube = new Cube(0, 0, 0, 10, 10, 10);

        assertTrue(cube.isWithin(new Pos(5, 5, 5)));
        assertTrue(cube.isWithin(0.5, 0.5, 0.5));

        // 边界被 1E-5 的余量排除（与 1.12 一致）
        assertFalse(cube.isWithin(new Pos(5, 5, 10)));
        assertFalse(cube.isWithin(new Pos(0, 5, 5)));

        assertTrue(cube.isWithinX(5.0));
        assertFalse(cube.isWithinX(-0.5));
        assertTrue(cube.isWithinXZ(new Vec3(5, 0, 5)));
        assertFalse(cube.isWithinXZ(new Vec3(5, 0, 50)));
    }

    @Test
    void cubeIntersectsAndBoundsChecks() {
        final Cube cube = new Cube(0, 0, 0, 10, 10, 10);

        assertTrue(cube.intersects(new Vec3(5, 5, 5)));
        assertTrue(cube.intersects(new Pos(5, 5, 5)));
        assertFalse(cube.intersects(new Vec3(5, 10, 5)));
        assertFalse(cube.intersects(new Vec3(50, 5, 5)));

        assertTrue(cube.doesOverlap(new AABB(2, 2, 2, 3, 3, 3)));
        assertTrue(cube.isInsideBounds(new AABB(2, 2, 2, 3, 3, 3)));
        assertFalse(cube.isInsideBounds(new AABB(2, 2, 2, 30, 3, 3)));
    }

    @Test
    void cubeConversionAndDistance() {
        final Cube cube = new Cube(0, 0, 0, 10, 10, 10);

        final AABB box = cube.toAABB();
        assertEquals(0.0, box.minX, DELTA);
        assertEquals(10.0, box.maxX, DELTA);
        assertEquals(10.0, box.maxY, DELTA);
        assertEquals(10.0, box.maxZ, DELTA);

        // 中心为 (5.5, 5.5, 5.5)，距离自身中心为 0
        assertEquals(0.0, cube.distance(5.5, 5.5, 5.5), DELTA);
        assertTrue(cube.distance(new Pos(50, 5.5, 5.5)) > 40);

        final IPos3D[] corners = Cube.getCorners(cube);
        assertEquals(8, corners.length);
        assertEquals(10.0, corners[6].x(), DELTA);
        assertEquals(10.0, corners[6].y(), DELTA);
        assertEquals(10.0, corners[6].z(), DELTA);
    }

    @Test
    void rectangleContainsPoints() {
        final Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        assertTrue(rectangle.isWithin(5, 5));
        assertTrue(rectangle.isWithin(new Point(10, 10)));
        assertFalse(rectangle.isWithin(11, 5));
        assertFalse(rectangle.isWithin(-1, 5));
        assertEquals(10.0, rectangle.getSizeX(), DELTA);
        assertEquals(100.0, rectangle.getArea(), DELTA);
    }

    @Test
    void triangleContainsPoints() {
        final Triangle triangle = new Triangle(new Point(0, 0), new Point(10, 0), new Point(0, 10));
        assertEquals(50.0, triangle.getArea(), DELTA);
        assertTrue(triangle.isWithin(new Point(1, 1)));
        assertFalse(triangle.isWithin(new Point(9, 9)));
        assertEquals(10.0, triangle.getSizeX(), DELTA);
        assertEquals(10.0, triangle.getSizeY(), DELTA);
    }
}
