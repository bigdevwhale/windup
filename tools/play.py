"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

    python tools/play.py

Takes docs/screenshots/<lang>/<n>-<name>.jpg (1080x2400 captures of the demo build, tools/shots.py) and writes the
512 px icon, the 1024x500 feature graphics, the 1600x900 launch posts and eight 1080x1920 captioned screenshots per
language. The phone's status bar is painted over with a clean one. Play wants screenshots no longer than twice their
width, so each screen runs off the bottom edge under its caption. Captions are in Nunito, the app's font, cream and
brass on the shelf's green striped wallpaper under the lamp; the banner is the shelf itself with the toys on a plank.
Needs `pip install pillow`.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
RES = ROOT / "app/src/main/res"
ART = RES / "drawable-nodpi"
HEAVY = RES / "font/nunito_900.ttf"
BOLD = RES / "font/nunito_800.ttf"
SEMI = RES / "font/nunito_700.ttf"

# The palette (ui/theme/Theme.kt): the wall, cream ink, brass, tin red.
WALL = (0x1D, 0x35, 0x32)
DEEP = (0x10, 0x1E, 0x1C)
INK = (0xF3, 0xE5, 0xC8)
BRASS = (0xF0, 0xC0, 0x40)
BRASS_DEEP = (0xC0, 0x8A, 0x20)
TIN = (0xD2, 0x4A, 0x32)
TIN_DEEP = (0x96, 0x2C, 0x1F)
WOOD = (0x4A, 0x33, 0x24)

BAR = 100  # the status bar of the captures, painted over

NAME = {"en": "Windup", "ru": "Заводилка"}
LINES = {
    "en": ("A little movement, all day long –", "like winding tin toys"),
    "ru": ("Понемногу, но весь день –", "как заводные игрушки"),
}

# The order they appear in the store and the caption: a line in cream and one in brass.
PAGES = [
    ("1-shelf", ("Nine tin toys, each wound", "by a minute of movement"), ("Девять жестяных игрушек –", "их заводит минута движения")),
    ("3-where", ("Where are you? Three moves", "for the desk, the kettle, a queue"), ("Где вы? Три упражнения –", "у стола, у чайника, в очереди")),
    ("4-exercise", ("Every rep is", "a click of the key"), ("Каждый повтор –", "щелчок ключа")),
    ("2-drum", ("A winding lasts two hours,", "so move a little, often"), ("Завода хватает на два часа –", "двигайтесь понемногу и часто")),
    ("5-box", ("Your day plays back", "as a tune"), ("Ваш день звучит", "как мелодия")),
    ("7-workshop", ("A weekend fair", "with new toys and moves"), ("Ярмарка по выходным –", "новые игрушки и движения")),
    ("8-metals", ("Recast them in brass,", "copper, silver and gold"), ("Латунь, медь,", "серебро и золото")),
    ("10-evening", ("Statistics, and an evening", "of walnut and felt"), ("Статистика и вечер –", "тёмный орех и сукно")),
]

# Toys on the banner's plank: drawable, x as a fraction of the width, height as a fraction of the height.
SHELF = [("toy_ballet", .53, .46), ("toy_monkey", .63, .40), ("toy_frog", .76, .30), ("toy_robot", .87, .44), ("toy_penguin", .97, .36)]


def font(size, path=BOLD):
    return ImageFont.truetype(str(path), size)


def fit(text, size, width, path=BOLD):
    while size > 20 and font(size, path).getlength(text) > width:
        size -= 2
    return font(size, path)


def art(name, height=None, width=None):
    im = Image.open(ART / f"{name}.webp").convert("RGBA")
    k = height / im.height if height else width / im.width
    return im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)


def wallpaper(W, H, tile=360, dim=.62):
    """The shelf's striped green wallpaper, dimmed so the text and the screens stand out."""
    t = art("scene_wall", tile)
    g = Image.new("RGBA", (W, H))
    for y in range(0, H, t.height):
        for x in range(0, W, t.width):
            g.alpha_composite(t, (x, y))
    g = ImageEnhance.Brightness(g.convert("RGB")).enhance(dim).convert("RGBA")
    return g


