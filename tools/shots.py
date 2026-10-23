"""Turns raw phone captures into the README screenshots.

Capture the demo build (`./gradlew -Pdemo assembleDebug`) with `adb exec-out screencap -p` into
<raw>/<lang>/<n>-<name>.png, then
    python tools/shots.py <raw>
which writes docs/screenshots/<lang>/<n>-<name>.jpg at phone width.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent


def main(raw: str):
    for lang in ("en", "ru"):
        out = ROOT / f"docs/screenshots/{lang}"
        out.mkdir(parents=True, exist_ok=True)
        for f in sorted((Path(raw) / lang).glob("*.png")):
            Image.open(f).convert("RGB").save(out / f"{f.stem}.jpg", quality=86, optimize=True)


if __name__ == "__main__":
    main(sys.argv[1])
