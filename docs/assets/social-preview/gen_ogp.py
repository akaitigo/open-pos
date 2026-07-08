"""open-pos の Social Preview (OGP) 画像を生成する。1280x640 PNG。"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

W, H = 1280, 640
FONT_DIR = Path("/usr/share/fonts/truetype/dejavu")
OUT = Path(__file__).parent / "open-pos-social-preview.png"


def font(size: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(str(FONT_DIR / name), size)


def lerp(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


img = Image.new("RGB", (W, H))
d = ImageDraw.Draw(img)

# 対角グラデーション背景 (#0d1526 -> #12213f -> #0e2a4a)
c1, c2, c3 = (13, 21, 38), (18, 33, 63), (14, 42, 74)
for y in range(H):
    for seg in range(1):
        pass
    t = y / H
    row = lerp(c1, c2, t * 1.6) if t < 0.55 else lerp(c2, c3, (t - 0.55) / 0.45)
    d.line([(0, y), (W, y)], fill=row)

# グリッド
grid = (33, 52, 86)
for x in range(0, W, 48):
    d.line([(x, 0), (x, H)], fill=grid, width=1)
for y in range(0, H, 48):
    d.line([(0, y), (W, y)], fill=grid, width=1)

ACCENT = (79, 156, 249)
ACCENT_L = (124, 196, 255)
TEXT = (232, 238, 247)
MUTED = (159, 179, 207)
SUB = (195, 210, 232)
BOX_BG = (26, 40, 66)
BOX_BORDER = (59, 90, 143)

M = 64  # 左右マージン

# ---- ヘッダー ----
d.text((M, 46), "open", font=font(72), fill=TEXT)
w_open = d.textlength("open", font=font(72))
d.text((M + w_open, 46), "-pos", font=font(72), fill=ACCENT)
w_logo = w_open + d.textlength("-pos", font=font(72))
d.text((M + w_logo + 26, 88), "Universal Point of Sale System", font=font(25, bold=False), fill=MUTED)

d.text((M, 148), "Self-hostable multi-tenant & offline-first POS reference implementation",
       font=font(21, bold=False), fill=SUB)

# ---- アーキテクチャ図 ----
def node(x: int, y: int, w: int, h: int, title: str, desc: str) -> None:
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=BOX_BG, outline=BOX_BORDER, width=2)
    tw = d.textlength(title, font=font(19))
    d.text((x + (w - tw) / 2, y + h / 2 - 24), title, font=font(19), fill=TEXT)
    dw = d.textlength(desc, font=font(14, bold=False))
    d.text((x + (w - dw) / 2, y + h / 2 + 4), desc, font=font(14, bold=False), fill=MUTED)


def arrow(x: int, y: int) -> None:
    d.text((x, y), "→", font=font(30), fill=ACCENT)

AY, AH = 208, 96
node(M, AY, 236, AH, "POS PWA / Admin", "React 19 · IndexedDB")
arrow(M + 244, AY + 30)
node(M + 282, AY, 200, AH, "gRPC Gateway", "OIDC · RBAC")
arrow(M + 490, AY + 30)

# 6サービスのグリッドボックス
SX, SW = M + 528, 320
d.rounded_rectangle([SX, AY - 8, SX + SW, AY + AH + 8], radius=12,
                    fill=(22, 36, 60), outline=BOX_BORDER, width=2)
services = ["sales", "inventory", "tenant", "customer", "analytics", "notification"]
cell_w, cell_h = (SW - 36) // 2, 26
for i, svc in enumerate(services):
    cx = SX + 14 + (i % 2) * (cell_w + 8)
    cy = AY + 2 + (i // 2) * (cell_h + 8)
    d.rounded_rectangle([cx, cy, cx + cell_w, cy + cell_h], radius=6, fill=(38, 62, 100))
    sw = d.textlength(svc, font=font(14, bold=False))
    d.text((cx + (cell_w - sw) / 2, cy + 5), svc, font=font(14, bold=False), fill=(207, 224, 245))

arrow(SX + SW + 10, AY + 30)

# インフラ2段
IX = SX + SW + 48
node(IX, AY - 8, 240, 50, "RabbitMQ", "event-driven")
node(IX, AY + 54, 240, 50, "PostgreSQL", "tenant isolation")

# ---- 技術バッジ ----
badges = ["Kotlin · Quarkus", "gRPC", "RabbitMQ", "React 19 · TypeScript", "Offline PWA", "Kubernetes"]
bx, by = M, 372
for label in badges:
    bw = d.textlength(label, font=font(18)) + 38
    d.rounded_rectangle([bx, by, bx + bw, by + 42], radius=21,
                        fill=(30, 50, 82), outline=(94, 148, 210), width=2)
    d.text((bx + 19, by + 10), label, font=font(18), fill=(188, 217, 255))
    bx += bw + 12

# ---- 品質ゲート行 ----
quality = "Quality gates:  CI · CodeQL · secret scanning · dependency audit · Playwright E2E"
d.text((M, 452), quality, font=font(17, bold=False), fill=(143, 168, 201))

# ---- フッターメトリクス ----
FY = 532
d.line([(M, FY - 26), (W - M, FY - 26)], fill=(45, 66, 100), width=1)
metrics = [("6", "microservices"), ("1,800+", "tests"), ("95%+", "coverage"), ("5", "ADRs")]
mx = M
for v, k in metrics:
    d.text((mx, FY), v, font=font(30), fill=ACCENT_L)
    vw = d.textlength(v, font=font(30))
    d.text((mx + vw + 10, FY + 11), k, font=font(16, bold=False), fill=MUTED)
    mx += vw + d.textlength(k, font=font(16, bold=False)) + 56

repo = "github.com/akaitigo/open-pos"
rw = d.textlength(repo, font=font(18, bold=False))
d.text((W - M - rw, FY + 6), repo, font=font(18, bold=False), fill=MUTED)

img.save(OUT, "PNG")
print(f"generated: {OUT} ({OUT.stat().st_size // 1024} KB)")
