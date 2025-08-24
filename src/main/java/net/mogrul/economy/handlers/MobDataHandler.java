package net.mogrul.economy.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.MobRewardData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static net.mogrul.economy.MogrulEconomy.*;
import static net.mogrul.economy.handlers.MainDataHandler.getJsonFiles;
import static net.mogrul.economy.handlers.MainDataHandler.mobsFolder;

@EventBusSubscriber(modid = MODID)
public class MobDataHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onServerStartingEvent(ServerStartedEvent event) {
        // Load all mob files into the memory data.
        try {
            List<Path> jsonFiles = getJsonFiles(mobsFolder);

            for (Path jsonFile : jsonFiles) {
                String json = new String(Files.readAllBytes(jsonFile));
                if (json.isEmpty()) continue;
                String fileNameWithoutType = jsonFile.getFileName().toString().substring(
                        0,
                        jsonFile.getFileName().toString().lastIndexOf(".")
                );
                MemoryData.mobRewards.put(fileNameWithoutType, GSON.fromJson(json, MobRewardData.class));
            }

        } catch (IOException e) {
            LOGGER.error("[{}] Failed to load mob files! {}", LOGNAME, e.getMessage());
        }
    }

    public static void save(MobRewardData mobRewardData) {
        Path mobsFile = mobsFolder.resolve(mobRewardData.mobID.replace(":", "_") + ".json");

        try {
            String json = GSON.toJson(mobRewardData);
            Files.writeString(mobsFile, json);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to save mobs file! {}", LOGNAME, e.getMessage());
        }
    }

    public static void remove(MobRewardData mobRewardData) {
        Path mobsFile = mobsFolder.resolve(mobRewardData.mobID.replace(":", "_") + ".json");

        if (Files.notExists(mobsFile)) return;
        try {
            Files.delete(mobsFile);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to delete mobs file! {}", LOGNAME, e.getMessage());
        }
    }
}
