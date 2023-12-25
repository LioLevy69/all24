package org.team100.lib.spline;

import java.util.List;
import java.util.Optional;

import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.geometry.Pose2dWithMotion;
import org.team100.lib.util.Math100;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;

/**
 * Holonomic spline.
 * 
 * If you don't care about rotation, just pass zero.
 * 
 * Note that some nonholonomic spline consumers assume that dx carries all the
 * motion; that's not true here.
 */
public class QuinticHermitePoseSplineHolonomic {
    private static final double kEpsilon = 1e-5;
    private static final double kStepSize = 1.0;
    private static final double kMinDelta = 0.001;
    private static final int kSamples = 100;
    private static final int kMaxIterations = 100;

    private final QuinticHermiteSpline1d x;
    private final QuinticHermiteSpline1d y;
    private final QuinticHermiteSpline1d theta;
    private final Rotation2d r0;

    /**
     * @param p0 The starting pose of the spline
     * @param p1 The ending pose of the spline
     */
    public QuinticHermitePoseSplineHolonomic(Pose2d p0, Pose2d p1, Rotation2d r0, Rotation2d r1) {

        // the 1.2 here is a magic number that makes the spline look nice.
        double scale = 1.2 * GeometryUtil.distance(p0.getTranslation(), p1.getTranslation());
        double x0 = p0.getTranslation().getX();
        double x1 = p1.getTranslation().getX();
        double dx0 = p0.getRotation().getCos() * scale;
        double dx1 = p1.getRotation().getCos() * scale;
        double ddx0 = 0;
        double ddx1 = 0;
        double y0 = p0.getTranslation().getY();
        double y1 = p1.getTranslation().getY();
        double dy0 = p0.getRotation().getSin() * scale;
        double dy1 = p1.getRotation().getSin() * scale;
        double ddy0 = 0;
        double ddy1 = 0;

        this.x = new QuinticHermiteSpline1d(x0, x1, dx0, dx1, ddx0, ddx1);
        this.y = new QuinticHermiteSpline1d(y0, y1, dy0, dy1, ddy0, ddy1);
        this.r0 = r0;
        double delta = r0.unaryMinus().rotateBy(r1).getRadians();
        theta = new QuinticHermiteSpline1d(0.0, delta, 0, 0, 0, 0);
    }

    private QuinticHermitePoseSplineHolonomic(
            QuinticHermiteSpline1d x,
            QuinticHermiteSpline1d y,
            QuinticHermiteSpline1d theta,
            Rotation2d r0) {
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.r0 = r0;
    }

    public Pose2dWithMotion getPose2dWithMotion(double p) {
        Optional<Rotation2d> course = getCourse(p);
        double dx = course.isPresent() ? course.get().getCos() : 0.0;
        double dy = course.isPresent() ? course.get().getSin() : 0.0;
        double dtheta = course.isPresent() ? getDHeadingDs(p) : getDHeading(p);

        return new Pose2dWithMotion(
                getPose2d(p),
                new Twist2d(dx, dy, dtheta),
                getCurvature(p),
                getDCurvatureDs(p));
    }

    /**
     * Course is the direction of motion, regardless of the direction the robot is
     * facing (heading). It's optional to account for the motionless case.
     *
     * Course is the same for holonomic and nonholonomic splines.
     */
    public Optional<Rotation2d> getCourse(double t) {
        if (Math100.epsilonEquals(dx(t), 0.0) && Math100.epsilonEquals(dy(t), 0.0)) {
            return Optional.empty();
        }
        return Optional.of(new Rotation2d(dx(t), dy(t)));
    }

