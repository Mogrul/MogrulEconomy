package net.mogrul.economy.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.MobRewardData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static net.mogrul.economy.MogrulEconomy.*;
import static net.mogrul.economy.handlers.MainDataHandler.mobsFile;

@EventBusSubscriber(modid = MODID)
public class MobDataHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        for (MobRewardData mobReward : load()) {
            MemoryData.mobRewards.put(mobReward.mobID, mobReward);
        }
    }

    public static List<MobRewardData> load() {
        LOGGER.info("[{}] Loading mobs file!", LOGNAME);

        if (Files.notExists(mobsFile)) {
            LOGGER.error("[{}] Mobs file not found!", LOGNAME);
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(mobsFile);
            if (json.trim().isEmpty()) {
                return new ArrayList<>();
            }

            return GSON.fromJson(json, new TypeToken<List<MobRewardData>>(){}.getType());
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to load mobs file! {}", LOGNAME, e.getMessage());
        } catch (JsonSyntaxException e) {
            LOGGER.error("[{}] Failed to parse mobs file to JSON! {}", LOGNAME, e.getMessage());
        }

        return new ArrayList<>();
    }

    public static void save(List<MobRewardData> mobsData) {
        try {
            String json = GSON.toJson(mobsData);
            Files.writeString(mobsFile, json);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to save mobs file! {}", LOGNAME, e.getMessage());
        }
    }
}
