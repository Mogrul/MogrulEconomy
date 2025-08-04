package com.mogrul.economy.utils.database;

import com.mogrul.economy.MogrulEconomy;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.mogrul.economy.utils.database.DatabaseManager.connection;

public class MobRewardsManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int getReward(String mobId) {
        String sql = "SELECT amount FROM mobrewards WHERE mob_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mobId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("amount");
            } else {
                return 0;
            }

        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to get reward for mob {}: {}", MogrulEconomy.MODID, mobId, e.getMessage());

            return 0;
        }
    }

    public static void setReward(String mobId, int amount) {
        String sql = "INSERT INTO mobrewards (mob_id, amount) " +
                "VALUES (?, ?) " +
                "ON CONFLICT(mob_id) DO UPDATE SET amount=excluded.amount;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mobId);
            stmt.setInt(2, amount);

            stmt.executeUpdate();

            LOGGER.info("[{}] Set reward for mob {} to {}", MogrulEconomy.MODID, mobId, amount);
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to set reward for mob {}", MogrulEconomy.MODID, mobId);
        }
    }

    public static Integer removeReward(String mobId) {
        String sql = "DELETE FROM mobrewards WHERE mob_id = ? RETURNING amount;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mobId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int amount = rs.getInt("amount");
                LOGGER.info("[{}] Removed reward for mob {} from rewards.", MogrulEconomy.MODID, mobId);

                return amount;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to remove reward for mob {}: {}", MogrulEconomy.MODID, mobId, e.getMessage());

            return 0;
        }
    }
}
