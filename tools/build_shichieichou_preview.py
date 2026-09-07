"""Build the offline preview from the compiled Java model. Run gradlew classes first."""
from pathlib import Path
import base64
import os
import subprocess

root = Path(__file__).resolve().parent.parent
output = root / 'build/preview'
classes = root / 'build/classes/java/main'
output.mkdir(parents=True, exist_ok=True)
subprocess.run(['javac', '-cp', str(classes), '-d', str(output),
                str(root / 'tools/ExportShichieichouPreview.java')], check=True)
subprocess.run(['java', '-cp', os.pathsep.join([str(classes), str(output)]),
                'ExportShichieichouPreview', str(output / 'mesh.json')], check=True)
page = (root / 'tools/shichieichou-preview.html').read_text(encoding='utf8')
page = page.replace('__MESH_DATA__', (output / 'mesh.json').read_text())
textures = root / 'src/main/resources/assets/ambientgogga/textures/entity/butterfly'
for token, filename in [('__TEXTURE_DATA__', 'shichieichou.png'),
                        ('__BODY_TEXTURE_DATA__', 'shichieichou_anatomy.png')]:
    page = page.replace(token, 'data:image/png;base64,' + base64.b64encode((textures / filename).read_bytes()).decode())
(output / 'shichieichou.html').write_text(page, encoding='utf8')
print(output / 'shichieichou.html')
