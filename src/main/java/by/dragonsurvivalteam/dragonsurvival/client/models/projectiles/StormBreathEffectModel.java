package by.dragonsurvivalteam.dragonsurvival.client.models.projectiles;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.common.entity.projectiles.StormBreathEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// FIXME :: Unused?
public class StormBreathEffectModel extends GeoModel<StormBreathEntity> {
	private ResourceLocation currentTexture = new ResourceLocation(DragonSurvivalMod.MODID, "textures/entity/storms_breath.png");

	@Override
	public ResourceLocation getModelResource(final StormBreathEntity entity) {
		return new ResourceLocation(DragonSurvivalMod.MODID, "geo/storms_breath.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(final StormBreathEntity entity) {
		return currentTexture;
	}

	public void setCurrentTexture(final ResourceLocation currentTexture) {
		this.currentTexture = currentTexture;
	}

	@Override
	public ResourceLocation getAnimationResource(final StormBreathEntity entity) {
		return new ResourceLocation(DragonSurvivalMod.MODID, "animations/storms_breath.animations.json");
	}
}