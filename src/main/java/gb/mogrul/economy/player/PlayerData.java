package gb.mogrul.economy.player;

public class PlayerData {
    public String UUID;
    public String username;
    public int currency;

    public PlayerData(String UUID, String username, int currency) {
        this.UUID = UUID;
        this.username = username;
        this.currency = currency;
    }
}
