package net.mogrul.economy.handlers;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class MainDataHandler {
    private static final String DATAPATH = "Mogrul";
    private static final String MODPATH = "Economy";
    private static final String PLAYERSPATH = "Players";
    private static final String MOBPATH = "Mobs";
    private static final String SHOPSPATH = "Shops";

    public static Path playersFolder;
    public static Path mobsFolder;
    public static Path shopsFolder;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer minecraftserver = event.getServer();
        Path serverRoot = minecraftserver.getServerDirectory();
        Path dataPath = serverRoot.resolve(DATAPATH);
        Path modPath = dataPath.resolve(MODPATH);
        Path playersPath = modPath.resolve(PLAYERSPATH);
        Path mobsPath = modPath.resolve(MOBPATH);
        Path shopsPath = modPath.resolve(SHOPSPATH);

        playersFolder = playersPath;
        mobsFolder = mobsPath;
        shopsFolder = shopsPath;

        // Create mod path.
        try {
            if (!Files.exists(modPath)) {
                Files.createDirectories(modPath);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create mod directory! {}", LOGNAME, e.getMessage());
        }

        // Create players path.
        try {
            if (!Files.exists(playersPath)) {
                Files.createDirectories(playersPath);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create player directory! {}", LOGNAME, e.getMessage());
        }

        // Create mobs path.
        try {
            if (!Files.exists(mobsPath)) {
                Files.createDirectories(mobsPath);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create mobs path! {}", LOGNAME, e.getMessage());
        }

        // Create shops path.
        try {
            if (!Files.exists(shopsPath)) {
                Files.createDirectories(shopsPath);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create shops path! {}", LOGNAME, e.getMessage());
        }
    }
}
