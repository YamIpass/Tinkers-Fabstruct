"""Renders the promo video for Tinkers' Fabstruct.

Frames are composed with Pillow and piped into ffmpeg; the narration comes from the System.Speech
wav files next to this script (see tts_gen.ps1), one clip per line of script.json.
"""

from __future__ import annotations

import json
import math
import pathlib
import subprocess
import wave

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent
GAME = ROOT.parent.parent
CAPTURE = GAME / "run" / "capture"
W, H = 1920, 1080
FPS = 30
GAP = 0.45

FFMPEG = pathlib.Path(r"D:/ACLOS/Cross/recorder-release/ffmpeg.exe")
FONT_BOLD = "C:/Windows/Fonts/msyhbd.ttc"
FONT_REG = "C:/Windows/Fonts/msyh.ttc"
FONT_CODE = "C:/Windows/Fonts/consola.ttf"

GOLD = (226, 170, 66)
CREAM = (240, 232, 216)
MUTED = (196, 186, 170)
SEARED = (206, 92, 58)
SLIME = (150, 205, 78)
INK = (13, 11, 9)


def font(size: int, bold: bool = False, code: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_CODE if code else (FONT_BOLD if bold else FONT_REG), size)


def wav_duration(path: pathlib.Path) -> float:
    with wave.open(str(path), "rb") as handle:
        return handle.getnframes() / float(handle.getframerate())


def cover(image: Image.Image, width: int, height: int) -> Image.Image:
    scale = max(width / image.width, height / image.height)
    resized = image.resize((int(image.width * scale + 0.5), int(image.height * scale + 0.5)), Image.LANCZOS)
    left = (resized.width - width) // 2
    top = (resized.height - height) // 2
    return resized.crop((left, top, left + width, top + height))


def ken_burns(image: Image.Image, progress: float, zoom: float = 0.12, pan: float = 0.05) -> Image.Image:
    scale = (1.0 + zoom * progress) * max(W / image.width, H / image.height)
    width = int(image.width * scale)
    height = int(image.height * scale)
    resized = image.resize((width, height), Image.LANCZOS)
    max_x = max(width - W, 0)
    max_y = max(height - H, 0)
    left = int(max_x * (0.5 + pan * (progress - 0.5)))
    top = int(max_y * (0.5 - pan * 0.4 * (progress - 0.5)))
    return resized.crop((left, top, left + W, top + H))


def darken(image: Image.Image, amount: float) -> Image.Image:
    return Image.blend(image, Image.new("RGB", image.size, (7, 6, 5)), amount)


def backdrop() -> Image.Image:
    base = Image.new("RGB", (W, H), INK)
    draw = ImageDraw.Draw(base)
    for y in range(H):
        ratio = y / H
        draw.line([(0, y), (W, y)], fill=(int(24 - 12 * ratio), int(20 - 10 * ratio), int(16 - 8 * ratio)))
    return base


def vignette(image: Image.Image) -> Image.Image:
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).ellipse((-W * 0.28, -H * 0.38, W * 1.28, H * 1.38), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(130))
    return Image.composite(image, Image.new("RGB", (W, H), (0, 0, 0)), mask)


def text(draw, xy, value, fnt, fill, stroke=3, shadow=True):
    x, y = xy
    if shadow:
        draw.text((x + 3, y + 4), value, font=fnt, fill=(0, 0, 0))
    draw.text((x, y), value, font=fnt, fill=fill, stroke_width=stroke, stroke_fill=(0, 0, 0))


def centered(draw, y, value, fnt, fill, stroke=3):
    width = draw.textlength(value, font=fnt)
    text(draw, ((W - width) / 2, y), value, fnt, fill, stroke)


def wrap(value: str, limit: int) -> list[str]:
    """Greedy wrap that measures CJK per character and ascii per word."""
    tokens, buffer = [], ""
    for char in value:
        if char.isascii() and char not in " ,.;:!?":
            buffer += char
            continue
        if buffer:
            tokens.append(buffer)
            buffer = ""
        tokens.append(char)
    if buffer:
        tokens.append(buffer)
    lines, current = [], ""
    for token in tokens:
        if len(current) + len(token) > limit and current.strip():
            lines.append(current.strip())
            current = token.lstrip()
        else:
            current += token
    if current.strip():
        lines.append(current.strip())
    return lines


