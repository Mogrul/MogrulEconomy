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
    private static final String MOBFILEPATH = "mobs.json";

    public static Path playersFolder;
    public static Path mobsFile;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer minecraftserver = event.getServer();
        Path serverRoot = minecraftserver.getServerDirectory();
        Path dataPath = serverRoot.resolve(DATAPATH);
        Path modPath = dataPath.resolve(MODPATH);
        Path playersPath = modPath.resolve(PLAYERSPATH);
        Path mobsFilePath = dataPath.resolve(MOBFILEPATH);

        playersFolder = playersPath;
        mobsFile = mobsFilePath;

        // Create mod path.
        try {
            if (!Files.exists(modPath)) {
                Files.createDirectories(modPath);
                LOGGER.info("[{}] Creating mod directory!", LOGNAME);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create mod directory! {}", LOGNAME, e.getMessage());
        }

        // Create players path.
        try {
            if (!Files.exists(playersPath)) {
                Files.createDirectories(playersPath);
                LOGGER.info("[{}] Creating player directory!", LOGNAME);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create player directory! {}", LOGNAME, e.getMessage());
        }

        // Create mobs file.
        try {
            if (!Files.exists(mobsFile)) {
                Files.createFile(mobsFile);
                LOGGER.info("[{}] Creating mobs file!", LOGNAME);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create mobs file! {}", LOGNAME, e.getMessage());
        }
    }
}
