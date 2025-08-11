package net.mogrul.economy.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class PlayerDataHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Get / Create player file.
        if (load(serverPlayer) instanceof PlayerData playerData) {
            LOGGER.info("[{}] Grabbed {}'s player data!", LOGNAME, playerData.name);
            MemoryData.players.put(playerData.UUID, playerData);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        MemoryData.players.remove(serverPlayer.getStringUUID());
        LOGGER.info("[{}] Unloading {}'s player data!", LOGNAME, serverPlayer.getName().getString());
    }

    public static PlayerData load(ServerPlayer serverPlayer) {
        Path playerFile = MainDataHandler.playersFolder.resolve(serverPlayer.getStringUUID() + ".json");

        if (Files.notExists(playerFile)) {
            PlayerData playerData = new PlayerData(
                    serverPlayer.getStringUUID(),
                    serverPlayer.getName().getString(),
                    Config.startingCurrency
            );

            save(playerData);
            return playerData;
        }

        try {
            String json = Files.readString(playerFile);
            return GSON.fromJson(json, PlayerData.class);
        } catch (IOException e) {
            LOGGER.error(
                    "[{}] Failed to load player [{}] data! {}",
                    LOGNAME,
                    serverPlayer.getName().getString(),
                    e.getMessage()
            );
            return null;
        }
    }

    public static void save(PlayerData playerData) {
        Path playerFile = MainDataHandler.playersFolder.resolve(playerData.UUID + ".json");

        try {
            String json = GSON.toJson(playerData);
            Files.writeString(playerFile, json);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to save player {} data! {}", LOGNAME, playerData.UUID, e.getMessage());
        }
    }
}
