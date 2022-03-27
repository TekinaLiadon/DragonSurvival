package by.dragonsurvivalteam.dragonsurvival.client.render.entity.creatures;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;


public class PrinceRenderer extends VillagerRenderer{
	private static final ResourceLocation TEXTURE = new ResourceLocation("dragonsurvival", "textures/dragon_prince.png");

	public PrinceRenderer(EntityRendererProvider.Context entityRendererManager){
		super(entityRendererManager);
		this.layers.removeIf(villagerEntityVillagerModelLayerRenderer -> villagerEntityVillagerModelLayerRenderer instanceof VillagerProfessionLayer<Villager, VillagerModel<Villager>>);
	}


	public ResourceLocation getTextureLocation(Villager villager){
		return TEXTURE;
	}
}