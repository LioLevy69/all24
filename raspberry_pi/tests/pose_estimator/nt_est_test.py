# pylint: disable=W0212


import time
import unittest

import ntcore

from app.config.identity import Identity
from app.network.fake_network import FakeNetwork
from app.network.network_protocol import Blip25, PoseEstimate25
from app.network.real_network import RealNetwork
from app.pose_estimator.field_map import FieldMap
from app.pose_estimator.nt_estimate import NTEstimate


class NTEstTest(unittest.TestCase):
    def test_real_nt_est(self) -> None:
        print()
        inst = ntcore.NetworkTableInstance.getDefault()
        inst.startServer()
        pub = inst.getStructArrayTopic("foo/1", Blip25).publish(
            ntcore.PubSubOptions(keepDuplicates=True)
        )
        sub = inst.getStructTopic("pose", PoseEstimate25).subscribe(None)
        field_map = FieldMap()
        net = RealNetwork(Identity.UNKNOWN)
        est = NTEstimate(field_map, net)
        estimate = None
        for i in range(10):
            time.sleep(0.02)
            # print("NTEstTest.test_real_nt_est() i ", i)
            time_us = ntcore._now()
            # print("NTEstTest.test_real_nt_est() time_us ", time_us)
            pub.set(
                [
                    Blip25(0, 190, 210, 210, 210, 210, 190, 190, 190),
                    Blip25(0, 190, 210, 210, 210, 210, 190, 190, 190),
                ],
                time_us,
            )
            est.step()
            estimate = sub.get()
            print(estimate)
        if estimate is not None:
            # so what are we left with?
            # right in front of the tag, as expected.
            self.assertAlmostEqual(2.351, estimate.x, 3)
            self.assertAlmostEqual(0, estimate.y, 3)
            self.assertAlmostEqual(0, estimate.theta, 3)
            # good at estimating range
            self.assertAlmostEqual(0.041, estimate.x_sigma, 3)
            # not good at estimating bearing
            self.assertAlmostEqual(1.169, estimate.y_sigma, 3)
            # not good at estimating yaw
            self.assertAlmostEqual(0.707, estimate.theta_sigma, 3)
            # no odometry
            self.assertAlmostEqual(0, estimate.dx, 3)
            self.assertAlmostEqual(0, estimate.dy, 3)
            self.assertAlmostEqual(0, estimate.dtheta, 3)
            self.assertAlmostEqual(0, estimate.dt, 3)

    def test_fake_nt_est(self) -> None:
        field_map = FieldMap()
        net = FakeNetwork()
        est = NTEstimate(field_map, net)
        start_time_us = ntcore._now()
        for _ in range(10):
            time.sleep(0.02)
            time_us = ntcore._now() - start_time_us
            net.received_blip25s["foo"] = [
                (
                    time_us,
                    [
                        Blip25(0, 190, 210, 210, 210, 210, 190, 190, 190),
                        Blip25(0, 190, 210, 210, 210, 210, 190, 190, 190),
                    ],
                )
            ]
            est.step()
            print(net.estimate)

        # so what are we left with?
        # right in front of the tag, as expected.
        self.assertAlmostEqual(2.351, net.estimate.x, 3)
        self.assertAlmostEqual(0, net.estimate.y, 3)
        self.assertAlmostEqual(0, net.estimate.theta, 3)
        # good at estimating range
        self.assertAlmostEqual(0.041, net.estimate.x_sigma, 3)
        # not good at estimating bearing
        self.assertAlmostEqual(1.169, net.estimate.y_sigma, 3)
        # not good at estimating yaw
        self.assertAlmostEqual(0.707, net.estimate.theta_sigma, 3)
        # no odometry
        self.assertAlmostEqual(0, net.estimate.dx, 3)
        self.assertAlmostEqual(0, net.estimate.dy, 3)
        self.assertAlmostEqual(0, net.estimate.dtheta, 3)
        self.assertAlmostEqual(0, net.estimate.dt, 3)




