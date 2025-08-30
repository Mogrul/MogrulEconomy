package gb.mogrul.economy.constructors;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import static gb.mogrul.economy.MogrulEconomy.*;
import static gb.mogrul.economy.constructors.PathConstructor.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@EventBusSubscriber(modid = MODID)
public class SQLConstructor {
    private static final String jdbcName = "jdbc:sqlite:";
    private static final String fileName = "economy.db";

    public static Connection CONNECTION;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStarting(ServerStartingEvent event) {
        Path filePath = modFolder.resolve(fileName);

        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                LOGGER.info("[{}] Created economy.db file!", LOGNAME);
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create economy.db file! {}", LOGNAME, e.getMessage());
            return;
        }

        try {
            CONNECTION = DriverManager.getConnection(jdbcName + filePath.toAbsolutePath());
            LOGGER.info("[{}] Connected to {}", LOGNAME, filePath);
        } catch (SQLException e) {
            LOGGER.error("[{}] Could not connect to database! {}", LOGNAME, e.getMessage());
            return;
        }
    }
}
