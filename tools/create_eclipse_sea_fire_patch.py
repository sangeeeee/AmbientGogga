"""Build a separate, opt-in Eclipse shader pack; preserve the original pack and all user settings."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import argparse
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('source', type=Path)
parser.add_argument('--output', type=Path, default=ROOT / 'build/compat/Eclipse-Shader-SeaFire.zip')
args = parser.parse_args()
assert args.source.resolve() != args.output.resolve(), 'Never overwrite the source shader pack'
args.output.parent.mkdir(parents=True, exist_ok=True)
vertex_path = 'shaders/dimensions/all_particles.vsh'
water_path = 'shaders/dimensions/all_translucent.vsh'
helper = (ROOT / 'compat/eclipse/ambientgogga_sea_fire.glsl').read_bytes()
with ZipFile(args.source) as source:
    vertex = source.read(vertex_path).decode('utf-8')
    water = source.read(water_path).decode('utf-8')
    # Refuse to silently adapt an unrelated or already modified wave implementation.
    for expected in ['(pos.xz + frameTimeCounter * WATER_WAVE_SPEED)/125.0',
                     'min(WATER_WAVE_STRENGTH, 1.0) * range', 'getWave(gl_Vertex.xyz + cameraPosition, range)*0.6-0.5']:
        assert expected in water, f'Unsupported water implementation: {expected}'
    assert 'ambientgogga_sea_fire' not in vertex, 'Shader pack is already patched'
    anchor = 'void main() {'
    assert vertex.count(anchor) == 1
    vertex = vertex.replace(anchor, '#include "/lib/ambientgogga_sea_fire.glsl"\n\n' + anchor)
    light = 'vec2 lmcoord = gl_MultiTexCoord1.xy / 240.0;'
    assert vertex.count(light) == 1
    vertex = vertex.replace(light, '''// Decode only the explicitly reserved Sea Fire marker; ordinary particles are unchanged.
    ivec2 agRawLight = ivec2(round(gl_MultiTexCoord1.xy));
    bool agSeaFire = false;
    #if defined PARTICLES
        agSeaFire = all(equal(agRawLight & ivec2(15), ivec2(15)));
    #endif
    if (agSeaFire) agRawLight = agRawLight & ivec2(240);
    vec2 lmcoord = vec2(agRawLight) / 240.0;''')
    position = 'vec3 worldpos = mat3(gbufferModelViewInverse) * position + gbufferModelViewInverse[3].xyz;'
    assert vertex.count(position) == 2
    vertex = vertex.replace(position, position + '''
        #if defined PARTICLES && defined OVERWORLD_SHADER
            if (agSeaFire) worldpos.y += agSeaFireWaterOffset(worldpos);
        #endif''')
    with ZipFile(args.output, 'w', compression=ZIP_DEFLATED) as output:
        for entry in source.infolist():
            output.writestr(entry, vertex.encode('utf-8') if entry.filename == vertex_path else source.read(entry))
        output.writestr('shaders/lib/ambientgogga_sea_fire.glsl', helper)
        output.writestr('shaders/ambientgogga-sea-fire.properties',
                        '# Explicit opt-in for Ambient Gogga Sea Fire vertices.\nambientgogga.seaFireWaveProtocol=1\n')
settings = Path(str(args.source) + '.txt')
if settings.exists():
    Path(str(args.output) + '.txt').write_bytes(settings.read_bytes())
report = {'source': str(args.source), 'output': str(args.output),
          'source_sha256': hashlib.sha256(args.source.read_bytes()).hexdigest(),
          'water_vertex_sha256': hashlib.sha256(water.encode()).hexdigest(),
          'modified_entries': [vertex_path], 'added_entries': ['shaders/lib/ambientgogga_sea_fire.glsl',
                             'shaders/ambientgogga-sea-fire.properties'], 'user_settings_preserved': settings.exists()}
(args.output.parent / 'sea-fire-patch.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
