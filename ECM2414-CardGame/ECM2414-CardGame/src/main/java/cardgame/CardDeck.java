package cardgame;

import java.util.LinkedList;
import java.util.Queue;

public class CardDeck {
    private final int deckId;
    private final Queue<Card> cards = new LinkedList<>();

    public CardDeck(int id) { this.deckId = id; }
    public synchronized void addCard(Card c) { cards.add(c); }
    public synchronized Card drawCard() { return cards.poll(); }
}
