"""Check the captured cards and write them as lossless PNGs.

Called by tools/build.mjs:  py tools/pack.py <job.json>

Every card is one capture of one section. This script checks its size (1672 px
wide, even height), that its four edges are clean lines (one flat colour along
the whole edge, so the card looks finished on any page colour), saves it as an
exact palette PNG when it has <= 256 colours, else RGB, optimize=True, decodes
the result again and compares it with the capture (lossless guard).
"""
from __future__ import annotations

import io
import json
import sys

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None


def encode(arr: np.ndarray) -> tuple[bytes, str]:
    h, w, _ = arr.shape
    flat = arr.reshape(-1, 3)
    packed = (flat[:, 0].astype(np.uint32) << 16) | (flat[:, 1].astype(np.uint32) << 8) | flat[:, 2].astype(np.uint32)
    uniq, inv = np.unique(packed, return_inverse=True)
    buf = io.BytesIO()
    if len(uniq) <= 256:
        im = Image.fromarray(inv.reshape(h, w).astype(np.uint8), mode="P")
        pal = np.stack([(uniq >> 16) & 255, (uniq >> 8) & 255, uniq & 255], axis=1).astype(np.uint8).reshape(-1).tolist()
        im.putpalette(pal)
        im.save(buf, format="PNG", optimize=True)
        mode = "P"
    else:
        Image.fromarray(arr, mode="RGB").save(buf, format="PNG", optimize=True)
        mode = "RGB"
    data = buf.getvalue()
    back = np.asarray(Image.open(io.BytesIO(data)).convert("RGB"))
    if not np.array_equal(back, arr):
        raise SystemExit("internal error: PNG encoding was not lossless")
    return data, mode


def flat_edge(line: np.ndarray) -> bool:
    return bool((line == line[0]).all())


def main():
    job = json.load(open(sys.argv[1], encoding="utf-8"))
    warnings: list[str] = []
    cards = []
    for c in job["cards"]:
        with Image.open(c["raw"]) as im:
            arr = np.asarray(im.convert("RGB"))
        h, w, _ = arr.shape
        name = c["file"]
        if w != job["width"]:
            raise SystemExit(f"{name}: capture is {w} px wide, expected {job['width']}")
        if h % 2:
            raise SystemExit(f"{name}: capture height {h} is odd")
        if h > job["maxHeight"]:
            warnings.append(f"{name}: {h}px tall, over the {job['maxHeight']}px target. Shorten the card or split it.")
        for edge, line in (("top", arr[0]), ("bottom", arr[-1]), ("left", arr[:, 0]), ("right", arr[:, -1])):
            if not flat_edge(line):
                warnings.append(f"{name}: the {edge} edge is not one clean line (something reaches the card edge)")
        data, mode = encode(arr)
        if len(data) > job["maxBytes"]:
            warnings.append(f"{name}: {len(data) / 1048576:.2f} MB, over the {job['maxBytes'] / 1048576:.0f} MB limit")
        with open(c["out"], "wb") as f:
            f.write(data)
        cards.append({"id": c["id"], "file": name, "width": w, "height": h, "bytes": len(data), "mode": mode})
    json.dump({"cards": cards, "warnings": warnings}, open(job["result"], "w", encoding="utf-8"), indent=2)


if __name__ == "__main__":
    main()
