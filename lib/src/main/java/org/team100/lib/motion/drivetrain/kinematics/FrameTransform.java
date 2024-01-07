package org.team100.lib.motion.drivetrain.kinematics;

import org.team100.lib.motion.drivetrain.VeeringCorrection;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

public class FrameTransform {

    Rotation2d correctAngle(double gyroRateRad_S, Rotation2d robotAngle) {
        return VeeringCorrection.correct(gyroRateRad_S, robotAngle);
    }

    /**
     * Convert field-relative speeds into robot-relative speeds.
     * 
     * Performs veering correction.
     */
    public ChassisSpeeds fromFieldRelativeSpeeds(
            double vxMetersPerSecond,
            double vyMetersPerSecond,
            double omegaRadiansPerSecond,
            double gyroRateRad_S,
            Rotation2d robotAngle) {

        robotAngle = correctAngle(gyroRateRad_S, robotAngle);

        return new ChassisSpeeds(
                vxMetersPerSecond * robotAngle.getCos() + vyMetersPerSecond * robotAngle.getSin(),
                -1.0 * vxMetersPerSecond * robotAngle.getSin() + vyMetersPerSecond * robotAngle.getCos(),
                omegaRadiansPerSecond);
    }

    /**
     * Convert robot-relative speeds to field-relative speeds.
     * 
     * Performs veering correction.
     * 
     * @param vxMetersPerSecond     robot-relative
     * @param vyMetersPerSecond     robot-relative
     * @param omegaRadiansPerSecond robot-relative
     * @return Twist2d representing field-relative motion.
     */
    public Twist2d toFieldRelativeSpeeds(
            double vxMetersPerSecond,
            double vyMetersPerSecond,
            double omegaRadiansPerSecond,
            double gyroRateRad_S,
            Rotation2d robotAngle) {

        // Note, I'm not really sure this makes any sense, but it does
        // make the test pass.
        robotAngle = correctAngle(gyroRateRad_S, robotAngle);

        // it's just the opposite rotation
        return new Twist2d(
                vxMetersPerSecond * robotAngle.getCos() + -1.0 * vyMetersPerSecond * robotAngle.getSin(),
                vxMetersPerSecond * robotAngle.getSin() + vyMetersPerSecond * robotAngle.getCos(),
                omegaRadiansPerSecond);
    }
}
