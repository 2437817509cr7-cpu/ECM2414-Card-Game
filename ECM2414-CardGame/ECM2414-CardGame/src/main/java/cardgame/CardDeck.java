package cardgame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe FIFO deck.
 * Uses synchronized methods for clarity and reliability.
 */
public class CardDeck {
    private final int deckId;
    private final Deque<Card> queue = new ArrayDeque<>();

    public CardDeck(int deckId) {
        this.deckId = deckId;
    }

    public int getDeckId() {
        return deckId;
    }

    // Take from the front; returns null if empty.
    public synchronized Card draw() {
        return queue.pollFirst();
    }

    // Add to the back.
    public synchronized void discard(Card c) {
        if (c == null) return;
        queue.addLast(c);
    }

    public synchronized int size() {
        return queue.size();
    }

    // Snapshot for deckX_output.txt
    public synchronized List<Integer> snapshotValues() {
        List<Integer> list = new ArrayList<>(queue.size());
        for (Card c : queue) list.add(c.getValue());
        return list;
    }
}
