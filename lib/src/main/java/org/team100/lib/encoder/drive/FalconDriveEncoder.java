package org.team100.lib.encoder.drive;

import org.team100.lib.encoder.Encoder100;
import org.team100.lib.motor.drive.FalconDriveMotor;
import org.team100.lib.telemetry.Telemetry;
import org.team100.lib.telemetry.Telemetry.Level;
import org.team100.lib.units.Distance;

/**
 * The built-in encoder in Falcon motors.
 * 
 * The encoder is a high-resolution magnetic sensor, 2048 ticks per turn.
 */
public class FalconDriveEncoder implements Encoder100<Distance> {
    private final Telemetry t = Telemetry.get();
    private final String m_name;
    private final FalconDriveMotor m_motor;
    private final double m_distancePerPulse;

    /**
     * @param name            do not use a leading slash.
     * @param distancePerTurn in meters
     */
    public FalconDriveEncoder(
            String name,
            FalconDriveMotor motor,
            double distancePerTurn) {
        if (name.startsWith("/"))
            throw new IllegalArgumentException();
        m_name = String.format("/%s/Falcon Drive Encoder", name);
        m_motor = motor;
        m_distancePerPulse = distancePerTurn / 2048;
    }

    @Override
    public double getPosition() {
        double result = m_motor.getPosition() * m_distancePerPulse;
        t.log(Level.DEBUG, m_name + "/Position m", result);
        return result;
    }

    @Override
    public double getRate() {
        // sensor velocity is 1/2048ths of a turn per 100ms
        double result = m_motor.getVelocity2048_100() * 10 * m_distancePerPulse;
        t.log(Level.DEBUG, m_name + "/Speed m_s", result);
        return result;
    }

    @Override
    public void reset() {
        m_motor.resetPosition();
    }

    @Override
    public void close() {
        //
    }

    @Override
    public void periodic() {
        //
    }
}
