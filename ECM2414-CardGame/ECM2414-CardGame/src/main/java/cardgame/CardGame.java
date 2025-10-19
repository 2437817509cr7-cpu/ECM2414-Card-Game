package cardgame;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Program entry point:
 * 1) Read number of players (n) and a pack filename from stdin.
 * 2) Validate pack: exactly 8n non-negative integers.
 * 3) Deal: 4 cards per player; remaining cards into each player's left deck.
 * 4) Start n player threads and wait for them to finish.
 * 5) Write deckX_output.txt files.
 *
 * Kept straightforward on purpose.
 */
public class CardGame {
    private static final Scanner SC = new Scanner(System.in);
    private static final AtomicBoolean GAME_OVER = new AtomicBoolean(false);
    private static volatile int WINNER_ID = -1;

    public static boolean isGameOver() {
        return GAME_OVER.get();
    }

    public static int getWinnerId() {
        return WINNER_ID;
    }

    // Only the first successful call sets the winner.
    public static boolean trySetWinner(int playerId) {
        if (GAME_OVER.compareAndSet(false, true)) {
            WINNER_ID = playerId;
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        int n = readPlayerCount();
        List<Integer> packValues = readAndValidatePack(n);

        // Build cards
        List<Card> cards = new ArrayList<>(packValues.size());
        for (int v : packValues) cards.add(new Card(v));

        // Build decks
        CardDeck[] decks = new CardDeck[n];
        for (int i = 0; i < n; i++) decks[i] = new CardDeck(i + 1);

        // Create players (left deck = deck i, right deck = deck (i+1) mod n)
        Player[] players = new Player[n];
        try {
            for (int i = 0; i < n; i++) {
                CardDeck left = decks[i];
                CardDeck right = decks[(i + 1) % n];
                players[i] = new Player(i + 1, left, right);
            }
        } catch (IOException e) {
            System.err.println("Failed to open player output file: " + e.getMessage());
            return;
        }

        // Deal 4 cards to each player (round-robin)
        int cursor = 0;
        for (int r = 0; r < 4; r++) {
            for (int p = 0; p < n; p++) {
                players[p].giveInitial(cards.get(cursor++));
            }
        }

        // Now that everyone has 4 cards, print initial hands (first line in each file).
        for (Player pl : players) {
            pl.printInitialHand();
        }

        // Put the remaining 4n cards into each player's left deck.
        // Since pack size is 8n, after 4n to players there are 4n left, which split evenly.
        for (int d = 0; d < n; d++) {
            while ((cursor % n) == d && cursor < cards.size()) {
                decks[d].discard(cards.get(cursor++));
            }
        }
        // If anything remains (shouldn't with 8n), fall back to round-robin to decks.
        while (cursor < cards.size()) {
            decks[cursor % n].discard(cards.get(cursor++));
        }

        // Start player threads
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            ts[i] = new Thread(players[i], "player-" + (i + 1));
            ts[i].start();
        }

        // Wait for completion
        for (Thread t : ts) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Write deck outputs
        writeDeckOutputs(decks);
    }

    private static void writeDeckOutputs(CardDeck[] decks) {
        for (CardDeck d : decks) {
            String fname = "deck" + d.getDeckId() + "_output.txt";
            try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
                List<Integer> vals = d.snapshotValues();
                // "deck X contents: 1 2 3 4" (no brackets/commas)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < vals.size(); i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(vals.get(i));
                }
                pw.println("deck " + d.getDeckId() + " contents: " + sb);
            } catch (IOException e) {
                System.err.println("Failed to write " + fname + ": " + e.getMessage());
            }
        }
    }

    // Read number of players (> 1)
    private static int readPlayerCount() {
        while (true) {
            System.out.print("Please enter the number of players: ");
            String line = safeReadLine();
            try {
                int n = Integer.parseInt(line.trim());
                if (n > 1) return n;
                System.out.println("Number of players must be an integer > 1. try again...");
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer, try again...");
            }
        }
    }

    // Read and validate a pack of exactly 8n non-negative integers.
    private static List<Integer> readAndValidatePack(int n) {
        while (true) {
            System.out.print("Please enter a valid pack filename: ");
            String path = safeReadLine();
            List<Integer> values = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String s;
                while ((s = br.readLine()) != null) {
                    s = s.trim();
                    if (s.isEmpty()) continue;
                    int v = Integer.parseInt(s);
                    if (v < 0) throw new NumberFormatException("negative");
                    values.add(v);
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage() + " try again...");
                continue;
            } catch (NumberFormatException e) {
                System.out.println("Pack contains a value that is not a non-negative integer. try again...");
                continue;
            }

            int need = 8 * n;
            if (values.size() != need) {
                System.out.println("Pack must contain exactly " + need + " integers (found " + values.size() + "). try again...");
                continue;
            }
            return values;
        }
    }

    private static String safeReadLine() {
        try {
            return SC.nextLine();
        } catch (NoSuchElementException e) {
            // In some environments stdin might be odd; return empty so prompts repeat.
            return "";
        }
    }
}