def glow(g, cx, cy, r, colour, alpha=150):
    """A soft round light, like the lamp's on the wall."""
    mask = Image.new("L", g.size, 0)
    ImageDraw.Draw(mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=alpha)
    g.paste(Image.new("RGBA", g.size, colour + (255,)), (0, 0), mask.filter(ImageFilter.GaussianBlur(r / 2.5)))


def vignette(g, strength=170):
    """Darker towards the edges and the bottom."""
    W, H = g.size
    mask = Image.new("L", (W, H), strength)
    ImageDraw.Draw(mask).ellipse((-W * .25, -H * .2, W * 1.25, H * .9), fill=0)
    g.paste(Image.new("RGBA", g.size, DEEP + (255,)), (0, 0), mask.filter(ImageFilter.GaussianBlur(min(W, H) / 5)))


def clean_bar(im):
    """Paints the status bar over with the row just below it, then a calm one: 9:41, signal, wifi, a full battery."""
    im = im.convert("RGB")
    row = im.crop((0, BAR + 2, im.width, BAR + 3)).resize((im.width, BAR + 2))
    im.paste(row, (0, 0))
    d = ImageDraw.Draw(im)
    d.text((70, 52), "9:41", font=font(42, SEMI), fill=INK, anchor="lm")
    x = im.width - 64
    d.rounded_rectangle((x - 62, 36, x - 6, 68), 7, outline=INK, width=3)
    d.rounded_rectangle((x - 56, 42, x - 12, 62), 3, fill=INK)
    d.rectangle((x - 4, 46, x, 58), fill=INK)
    cx, cy = x - 110, 66
    for r in (34, 23, 12):
        d.arc((cx - r, cy - r, cx + r, cy + r), 225, 315, fill=INK, width=5)
    d.ellipse((cx - 4, cy - 6, cx + 4, cy + 2), fill=INK)
    for i in range(4):
        bx = x - 196 + i * 13
        d.rectangle((bx, 66 - 8 - i * 8, bx + 8, 66), fill=INK)
    return im


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=48):
    """A screen on the wall: rounded corners, a deep shadow and a thin brass edge."""
    sh = Image.new("RGBA", (card.width + 160, card.height + 160), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((80, 100, 80 + card.width, 100 + card.height), r, fill=(0, 0, 0, 160))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(30)), (x - 80, y - 80))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 2, y - 2, x + card.width + 1, y + card.height + 1), r + 2, outline=BRASS_DEEP, width=4)


