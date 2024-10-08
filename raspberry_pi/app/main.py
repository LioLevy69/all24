""" This is the coprocessor main loop.

It takes data from the camera and the gyro, and publishes it
on network tables.

You can't run this from the command line.  To run the app,
use the script called "runapp.py" in the raspberry_pi directory
(one level above this one).
"""

# pylint: disable=R0914

from app.camera.camera_factory import CameraFactory
from app.camera.camera_protocol import Camera, Size
from app.config.identity import Identity
from app.dashboard.display import Display
from app.localization.network import Network
from app.localization.tag_detector import TagDetector
from app.sensors.gyro_protocol import Gyro
from app.sensors.gyro_factory import GyroFactory
from app.util.timer import Timer


def main() -> None:
    print("main")
    identity: Identity = Identity.get()
    cameras: list[Camera] = CameraFactory.get(identity)
    num = 0
    tag_detectors = []
    for camera in cameras:
        size: Size = camera.get_size()
        display: Display = Display(size.width, size.height, num)
        network: Network = Network(identity, num)
        tag_detectors.append(
            TagDetector(identity, size.width, size.height, camera, display, network)
        )
        num += 1
        # TODO: make network not just for cameras
    # gyronetwork: Network = Network(identity, "Gyro")
    gyronetwork: Network = Network(identity, 2)
    gyro: Gyro = GyroFactory.get(gyronetwork)

    for camera in cameras:
        camera.start()
    try:
        while True:
            # the most recent completed frame, from the recent past
            capture_start: int = Timer.time_ns()
            requests = []
            for camera in cameras:
                requests.append(camera.capture_request())
            capture_end: int = Timer.time_ns()
            # capture time is how long we wait for the camera, it should be close to zero.
            capture_time_ms: int = (capture_end - capture_start) // 1000000
            network.vision_capture_time_ms.set(capture_time_ms)
            try:
                num = 0
                for tag_detector in tag_detectors:
                    tag_detector.analyze(requests[num])
                    num += 1
                gyro.sample()
            finally:
                for request in requests:
                    request.release()
    finally:
        for camera in cameras:
            camera.stop()
