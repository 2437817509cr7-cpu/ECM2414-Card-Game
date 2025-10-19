package cardgame;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Player logic (runs in its own thread):
 * - Preference value = player id (common rule in this coursework).
 * - Loop: atomically draw from left deck and discard one unwanted card to right deck.
 * - If the 4-card hand all equals the preference value, announce a win.
 * - Writes required messages to playerX_output.txt.
 */
public class Player implements Runnable {
    private final int id;
    private final CardDeck leftDeck;
    private final CardDeck rightDeck;
    private final List<Card> hand = new ArrayList<>(4);
    private final PrintWriter out;
    private final int preferred;
    private volatile boolean hasLoggedExit = false;
    private volatile boolean hasAnnouncedWin = false;

    public Player(int id, CardDeck leftDeck, CardDeck rightDeck) throws IOException {
        this.id = id;
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.preferred = id;
        this.out = new PrintWriter(new FileWriter("player" + id + "_output.txt"), true);
    }

    public int getId() {
        return id;
    }

    public void giveInitial(Card c) {
        hand.add(c);
    }

    // Print exactly like "1 1 2 3" (no brackets/commas).
    private String handString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(hand.get(i).getValue());
        }
        return sb.toString();
    }

    public void printInitialHand() {
        out.println("player " + id + " initial hand " + handString());
    }

    private boolean hasWinningHand() {
        if (hand.size() != 4) return false;
        for (Card c : hand) {
            if (c.getValue() != preferred) return false;
        }
        return true;
    }

    // Discard the first non-preferred value; simple and deterministic.
    // TODO: maybe try a smarter/random strategy later if needed.
    private Card chooseDiscard() {
        for (Card c : hand) {
            if (c.getValue() != preferred) {
                return c;
            }
        }
        // If all equal (rare), just drop the first.
        return hand.get(0);
    }

    private void logInformedAndExitIfNeeded() {
        if (!hasLoggedExit) {
            int winner = CardGame.getWinnerId();
            if (winner > 0 && winner != id) {
                // Wording per typical spec: winner informs the others.
                out.println("player " + winner + " has informed player " + id + " that player " + winner + " has won");
                out.println("player " + id + " exits");
                out.println("player " + id + " hand: " + handString());
            }
            hasLoggedExit = true;
            out.flush();
            out.close();
        }
    }

    @Override
    public void run() {
        try {
            // If the initial 4 cards already win (happens), claim immediately.
            if (hasWinningHand() && CardGame.trySetWinner(id)) {
                System.out.println("player " + id + " wins");
                out.println("player " + id + " wins");
                out.println("player " + id + " final hand: " + handString());
                hasAnnouncedWin = true;
            }

            while (!CardGame.isGameOver()) {
                if (hasAnnouncedWin) break;

                // One atomic turn: lock both decks in a fixed order, replace a card with the drawn one,
                // and discard the replaced card to the right deck. Hand size stays at 4 the whole time.
                Card drawn = null;
                Card toDiscard = null;

                CardDeck first = (leftDeck.getDeckId() < rightDeck.getDeckId()) ? leftDeck : rightDeck;
                CardDeck second = (first == leftDeck) ? rightDeck : leftDeck;

                synchronized (first) {
                    synchronized (second) {
                        drawn = leftDeck.draw();
                        if (drawn != null) {
                            toDiscard = chooseDiscard();
                            int idx = hand.indexOf(toDiscard);
                            // Replace the chosen card with the drawn one (never reach 5 cards)
                            hand.set(idx, drawn);
                            rightDeck.discard(toDiscard);
                        }
                    }
                }

                if (drawn == null) {
                    // Left deck was empty for the moment; try again later.
                    Thread.yield();
                    continue;
                }

                // Logs for this move
                out.println("player " + id + " draws a " + drawn.getValue() + " from deck " + leftDeck.getDeckId());
                out.println("player " + id + " discards a " + toDiscard.getValue() + " to deck " + rightDeck.getDeckId());
                out.println("player " + id + " current hand is " + handString());

                // Check for win after the atomic move
                if (hasWinningHand() && CardGame.trySetWinner(id)) {
                    System.out.println("player " + id + " wins");
                    out.println("player " + id + " wins");
                    out.println("player " + id + " final hand: " + handString());
                    hasAnnouncedWin = true;
                    break;
                }
            }
        } finally {
            // Winner does not add "informed/exits" lines; non-winners do.
            if (CardGame.getWinnerId() != id) {
                logInformedAndExitIfNeeded();
            } else {
                // Winner still needs to close the stream.
                if (!hasLoggedExit) {
                    hasLoggedExit = true;
                    out.flush();
                    out.close();
                }
            }
        }
    }
}
