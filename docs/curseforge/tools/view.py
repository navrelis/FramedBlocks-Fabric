"""Make copies of the cards that are easy to look at.

    py tools/view.py            all cards
    py tools/view.py 03 06      only these (file name starts with)

Writes to build/view/:
  NN-slug.png              the card at half size (836 px wide, as CurseForge shows it on a desktop)
  phone-NN-slug.png        the card at 360 px wide (as CurseForge shows it on a phone)
  sheet-dark.png           all cards at 418 px on the CurseForge page colour (#0d0d0d)
  sheet-light.png          the same on a light page
Run it from any folder; it finds dist/ next to tools/.
"""
import glob
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "build", "view")
os.makedirs(OUT, exist_ok=True)
want = sys.argv[1:]
cards = []
for f in sorted(glob.glob(os.path.join(ROOT, "dist", "img", "*.png"))):
    n = os.path.basename(f)
    if want and not any(n.startswith(w) for w in want):
        continue
    im = Image.open(f).convert("RGB")
    im.resize((im.width // 2, im.height // 2), Image.LANCZOS).save(os.path.join(OUT, n))
    im.resize((360, round(im.height * 360 / im.width)), Image.LANCZOS).save(os.path.join(OUT, "phone-" + n))
    cards.append((n, im))
    print(f"{n}  {im.width}x{im.height}")

if not want and cards:
    thumbs = [im.resize((418, im.height // 4), Image.LANCZOS) for _, im in cards]
    cols = 4
    rows = [thumbs[i:i + cols] for i in range(0, len(thumbs), cols)]
    width = cols * 418 + (cols + 1) * 24
    height = sum(max(t.height for t in r) + 24 for r in rows) + 24
    for name, colour in (("sheet-dark.png", (13, 13, 13)), ("sheet-light.png", (244, 244, 244))):
        sheet = Image.new("RGB", (width, height), colour)
        y = 24
        for r in rows:
            x = 24
            for t in r:
                sheet.paste(t, (x, y))
                x += 418 + 24
            y += max(t.height for t in r) + 24
        sheet.save(os.path.join(OUT, name))
    print(f"sheets -> {OUT}")
