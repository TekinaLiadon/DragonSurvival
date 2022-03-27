package by.dragonsurvivalteam.dragonsurvival.client.gui.settings;

import by.dragonsurvivalteam.dragonsurvival.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerSettingsScreen extends ClientSettingsScreen{
	public ServerSettingsScreen(Screen p_i225930_1_, Options p_i225930_2_, Component p_i225930_3_){
		super(p_i225930_1_, p_i225930_2_, p_i225930_3_);
	}

	@Override
	public void tick(){
		super.tick();

		if(!Minecraft.getInstance().player.hasPermissions(2)){
			this.minecraft.setScreen(this.lastScreen);
		}
	}

	@Override
	public ForgeConfigSpec getSpec(){
		return ConfigHandler.serverSpec;
	}

	@Override
	public String getConfigName(){
		return "server";
	}
}