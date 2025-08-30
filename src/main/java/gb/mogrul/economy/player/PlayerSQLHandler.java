package gb.mogrul.economy.player;

import static gb.mogrul.economy.constructors.SQLConstructor.CONNECTION;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import gb.mogrul.economy.Config;

import static gb.mogrul.economy.MogrulEconomy.*;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = MODID)
public class PlayerSQLHandler {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerStarted(ServerStartedEvent event) {
        String sql = "CREATE TABLE players(" +
            "uuid TEXT PRIMARY KEY NOT NULL, " +
            "username TEXT NOT NULL, " +
            "currency INT DEFAULT 0" +
            ");";
        
        try (Statement stmt = CONNECTION.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                LOGGER.info("[{}] Failed to create chunks table! {}", LOGNAME, e.getMessage());
            }
        }
    }

    public static PlayerData getOne(ServerPlayer serverPlayer) {
        String playerUUID = serverPlayer.getStringUUID();
        String sql = "SELECT * FROM players WHERE uuid = ?";

        try (PreparedStatement stmt = CONNECTION.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                int currency = rs.getInt("currency");

                return new PlayerData(playerUUID, username, currency);
            }
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to get player from SQL! {}", LOGNAME, e.getMessage());
        }

        // Create row if not exist.
        PlayerData playerData = new PlayerData(playerUUID, serverPlayer.getScoreboardName(), Config.economyStartingCurrency);
        add(serverPlayer);

        return playerData;
    }

    public static void add(ServerPlayer serverPlayer) {
        String sql = "INSERT INTO players (uuid, username, currency) " +
            "VALUES (?, ?, ?);";
        
        try (PreparedStatement stmt = CONNECTION.prepareStatement(sql)) {
            stmt.setString(1, serverPlayer.getStringUUID());
            stmt.setString(2, serverPlayer.getScoreboardName());
            stmt.setInt(3, Config.economyStartingCurrency);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to insert player {} into db! {}", LOGNAME, serverPlayer.getScoreboardName(), e.getMessage());
        }
    }

    public static void update(PlayerData playerData) {
        String sql = "UPDATE players SET username = ?, currency = ? " +
            "WHERE uuid = ?";
        
        try (PreparedStatement stmt = CONNECTION.prepareStatement(sql)) {
            stmt.setString(1, playerData.username);
            stmt.setInt(2, playerData.currency);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to update {}'s values in db! {}", 
                LOGNAME,
                playerData.username,
                e.getMessage()
            );
        }
    }
}
