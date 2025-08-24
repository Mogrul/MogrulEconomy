package net.mogrul.economy.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mogrul.economy.data.BountyData;
import net.mogrul.economy.data.MemoryData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.mogrul.economy.MogrulEconomy.*;
import static net.mogrul.economy.handlers.MainDataHandler.bountyFolder;
import static net.mogrul.economy.handlers.MainDataHandler.getJsonFiles;

@EventBusSubscriber(modid = MODID)
public class BountyDataHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onServerStarting(ServerStartedEvent event) {
        // Load all bounties into memory.
        try {
            List<Path> jsonFiles = getJsonFiles(bountyFolder);

            for (Path jsonFile : jsonFiles) {
                String json = new String(Files.readAllBytes(jsonFile));
                if (json.isEmpty()) continue;
                String fileNameWithoutType = jsonFile.getFileName().toString().substring(
                        0,
                        jsonFile.getFileName().toString().lastIndexOf('.')
                );

                MemoryData.bounties.put(fileNameWithoutType, GSON.fromJson(json, BountyData.class));
            }
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to load bounty folder to memory! {}", LOGNAME, e.getMessage());
        }
    }

    public static void save(BountyData bountyData) {
        Path bountyFile = bountyFolder.resolve(bountyData.playerUUID + ".json");

        try {
            String json = GSON.toJson(bountyData);
            Files.writeString(bountyFile, json);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to save bounty to folder! {}", LOGNAME, e.getMessage());
        }
    }

    public static void remove(BountyData bountyData) {
        Path bountyFile = bountyFolder.resolve(bountyData.playerUUID + ".json");

        if (Files.notExists(bountyFile)) return;
        try {
            Files.delete(bountyFile);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to delete bounty file! {}", LOGNAME, e.getMessage());
        }
    }

    public static List<BountyData> loadAll() {
        File[] files = bountyFolder.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        List<BountyData> bountyDataList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    String json = Files.readString(file.toPath());
                    bountyDataList.add(GSON.fromJson(json, BountyData.class));
                } catch (IOException e) {
                    LOGGER.error("[{}] Failed to load bounty file data [{}]! {}",
                            LOGNAME,
                            file.getName(),
                            e.getMessage()
                    );
                }
            }
        }

        return bountyDataList;
    }
}
