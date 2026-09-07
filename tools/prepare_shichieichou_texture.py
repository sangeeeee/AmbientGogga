"""Import imagegen pixel artwork as small Minecraft textures (Pillow).

python tools/prepare_shichieichou_texture.py WING.png OUTPUT.png --body-source BODY.png
The generated wing uses a black matte. Local matte removal was authorized by the user.
"""
import argparse
from pathlib import Path
from PIL import Image, PngImagePlugin

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('source', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--body-source', type=Path)
args = parser.parse_args()
source = Image.open(args.source).convert('RGBA')
alpha = source.getchannel('A')
if alpha.getextrema() == (255, 255):
    alpha = source.convert('L').point(lambda value: 255 if value > 35 else 0)
source.putalpha(alpha)
source = source.resize((256, 256), Image.Resampling.NEAREST)
alpha = source.getchannel('A')
result = source.convert('RGB').quantize(colors=48, dither=Image.Dither.NONE).convert('RGBA')
result.putalpha(alpha)
metadata = PngImagePlugin.PngInfo()
metadata.add_text('Description', 'Shichieichou pixel wing: one tail per side; imagegen artwork with local matte removal')
args.output.parent.mkdir(parents=True, exist_ok=True)
result.save(args.output, pnginfo=metadata)
assert alpha.getextrema() == (0, 255)
assert alpha.getpixel((0, 0)) == 0
print(f'{args.output}: 256x256 RGBA, bounds {alpha.getbbox()}')
if args.body_source:
    body = Image.open(args.body_source).convert('RGB').resize((64, 64), Image.Resampling.NEAREST)
    body = body.quantize(colors=32, dither=Image.Dither.NONE).convert('RGBA')
    body_path = args.output.with_name('shichieichou_anatomy.png')
    body.save(body_path)
    print(f'{body_path}: 64x64 pixel material')
