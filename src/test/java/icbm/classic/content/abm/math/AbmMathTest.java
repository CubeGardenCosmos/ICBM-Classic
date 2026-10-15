package icbm.classic.content.abm.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbmMathTest
{
    @Test
    void predictsImpactForVerticalAndBallisticFlights()
    {
        Prediction straightDrop = TrajectoryPredictor.predictImpact(
            new Vec3d(0, 10, 0), new Vec3d(0, -2, 0), new Vec3d(0, 0, 0), 0, 10);
        assertNotNull(straightDrop);
        assertEquals(0, straightDrop.impact().y(), 0.5);
        assertEquals(5, straightDrop.ticks());

        Prediction ballistic = TrajectoryPredictor.predictImpact(
            new Vec3d(0, 10, 0), new Vec3d(0.5, -1, 0), new Vec3d(0, -0.1, 0), 0, 20);
        assertNotNull(ballistic);
        double analyticTime = (-1 + Math.sqrt(1 + 2)) / 0.1;
        assertEquals(0, ballistic.impact().y(), 0.5);
        assertEquals(analyticTime * 0.5, ballistic.impact().x(), 0.5);

        assertNull(TrajectoryPredictor.predictImpact(
            new Vec3d(0, 1, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 0), 0, 10));

        Prediction alreadyBelow = TrajectoryPredictor.predictImpact(
            new Vec3d(2, -1, 3), new Vec3d(0, -2, 0), new Vec3d(0, 0, 0), 0, 10);
        assertNotNull(alreadyBelow);
        assertEquals(0, alreadyBelow.ticks());
        assertEquals(new Vec3d(2, -1, 3), alreadyBelow.impact());
    }

    @Test
    void solvesInterceptsAndHandlesDegenerateCases()
    {
        Intercept stationary = InterceptMath.solve(new Vec3d(0, 0, 0), 5,
            new Vec3d(10, 0, 0), new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), 10);
        assertNotNull(stationary);
        assertEquals(new Vec3d(10, 0, 0), stationary.aim());
        assertEquals(2, stationary.t(), 1e-6);

        Intercept lateral = InterceptMath.solve(new Vec3d(0, 0, 0), 5,
            new Vec3d(10, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 0), 10);
        assertNotNull(lateral);
        assertEquals(5 * lateral.t(), lateral.aim().sub(new Vec3d(0, 0, 0)).length(), 1e-6);

        assertNull(InterceptMath.solve(new Vec3d(0, 0, 0), 5,
            new Vec3d(10, 0, 0), new Vec3d(6, 0, 0), new Vec3d(0, 0, 0), 100));

        Intercept degenerate = InterceptMath.solve(new Vec3d(0, 0, 0), 5,
            new Vec3d(10, 0, 0), new Vec3d(-5, 0, 0), new Vec3d(0, 0, 0), 10);
        assertNotNull(degenerate);
        assertTrue(Double.isFinite(degenerate.t()));
        assertEquals(1, degenerate.t(), 1e-6);

        assertNull(InterceptMath.solve(new Vec3d(0, 0, 0), 1,
            new Vec3d(10, 0, 0), new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), 5));
        assertEquals(2, InterceptMath.smallestPositive(2, 5), 1e-6);
        assertEquals(3, InterceptMath.smallestPositive(-1, 3), 1e-6);
        assertEquals(-1, InterceptMath.smallestPositive(-1, 0), 1e-6);
    }

    @Test
    void findsClosestApproachWithinTickSegments()
    {
        assertEquals(0, InterceptMath.closestApproach(
            new Vec3d(-1, 0, 0), new Vec3d(1, 0, 0),
            new Vec3d(1, 0, 0), new Vec3d(-1, 0, 0)), 1e-6);
        assertEquals(3, InterceptMath.closestApproach(
            new Vec3d(0, 0, 0), new Vec3d(1, 0, 0),
            new Vec3d(0, 3, 0), new Vec3d(1, 3, 0)), 1e-6);
        assertEquals(2, InterceptMath.closestApproach(
            new Vec3d(0, 0, 0), new Vec3d(0, 0, 0),
            new Vec3d(1, 2, 0), new Vec3d(-1, 2, 0)), 1e-6);
        assertEquals(5, InterceptMath.closestApproach(
            new Vec3d(0, 0, 0), new Vec3d(1, 0, 0),
            new Vec3d(0, 5, 0), new Vec3d(0, 6, 0)), 1e-6);
    }

    @Test
    void rotatesTowardDesiredDirectionWithinAngularLimit()
    {
        Vec3d current = new Vec3d(1, 0, 0);
        Vec3d closeDesired = new Vec3d(1, 1, 0).normalize();
        assertEquals(closeDesired, Rotation.rotateTowards(current, closeDesired, Math.PI / 2));

        Vec3d desired = new Vec3d(0, 1, 0);
        Vec3d rotated = Rotation.rotateTowards(current, desired, Math.PI / 6);
        assertEquals(Math.PI / 6, Math.acos(rotated.normalize().dot(current)), 1e-6);

        Vec3d opposite = Rotation.rotateTowards(current, new Vec3d(-1, 0, 0), Math.PI / 4);
        assertTrue(Double.isFinite(opposite.x()) && Double.isFinite(opposite.y()) && Double.isFinite(opposite.z()));
        assertEquals(1, opposite.length(), 1e-6);
        assertEquals(Math.PI / 4, Math.acos(opposite.dot(current)), 1e-6);
    }
}
