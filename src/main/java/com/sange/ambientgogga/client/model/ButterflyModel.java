package com.sange.ambientgogga.client.model;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.Butterfly;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ButterflyModel extends HierarchicalModel<Butterfly> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AmbientGogga.MODID, "butterfly"),
            "main"
    );

    private static final float MIN_WING_ANGLE = (float) (Math.PI / 5.0D);
    private static final float MAX_WING_ANGLE = (float) (Math.PI * 4.0D / 5.0D);
    private static final float FOLDED_WING_ANGLE = 0.05F;

    private final ModelPart root;
    private final ModelPart group;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public ButterflyModel(ModelPart root) {
        this.root = root;
        this.group = root.getChild("group");
        ModelPart body = this.group.getChild("body");
        this.leftWing = body.getChild("leftWing");
        this.rightWing = body.getChild("rightWing");
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(
            Butterfly butterfly,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        this.group.xRot = butterfly.isResting() ? 0.0F : -0.2618F;
        float flyingWingAngle = Mth.lerp(
                butterfly.getWingRotation(ageInTicks),
                MIN_WING_ANGLE,
                MAX_WING_ANGLE
        );
        float wingAngle = Mth.lerp(
                butterfly.getWingFoldProgress(ageInTicks),
                flyingWingAngle,
                FOLDED_WING_ANGLE
        );
        this.leftWing.zRot = wingAngle;
        this.rightWing.zRot = -wingAngle;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition group = root.addOrReplaceChild("group", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = group.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -2.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "antennae",
                CubeListBuilder.create()
                        .texOffs(29, 12)
                        .addBox(-7.0F, -4.0F, 0.0F, 7.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -1.0F, -2.0F, 1.0472F, 0.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "leftWing",
                CubeListBuilder.create()
                        .texOffs(8, 11)
                        .addBox(0.0F, -7.0F, 0.0F, 0.0F, 13.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 2.0F, 1.5708F, 0.0F, 0.7854F)
        );
        body.addOrReplaceChild(
                "rightWing",
                CubeListBuilder.create()
                        .texOffs(8, 11)
                        .mirror()
                        .addBox(0.0F, -7.0F, 0.0F, 0.0F, 13.0F, 8.0F),
                PartPose.offsetAndRotation(-1.0F, -0.5F, 2.0F, 1.5708F, 0.0F, -0.7854F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }
}