    /**
     * Finds the optimal second derivative values for a set of splines to reduce the
     * sum of the change in curvature
     * squared over the path
     *
     * @param splines the list of splines to optimize
     * @return the final sumDCurvature2
     */
    public static double optimizeSpline(List<QuinticHermitePoseSplineHolonomic> splines) {
        int count = 0;
        double prev = sumDCurvature2(splines);
        while (count < kMaxIterations) {
            runOptimizationIteration(splines);
            double current = sumDCurvature2(splines);
            if (prev - current < kMinDelta)
                return current;
            prev = current;
            count++;
        }
        return prev;
    }

    Pose2d getPose2d(double p) {
        return new Pose2d(getPoint(p), getHeading(p));
    }

    ////////////////////////////////////////////////////////////////////////

    protected Rotation2d getHeading(double t) {
        return r0.rotateBy(Rotation2d.fromRadians(theta.getPosition(t)));
    }

    protected double getDHeading(double t) {
        return theta.getVelocity(t);
    }

    /**
     * Return a new spline that is a copy of this one, but with adjustments to
     * second derivatives.
     */
    private QuinticHermitePoseSplineHolonomic adjustSecondDerivatives(double ddx0_adjustment, double ddx1_adjustment,
            double ddy0_adjustment, double ddy1_adjustment) {
        return new QuinticHermitePoseSplineHolonomic(
                x.addCoefs(new QuinticHermiteSpline1d(0, 0, 0, 0, ddx0_adjustment, ddx1_adjustment)),
                y.addCoefs(new QuinticHermiteSpline1d(0, 0, 0, 0, ddy0_adjustment, ddy1_adjustment)),
                theta,
                r0);
    }

    /**
     * Change in heading per distance traveled, i.e. spatial change in heading.
     * dtheta/ds (radians/meter).
     */
    private double getDHeadingDs(double p) {
        return getDHeading(p) / getVelocity(p);
    }

    /**
     * DCurvatureDs is the change in curvature per distance traveled, i.e. the
     * "spatial change in curvature"
     * 
     * dk/dp / ds/dp = dk/ds
     * rad/mp / m/p = rad/m^2
     */
    private double getDCurvatureDs(double p) {
        return getDCurvature(p) / getVelocity(p);
    }

    /** Returns pose in the nonholonomic sense, where the rotation is the course */
    private Pose2d getStartPose() {
        return new Pose2d(
                getPoint(0),
                new Rotation2d(dx(0), dy(0)));
    }

    /** Returns pose in the nonholonomic sense, where the rotation is the course */
    private Pose2d getEndPose() {
        return new Pose2d(
                getPoint(1),
                new Rotation2d(dx(1), dy(1)));
    }

    /**
     * Cartesian coordinate in meters at p.
     * 
     * @param t ranges from 0 to 1
     * @return the point on the spline for that t value
     */
    protected Translation2d getPoint(double t) {
        return new Translation2d(x.getPosition(t), y.getPosition(t));
    }

    private double dx(double t) {
        return x.getVelocity(t);
    }

    private double dy(double t) {
        return y.getVelocity(t);
    }

    private double ddx(double t) {
        return x.getAcceleration(t);
    }

    private double ddy(double t) {
        return y.getAcceleration(t);
    }

    private double dddx(double t) {
        return x.getJerk(t);
    }

    private double dddy(double t) {
        return y.getJerk(t);
    }

    /**
     * Velocity is the change in position per parameter, p: ds/dp (meters per p).
     * Since p is not time, it is not "velocity" in the usual sense.
     */
    protected double getVelocity(double t) {
        return Math.hypot(dx(t), dy(t));
    }

    /**
     * Curvature is the change in motion direction per distance traveled.
     * rad/m.
     * Note the denominator is distance in this case, not the parameter, p.
     */
    protected double getCurvature(double t) {
        return (dx(t) * ddy(t) - ddx(t) * dy(t))
                / ((dx(t) * dx(t) + dy(t) * dy(t)) * Math.sqrt((dx(t) * dx(t) + dy(t) * dy(t))));
    }

