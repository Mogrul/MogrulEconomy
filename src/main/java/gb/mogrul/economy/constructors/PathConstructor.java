package gb.mogrul.economy.constructors;

import static gb.mogrul.economy.MogrulEconomy.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = MODID)
public class PathConstructor {
    public static Path serverFolder;
    public static Path dataFolder;
    public static Path modFolder;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStarting(ServerStartingEvent event) {
        serverFolder = event.getServer().getServerDirectory();

        dataFolder = serverFolder.resolve("Mogrul");
        modFolder = dataFolder.resolve("Economy");

        for (Path path : List.of(dataFolder, modFolder)) {
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                LOGGER.error("[{}] Failed to create directory: {} - {}",
                    LOGNAME,
                    path.toAbsolutePath(),
                    e.getMessage()
                );
            }
        }
    }
}
