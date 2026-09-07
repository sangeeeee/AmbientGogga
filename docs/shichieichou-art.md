# 七影蝶像素贴图制作记录

当前游戏资产：

- `src/main/resources/assets/ambientgogga/textures/entity/butterfly/shichieichou.png`：256×256 RGBA，48 色；一侧翅膀及一条长尾。
- `src/main/resources/assets/ambientgogga/textures/entity/butterfly/shichieichou_anatomy.png`：64×64 RGBA，32 色；全新身体/触须材质。

两项均使用内置 imagegen。用户已允许本地背景处理，并要求最高 256 的像素风纹理；导入脚本去掉纯黑底色、按最近邻缩放并限制调色板。此前 1024 翅膀和旧身体图集已被替换。

参考：用户提供的侧视概念图、原项目贴图以及 [Morphidae.gif](https://storage.moegirl.org.cn/moegirl/commons/d/d6/Morphidae.gif)。侧视图中的两条尾属于左右两侧；当前单侧纹理只有一条尾。

## 翅膀提示词

```text
Edit the provided butterfly artwork into a Minecraft PIXEL ART texture, guided closely by the second image (the user's original SIDE VIEW concept).
Crucial correction: the concept shows two overlapping sides. This texture must depict only ONE SIDE: ONE forewing, ONE small hindwing, and exactly ONE long thin tail attached to that hindwing. NEVER TWO TAILS in this asset.
Single right wing half, flat orthographic dorsal UV artwork, wing root at left center, wing spreads toward upper right, single ribbon tail descends from hindwing toward bottom. Preserve the reference's pale icy white/light cyan outlines, elegant lavender-to-blue teardrop membrane cells, tiny restrained white-blue sparkles. Pale and ethereal, with clearly visible blue and violet cells. No dense noisy photographic scale texture. No outer bloom.
Style: carefully hand-pixeled 128x128 sprite enlarged with nearest-neighbor pixels, readable stepped silhouettes and crisp clustered shading, NO smooth painting or fine line scratches. Will be imported as a 256x256 maximum game texture. Original shape is delicate and narrow rather than massive wide wings.
Composition: ONE joined wing half filling roughly x=24%..88%, y=4%..65%, root near (25%,42%). Exactly ONE slim tapering tail from about (63%,65%) to (68%,96%), with a slight natural curved shape and a tiny pointed tip. Single tail must be narrow after y=68%. The entire bottom zone contains ONLY this one tail. Empty margin around all edges.
Background: uniform pure black RGB(0,0,0) for local alpha extraction. NO checkerboard, no transparency simulation. No body, no antennae, no other wings, no extra detached pieces, no text.
```

## 身体材质提示词

```text
Asset type: Minecraft pixel-art butterfly body UV texture, to wrap around a small tapered 3D insect body.
Create a 64x64 pixel-art TEXTURE TILE enlarged with crisp square nearest-neighbor pixels. This is a rectangular material swatch, NOT an illustration of an insect.
Full canvas opaque, no background, no words, no wings or outlines.
Palette: deep desaturated navy and steel-blue base with icy cyan and lavender highlights. Arrange around 8 horizontal subtle segmented bands, separated by narrow darker blue bands, to suggest a segmented butterfly abdomen when wrapped onto an elongated rounded mesh. Center-left contains a soft vertical icy cyan highlight stripe; left and right canvas edges match seamlessly in dark blue. Each segment has a few blue-lavender pixel clusters indicating iridescent scales. Keep it readable and restrained, like elegant fantasy Minecraft insect pixel art. Body should be much darker than luminous pale wings. No photoreal detail, no smooth gradient, no brushed texture, no embossed box frame.
```

## 导入

```shell
python tools/prepare_shichieichou_texture.py WING.png src/main/resources/assets/ambientgogga/textures/entity/butterfly/shichieichou.png --body-source BODY.png
```
