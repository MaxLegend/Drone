// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class DroneModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "dronemodel"), "main");
	private final ModelPart drone;
	private final ModelPart body;
	private final ModelPart prop1;
	private final ModelPart prop2;
	private final ModelPart prop3;
	private final ModelPart prop4;
	private final ModelPart cam;

	public DroneModel(ModelPart root) {
		this.drone = root.getChild("drone");
		this.body = this.drone.getChild("body");
		this.prop1 = this.drone.getChild("prop1");
		this.prop2 = this.drone.getChild("prop2");
		this.prop3 = this.drone.getChild("prop3");
		this.prop4 = this.drone.getChild("prop4");
		this.cam = this.drone.getChild("cam");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition drone = partdefinition.addOrReplaceChild("drone", CubeListBuilder.create(), PartPose.offset(0.0F, 22.0F, 0.0F));

		PartDefinition body = drone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-2.7273F, -2.0063F, -4.1413F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(8, 21).addBox(1.2727F, -0.0063F, -4.1413F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 21).addBox(-0.7273F, -0.0063F, -4.1413F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.7273F, 0.2563F, -1.8587F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 11).addBox(-7.75F, 0.0F, 1.3F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(22, 0).addBox(7.975F, -1.0F, -0.8F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(14, 13).addBox(2.9F, -2.0F, -0.8F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7273F, 0.4937F, 1.8587F, 0.0F, 0.6109F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 9).addBox(3.5F, 0.75F, 0.75F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(20, 21).addBox(-8.1F, -0.25F, -1.425F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(-8.0F, -1.25F, -1.4F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7273F, -0.2563F, 1.8587F, 0.0F, -0.6109F, 0.0F));

		PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(3, 4).addBox(-2.0F, -1.25F, 1.0F, 5.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -1.25F, -5.0F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7273F, -0.2563F, 2.8587F, -0.1309F, 0.0F, 0.0F));

		PartDefinition prop1 = drone.addOrReplaceChild("prop1", CubeListBuilder.create().texOffs(8, 17).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.75F, 0.8F, 5.5F));

		PartDefinition prop2 = drone.addOrReplaceChild("prop2", CubeListBuilder.create().texOffs(18, 17).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(5.75F, 0.8F, 5.5F));

		PartDefinition prop3 = drone.addOrReplaceChild("prop3", CubeListBuilder.create().texOffs(18, 19).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(6.5F, -1.2F, -5.0F));

		PartDefinition prop4 = drone.addOrReplaceChild("prop4", CubeListBuilder.create().texOffs(8, 19).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.5F, -1.2F, -5.0F));

		PartDefinition cam = drone.addOrReplaceChild("cam", CubeListBuilder.create(), PartPose.offset(0.5F, 0.75F, -5.75F));

		PartDefinition cam_r1 = cam.addOrReplaceChild("cam_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -2.6F, -6.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.85F, 5.75F, 0.4363F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		drone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}