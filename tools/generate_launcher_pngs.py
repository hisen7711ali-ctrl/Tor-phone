#!/usr/bin/env python3
"""Generate legacy launcher PNGs from the canonical vector artwork."""
from io import BytesIO
from pathlib import Path
import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools/icon/ic_launcher.svg"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

svg = SOURCE.read_bytes()
for density, size in DENSITIES.items():
    output = BytesIO()
    cairosvg.svg2png(bytestring=svg, write_to=output, output_width=size, output_height=size)
    image = Image.open(BytesIO(output.getvalue())).convert("RGBA")
    image = image.quantize(colors=64, method=Image.Quantize.FASTOCTREE)
    target = ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, format="PNG", optimize=True)
    print(f"{target.relative_to(ROOT)}: {target.stat().st_size} bytes")
