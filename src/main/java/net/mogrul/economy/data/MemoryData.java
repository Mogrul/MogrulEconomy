package net.mogrul.economy.data;

import java.util.HashMap;
import java.util.Map;

public class MemoryData {
    public static Map<String, PlayerData> players = new HashMap<>();
    public static Map<String, MobRewardData> mobRewards = new HashMap<>();
    public static Map<String, ShopData> shops = new HashMap<>();
    public static Map<String, BountyData> bounties = new HashMap<>();
}