    /**
     * DCurvature is the change in curvature per change in p.
     * dk/dp (rad/m per p)
     * If you want change in curvature per meter, use getDCurvatureDs.
     */
    protected double getDCurvature(double t) {
        double dx2dy2 = (dx(t) * dx(t) + dy(t) * dy(t));
        double num = (dx(t) * dddy(t) - dddx(t) * dy(t)) * dx2dy2
                - 3 * (dx(t) * ddy(t) - ddx(t) * dy(t)) * (dx(t) * ddx(t) + dy(t) * ddy(t));
        return num / (dx2dy2 * dx2dy2 * Math.sqrt(dx2dy2));
    }

    private double dCurvature2(double t) {
        double dx2dy2 = (dx(t) * dx(t) + dy(t) * dy(t));
        double num = (dx(t) * dddy(t) - dddx(t) * dy(t)) * dx2dy2
                - 3 * (dx(t) * ddy(t) - ddx(t) * dy(t)) * (dx(t) * ddx(t) + dy(t) * ddy(t));
        return num * num / (dx2dy2 * dx2dy2 * dx2dy2 * dx2dy2 * dx2dy2);
    }

    /**
     * @return integral of dCurvature^2 over the length of the spline
     */
    private double sumDCurvature2() {
        double dt = 1.0 / kSamples;
        double sum = 0;
        for (double t = 0; t < 1.0; t += dt) {
            sum += (dt * dCurvature2(t));
        }
        return sum;
    }

    /**
     * @return integral of dCurvature^2 over the length of multiple splines
     */
    private static double sumDCurvature2(List<QuinticHermitePoseSplineHolonomic> splines) {
        double sum = 0;
        for (QuinticHermitePoseSplineHolonomic s : splines) {
            sum += s.sumDCurvature2();
        }
        return sum;
    }

    /**
     * Makes optimization code a little more readable
     */
    private static class ControlPoint {
        private double ddx;
        private double ddy;
    }

