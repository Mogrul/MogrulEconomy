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

public class CurrencyManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int getCurrency(ServerPlayer player) {
        String uuid = player.getStringUUID();

        DatabaseManager.addPlayer(player);

        String sql = "SELECT amount FROM economy WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("amount");
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get {}'s for player [{}]: {}", uuid, Config.currencyName, e.getMessage());
        }

        return 0;
    }

    public static void addCurrency(ServerPlayer player, Integer amount) {
        String uuid = player.getStringUUID();

        String sql = "INSERT INTO economy (uuid, amount) " +
                "VALUES (?, ?) " +
                "ON CONFLICT (uuid) DO UPDATE SET amount = amount + excluded.amount" +
                ";";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setInt(2, amount);

            stmt.executeUpdate();

            LOGGER.info("[{}] Added {} {}'s to player [{}]",  MogrulEconomy.MODID, amount, Config.currencyName, uuid);
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to add {} {}'s to player [{}]: {}", MogrulEconomy.MODID, amount, Config.currencyName, uuid, e.getMessage());
        }

        getCurrency(player);
    }

    public static void removeCurrency(ServerPlayer player, Integer amount) {
        String uuid = player.getStringUUID();

        String sql = "INSERT INTO economy (uuid, amount) " +
                "VALUES (?, ?) " +
                "ON CONFLICT (uuid) " +
                "DO UPDATE SET amount = CASE WHEN amount - ? < 0 THEN 0 ELSE amount - ? END;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setInt(2, Config.startingCurrency);
            stmt.setInt(3, amount);
            stmt.setInt(4, amount);

            stmt.executeUpdate();

            LOGGER.info("[{}] Removed [{}{}] from [{}]", MogrulEconomy.MODID, Config.currencySymbol, amount, uuid);
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to remove [{}{}] from [{}]: {}", MogrulEconomy.MODID, Config.currencySymbol, amount, uuid, e.getMessage());
            return;
        }

        getCurrency(player);
    }

    public static void setCurrency(ServerPlayer player, Integer amount) {
        String uuid = player.getStringUUID();
        DatabaseManager.addPlayer(player);

        String sql = "UPDATE economy SET amount = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid);
            stmt.executeUpdate();

            LOGGER.info("[{}] Set [{}] {} to {}", MogrulEconomy.MODID, uuid, Config.currencyName, amount);
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to set [{}] {} to {}: {}", MogrulEconomy.MODID, uuid, Config.currencyName, amount, e.getMessage());
        }
    }
}
