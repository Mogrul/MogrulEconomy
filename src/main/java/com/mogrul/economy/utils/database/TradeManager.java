package com.mogrul.economy.utils.database;

import com.mogrul.economy.commands.TradeCommands;
import com.mojang.logging.LogUtils;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import static com.mogrul.economy.utils.database.DatabaseManager.connection;

public class TradeManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void addTrade(TradeCommands.PendingTrade trade) {
        String sql = "INSERT INTO trade (id, from_player, to_player, item, count, price, date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, trade.id());
            stmt.setString(2, trade.fromPlayer().getStringUUID());
            stmt.setString(3, trade.toPlayer().getStringUUID());
            stmt.setString(4, Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(trade.item().getItem())).toString());
            stmt.setInt(5, trade.count());
            stmt.setInt(6, trade.price());
            stmt.setLong(7, System.currentTimeMillis());

            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to add a trade: {}", sql, e.getMessage());
        }
    }

    public static void updateTradeAccepted(TradeCommands.PendingTrade trade) {
        String sql = "UPDATE trade SET accepted = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, 1);
            stmt.setString(2, trade.id());

            stmt.executeUpdate();

            LOGGER.info("[{}] Updated accepted trade: {}", sql, trade.id());
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to update accepted trade: {}", sql, e.getMessage());
        }
    }
}