    /**
     * Runs a single optimization iteration
     */
    private static void runOptimizationIteration(List<QuinticHermitePoseSplineHolonomic> splines) {
        // can't optimize anything with less than 2 splines
        if (splines.size() <= 1) {
            return;
        }

        ControlPoint[] controlPoints = new ControlPoint[splines.size() - 1];
        double magnitude = 0;

        for (int i = 0; i < splines.size() - 1; ++i) {
            // don't try to optimize colinear points
            if (GeometryUtil.isColinear(splines.get(i).getStartPose(), splines.get(i + 1).getStartPose())
                    || GeometryUtil.isColinear(splines.get(i).getEndPose(), splines.get(i + 1).getEndPose())) {
                continue;
            }
            double original = sumDCurvature2(splines);

            // holds the gradient at a control point
            controlPoints[i] = new ControlPoint();

            // calculate partial derivatives of sumDCurvature2
            splines.set(i, splines.get(i).adjustSecondDerivatives(0, kEpsilon, 0, 0));
            splines.set(i + 1, splines.get(i + 1).adjustSecondDerivatives(kEpsilon, 0, 0, 0));
            controlPoints[i].ddx = (sumDCurvature2(splines) - original) / kEpsilon;

            splines.set(i, splines.get(i).adjustSecondDerivatives(0, 0, 0, kEpsilon));
            splines.set(i + 1, splines.get(i + 1).adjustSecondDerivatives(0, 0, kEpsilon, 0));
            controlPoints[i].ddy = (sumDCurvature2(splines) - original) / kEpsilon;

            splines.set(i, splines.get(i));
            splines.set(i + 1, splines.get(i + 1));
            magnitude += controlPoints[i].ddx * controlPoints[i].ddx + controlPoints[i].ddy * controlPoints[i].ddy;
        }

        magnitude = Math.sqrt(magnitude);

        // minimize along the direction of the gradient
        // first calculate 3 points along the direction of the gradient

        // middle point is at the current location
        Translation2d p2 = new Translation2d(0, sumDCurvature2(splines));

        // first point is offset from the middle location by -stepSize
        for (int i = 0; i < splines.size() - 1; ++i) {
            if (GeometryUtil.isColinear(splines.get(i).getStartPose(), splines.get(i + 1).getStartPose())
                    || GeometryUtil.isColinear(splines.get(i).getEndPose(), splines.get(i + 1).getEndPose())) {
                continue;
            }

            // why would this happen?
            if (controlPoints[i] == null)
                continue;

            // normalize to step size
            controlPoints[i].ddx *= kStepSize / magnitude;
            controlPoints[i].ddy *= kStepSize / magnitude;

            // move opposite the gradient by step size amount
            splines.set(i, splines.get(i).adjustSecondDerivatives(0, -controlPoints[i].ddx, 0, -controlPoints[i].ddy));
            splines.set(i + 1,
                    splines.get(i + 1).adjustSecondDerivatives(-controlPoints[i].ddx, 0, -controlPoints[i].ddy, 0));
        }

        // last point is offset from the middle location by +stepSize
        Translation2d p1 = new Translation2d(-kStepSize, sumDCurvature2(splines));
        for (int i = 0; i < splines.size() - 1; ++i) {
            if (GeometryUtil.isColinear(splines.get(i).getStartPose(), splines.get(i + 1).getStartPose())
                    || GeometryUtil.isColinear(splines.get(i).getEndPose(), splines.get(i + 1).getEndPose())) {
                continue;
            }

            // why would this happen?
            if (controlPoints[i] == null)
                continue;

            // move along the gradient by 2 times the step size amount (to return to
            // original location and move by 1 step)
            splines.set(i,
                    splines.get(i).adjustSecondDerivatives(0, 2 * controlPoints[i].ddx, 0, 2 * controlPoints[i].ddy));
            splines.set(i + 1, splines.get(i + 1).adjustSecondDerivatives(2 * controlPoints[i].ddx, 0,
                    2 * controlPoints[i].ddy, 0));
        }

        Translation2d p3 = new Translation2d(kStepSize, sumDCurvature2(splines));
        // approximate step size to minimize sumDCurvature2 along the gradient
        double stepSize = fitParabola(p1, p2, p3);

        for (int i = 0; i < splines.size() - 1; ++i) {
            if (GeometryUtil.isColinear(splines.get(i).getStartPose(), splines.get(i + 1).getStartPose())
                    || GeometryUtil.isColinear(splines.get(i).getEndPose(), splines.get(i + 1).getEndPose())) {
                continue;
            }

            // why would this happen?
            if (controlPoints[i] == null)
                continue;

            // move by the step size calculated by the parabola fit (+1 to offset for the
            // final transformation to find p3)
            controlPoints[i].ddx *= 1 + stepSize / kStepSize;
            controlPoints[i].ddy *= 1 + stepSize / kStepSize;

            splines.set(i, splines.get(i).adjustSecondDerivatives(0, controlPoints[i].ddx, 0, controlPoints[i].ddy));
            splines.set(i + 1,
                    splines.get(i + 1).adjustSecondDerivatives(controlPoints[i].ddx, 0, controlPoints[i].ddy, 0));
        }
    }

    /**
     * fits a parabola to 3 points
     *
     * @return the x coordinate of the vertex of the parabola
     */
    private static double fitParabola(Translation2d p1, Translation2d p2, Translation2d p3) {
        double A = (p3.getX() * (p2.getY() - p1.getY()) + p2.getX() * (p1.getY() - p3.getY())
                + p1.getX() * (p3.getY() - p2.getY()));
        double B = (p3.getX() * p3.getX() * (p1.getY() - p2.getY()) + p2.getX() * p2.getX() * (p3.getY() - p1.getY())
                + p1.getX() * p1.getX() *
                        (p2.getY() - p3.getY()));
        return -B / (2 * A);
    }

}