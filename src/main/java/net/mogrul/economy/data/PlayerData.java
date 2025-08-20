package net.mogrul.economy.data;

public class PlayerData {
    public String UUID;
    public String name;
    public Integer currency;

    public PlayerData(String UUID, String name, Integer currency) {
        this.UUID = UUID;
        this.name = name;
        this.currency = currency;
    }
}