def subtitle(image: Image.Image, narration: str) -> Image.Image:
    fnt = font(44)
    lines = wrap(narration, 27)
    line_height = 58
    block = line_height * len(lines)
    top = H - block - 58
    # soft gradient instead of a hard black bar, so the footage keeps breathing
    fade = Image.new("RGBA", (W, block + 118), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fade)
    height = block + 118
    for row in range(height):
        alpha = int(196 * (row / height) ** 1.6)
        fd.line([(0, row), (W, row)], fill=(5, 4, 3, alpha))
    image.paste(fade, (0, top - 60), fade)
    draw = ImageDraw.Draw(image)
    for index, line in enumerate(lines):
        width = draw.textlength(line, font=fnt)
        text(draw, ((W - width) / 2, top + index * line_height), line, fnt, CREAM, 2, False)
    return image


def missing_texture(size: int = 200) -> Image.Image:
    tile = Image.new("RGB", (16, 16), (0, 0, 0))
    draw = ImageDraw.Draw(tile)
    draw.rectangle((0, 0, 7, 7), fill=(248, 0, 248))
    draw.rectangle((8, 8, 15, 15), fill=(248, 0, 248))
    block = Image.new("RGB", (size, size))
    step = max(size // 8, 16)
    scaled = tile.resize((step, step), Image.NEAREST)
    for y in range(0, size, step):
        for x in range(0, size, step):
            block.paste(scaled, (x, y))
    return block


_CACHE: dict[str, Image.Image] = {}


def load(name: str) -> Image.Image:
    if name not in _CACHE:
        _CACHE[name] = Image.open(CAPTURE / f"{name}.png").convert("RGB")
    return _CACHE[name]


def icon() -> Image.Image:
    if "icon" not in _CACHE:
        raw = Image.open(GAME / "docs" / "modrinth" / "icon.png").convert("RGBA")
        _CACHE["icon"] = raw.crop(raw.getbbox())
    return _CACHE["icon"]


def panel(image: Image.Image, lines, xy, width=1520, highlight=(), size=33) -> Image.Image:
    fnt = font(size, code=True)
    line_height = 50
    height = line_height * len(lines) + 52
    sheet = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)
    draw.rounded_rectangle((0, 0, width, height), radius=16, fill=(11, 11, 13, 236), outline=(86, 86, 96, 190), width=2)
    for index, line in enumerate(lines):
        color = (236, 186, 120) if index in highlight else (205, 212, 222)
        draw.text((30, 24 + index * line_height), line, font=fnt, fill=color)
    image.paste(sheet, xy, sheet)
    return image


def badge(image: Image.Image, label: str, xy, color=GOLD, size=36) -> tuple[Image.Image, int]:
    draw = ImageDraw.Draw(image, "RGBA")
    fnt = font(size, bold=True)
    width = int(draw.textlength(label, font=fnt) + 46)
    x, y = xy
    draw.rounded_rectangle((x, y, x + width, y + size + 26), radius=(size + 26) / 2, fill=(*color, 40), outline=(*color, 230), width=3)
    draw.text((x + 23, y + 11), label, font=fnt, fill=color)
    return image, width


