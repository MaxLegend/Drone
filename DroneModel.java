// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class DroneModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "dronemodel"), "main");
	private final ModelPart legs;
	private final ModelPart body;
	private final ModelPart prop1;
	private final ModelPart prop2;
	private final ModelPart prop3;
	private final ModelPart prop4;
	private final ModelPart cam;

	public DroneModel(ModelPart root) {
		this.legs = root.getChild("legs");
		this.body = root.getChild("body");
		this.prop1 = root.getChild("prop1");
		this.prop2 = root.getChild("prop2");
		this.prop3 = root.getChild("prop3");
		this.prop4 = root.getChild("prop4");
		this.cam = root.getChild("cam");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(-4.5F, 22.75F, 3.75F));

		PartDefinition cube_r1 = legs.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 11).addBox(-1.5F, 0.0F, 0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.6109F, 0.0F));

		PartDefinition cube_r2 = legs.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 9).addBox(-3.5F, 0.0F, 0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, 0.0F, 0.0F, 0.0F, -0.6109F, 0.0F));

		PartDefinition cube_r3 = legs.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(20, 21).addBox(-2.5F, 0.0F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(-2.5F, -1.0F, -1.5F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -7.25F, 0.0F, -0.6109F, 0.0F));

		PartDefinition cube_r4 = legs.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(22, 0).addBox(1.5F, 1.0F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(14, 13).addBox(-3.5F, 0.0F, -1.5F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, -2.0F, -7.25F, 0.0F, 0.6109F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-2.5F, -2.5F, -0.25F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(14, 21).addBox(-2.5F, -0.5F, -0.25F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(8, 21).addBox(1.5F, -0.5F, -0.25F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 21).addBox(-1.5F, -0.5F, -0.25F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 22.75F, -5.75F));

		PartDefinition cube_r5 = body.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, -1.5F, 1.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -1.5F, -5.0F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.5F, 6.75F, -0.1309F, 0.0F, 0.0F));

		PartDefinition prop1 = partdefinition.addOrReplaceChild("prop1", CubeListBuilder.create().texOffs(8, 17).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.75F, 22.8F, 5.5F));

		PartDefinition prop2 = partdefinition.addOrReplaceChild("prop2", CubeListBuilder.create().texOffs(18, 17).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(5.75F, 22.8F, 5.5F));

		PartDefinition prop3 = partdefinition.addOrReplaceChild("prop3", CubeListBuilder.create().texOffs(18, 19).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(6.5F, 20.8F, -5.0F));

		PartDefinition prop4 = partdefinition.addOrReplaceChild("prop4", CubeListBuilder.create().texOffs(8, 19).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.5F, 20.8F, -5.0F));

		PartDefinition cam = partdefinition.addOrReplaceChild("cam", CubeListBuilder.create(), PartPose.offset(0.5F, 22.75F, -5.75F));

		PartDefinition cam_r1 = cam.addOrReplaceChild("cam_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		legs.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		prop1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		prop2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		prop3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		prop4.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		cam.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}