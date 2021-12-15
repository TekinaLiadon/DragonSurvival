package by.jackraidenph.dragonsurvival.handlers;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.capability.DragonStateProvider;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.config.ConfigUtils;
import by.jackraidenph.dragonsurvival.handlers.ServerSide.NetworkHandler;
import by.jackraidenph.dragonsurvival.network.SyncSize;
import by.jackraidenph.dragonsurvival.network.SynchronizeDragonCap;
import by.jackraidenph.dragonsurvival.util.DragonLevel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = DragonSurvivalMod.MODID)
public class DragonGrowthHandler {
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();

        PlayerEntity player = event.getPlayer();
        World world = player.getCommandSenderWorld();

        DragonStateProvider.getCap(player).ifPresent(handler -> {
            if (!handler.isDragon())
                return;

            double size = handler.getSize();

            if (size >= ConfigHandler.SERVER.maxGrowthSize.get())
                return;

            boolean canContinue = false;

            List<Item> newbornList = ConfigUtils.parseConfigItemList(ConfigHandler.SERVER.growNewborn.get());
            List<Item> youngList = ConfigUtils.parseConfigItemList(ConfigHandler.SERVER.growYoung.get());
            List<Item> adultList = ConfigUtils.parseConfigItemList(ConfigHandler.SERVER.growAdult.get());

            List<Item> allowedItems = new ArrayList<>();

            switch (handler.getLevel()) {
                case BABY:
                    if (newbornList.contains(item))
                        canContinue = true;
                    else if (youngList.contains(item) || adultList.contains(item))
                        allowedItems = newbornList;

                    break;
                case YOUNG:
                    if (youngList.contains(item))
                        canContinue = true;
                    else if (newbornList.contains(item) || adultList.contains(item))
                        allowedItems = youngList;

                    break;
                case ADULT:
                    if (adultList.contains(item))
                        canContinue = true;
                    else if (newbornList.contains(item) || youngList.contains(item))
                        allowedItems = adultList;

                    break;
            }

            if (!canContinue) {
                if (!allowedItems.isEmpty() && world.isClientSide()) {
                    List<String> displayData = allowedItems.stream()
                            .map(i -> new ItemStack(i).getDisplayName().getString())
                            .collect(Collectors.toList());
                    StringBuilder result = new StringBuilder();

                    for (int i = 0; i < displayData.size(); i++) {
                        String entry = displayData.get(i);

                        result.append(entry).append(i + 1 < displayData.size() ? ", " : "");
                    }

                    player.displayClientMessage(new TranslationTextComponent("ds.invalid_grow_item", result), false);
                }

                return;
            }

            handler.setSize(++size, player);
            event.getItemStack().shrink(1);

            if (world.isClientSide)
                return;

            NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncSize(player.getId(), size));

            if (player.getVehicle() == null || !(player.getVehicle() instanceof ServerPlayerEntity))
                return;

            ServerPlayerEntity vehicle = (ServerPlayerEntity) player.getVehicle();

            DragonStateProvider.getCap(vehicle).ifPresent(vehicleCap -> {
                player.stopRiding();

                vehicle.connection.send(new SSetPassengersPacket(vehicle));

                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> vehicle), new SynchronizeDragonCap(vehicle.getId(),
                                                                                                                   vehicleCap.isHiding(), vehicleCap.getType(), vehicleCap.getSize(), vehicleCap.hasWings(), vehicleCap.getLavaAirSupply(), 0));
            });

            player.refreshDimensions();
        });
    }
    
    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if (!ConfigHandler.SERVER.alternateGrowing.get())
            return;
        
        PlayerEntity player = event.player;
        World world = player.getCommandSenderWorld();
        
        if (world.isClientSide || event.phase == Phase.END)
            return;
        
        if(!DragonStateProvider.isDragon(player)) return;
        
        if (player.tickCount % (60 * 20) != 0)
            return;
    
        DragonStateProvider.getCap(player).ifPresent(handler -> {
            if(handler.growing) {
                //TODO Add item to stop growing and a way to start growing again, maybe same item toggles it?
                
                /*
                    1. Newborn - young = 3-4 h
                    2. Young - adult = 15-20h
                    3. Adult - maximum growth = 24h
                    4. After maximum growth. = 30 days for max growth
                 */
            
                long newbornToYoung = TimeUnit.SECONDS.convert(3, TimeUnit.HOURS);
                long youngToAdult = TimeUnit.SECONDS.convert(15, TimeUnit.HOURS);
                long adultToMax = TimeUnit.SECONDS.convert(24, TimeUnit.HOURS);
                long beyond = TimeUnit.SECONDS.convert(30, TimeUnit.DAYS);
            
                double d = 0;
                double timeIncrement = 60 * 20;
            
                if(handler.getSize() < DragonLevel.YOUNG.size){
                    d = ((DragonLevel.YOUNG.size - DragonLevel.BABY.size) / ((newbornToYoung * 20.0))) * timeIncrement;
                
                }else if(handler.getSize() < DragonLevel.ADULT.size){
                    d = ((DragonLevel.ADULT.size - DragonLevel.YOUNG.size) / ((youngToAdult * 20.0))) * timeIncrement;
                
                }else if(handler.getSize() < 40){
                    d = ((40 - DragonLevel.ADULT.size) / ((adultToMax * 20.0))) * timeIncrement;
                }else{
                    d = ((60 - 40) / ((beyond * 20.0))) * timeIncrement;
                }
            
                double size = handler.getSize() + d;
                size = Math.min(size, ConfigHandler.SERVER.maxGrowthSize.get());
                
                if(handler.getSize() != size) {
                    handler.setSize(size, player);
    
                    NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncSize(player.getId(), size));
                    player.refreshDimensions();
                }
            }
        });
    }
    
}