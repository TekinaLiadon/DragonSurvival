package by.jackraidenph.dragonsurvival.common.handlers;

import by.jackraidenph.dragonsurvival.common.blocks.TreasureBlock;
import by.jackraidenph.dragonsurvival.common.capability.DragonStateHandler;
import by.jackraidenph.dragonsurvival.common.capability.DragonStateProvider;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.mixins.MixinServerWorld;
import by.jackraidenph.dragonsurvival.network.NetworkHandler;
import by.jackraidenph.dragonsurvival.network.status.SyncTreasureRestStatus;
import by.jackraidenph.dragonsurvival.util.Functions;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class DragonTreasureResting
{
	@SubscribeEvent
	public static void playerTick(PlayerTickEvent event){
		if(event.phase == Phase.START || event.side == LogicalSide.CLIENT) return;
		PlayerEntity player = event.player;
		
		if(DragonStateProvider.isDragon(player)){
			DragonStateHandler handler = DragonStateProvider.getCap(player).orElse(null);
			
			if(handler != null){
				if(handler.treasureResting){
					if(player.isCrouching() || !(player.getFeetBlockState().getBlock() instanceof TreasureBlock) || handler.getMovementData().bite){
						handler.treasureResting = false;
						NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncTreasureRestStatus(player.getId(), false));
						return;
					}
					
					handler.treasureSleepTimer++;
					
					if(ConfigHandler.SERVER.treasureHealthRegen.get()) {
						int horizontalRange = 16;
						int verticalRange = 9;
						int treasureNearby = 0;
						
						for(int x = -(horizontalRange / 2); x < (horizontalRange / 2); x++){
							for(int y = -(verticalRange / 2); y < (verticalRange / 2); y++){
								for(int z = -(horizontalRange / 2); z < (horizontalRange / 2); z++){
									BlockPos pos = player.blockPosition().offset(x, y, z);
									BlockState state = player.level.getBlockState(pos);
									
									if(state.getBlock() instanceof TreasureBlock){
										int layers = state.getValue(TreasureBlock.LAYERS);
										treasureNearby += layers;
									}
								}
							}
						}
						treasureNearby = MathHelper.clamp(treasureNearby, 0, ConfigHandler.SERVER.maxTreasures.get());
						
						int totalTime = ConfigHandler.SERVER.treasureRegenTicks.get();
						int restTimer = totalTime - (ConfigHandler.SERVER.treasureRegenTicksReduce.get() * treasureNearby);
						
						if(handler.treasureRestTimer >= restTimer){
							handler.treasureRestTimer = 0;
							
							if(player.getHealth() < player.getMaxHealth()) {
								player.heal(1);
							}
						}else{
							handler.treasureRestTimer++;
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void serverTick(WorldTickEvent event){
		if(event.phase == Phase.START) return;
		if(event.side == LogicalSide.CLIENT) return;
		
		ServerWorld world = (ServerWorld)event.world;
		MixinServerWorld serverWorld = (MixinServerWorld)world;
		
		if(world.isNight()) {
			if (!serverWorld.getallPlayersSleeping()) {
				if (!serverWorld.getPlayers().isEmpty()) {
					int i = 0;
					int j = 0;
					
					for (ServerPlayerEntity serverplayerentity : serverWorld.getPlayers()) {
						if (serverplayerentity.isSpectator()) {
							++i;
						} else if (serverplayerentity.isSleeping()) {
							++j;
						} else if (DragonStateProvider.isDragon(serverplayerentity)) {
							DragonStateHandler handler = DragonStateProvider.getCap(serverplayerentity).orElse(null);
							
							if (handler != null) {
								if( handler.lastTreasureResync + Functions.secondsToTicks(10) < serverplayerentity.tickCount){
									handler.lastTreasureResync = serverplayerentity.tickCount;
									NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverplayerentity), new SyncTreasureRestStatus(serverplayerentity.getId(), handler.treasureResting));
								}
								
								if (handler.treasureResting) {
									++j;
								}
							}
						}
					}
					
					boolean all = j > 0 && j >= serverWorld.getPlayers().size() - i;
					
					if (all) {
						serverWorld.setallPlayersSleeping(true);
					}
				}
			}
		}
	}
	
	@OnlyIn( Dist.CLIENT)
	@SubscribeEvent
	public static void playerTick(ClientTickEvent event){
		if(event.phase == Phase.START) return;
		PlayerEntity player = Minecraft.getInstance().player;
		
		if(DragonStateProvider.isDragon(player)){
			DragonStateHandler handler = DragonStateProvider.getCap(player).orElse(null);
			
			if(handler != null){
				if(handler.treasureResting){
					Vector3d velocity = player.getDeltaMovement();
					float groundSpeed = MathHelper.sqrt((velocity.x * velocity.x) + (velocity.z * velocity.z));
					if(Math.abs(groundSpeed) > 0.05){
						NetworkHandler.CHANNEL.sendToServer(new SyncTreasureRestStatus(player.getId(), false));
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void playerAttacked(LivingHurtEvent event){
		LivingEntity entity = event.getEntityLiving();
		
		if(entity instanceof PlayerEntity){
			PlayerEntity player = (PlayerEntity)entity;
			
			if(!player.level.isClientSide) {
				DragonStateProvider.getCap(player).ifPresent(cap -> {
					if (cap.treasureResting) {
						NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncTreasureRestStatus(player.getId(), false));
					}
				});
			}
		}
	}
}
