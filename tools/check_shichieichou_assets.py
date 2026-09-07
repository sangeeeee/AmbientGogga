"""Validate the pixel texture limits, single tail, particle sprite coverage and final JAR."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from collections import deque
import json
from PIL import Image

root = Path(__file__).resolve().parent.parent
resources = root / 'src/main/resources'
base = resources / 'assets/ambientgogga'
wing = Image.open(base / 'textures/entity/butterfly/shichieichou.png')
body = Image.open(base / 'textures/entity/butterfly/shichieichou_anatomy.png')
assert wing.size == (256, 256) and body.size == (64, 64)
assert wing.mode == body.mode == 'RGBA'
alpha = wing.getchannel('A')
assert alpha.getextrema() == (0, 255) and alpha.getpixel((0, 0)) == 0
assert len(wing.getcolors(256)) <= 49, 'Wing should use a restricted pixel-art palette'
assert len(body.getcolors(256)) <= 32, 'Body should use a restricted pixel-art palette'
assert min(body.convert('L').getdata()) > 185, 'Body material should be near-white, not dark blue'
assert alpha.getbbox()[0] >= int(0.288 * 256) and alpha.getbbox()[2] <= 0.79 * 256
# Count connected, visible components below the tail attachment; there must be one.
remaining = {(x, y) for y in range(175, 256) for x in range(256) if alpha.getpixel((x, y)) > 128}
components = []
while remaining:
    seed = remaining.pop()
    queue = deque([seed])
    size = 1
    while queue:
        x, y = queue.popleft()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                candidate = (x + dx, y + dy)
                if candidate in remaining:
                    remaining.remove(candidate)
                    queue.append(candidate)
                    size += 1
    components.append(size)
assert sum(size > 8 for size in components) == 1, components
definition = json.loads((base / 'particles/shichieichou_trail.json').read_text())
with ZipFile(root / 'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar') as vanilla:
    for name in definition['textures']:
        sprite = Image.open(BytesIO(vanilla.read('assets/minecraft/textures/particle/' + name.split(':')[1] + '.png'))).convert('RGBA')
        visible = sprite.crop((2, 1, 7, 7)).getchannel('A')
        assert sum(value > 128 for value in visible.getdata()) >= 8, 'Particle has too little visible coverage'
for path in base.rglob('*.json'):
    json.loads(path.read_text(encoding='utf8'))
with ZipFile(root / 'build/libs/ambientgogga-1.0.0.jar') as jar:
    assert not any(any(token in name for token in ('VisualCheck', 'AnimationTest', 'FlightCheck', 'FlightTest')) for name in jar.namelist()), 'Development checks leaked into final JAR'
    for path in base.rglob('*'):
        if path.is_file():
            assert jar.read(path.relative_to(resources).as_posix()) == path.read_bytes(), path
print('PASS: 256/64 textures, pixel palette, one tail, visible dust sprites, resource JSON and production JAR contents')
