""" This is a wrapper for the note detector."""

# pylint: disable=C0103,E1101,R0902,R0913,R0914,W0612

from typing import Any

import cv2
import numpy as np
from cv2.typing import MatLike
from numpy.typing import NDArray
from wpimath.geometry import Rotation3d

from app.camera.camera_protocol import Camera, Request
from app.camera.interpreter_protocol import Interpreter
from app.config.identity import Identity
from app.dashboard.display import Display
from app.localization.network import Network
from app.util.timer import Timer

Mat = NDArray[np.uint8]


class NoteDetector(Interpreter):
    def __init__(
        self,
        identity: Identity,
        cam: Camera,
        camera_num: int,
        display: Display,
        network: Network,
    ) -> None:
        self.camera = cam
        self.display = display
        self.frame_time = Timer.time_ns()

        # opencv hue values are 0-180, half the usual number
        self.object_lower = np.array((0, 150, 50))
        self.object_lower2 = np.array((178, 150, 50))
        self.object_higher = np.array((8, 255, 255))
        self.object_higher2 = np.array((180, 255, 255))

        # TODO: move the identity part of this path to the Network object
        path = "noteVision/" + identity.value + "/" + str(camera_num)
        self._notes = network.get_note_sender(path + "/Rotation3d")
        self._et = network.get_double_sender(path + "/et_ms")


    def analyze(self, req: Request) -> None:
        metadata: dict[str, Any] = req.metadata()
        with req.array() as mapped_array:
            self.analyze2(metadata, mapped_array.array)

    def analyze2(self, metadata: dict[str, Any], img_yuv: Mat) -> None:
        t0 = Timer.time_ns()

        # this says YUV->RGB but it actually makes BGR.
        # github.com/raspberrypi/picamera2/issues/848
        img_bgr = cv2.cvtColor(img_yuv, cv2.COLOR_YUV420p2RGB)

        # TODO: figure out the crop
        # img_bgr : Mat = img_bgr[65:583, :, :]

        mtx = self.camera.get_intrinsic()
        dist = self.camera.get_dist()
        img_bgr = cv2.undistort(img_bgr, mtx, dist)
        img_hsv = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2HSV)
        img_hsv = np.ascontiguousarray(img_hsv)

        img_range = cv2.inRange(img_hsv, self.object_lower, self.object_higher)

        floodfill = img_range.copy()
        h, w = img_range.shape[:2]
        mask = np.zeros((h + 2, w + 2), np.uint8)
        cv2.floodFill(floodfill, mask, [0, 0], [255])
        floodfill_inv = cv2.bitwise_not(floodfill)
        img_floodfill = cv2.bitwise_or(img_range, floodfill_inv)
        median = cv2.medianBlur(img_floodfill, 5)
        contours, _ = cv2.findContours(median, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        size = self.camera.get_size()
        width = size.width
        height = size.height

        objects: list[Rotation3d] = []
        for c in contours:
            # _, _, cnt_width, cnt_height = cv2.boundingRect(c)
            # reject anything taller than it is wide
            # if cnt_width / cnt_height < 1.2:
            #     continue
            # # reject big bounding box
            # if cnt_width > width / 2 or cnt_height > height / 2:
            #     continue

            # if (cnt_height < 20 or cnt_width < 20) and cnt_width/cnt_height < 3:
            #     continue

            mmnts = cv2.moments(c)
            # reject too small (m00 is in pixels)
            # TODO: make this adjustable at runtime
            # to pick out distant targets
            if mmnts["m00"] < 100:
                continue

            cX = int(mmnts["m10"] / mmnts["m00"])
            cY = int(mmnts["m01"] / mmnts["m00"])

            yNormalized = (height / 2 - cY) / mtx[1, 1]
            xNormalized = (width / 2 - cX) / mtx[0, 0]

            rotation = Rotation3d(
                initial=np.array([1, 0, 0]),
                final=np.array([1, xNormalized, yNormalized]),
            )

            objects.append(rotation)
            self.draw_result(img_bgr, c, cX, cY)
        
        t1: int = Timer.time_ns()
        et_ms = (t1 - t0) // 1000000
        self._et.send(et_ms, 0)


        # img_output = cv2.resize(img_bgr, (269, 162))

        self.display.put_frame(img_range)

    def draw_result(self, img: MatLike, cnt: MatLike, cX: int, cY: int) -> None:
        # float_formatter: dict[str, Callable[[float], str]] = {"float_kind": lambda x: f"{x:4.1f}"}
        cv2.drawContours(img, [cnt], -1, (0, 255, 0), 2)
        cv2.circle(img, (cX, cY), 7, (0, 0, 0), -1)
        # cv2.putText(img, f"t: {np.array2string(wpi_t.flatten(), formatter=float_formatter)}", (cX - 20, cY - 20),
        #             cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 2)
