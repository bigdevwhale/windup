"""Turns the launcher picture into vector drawables (and docs/logo.png).

The picture is drawn by the app's own sprite code: run
    ./gradlew testDebugUnitTest -Prender --tests '*RenderArtIcon*'
which writes docs/icon-src.png (60×60, transparent ground), then
    python tools/icon.py

Each run of same-coloured pixels in a row becomes one rectangle in the vector path.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / "app/src/main/res/drawable"
BG = (29, 53, 50)


def main():
    img = Image.open(ROOT / "docs/icon-src.png").convert("RGBA")
    w, h = img.size
    runs = {}
    for y in range(h):
        x = 0
        while x < w:
            c = img.getpixel((x, y))
            if c[3] == 0:
                x += 1
                continue
            s = x
            while x < w and img.getpixel((x, y)) == c:
                x += 1
            runs.setdefault(c[:3], []).append((s, y, x - s))
    scale = 108 / w
    head = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')
    paths = []
    for (r, g, b), rs in runs.items():
        d = " ".join(f"M{x * scale:.2f},{y * scale:.2f}h{n * scale:.2f}v{scale:.2f}h{-n * scale:.2f}z" for x, y, n in rs)
        paths.append(f'    <path android:fillColor="#FF{r:02X}{g:02X}{b:02X}" android:pathData="{d}" />')
    (DRAW / "ic_launcher_foreground.xml").write_text(head + "\n".join(paths) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(head + '    <path android:fillColor="#FF%02X%02X%02X" android:pathData="M0,0h108v108h-108z" />\n</vector>\n' % BG, encoding="utf-8", newline="\n")
    logo = Image.new("RGBA", (w, h), BG + (255,))
    logo.alpha_composite(img)
    logo.resize((512, 512), Image.NEAREST).save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