def scene_title(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(ken_burns(load("01_overview"), progress, 0.05, 0.0), 0.72))
    logo = icon()
    size = int(290 + 12 * math.sin(progress * math.pi))
    scaled = logo.resize((size, size), Image.LANCZOS)
    glow = Image.new("RGBA", (size + 120, size + 120), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((0, 0, size + 120, size + 120), fill=(*GOLD, 46))
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    x = (W - scaled.width) // 2
    y = 120
    image.paste(glow, (x - 60, y - 60), glow)
    image.paste(scaled, (x, y), scaled)
    draw = ImageDraw.Draw(image)
    centered(draw, y + size + 34, "匠魂织布", font(104, bold=True), CREAM)
    centered(draw, y + size + 158, "Tinkers' Fabstruct", font(62, bold=True), GOLD)
    centered(draw, y + size + 244, "Tinkers' Construct 3.12.1  →  Minecraft 1.20.1 Fabric", font(38), MUTED)
    return subtitle(image, narration)


def scene_problem(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(ken_burns(load("07_smeltery_close"), progress, 0.10), 0.76))
    draw = ImageDraw.Draw(image)
    centered(draw, 92, "经典，却只有 Forge", font(86, bold=True), CREAM)
    lines = ["Forge 版本一路从 1.7.10 走到 1.20.1", "Fabric 能选的移植，版本老旧、bug 不少"]
    for index, line in enumerate(lines):
        centered(draw, 262 + index * 74, line, font(48), MUTED, 2)
    tile = missing_texture(230)
    image.paste(tile, (W // 2 - 115, 440))
    centered(draw, 700, "材质与模型缺失，是移植路上的第一道墙", font(42), (226, 150, 226), 2)
    return subtitle(image, narration)


def scene_port(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(backdrop(), 0.1))
    draw = ImageDraw.Draw(image)
    centered(draw, 110, "一个类一个类地搬", font(88, bold=True), CREAM)
    centered(draw, 236, "不是重做，是 1:1 源码搬运 + 加载器接口适配", font(46), GOLD, 2)
    boxes = [
        (220, 360, "Tinkers' Construct\n3.12.1 Forge 源码", MUTED),
        (760, 360, "Mantle 1.11\nFabric 版", MUTED),
        (1300, 360, "Fabric 1.20.1\nTinkers' Fabstruct", SLIME),
    ]
    for index, (x, y, label, color) in enumerate(boxes):
        box = Image.new("RGBA", (400, 220), (0, 0, 0, 0))
        d2 = ImageDraw.Draw(box)
        d2.rounded_rectangle((0, 0, 400, 220), radius=18, fill=(20, 17, 14, 215), outline=(*color, 150), width=3)
        for li, part in enumerate(label.split("\n")):
            wd = d2.textlength(part, font=font(36))
            d2.text(((400 - wd) / 2, 66 + li * 52), part, font=font(36), fill=color)
        image.paste(box, (x, y), box)
        if index < 2:
            draw.line([(x + 410, y + 110), (x + 520, y + 110)], fill=GOLD, width=6)
            draw.polygon([(x + 520, y + 110), (x + 486, y + 90), (x + 486, y + 130)], fill=GOLD)
    centered(draw, 650, "Porting Lib 2.3.16 补齐 Forge 与 Fabric 之间的接口", font(42), MUTED, 2)
    centered(draw, 730, "34 个依赖全部内嵌，玩家只需装 Fabric API", font(42), MUTED, 2)
    return subtitle(image, narration)


def scene_compile(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(ken_burns(load("01_overview"), progress, 0.10, 0.04), 0.74))
    draw = ImageDraw.Draw(image)
    centered(draw, 84, "把 7,516 个错误磨到 0", font(80, bold=True), CREAM)
    stats = [("1,778", "Java 文件"), ("165,963", "行源码"), ("7,516", "编译错误 → 0"), ("60+", "服务端自测")]
    for index, (value, label) in enumerate(stats):
        x = 130 + index * 430
        box = Image.new("RGBA", (390, 200), (0, 0, 0, 0))
        d2 = ImageDraw.Draw(box)
        d2.rounded_rectangle((0, 0, 390, 200), radius=20, fill=(16, 14, 11, 216), outline=(*GOLD, 120), width=3)
        wd = d2.textlength(value, font=font(74, bold=True))
        d2.text(((390 - wd) / 2, 30), value, font=font(74, bold=True), fill=GOLD)
        wl = d2.textlength(label, font=font(36))
        d2.text(((390 - wl) / 2, 128), label, font=font(36), fill=CREAM)
        image.paste(box, (x, 300), box)
    centered(draw, 600, "一个包一个包地三方合并，改到能编、能跑、能自测", font(44), MUTED, 2)
    centered(draw, 686, "存档、数据包、mod id 全部沿用 tconstruct", font(42), MUTED, 2)
    return subtitle(image, narration)


def scene_models(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(ken_burns(load("04_stations"), progress, 0.06), 0.80))
    draw = ImageDraw.Draw(image)
    centered(draw, 74, "难点 ①  Forge 自定义模型加载器", font(70, bold=True), CREAM)
    tile = missing_texture(190)
    image.paste(tile, (300, 250))
    text(draw, (300, 470), "移植后：满屏黑紫方块", font(38), (250, 128, 246), 2)
    after = cover(load("04_stations"), 520, 300)
    image.paste(after, (1100, 240))
    text(draw, (1100, 566), "修复后：部件台 · 工作台 · 冶炼炉", font(36), SLIME, 2)
    draw.line([(520, 400), (1080, 400)], fill=GOLD, width=6)
    draw.polygon([(1080, 400), (1040, 378), (1040, 422)], fill=GOLD)
    centered(draw, 680, "把 tconstruct / mantle 的自定义 loader 逐个接到 Fabric 资源管线", font(42), CREAM, 2)
    centered(draw, 758, "1,549 个模型 · 13,888 张贴图 · 435 个 blockstate 全部重新烘焙", font(38), MUTED, 2)
    return subtitle(image, narration)


def scene_fluid(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(backdrop(), 0.05))
    draw = ImageDraw.Draw(image)
    centered(draw, 84, "难点 ②  流体单位不兼容", font(72, bold=True), CREAM)
    left = ["Fabric transfer API", "1 bucket = 81000 droplet", "1 ingot  = 90 droplet"]
    right = ["匠魂内部（Forge 习惯）", "1 bucket = 81000 mB", "1 ingot  = 90 mB"]
    image = panel(image, left, (150, 250), 780, (), 32)
    image = panel(image, right, (1010, 250), 780, (), 32)
    draw = ImageDraw.Draw(image)
    draw.line([(946, 372), (996, 372)], fill=GOLD, width=6)
    centered(draw, 560, "换算层全部重接，否则每条熔化、浇铸、合金配方都会差 81 倍", font(42), CREAM, 2)
    centered(draw, 640, "顺带把流体着色器、流体贴图与光照明暗重新注册", font(40), MUTED, 2)
    return subtitle(image, narration)


def scene_transaction(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(backdrop(), 0.05))
    draw = ImageDraw.Draw(image)
    centered(draw, 84, "难点 ③  事务回调顺序反了", font(72, bold=True), CREAM)
    lines = [
        "tx.addOuterCloseCallback(result -> onContentsChanged());   // 先跑：冷却计时 = -1",
        "tx.addOuterCloseCallback(result -> currentRecipe = recipe); // 后跑：配方才写进去",
    ]
    image = panel(image, lines, (170, 250), 1580, (0, 1), 31)
    draw = ImageDraw.Draw(image)
    centered(draw, 420, "Fabric 的关闭回调后进先出，Forge 是先进先出", font(42), SEARED, 2)
    centered(draw, 500, "结果是铸造台永远不产出产物，玩家只看到一汪凝住的岩浆", font(42), CREAM, 2)
    centered(draw, 592, "修法：配方写入之后，主动重算一次冷却计时", font(44), SLIME, 2)
    centered(draw, 668, "自测断言：90 mB 熔铁 → 1 个铁锭", font(38), MUTED, 2)
    return subtitle(image, narration)


def scene_reveal(progress: float, narration: str) -> Image.Image:
    image = vignette(ken_burns(load("02_smeltery"), progress, 0.20, 0.06))
    draw = ImageDraw.Draw(image)
    text(draw, (90, 70), "它动起来了", font(76, bold=True), CREAM, 4)
    text(draw, (92, 168), "冶炼炉 · 铸造炉 · 熔化炉 · 龙头 · 排液口", font(38), MUTED, 2)
    return subtitle(image, narration)


def scene_stations(progress: float, narration: str) -> Image.Image:
    image = vignette(ken_burns(load("04_stations"), progress, 0.18, 0.05))
    draw = ImageDraw.Draw(image)
    text(draw, (90, 70), "十三种工具 · 二十四种部件", font(66, bold=True), CREAM, 4)
    return subtitle(image, narration)


def scene_casting(progress: float, narration: str) -> Image.Image:
    image = vignette(ken_burns(load("05_casting"), progress, 0.18, 0.04))
    draw = ImageDraw.Draw(image)
    text(draw, (90, 70), "浇铸台 · 龙头 · 强化系统", font(66, bold=True), CREAM, 4)
    return subtitle(image, narration)


def scene_release(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(backdrop(), 0.05))
    draw = ImageDraw.Draw(image)
    centered(draw, 96, "已经上线", font(92, bold=True), CREAM)
    image, width = badge(image, "GitHub", (300, 268), CREAM, 38)
    draw = ImageDraw.Draw(image)
    draw.text((300 + width + 36, 278), "github.com/YamIpass/Tinkers-Fabstruct", font=font(44, code=True), fill=CREAM)
    image, width = badge(image, "Modrinth", (300, 388), SLIME, 38)
    draw = ImageDraw.Draw(image)
    draw.text((300 + width + 36, 398), "modrinth.com/mod/tinkers-fabstruct", font=font(44, code=True), fill=SLIME)
    text(draw, (300, 540), "中文名：匠魂织布", font(52, bold=True), GOLD, 3)
    text(draw, (302, 620), "Minecraft 1.20.1 · Fabric Loader 0.20.1+ · 需要 Fabric API", font(36), MUTED, 2)
    shot = cover(load("04_stations"), 620, 260)
    image.paste(shot, (1180, 250))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((1174, 244, 1174 + 632, 256), radius=6, fill=GOLD)
    return subtitle(image, narration)


def scene_credits(progress: float, narration: str) -> Image.Image:
    image = vignette(darken(backdrop(), 0.1))
    draw = ImageDraw.Draw(image)
    centered(draw, 104, "致谢与声明", font(78, bold=True), CREAM)
    lines = [
        "基于 MIT 许可的 Tinkers' Construct（© SlimeKnights）与 Mantle 移植",
        "保留全部版权声明，许可全文见 jar 内 META-INF/LICENSE 与 NOTICE",
        "Porting Lib（LGPL-3.0）、Fabric API 与其余依赖原样内嵌",
    ]
    for index, line in enumerate(lines):
        centered(draw, 268 + index * 84, line, font(42), CREAM, 2)
    centered(draw, 552, "非官方作品，与 SlimeKnights 没有隶属或背书关系", font(46, bold=True), (250, 202, 120), 2)
    logo = icon().resize((140, 140), Image.LANCZOS)
    image.paste(logo, ((W - 140) // 2, 636), logo)
    centered(draw, 822, "MIT License", font(46, bold=True), GOLD)
    return image


def write_music(seconds: float) -> pathlib.Path:
    """Slow synth pad so the narration is not sitting on silence."""
    import numpy as np
    import wave as wave_module

    rate = 48000
    count = int(seconds * rate)
    time = np.arange(count) / rate
    # A minor pad: A2, E3, A3, C4 with a little detune for width
    voices = [(110.0, 0.30), (164.81, 0.22), (220.0, 0.20), (261.63, 0.14)]
    signal = np.zeros(count, dtype=np.float64)
    for frequency, weight in voices:
        for detune in (-0.15, 0.0, 0.15):
            phase = 2 * np.pi * (frequency + detune) * time
            signal += weight / 3.0 * (np.sin(phase) + 0.35 * np.sin(2 * phase) + 0.12 * np.sin(3 * phase))
    breathe = 0.72 + 0.28 * np.sin(2 * np.pi * time / 11.0)
    signal *= breathe
    fade = np.minimum(1.0, np.minimum(time / 3.0, (seconds - time) / 4.0))
    signal *= np.clip(fade, 0.0, 1.0)
    signal *= 0.16 / max(np.max(np.abs(signal)), 1e-6)
    frames = (np.stack([signal, signal * 0.96], axis=1) * 32767).astype("<i2")
    path = ROOT / "music.wav"
    with wave_module.open(str(path), "wb") as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(rate)
        handle.writeframes(frames.tobytes())
    return path


def mix_audio(count: int) -> tuple[list[float], float]:
    durations = [wav_duration(ROOT / f"{index:02d}.wav") for index in range(count)]
    inputs, filters, concat = [], [], []
    offset = 0.0
    for index, duration in enumerate(durations):
        inputs += ["-i", str(ROOT / f"{index:02d}.wav")]
        delay = int((offset + (GAP if index else 0.35)) * 1000)
        filters.append(f"[{index}:a]aresample=48000,adelay={delay}|{delay}[a{index}]")
        concat.append(f"[a{index}]")
        offset += duration + GAP
    filters.append("".join(concat) + f"amix=inputs={count}:duration=longest:normalize=0[voice]")
    filters.append(f"[voice]apad=pad_dur=3[voicepad]")
    result = subprocess.run(
        [str(FFMPEG), "-hide_banner", "-loglevel", "error", *inputs,
         "-i", str(write_music(offset + 3.0)),
         "-filter_complex", ";".join(filters + [f"[{count}:a]volume=0.55,atrim=0:{offset + 3.0}[music]",
                                                "[voicepad][music]amix=inputs=2:duration=longest:normalize=0[aout]"]),
         "-map", "[aout]", "-ac", "2", "-ar", "48000", str(ROOT / "voice_mix.wav"), "-y"],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(result.stderr[-2000:])
        raise SystemExit("audio mix failed")
    return durations, offset + 0.9


def main() -> None:
    lines = json.loads((ROOT / "script.json").read_text(encoding="utf-8"))
    durations, total = mix_audio(len(lines))
    scenes = [
        scene_title,        # 匠魂是经典老牌模组
        scene_problem,      # 可惜只有 Forge
        scene_port,         # 一个类一个类地搬
        scene_compile,      # 一千七百多个文件，七千多个错误
        scene_models,       # 难点一：模型加载器
        scene_fluid,        # 难点二：流体单位
        scene_transaction,  # 难点三：事务回调
        scene_reveal,       # 填平之后动起来了
        scene_stations,     # 十三种工具、二十四种部件
        scene_release,      # 已上线 GitHub 与 Modrinth
        scene_credits,      # 致谢与声明
    ]
    assert len(scenes) == len(lines), f"{len(scenes)} scenes for {len(lines)} lines"
    starts, cursor = [], 0.0
    for duration in durations:
        starts.append(cursor)
        cursor += duration + GAP
    total_frames = int(total * FPS)
    output = ROOT / "Tinkers-Fabstruct-promo.mp4"
    command = [
        str(FFMPEG), "-hide_banner", "-loglevel", "error", "-y",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
        "-i", str(ROOT / "voice_mix.wav"),
        "-c:v", "h264_nvenc", "-preset", "p5", "-rc", "vbr", "-cq", "20", "-b:v", "0",
        "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart",
        str(output),
    ]
    log = (ROOT / "ffmpeg.log").open("wb")
    process = subprocess.Popen(command, stdin=subprocess.PIPE, stderr=log)
    assert process.stdin is not None
    for frame in range(total_frames):
        time = frame / FPS
        index = 0
        for position, start in enumerate(starts):
            if time >= start:
                index = position
        index = min(index, len(scenes) - 1)
        local = min(max((time - starts[index]) / max(durations[index], 0.001), 0.0), 1.0)
        image = scenes[index](local, lines[index])
        edge = 6 / FPS
        into = time - starts[index]
        remaining = durations[index] - into
        fade = min(into / edge, remaining / edge, 1.0)
        if fade < 1.0:
            image = Image.blend(Image.new("RGB", (W, H), (0, 0, 0)), image, max(fade, 0.0))
        try:
            process.stdin.write(image.convert("RGB").tobytes())
        except OSError:
            log.flush()
            print((ROOT / "ffmpeg.log").read_text(errors="replace")[-3000:])
            raise
        if frame % 300 == 0:
            print(f"frame {frame}/{total_frames}", flush=True)
    process.stdin.close()
    process.wait()
    print("wrote", output, output.stat().st_size, "bytes", flush=True)


if __name__ == "__main__":
    main()
