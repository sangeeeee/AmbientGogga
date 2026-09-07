package com.sange.ambientgogga.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sange.ambientgogga.entity.Shichieichou;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import java.util.Map;
import java.util.WeakHashMap;

/** A flexible wing membrane and gravity-driven hanging tails, separate from ordinary butterflies. */
public final class ShichieichouModel extends EntityModel<Shichieichou> {
    private final Map<Shichieichou, ShichieichouPose> poses = new WeakHashMap<>();
    private final float[][] wings = new float[2][ShichieichouAnimation.ROWS * ShichieichouAnimation.COLUMNS * 3];
    private final Vector3f normal = new Vector3f();
    private float renderAlpha = 1.0F;
    private float bodyPitch;
    private float bodyRoll;

    @Override
    public void setupAnim(Shichieichou butterfly, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float phase = ShichieichouAnimation.phase(ageInTicks - 1, butterfly.getId());
        float speed = Mth.clamp((float) butterfly.getClientFlightMovement().length() * 7.0F, 0.0F, 1.0F);
        float turn = Mth.clamp(Mth.wrapDegrees(butterfly.yBodyRot - butterfly.yBodyRotO) / 25.0F, -1.0F, 1.0F);
        this.bodyPitch = ShichieichouAnimation.bodyPitch(phase, speed);
        this.bodyRoll = ShichieichouAnimation.bodyRoll(phase, turn);
        this.renderAlpha = butterfly.getClientFadeAlpha();
        this.poses.computeIfAbsent(butterfly, entity -> new ShichieichouPose()).sample(
                this.wings, ageInTicks, butterfly.getId(), speed, turn,
                butterfly.getX(), butterfly.getY(), butterfly.getZ(), butterfly.yBodyRot,
                Math.max(0.01F, butterfly.getSizeModifier() * butterfly.getScale()));
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        poseStack.pushPose();
        poseStack.translate(0, 1.5F, 0);
        int fadedColor = this.fadeColor(color);
        for (int side = 0; side < 2; side++) {
            float[] vertices = this.wings[side];
            for (int row = 0; row < ShichieichouAnimation.ROWS - 1; row++) {
                for (int column = 0; column < ShichieichouAnimation.SPAN_SEGMENTS; column++) {
                    int a = ShichieichouAnimation.index(row, column);
                    int b = ShichieichouAnimation.index(row, column + 1);
                    int c = ShichieichouAnimation.index(row + 1, column + 1);
                    int d = ShichieichouAnimation.index(row + 1, column);
                    float ux = vertices[b] - vertices[a];
                    float uy = vertices[b + 1] - vertices[a + 1];
                    float uz = vertices[b + 2] - vertices[a + 2];
                    float vx = vertices[d] - vertices[a];
                    float vy = vertices[d + 1] - vertices[a + 1];
                    float vz = vertices[d + 2] - vertices[a + 2];
                    this.normal.set(uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx).normalize();
                    if (side == 1) {
                        this.normal.negate();
                    }
                    // Mirroring reverses winding; keep the upper normals consistent.
                    this.vertex(poseStack, buffer, vertices, a, row, column, light, overlay, fadedColor);
                    if (side == 0) {
                        this.vertex(poseStack, buffer, vertices, b, row, column + 1, light, overlay, fadedColor);
                        this.vertex(poseStack, buffer, vertices, c, row + 1, column + 1, light, overlay, fadedColor);
                        this.vertex(poseStack, buffer, vertices, d, row + 1, column, light, overlay, fadedColor);
                    } else {
                        this.vertex(poseStack, buffer, vertices, d, row + 1, column, light, overlay, fadedColor);
                        this.vertex(poseStack, buffer, vertices, c, row + 1, column + 1, light, overlay, fadedColor);
                        this.vertex(poseStack, buffer, vertices, b, row, column + 1, light, overlay, fadedColor);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    private void vertex(PoseStack stack, VertexConsumer buffer, float[] vertices, int index,
                        int row, int column, int light, int overlay, int color) {
        float u = Mth.lerp((float) column / ShichieichouAnimation.SPAN_SEGMENTS,
                ShichieichouAnimation.U_MIN, ShichieichouAnimation.U_MAX);
        buffer.addVertex(stack.last(), vertices[index] / 16.0F, vertices[index + 1] / 16.0F, vertices[index + 2] / 16.0F)
                .setColor(color).setUv(u, ShichieichouAnimation.textureV(row))
                .setOverlay(overlay).setLight(light)
                .setNormal(stack.last(), this.normal.x, this.normal.y, this.normal.z);
    }

    public void renderAnatomy(PoseStack stack, VertexConsumer buffer, int light, int overlay, int color) {
        this.applyBodyPose(stack);
        float[] points = ShichieichouAnatomy.MESH.vertices();
        int[] colors = ShichieichouAnatomy.MESH.colors();
        for (int quad = 0; quad < points.length; quad += 20) {
            float ax = points[quad + 5] - points[quad], ay = points[quad + 6] - points[quad + 1], az = points[quad + 7] - points[quad + 2];
            float bx = points[quad + 15] - points[quad], by = points[quad + 16] - points[quad + 1], bz = points[quad + 17] - points[quad + 2];
            this.normal.set(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx).normalize();
            for (int i = quad; i < quad + 20; i += 5) {
                int tint = FastColor.ARGB32.multiply(colors[i / 5], color);
                buffer.addVertex(stack.last(), points[i] / 16, points[i + 1] / 16, points[i + 2] / 16)
                        .setColor(this.fadeColor(tint)).setUv(points[i + 3], points[i + 4])
                        .setLight(light).setOverlay(overlay)
                        .setNormal(stack.last(), this.normal.x, this.normal.y, this.normal.z);
            }
        }
        stack.popPose();
    }

    private void applyBodyPose(PoseStack stack) {
        stack.pushPose();
        stack.translate(0.0F, 1.5F, 0.0F);
        stack.mulPose(Axis.ZP.rotation(this.bodyRoll));
        stack.mulPose(Axis.XP.rotation(this.bodyPitch));
    }

    private int fadeColor(int color) {
        return FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(color) * this.renderAlpha), color);
    }

}
