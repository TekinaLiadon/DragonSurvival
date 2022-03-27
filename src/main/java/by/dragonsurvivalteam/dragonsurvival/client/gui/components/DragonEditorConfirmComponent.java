package by.dragonsurvivalteam.dragonsurvival.client.gui.components;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.client.gui.dragon_editor.DragonEditorScreen;
import by.dragonsurvivalteam.dragonsurvival.client.util.TextRenderUtil;
import by.dragonsurvivalteam.dragonsurvival.config.ConfigHandler;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class DragonEditorConfirmComponent extends AbstractContainerEventHandler implements Widget{
	public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/dragon_altar_warning.png");
	private final DragonEditorScreen screen;
	private final AbstractWidget btn1;
	private final AbstractWidget btn2;
	private final int x;
	private final int y;
	private final int xSize;
	private final int ySize;
	public boolean visible;


	public DragonEditorConfirmComponent(DragonEditorScreen screen, int x, int y, int xSize, int ySize){
		this.screen = screen;
		this.x = x;
		this.y = y;
		this.xSize = xSize;
		this.ySize = ySize;

		btn1 = new ExtendedButton(x + 19, y + 133, 41, 21, CommonComponents.GUI_YES, null){
			@Override
			public void renderButton(PoseStack mStack, int mouseX, int mouseY, float partial){
				drawCenteredString(mStack, Minecraft.getInstance().font, getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor());

				if(isHovered){
					Minecraft.getInstance().screen.renderTooltip(mStack, new TranslatableComponent("ds.gui.dragon_editor.tooltip.done"), mouseX, mouseY);
				}
			}

			@Override
			public void onPress(){
				screen.confirm();
			}
		};

		btn2 = new ExtendedButton(x + 66, y + 133, 41, 21, CommonComponents.GUI_NO, null){
			@Override
			public void renderButton(PoseStack mStack, int mouseX, int mouseY, float partial){
				drawCenteredString(mStack, Minecraft.getInstance().font, getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor());

				if(isHovered){
					Minecraft.getInstance().screen.renderTooltip(mStack, new TranslatableComponent("ds.gui.dragon_editor.tooltip.cancel"), mouseX, mouseY);
				}
			}


			@Override
			public void onPress(){
				screen.confirmation = false;
			}
		};
	}

	@Override
	public List<? extends GuiEventListener> children(){
		return ImmutableList.of(btn1, btn2);
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton){
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public void render(PoseStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks){
		this.fillGradient(pMatrixStack, 0, 0, Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), -1072689136, -804253680);

		RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
		String key = "ds.gui.dragon_editor.confirm." + (!ConfigHandler.SERVER.saveAllAbilities.get() && !ConfigHandler.SERVER.saveGrowthStage.get() ? "all" : (ConfigHandler.SERVER.saveAllAbilities.get() && !ConfigHandler.SERVER.saveGrowthStage.get() ? "ability" : !ConfigHandler.SERVER.saveAllAbilities.get() && ConfigHandler.SERVER.saveGrowthStage.get() ? "growth" : ""));
		String text = new TranslatableComponent(key).getString();
		blit(pMatrixStack, x, y, 0, 0, xSize, ySize);
		TextRenderUtil.drawCenteredScaledTextSplit(pMatrixStack, x + xSize / 2, y + 42, 1f, text, DyeColor.WHITE.getTextColor(), xSize - 10, 800);


		btn1.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
		btn2.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
	}
}