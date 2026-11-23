#!/usr/bin/env python3
"""生成墨水屏版启动图标（每档按目标尺寸直接绘制，线条固定 2px）。

设计稿见 management/brand/pagelence-eink.svg：白底、黑线、翻开的三页书。
这里不把 1024 的主图缩小（缩小时 2px 线会变细、发虚），而是按每档目标像素
重新描边：线宽始终是 2 像素，图形坐标按 128 设计格等比换算。

用法：python3 tools/build_eink_icon.py
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

# 不做超采样：墨水屏本身没有灰阶抗锯齿，硬像素的 2px 线在设备上更清楚，
# 也保证「线宽恰好 2 像素」不会因为缩放变成 1px 的灰边。
SUPERSAMPLE = 1
STROKE_PX = 2
VIEWBOX = 128.0
SCALE = 0.825  # 与 SVG 的 transform scale 一致，留出边距

# 设计稿里的三条闭合路径（viewBox 128）。
PATHS: list[list[tuple[float, float] | tuple[str, tuple[float, float, float, float, float, float]]]] = [
    [
        ("M", (24, 52)),
        ("C", (38, 50, 50, 52, 60, 60)),
        ("L", (60, 98)),
        ("C", (50, 90, 38, 88, 24, 90)),
        ("L", (24, 52)),
    ],
    [
        ("M", (68, 72)),
        ("C", (78, 62, 90, 58, 104, 58)),
        ("L", (104, 90)),
        ("C", (90, 88, 78, 90, 68, 98)),
        ("L", (68, 72)),
    ],
    [
        ("M", (68, 52)),
        ("C", (76, 40, 86, 32, 98, 30)),
        ("L", (98, 52)),
        ("C", (88, 52, 76, 58, 68, 66)),
        ("L", (68, 52)),
    ],
]


def _cubic(p0, p1, p2, p3, steps: int = 48):
    for i in range(1, steps + 1):
        t = i / steps
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        yield (x, y)


def path_points(path) -> list[tuple[float, float]]:
    """把一条路径展平成折线（viewBox 坐标）。"""
    points: list[tuple[float, float]] = []
    current = (0.0, 0.0)
    for command, data in path:
        if command == "M":
            current = data  # type: ignore[assignment]
            points.append(current)
        elif command == "L":
            current = data  # type: ignore[assignment]
            points.append(current)
        elif command == "C":
            x1, y1, x2, y2, x3, y3 = data  # type: ignore[misc]
            points.extend(_cubic(current, (x1, y1), (x2, y2), (x3, y3)))
            current = (x3, y3)
        else:  # pragma: no cover
            raise ValueError(command)
    return points


def to_pixels(points, size: int) -> list[tuple[float, float]]:
    """viewBox → 超采样画布像素：先按 0.825 居中缩放，再换算到目标尺寸。"""
    scale_to_px = size * SUPERSAMPLE / VIEWBOX
    return [
        (
            (64 + (x - 64) * SCALE) * scale_to_px,
            (64 + (y - 64) * SCALE) * scale_to_px,
        )
        for x, y in points
    ]


def render(size: int, out: Path) -> None:
    canvas = size * SUPERSAMPLE
    image = Image.new("RGB", (canvas, canvas), "white")
    draw = ImageDraw.Draw(image)
    width = STROKE_PX * SUPERSAMPLE
    radius = width / 2
    for path in PATHS:
        points = to_pixels(path_points(path), size)
        draw.line(points, fill="black", width=width, joint="curve")
        for x, y in (points[0], points[-1]):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill="black")
    image = image.resize((size, size), Image.LANCZOS)
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)


DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    for name, size in DENSITIES.items():
        target = root / "app-eink" / "src" / "main" / "res" / f"mipmap-{name}" / "ic_launcher.png"
        render(size, target)
        print(f"{target.relative_to(root)}  {size}x{size}  stroke={STROKE_PX}px")


if __name__ == "__main__":
    main()
