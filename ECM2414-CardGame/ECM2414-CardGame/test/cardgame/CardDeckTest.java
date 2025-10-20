package cardgame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic FIFO and a light concurrency sanity check.
 */
public class CardDeckTest {

    @Test
    void fifoOrder_isPreserved() {
        CardDeck deck = new CardDeck(1);
        deck.discard(new Card(10));
        deck.discard(new Card(20));
        deck.discard(new Card(30));

        assertEquals(10, deck.draw().getValue());
        assertEquals(20, deck.draw().getValue());
        assertEquals(30, deck.draw().getValue());
        assertNull(deck.draw()); // empty now
    }

    @Test
    void concurrentDiscardAndDraw_balancesOut() throws InterruptedException {
        CardDeck deck = new CardDeck(2);
        int producers = 3;
        int perProducer = 200;
        int totalCards = producers * perProducer;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(producers + 1);

        // producers: push cards
        for (int p = 0; p < producers; p++) {
            final int base = p * perProducer;
            new Thread(() -> {
                try { start.await(); } catch (InterruptedException ignored) {}
                for (int i = 0; i < perProducer; i++) {
                    deck.discard(new Card(base + i));
                }
                done.countDown();
            }).start();
        }

        // consumer: pop until we read exactly totalCards
        List<Integer> seen = new ArrayList<>(totalCards);
        new Thread(() -> {
            try { start.await(); } catch (InterruptedException ignored) {}
            while (seen.size() < totalCards) {
                Card c = deck.draw();
                if (c != null) {
                    seen.add(c.getValue());
                } else {
                    Thread.yield();
                }
            }
            done.countDown();
        }).start();

        start.countDown();
        done.await();

        assertEquals(0, deck.size(), "all cards should have been drawn");
        assertEquals(totalCards, seen.size(), "we should draw exactly what was produced");
        // not asserting order here (multiple producers) — just the balance and safety.
    }
}

