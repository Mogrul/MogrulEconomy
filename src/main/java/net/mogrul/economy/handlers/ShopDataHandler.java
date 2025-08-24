package net.mogrul.economy.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.ShopData;
import net.mogrul.economy.goals.ShopVillagerGoal;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static net.mogrul.economy.MogrulEconomy.*;
import static net.mogrul.economy.handlers.MainDataHandler.getJsonFiles;
import static net.mogrul.economy.handlers.MainDataHandler.shopsFolder;

@EventBusSubscriber(modid = MODID)
public class ShopDataHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerStartingEvent(ServerStartingEvent event) {
        // Load all shop files into the memory data.
        try {
            List<Path> jsonFiles = getJsonFiles(shopsFolder);
            for (Path jsonFile : jsonFiles) {
                String json = new String(Files.readAllBytes(jsonFile));
                if (json.isEmpty()) continue;
                ShopData shopData = GSON.fromJson(json, ShopData.class);
                if (shopData == null) continue;

                // Store data object and spawn the entity.
                MemoryData.shops.put(shopData.shopUUID, shopData);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to load shop data to memory! {}", LOGNAME, e.getMessage());
        }
    }

    public static ShopData load(String shopUUID) {
        Path shopsFile = shopsFolder.resolve(shopUUID + ".json");
        if (!Files.exists(shopsFile)) return null;

        try {
            String json = Files.readString(shopsFile);
            return GSON.fromJson(json, ShopData.class);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to load shop data! {}", shopUUID, e.getMessage());
            return null;
        }
    }

    public static void save(ShopData shopData) {
        Path shopsFile = shopsFolder.resolve(shopData.shopUUID + ".json");

        try {
            String json = GSON.toJson(shopData);
            Files.writeString(shopsFile, json);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to save shop data! {}", LOGNAME, e.getMessage());
        }
    }

    public static void createShopEntity(ShopData shopData) {
        ResourceLocation levelID = ResourceLocation.tryParse(shopData.entityLevel);
        if (levelID == null) return;

        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, levelID);
        ServerLevel serverLevel = server.getLevel(level);

        EntityType<Villager> villagerType = EntityType.VILLAGER;
        assert serverLevel != null;
        Villager shopVillager = new Villager(villagerType, serverLevel);

        shopVillager.setPos(shopData.entityX, shopData.entityY, shopData.entityZ);
        shopVillager.setUUID(UUID.fromString(shopData.shopUUID));
        shopVillager.setCustomName(Component.literal(shopData.entityName));
        shopVillager.getPersistentData().putBoolean(MODID, true);

        shopVillager.goalSelector.removeAllGoals(g -> true);
        shopVillager.targetSelector.removeAllGoals(g -> true);

        shopVillager.goalSelector.addGoal(0, new ShopVillagerGoal(shopVillager));

        // Add the entity to the level.
        serverLevel.addFreshEntity(shopVillager);
    }

    public static void remove(ShopData shopData) {
        Path shopFile = shopsFolder.resolve(shopData.shopUUID + ".json");

        if (Files.notExists(shopFile)) return;
        try {
            Files.delete(shopFile);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to delete mobs file! {}", LOGNAME, e.getMessage());
        }

    }
}
