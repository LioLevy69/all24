package org.team100.lib.motion.drivetrain.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.team100.lib.motion.drivetrain.SpeedLimits;

import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;

public class ManualModuleStatesTest {
    private static final double kDelta = 0.001;

    @Test
    void testModuleStatesZero() {
        Supplier<Twist2d> input = () -> new Twist2d();
        SpeedLimits limits = new SpeedLimits(1, 1, 1, 1);
        ManualModuleStates manual = new ManualModuleStates(input, limits);
        SwerveModuleState[] ms = manual.get();
        assertEquals(0, ms[0].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[1].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[2].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[3].speedMetersPerSecond, kDelta);

        assertEquals(0, ms[0].angle.getRadians(), kDelta);
        assertEquals(0, ms[1].angle.getRadians(), kDelta);
        assertEquals(0, ms[2].angle.getRadians(), kDelta);
        assertEquals(0, ms[3].angle.getRadians(), kDelta);
    }

    @Test
    void testModuleStatesInDeadband() {
        Supplier<Twist2d> input = () -> new Twist2d(0.1, 0.1, 0);
        SpeedLimits limits = new SpeedLimits(1, 1, 1, 1);
        ManualModuleStates manual = new ManualModuleStates(input, limits);
        SwerveModuleState[] ms = manual.get();
        assertEquals(0, ms[0].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[1].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[2].speedMetersPerSecond, kDelta);
        assertEquals(0, ms[3].speedMetersPerSecond, kDelta);

        assertEquals(0, ms[0].angle.getRadians(), kDelta);
        assertEquals(0, ms[1].angle.getRadians(), kDelta);
        assertEquals(0, ms[2].angle.getRadians(), kDelta);
        assertEquals(0, ms[3].angle.getRadians(), kDelta);
    }

    @Test
    void testModuleStatesOutsideDeadband() {
        Supplier<Twist2d> input = () -> new Twist2d(0.5, 0.5, 0);
        SpeedLimits limits = new SpeedLimits(1, 1, 1, 1);
        ManualModuleStates manual = new ManualModuleStates(input, limits);
        SwerveModuleState[] ms = manual.get();
        assertEquals(0.634, ms[0].speedMetersPerSecond, kDelta);
        assertEquals(0.634, ms[1].speedMetersPerSecond, kDelta);
        assertEquals(0.634, ms[2].speedMetersPerSecond, kDelta);
        assertEquals(0.634, ms[3].speedMetersPerSecond, kDelta);

        assertEquals(Math.PI / 4, ms[0].angle.getRadians(), kDelta);
        assertEquals(Math.PI / 4, ms[1].angle.getRadians(), kDelta);
        assertEquals(Math.PI / 4, ms[2].angle.getRadians(), kDelta);
        assertEquals(Math.PI / 4, ms[3].angle.getRadians(), kDelta);
    }
}