def key_rule(g, cx, y, half=170, size=58):
    """The brass rule under a caption, with the winding key in the middle."""
    d = ImageDraw.Draw(g)
    d.line((cx - half, y, cx - 44, y), fill=BRASS, width=4)
    d.line((cx + 44, y, cx + half, y), fill=BRASS, width=4)
    key = art("scene_key", size)
    g.alpha_composite(key, (cx - key.width // 2, y - key.height // 2))


def shadow_text(d, xy, text, f, fill, anchor="mm", off=4):
    d.text((xy[0], xy[1] + off), text, font=f, fill=(0, 0, 0), anchor=anchor)
    d.text(xy, text, font=f, fill=fill, anchor=anchor)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = wallpaper(W, H, dim=.42)
    glow(g, W // 2, 150, 420, (0x6A, 0x58, 0x2A), 150)
    vignette(g)
    d = ImageDraw.Draw(g)
    f = min((fit(c, 78, W - 120) for c in caption), key=lambda x: x.size)
    shadow_text(d, (W / 2, 100), caption[0], f, INK)
    shadow_text(d, (W / 2, 192), caption[1], f, BRASS)
    key_rule(g, W // 2, 276)
    im = clean_bar(Image.open(src))
    w = 880
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 340
    # only the top corners show: the screen runs on past the bottom edge
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def icon_square(size):
    """The adaptive icon flattened: the visible middle two thirds of both 108 dp layers, as a launcher shows them."""
    bg = Image.open(ART / "ic_launcher_background.webp").convert("RGBA")
    fg = Image.open(ART / "ic_launcher_foreground.webp").convert("RGBA")
    bg.alpha_composite(fg)
    s = bg.width
    k = s * 72 // 108
    o = (s - k) // 2
    return bg.crop((o, o, o + k, o + k)).resize((size, size), Image.LANCZOS)


def icon():
    """Play wants a full square and rounds the corners itself."""
    icon_square(512).convert("RGB").save(OUT / "icon-512.png", optimize=True)


def logo(size):
    """The icon as a launcher shows it: a rounded square."""
    sq = icon_square(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), size * .22, fill=255)
    sq.putalpha(mask)
    return sq


def shelf(W, H):
    """The wall under the lamp, a plank near the bottom and the toys standing on it."""
    g = wallpaper(W, H, tile=round(H * .72), dim=.8)
    lamp_x = int(W * .7)
    glow(g, lamp_x, int(H * .32), int(H * .62), (0xF6, 0xDB, 0x92), 140)
    lamp = art("scene_lamp", width=int(H * .36))
    g.alpha_composite(lamp, (lamp_x - lamp.width // 2, -int(lamp.height * .12)))
    floor = int(H * .86)
    plank = art("scene_plank", width=W + 40)
    for name, fx, fh in SHELF:
        p = art(name, int(H * fh))
        px, py = int(W * fx - p.width / 2), floor - p.height + int(H * .012)
        sh = Image.new("RGBA", g.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).ellipse((px + p.width * .12, floor - H * .02, px + p.width * .88, floor + H * .015), fill=(0, 0, 0, 120))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(5)))
        g.alpha_composite(p, (px, py))
    g.alpha_composite(plank, (-20, floor - plank.height // 4))
    below = floor + plank.height * 3 // 4
    if below < H:
        ImageDraw.Draw(g).rectangle((0, below, W, H), fill=WOOD + (255,))
    return g


def shade(g, until, strength=235):
    """A dark veil from the left edge, solid for the first half and fading out smoothly by `until`, for the text."""
    mask = Image.new("L", g.size, 0)
    d = ImageDraw.Draw(mask)
    for x in range(until):
        t = max(0., (x / until - .5) / .5)
        d.line([(x, 0), (x, g.height)], fill=round(strength * (1 - t * t * (3 - 2 * t))))
    g.paste(Image.new("RGBA", g.size, DEEP + (255,)), (0, 0), mask)


def feature(lang):
    W, H = 1024, 500
    g = shelf(W, H)
    shade(g, 640)
    g.alpha_composite(logo(108), (52, 52))
    d = ImageDraw.Draw(g)
    name = NAME[lang]
    d.text((182, 106), name, font=fit(name, 84, 300, HEAVY), fill=INK, anchor="lm")
    lines = LINES[lang]
    f = min((fit(l, 34, 420) for l in lines), key=lambda x: x.size)
    d.line((56, 206, 210, 206), fill=BRASS, width=5)
    for i, l in enumerate(lines):
        d.text((54, 232 + i * 48), l, font=f, fill=INK if i == 0 else BRASS)
    x = 56
    for n in ("ui_gear", "ui_spring", "scene_key"):
        p = art(n, 54)
        g.alpha_composite(p, (x, 370))
        x += p.width + 22
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def post(lang):
    """The launch post: the banner's look at 1600x900, with a «now on Google Play» plaque."""
    W, H = 1600, 900
    g = shelf(W, H)
    shade(g, 1000)
    g.alpha_composite(logo(160), (92, 92))
    d = ImageDraw.Draw(g)
    name = NAME[lang]
    d.text((284, 172), name, font=fit(name, 130, 470, HEAVY), fill=INK, anchor="lm")
    d.line((96, 330, 360, 330), fill=BRASS, width=7)
    lines = LINES[lang]
    f = min((fit(l, 56, 640) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        d.text((92, 370 + i * 76), l, font=f, fill=INK if i == 0 else BRASS)
    plaque = "Now on Google Play" if lang == "en" else "Теперь в Google Play"
    pf = font(46, HEAVY)
    tw = int(pf.getlength(plaque))
    px, py, ph = 96, 640, 96
    d.rounded_rectangle((px, py + 8, px + tw + 96, py + ph + 8), 48, fill=TIN_DEEP)
    d.rounded_rectangle((px, py, px + tw + 96, py + ph), 48, fill=TIN)
    d.text((px + 48, py + ph // 2), plaque, font=pf, fill=(0xFF, 0xF8, 0xE8), anchor="lm")
    g.convert("RGB").save(OUT / f"post-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        post(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
