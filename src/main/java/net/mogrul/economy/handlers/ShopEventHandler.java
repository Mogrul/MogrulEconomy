package net.mogrul.economy.handlers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.mogrul.economy.data.ShopData;
import net.mogrul.economy.goals.ShopVillagerGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class ShopEventHandler {
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Villager villagerEntity) {
            if (!villagerEntity.getPersistentData().getBoolean(MODID)) return;
        } else {
            return;
        }

        if (MemoryData.shops.get(villagerEntity.getStringUUID()) == null) {
            // Cancel spawning.
            event.setCanceled(true);
            return;
        }

        villagerEntity.goalSelector.removeAllGoals(g -> true);
        villagerEntity.targetSelector.removeAllGoals(g -> true);

        villagerEntity.goalSelector.addGoal(0, new ShopVillagerGoal(villagerEntity));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Villager villagerEntity) {
            if (!villagerEntity.getPersistentData().getBoolean(MODID)) return;
        } else {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (event.getTarget() instanceof Villager villagerEntity) {
            if (!villagerEntity.getPersistentData().getBoolean(MODID)) return;
        } else {
            return;
        }

        handleShopInteraction(serverPlayer, villagerEntity);
    }

    private static void handleShopInteraction(ServerPlayer serverPlayer, Villager villagerEntity) {
        PlayerData playerData = MemoryData.players.get(serverPlayer.getStringUUID());
        if (playerData == null) {
            LOGGER.error("[{}] Unable to fetch playerData on shop interaction! {}", LOGNAME, serverPlayer.getStringUUID());
            return;
        }

        ShopData shopData = MemoryData.shops.get(villagerEntity.getStringUUID());
        if (shopData == null) {
            LOGGER.error("[{}] Unable to fetch shopData {} on shop interaction from {}", LOGNAME, villagerEntity.getStringUUID(), serverPlayer.getStringUUID());
            return;
        }

        // Check if player has enough money.
        if (shopData.sellingPrice > playerData.currency) {
            serverPlayer.sendSystemMessage(Component.literal("Not enough money!"));
            return;
        }

        // Copy the itemstack to test if can fit in player inventory.
        ResourceLocation itemResource = ResourceLocation.tryParse(shopData.sellingItemID);
        if (itemResource == null) {
            LOGGER.error("[{}] Unable to parse item ID {} to ResourceLocation from shop {}",
                    LOGNAME,
                    shopData.sellingItemID,
                    shopData.shopUUID
            );
            return;
        }

        Optional<Holder.Reference<Item>> optionalRef = BuiltInRegistries.ITEM.get(itemResource);
        Item item;
        if (optionalRef.isPresent()) {
            item = optionalRef.get().value();
        } else {
            LOGGER.info("[{}] Unable to obtain reference to object: {}", LOGNAME, itemResource);
            return;
        }

        ItemStack itemStack = new ItemStack(item, shopData.sellingAmount);

        if (!canFitInPlayerInventory(serverPlayer.getInventory(), itemStack)) {
            serverPlayer.sendSystemMessage(Component.literal("Clear some space in your inventory!"));
            return;
        }

        serverPlayer.getInventory().add(itemStack);

        // Remove the currency from the player and add the sold quantity to the shop.
        playerData.currency -= shopData.sellingPrice;
        shopData.quantitySold += shopData.sellingAmount;

        PlayerDataHandler.save(playerData);
        ShopDataHandler.save(shopData);


        // Message the player with success.
        LOGGER.info("[{}] Player {} bought x{} {} from shop {} for {}{}!",
                LOGNAME,
                playerData.name,
                shopData.sellingAmount,
                shopData.sellingItemID,
                shopData.shopUUID,
                Config.currencySymbol,
                shopData.sellingPrice
        );

        Component successfulTransactrionMessage = Component.literal("[Shops]\n")
                .append(Component.literal("You've bought "))
                .append(Component.literal(shopData.sellingItemID + " x" + shopData.sellingAmount + "\n")
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(Component.literal(playerData.name)
                        .withStyle(ChatFormatting.BLUE)
                )
                .append(Component.literal(Config.currencySymbol + shopData.sellingPrice )
                        .withStyle(ChatFormatting.YELLOW)
                )
                .append(Component.literal(" -> "))
                .append(Component.literal(shopData.entityName + "\n")
                        .withStyle(ChatFormatting.BLUE)
                )
                .append(Component.literal(playerData.name)
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(Component.literal(" = "))
                .append(Component.literal(Config.currencySymbol + playerData.currency)
                        .withStyle(ChatFormatting.YELLOW)
                );

        serverPlayer.sendSystemMessage(successfulTransactrionMessage);
    }
}
