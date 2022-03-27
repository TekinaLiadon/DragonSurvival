package by.dragonsurvivalteam.dragonsurvival.client.gui.widgets.buttons;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

public class UndoRedoButton extends by.dragonsurvivalteam.dragonsurvival.client.gui.widgets.buttons.ArrowButton{
	public static final ResourceLocation undo = new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/arrow_undo.png");
	public static final ResourceLocation redo = new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/arrow_redo.png");

	public UndoRedoButton(int x, int y, int xSize, int ySize, boolean next, OnPress pressable){
		super(x, y, xSize, ySize, next, pressable);
	}

	@Override
	public void renderButton(PoseStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_){
		stack.pushPose();
		stack.translate(0, 0, 200);

		if(next){
			RenderSystem.setShaderTexture(0, redo);
			blit(stack, x, y, 0, 0, width, height, width, height);
		}else{
			RenderSystem.setShaderTexture(0, undo);
			blit(stack, x, y, 0, 0, width, height, width, height);
		}
		stack.popPose();
	}
}