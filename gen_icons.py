"""
Generates ic_launcher.png and ic_launcher_round.png for all Android mipmap densities.
Design: dark navy background, cyan shield, white location pin, "IGot-U" text to the right.
"""
import math
from PIL import Image, ImageDraw, ImageFont

BASE = r"c:\Users\user\Documents\IGot-U\android\app\src\main\res"

DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

BG      = (13, 27, 42)       # #0D1B2A
CYAN    = (0, 201, 255)      # #00C9FF
CYAN2   = (0, 153, 204)      # #0099CC
WHITE   = (255, 255, 255)


def draw_shield(draw, cx, cy, r):
    """Draw a shield centred at (cx,cy) with radius r."""
    # Shield is roughly: top arc + two angled sides + pointed bottom
    pts = []
    # Top-left curve down to bottom point
    # Parameterise shield outline
    def shield_pts(cx, cy, r, n=60):
        out = []
        for i in range(n + 1):
            t = i / n  # 0..1
            # angle sweeps from -90 (top) clockwise
            if t < 0.5:
                # left half: top-left arc
                angle = math.pi + t * math.pi  # pi..2pi (left side going down)
                rx, ry = r * 0.55, r * 0.6
                x = cx - rx * math.cos(math.pi * t * 2)
                y = cy - r * 0.45 + ry * math.sin(math.pi * t * 2)
            else:
                angle = (t - 0.5) * math.pi * 2
                rx, ry = r * 0.55, r * 0.6
                x = cx + rx * math.cos(math.pi * (t - 0.5) * 2)
                y = cy - r * 0.45 + ry * math.sin(math.pi * (t - 0.5) * 2)
            out.append((x, y))
        return out

    # Simple polygon shield
    w = r * 0.75
    top_y = cy - r * 0.72
    mid_y = cy - r * 0.05
    bot_y = cy + r * 0.72

    outer = [
        (cx - w,      top_y),
        (cx + w,      top_y),
        (cx + w,      mid_y),
        (cx,          bot_y),
        (cx - w,      mid_y),
    ]
    draw.polygon(outer, fill=CYAN)

    # Inner shield (slightly smaller, darker)
    s = 0.82
    inner = [(cx + (x - cx) * s, cy + (y - cy) * s) for x, y in outer]
    draw.polygon(inner, fill=CYAN2)


def draw_pin(draw, cx, cy, r):
    """Draw a location pin (circle + teardrop tail) centred at cx, cy-r*0.1."""
    pr = r * 0.22   # pin circle radius
    px = cx
    py = cy - r * 0.12

    # Circle
    draw.ellipse([px - pr, py - pr, px + pr, py + pr], fill=WHITE)

    # Tail triangle
    tail_top_y = py + pr * 0.6
    tail_bot_y = py + pr * 2.8
    tail_w = pr * 0.85
    draw.polygon([
        (px - tail_w, tail_top_y),
        (px + tail_w, tail_top_y),
        (px,          tail_bot_y),
    ], fill=WHITE)

    # Inner hole
    hr = pr * 0.45
    draw.ellipse([px - hr, py - hr, px + hr, py + hr], fill=CYAN2)


def make_icon(size, round_=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if round_:
        # Circular background
        draw.ellipse([0, 0, size - 1, size - 1], fill=BG)
    else:
        draw.rectangle([0, 0, size, size], fill=BG)

    cx, cy = size / 2, size / 2
    r = size * 0.42

    draw_shield(draw, cx, cy, r)
    draw_pin(draw, cx, cy, r)

    return img


import os

for folder, size in DENSITIES.items():
    path = os.path.join(BASE, folder)
    os.makedirs(path, exist_ok=True)

    sq = make_icon(size, round_=False)
    sq.save(os.path.join(path, "ic_launcher.png"))

    rd = make_icon(size, round_=True)
    rd.save(os.path.join(path, "ic_launcher_round.png"))

    print(f"  {folder}: {size}x{size} done")

print("All icons generated.")
