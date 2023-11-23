package org.team100.lib.motion.drivetrain;

import org.team100.lib.motion.components.DriveServo;
import org.team100.lib.motion.components.TurningServo;

import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;

/** Feedforward and feedback control of a single module. */
public class SwerveModule100 {
    private final DriveServo m_driveServo;
    private final TurningServo m_turningServo;

    public SwerveModule100(DriveServo driveServo, TurningServo turningServo) {
        m_driveServo = driveServo;
        m_turningServo = turningServo;
    }

    public void setDesiredState(SwerveModuleState desiredState) {
        SwerveModuleState state = SwerveModuleState.optimize(desiredState, m_turningServo.getTurningRotation());
        m_driveServo.setVelocity(state.speedMetersPerSecond);
        m_turningServo.setAngle(state.angle);
    }

    /////////////////////////////////////////////////////////////

    /**
     * Package private for SwerveModuleCollection.states only.
     */
    SwerveModuleState getState() {
        return new SwerveModuleState(m_driveServo.getDriveSpeedMS(), m_turningServo.getTurningRotation());
    }

    /**
     * Package private for SwerveModuleCollection.positions only.
     */
    SwerveModulePosition getPosition() {
        return new SwerveModulePosition(m_driveServo.getDriveDistanceM(), m_turningServo.getTurningRotation());
    }

    /**
     * Package private for SwerveModuleCollection.stop only.
     */
    void stop() {
        m_driveServo.set(0);
        m_turningServo.set(0);
    }

    public void close() {
        m_turningServo.close();
    }
}
