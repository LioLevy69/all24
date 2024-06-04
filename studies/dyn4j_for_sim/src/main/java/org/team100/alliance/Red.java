package org.team100.alliance;

import org.team100.commands.SourceDefault;
import org.team100.control.SelectorPilot;
import org.team100.control.auto.Auton;
import org.team100.control.auto.Defender;
import org.team100.control.auto.Passer;
import org.team100.control.auto.Scorer;
import org.team100.robot.RobotAssembly;
import org.team100.robot.Source;
import org.team100.sim.Foe;
import org.team100.sim.SimWorld;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class Red implements Alliance {
    private final RobotAssembly scorer;
    private final RobotAssembly passer;
    private final RobotAssembly defender;
    private final Source source;

    public Red(SimWorld world) {
        // near 3
        scorer = new RobotAssembly(
                x -> SelectorPilot.autonSelector(
                        new Auton(x.getDrive(), x.getCamera(), x.getIndexer(),
                                new Pose2d(14, 7, new Rotation2d()),
                                11, 10, 9),
                        new Scorer(x.getDrive(), x.getCamera(), x.getIndexer(),
                                new Pose2d(13.5, 5.5, new Rotation2d()))),
                new Foe("red scorer", world, false),
                false);
        scorer.setState(14, 7, 0, 0);

        // far 3
        passer = new RobotAssembly(
                x -> SelectorPilot.autonSelector(
                        new Auton(x.getDrive(), x.getCamera(), x.getIndexer(),
                                new Pose2d(14, 5.5, new Rotation2d()),
                                8, 7, 6),
                        new Passer(x.getDrive(), x.getCamera(), x.getIndexer())),
                new Foe("red passer", world, false),
                false);
        passer.setState(14, 5.5, 0, 0);

        // complement 2
        defender = new RobotAssembly(
                x -> SelectorPilot.autonSelector(
                        new Auton(x.getDrive(), x.getCamera(), x.getIndexer(),
                                new Pose2d(14, 3, new Rotation2d()),
                                4, 5),
                        new Defender()),
                new Foe("red defender", world, false),
                false);
        defender.setState(14, 3, 0, 0);

        source = new Source(world, new Translation2d(1.0, 1.0));
        source.setDefaultCommand(new SourceDefault(source, world, false, false));
    }

    @Override
    public void reset() {
        scorer.reset();
        passer.reset();
        defender.reset();
    }

    @Override
    public void begin() {
        scorer.begin();
        passer.begin();
        defender.begin();
    }

    @Override
    public void periodic() {
        scorer.periodic();
        passer.periodic();
        defender.periodic();
    }
}
