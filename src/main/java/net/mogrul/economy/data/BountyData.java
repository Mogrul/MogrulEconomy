package net.mogrul.economy.data;

public class BountyData {
    public String playerUUID;
    public int bounty;

    public BountyData(String playerUUID, int bounty) {
        this.playerUUID = playerUUID;
        this.bounty = bounty;
    }
}
