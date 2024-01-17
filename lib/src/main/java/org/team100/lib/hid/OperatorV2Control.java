package org.team100.lib.hid;

import static org.team100.lib.hid.ControlUtil.clamp;
import static org.team100.lib.hid.ControlUtil.deadband;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class OperatorV2Control implements OperatorControl {
    private static final double kDeadband = 0.2;
    private final CommandXboxController m_controller;

    public OperatorV2Control() {
        m_controller = new CommandXboxController(1);
    }

    @Override
    public String getHIDName() {
        return m_controller.getHID().getName();
    }

    @Override
    public Trigger intake() {
        return m_controller.x();
    }

    @Override
    public Trigger index() {
        return m_controller.b();
    }

    @Override
    public Trigger shooter() {
        return m_controller.a();
    }

    @Override
    public double climberState() {
        return deadband(clamp(-1.0 * m_controller.getRightY(), 1), kDeadband, 1);
    }
}