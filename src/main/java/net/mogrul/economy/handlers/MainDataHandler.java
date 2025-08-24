package net.mogrul.economy.handlers;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class MainDataHandler {
    public static Path serverFolder;
    public static Path dataFolder;
    public static Path modFolder;
    public static Path playersFolder;
    public static Path mobsFolder;
    public static Path shopsFolder;
    public static Path bountyFolder;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStarting(ServerStartingEvent event) {
        serverFolder = event.getServer().getServerDirectory();

        // serverRoot/Mogrul/Economy
        dataFolder = serverFolder.resolve("Mogrul");
        modFolder = dataFolder.resolve("Economy");

        // serverRoot/Mogrul/Economy/Players, ect.
        playersFolder = modFolder.resolve("Players");
        mobsFolder = modFolder.resolve("Mobs");
        shopsFolder = modFolder.resolve("Shops");
        bountyFolder = modFolder.resolve("Bounties");

        // Create default directories.
        for (Path path : List.of(
                dataFolder,
                modFolder,
                playersFolder,
                mobsFolder,
                shopsFolder,
                bountyFolder
        )) {
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                LOGGER.error("[{}] Failed to create directory: {} - {}", LOGNAME, path.toAbsolutePath(), e.getMessage());
            }
        }
    }

    // Helper functions.
    public static List<Path> getJsonFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                    .toList();
        }
    }
}
