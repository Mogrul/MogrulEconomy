package com.mogrul.economy.utils.database;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_NAME = "MogrulEconomy.db";
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    public static Connection connection;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        try {
            File dbFile = new File(FMLPaths.CONFIGDIR.get().toFile(), DB_NAME);
            String url = JDBC_PREFIX + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createDefaultTables();
            LOGGER.info("[{}] Connected to database.", MogrulEconomy.MODID);

        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to connect to database: {}", MogrulEconomy.MODID, e.getMessage());
        }
    }

    private static void createDefaultTables() {
        // Economy table.
        String sql = "CREATE TABLE IF NOT EXISTS economy (" +
                "uuid TEXT PRIMARY KEY, " +
                "amount INTEGER NOT NULL DEFAULT " + Config.startingCurrency +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);

            LOGGER.info("[{}] Created default tables for economy", MogrulEconomy.MODID);
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to create table economy: {}", MogrulEconomy.MODID, e.getMessage());
        }

        // Mob Reward table.
        sql = "CREATE TABLE IF NOT EXISTS mobrewards (" +
                "mob_id TEXT PRIMARY KEY, " +
                "amount INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);

            LOGGER.info("[{}] Created default tables for mobRewards", MogrulEconomy.MODID);
        } catch (SQLException e) {
            LOGGER.info("[{}] Failed to create Default tables for mobRewards", e.getMessage());
        }

        // Trade table.
        sql = "CREATE TABLE IF NOT EXISTS trade (" +
                "id TEXT PRIMARY KEY, " +
                "from_player TEXT NOT NULL, " +
                "to_player TEXT NOT NULL, " +
                "item TEXT NOT NULL, " +
                "count INTEGER NOT NULL, " +
                "price INTEGER NOT NULL, " +
                "date INTEGER NOT NULL, " +
                "accepted INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);

            LOGGER.info("[{}] Created default tables for trade.", MogrulEconomy.MODID);
        } catch (SQLException e) {
            LOGGER.info("[{}] Failed to create default tables for trade.", e.getMessage());
        }

        // Bounties table.
        sql = "CREATE TABLE IF NOT EXISTS bounties (" +
                "uuid TEXT PRIMARY KEY NOT NULL, " +
                "price INTEGER NOT NULL DEFAULT 0 " +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);

            LOGGER.info("[{}] Created default tables for bounties.", MogrulEconomy.MODID);
        } catch (SQLException e) {
            LOGGER.info("[{}] Failed to create Default tables for bounties.", e.getMessage());
        }
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Database connection closed");
            } catch (SQLException e) {
                LOGGER.error("[{}] Failed to close connection to database: {}", MogrulEconomy.MODID, e.getMessage());
            }
        }
    }

    public static void addPlayer(ServerPlayer player) {
        String uuid = player.getStringUUID();

        String sql = "INSERT INTO economy (uuid) " +
                "VALUES (?) " +
                "ON CONFLICT (uuid) DO NOTHING";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                LOGGER.info("[{}] Added player [{}] to economy table with {}{}", MogrulEconomy.MODID, uuid, Config.currencySymbol, Config.startingCurrency);
            }
        } catch (SQLException e) {
            LOGGER.error("[{}] Failed to insert player into economy table: {}",  MogrulEconomy.MODID, e.getMessage());
        }
    }
}
