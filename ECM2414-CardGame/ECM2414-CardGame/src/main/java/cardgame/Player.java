package cardgame;

public class Player extends Thread {
    private final int playerId;
    public Player(int id) { this.playerId = id; }
    public void run() { System.out.println("Player " + playerId + " running..."); }
}
