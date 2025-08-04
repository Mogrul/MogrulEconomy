package com.mogrul.economy.utils.database;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.mogrul.economy.utils.database.DatabaseManager.connection;

public class BountyManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void addBounty(ServerPlayer player, Integer price) {
        String uuid = player.getStringUUID();

        String sql = "INSERT INTO bounties (uuid, price) " +
                "VALUES (?, ?) " +
                "ON CONFLICT (uuid) " +
                "DO UPDATE SET price = excluded.price;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setInt(2, price);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to add bounty to {}: {}", MogrulEconomy.MODID, uuid, e.getMessage());
        }
    }

    public static int getBounty(ServerPlayer player) {
        String uuid = player.getStringUUID();

        String sql = "SELECT price FROM bounties WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("price");
            }
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to get bounty from {}: {}", MogrulEconomy.MODID, uuid, e.getMessage());
        }

        return 0;
    }
}
