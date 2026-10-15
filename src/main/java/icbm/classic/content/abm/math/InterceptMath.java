package icbm.classic.content.abm.math;

import org.jspecify.annotations.Nullable;

public final class InterceptMath
{
    private InterceptMath()
    {
    }

    public static double smallestPositive(double a, double b)
    {
        if (a > 0 && b > 0)
        {
            return Math.min(a, b);
        }
        if (a > 0)
        {
            return a;
        }
        return b > 0 ? b : -1;
    }

    public static @Nullable Intercept solve(Vec3d S, double s, Vec3d P, Vec3d V, Vec3d A, int maxTicks)
    {
        Vec3d D = P.sub(S);
        double a = V.dot(V) - s * s;
        double b = 2 * D.dot(V);
        double c = D.dot(D);
        double t;
        if (Math.abs(a) < 1e-9)
        {
            if (Math.abs(b) < 1e-9)
            {
                return null;
            }
            t = -c / b;
        }
        else
        {
            double disc = b * b - 4 * a * c;
            if (disc < 0)
            {
                return null;
            }
            double sq = Math.sqrt(disc);
            double t1 = (-b - sq) / (2 * a);
            double t2 = (-b + sq) / (2 * a);
            t = smallestPositive(t1, t2);
        }
        if (t <= 0 || t > maxTicks)
        {
            return null;
        }
        for (int i = 0; i < 3; i++)
        {
            Vec3d aim = P.add(V.scale(t)).add(A.scale(0.5 * t * t));
            t = aim.sub(S).length() / s;
            if (t > maxTicks)
            {
                return null;
            }
        }
        Vec3d aim = P.add(V.scale(t)).add(A.scale(0.5 * t * t));
        return new Intercept(aim, t);
    }

    public static double closestApproach(Vec3d aPrev, Vec3d aNow, Vec3d tPrev, Vec3d tNow)
    {
        Vec3d d0 = tPrev.sub(aPrev);
        Vec3d dv = tNow.sub(tPrev).sub(aNow.sub(aPrev));
        double dd = dv.dot(dv);
        double tau = dd < 1e-12 ? 0 : clamp(-d0.dot(dv) / dd, 0, 1);
        return d0.add(dv.scale(tau)).length();
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
