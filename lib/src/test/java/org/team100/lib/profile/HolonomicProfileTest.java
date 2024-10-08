package org.team100.lib.profile;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.motion.drivetrain.SwerveState;
import org.team100.lib.motion.drivetrain.kinodynamics.FieldRelativeVelocity;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.Timer;

class HolonomicProfileTest {
    @Test
    void test2d() {
        HolonomicProfile hp = new HolonomicProfile(0.02, 1, 1, 0.01, 1, 1, 0.01);
        SwerveState i = new SwerveState();
        SwerveState g = new SwerveState(new Pose2d(1, 5, GeometryUtil.kRotationZero));
        hp.solve(i, g);
        SwerveState s = i;
        for (double t = 0; t < 10; t += 0.02) {
            s = hp.calculate(s, g);
            System.out.printf("%.2f %.3f %.3f\n", t, s.x().x(), s.y().x());
        }
    }

    @Test
    void test2dWithEntrySpeed() {
        HolonomicProfile hp = new HolonomicProfile(0.02, 1, 1, 0.01, 1, 1, 0.01);
        SwerveState i = new SwerveState(new Pose2d(), new FieldRelativeVelocity(1, 0, 0));
        SwerveState g = new SwerveState(new Pose2d(0, 1, GeometryUtil.kRotationZero));
        hp.solve(i, g);
        SwerveState s = i;
        for (double t = 0; t < 10; t += 0.02) {
            s = hp.calculate(s, g);
            System.out.printf("%.2f %.3f %.3f\n", t, s.x().x(), s.y().x());
        }
    }

    /**
     * On my desktop, the solve() method takes about 1 microsecond, so it seems
     * ok to not worry about how long it takes.
     */
    @Test
    void testSolvePerformance() {
        HolonomicProfile hp = new HolonomicProfile(0.02, 1, 1, 0.01, 1, 1, 0.01);
        SwerveState i = new SwerveState(new Pose2d(), new FieldRelativeVelocity(1, 0, 0));
        SwerveState g = new SwerveState(new Pose2d(0, 1, GeometryUtil.kRotationZero));
        int N = 1000000;
        double t0 = Timer.getFPGATimestamp();
        for (int ii = 0; ii < N; ++ii) {
            hp.solve(i, g);
        }
        double t1 = Timer.getFPGATimestamp();
        System.out.printf("duration (ms)  %5.1f\n", 1e3 * (t1 - t0));
        System.out.printf("per op (ns)    %5.1f\n", 1e9 * (t1 - t0) / N);
    }
}