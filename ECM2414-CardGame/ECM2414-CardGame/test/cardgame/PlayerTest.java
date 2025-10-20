package cardgame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests around a single player's behavior. We keep things small and deterministic.
 * Uses reflection only for resetting CardGame static state and peeking hand size.
 */
public class PlayerTest {

    @BeforeEach
    @AfterEach
    void resetCardGameStatics() throws Exception {
        // Reset CardGame.GAME_OVER and WINNER_ID between tests
        Field gameOver = CardGame.class.getDeclaredField("GAME_OVER");
        gameOver.setAccessible(true);
        Object atomic = gameOver.get(null); // AtomicBoolean
        atomic.getClass().getMethod("set", boolean.class).invoke(atomic, false);

        Field winner = CardGame.class.getDeclaredField("WINNER_ID");
        winner.setAccessible(true);
        winner.setInt(null, -1);
    }

    @Test
    void initialWinningHand_announcesAndStops() throws Exception {
        CardDeck left = new CardDeck(1);
        CardDeck right = new CardDeck(2);
        Player p1 = new Player(1, left, right);

        // Give four preferred (=1) cards so the player wins immediately.
        p1.giveInitial(new Card(1));
        p1.giveInitial(new Card(1));
        p1.giveInitial(new Card(1));
        p1.giveInitial(new Card(1));
        p1.printInitialHand();

        Thread t = new Thread(p1);
        t.start();
        t.join();

        assertEquals(1, CardGame.getWinnerId(), "player 1 should win right away");

        // winner file should contain "wins" and "final hand:"
        String content = Files.readString(Paths.get("player1_output.txt"));
        assertTrue(content.contains("player 1 wins"));
        assertTrue(content.contains("player 1 final hand: 1 1 1 1"));
    }

    @Test
    void atomicDrawDiscard_keepsHandSizeFour_and_discardsNonPreferred() throws Exception {
        int id = 2; // preferred value = 2
        CardDeck left = new CardDeck(2);
        CardDeck right = new CardDeck(3);
        Player p2 = new Player(id, left, right);

        // Start hand: two preferred, two non-preferred
        p2.giveInitial(new Card(2));
        p2.giveInitial(new Card(5));
        p2.giveInitial(new Card(2));
        p2.giveInitial(new Card(7));
        p2.printInitialHand();

        // Put a few cards into the left deck so at least one turn happens.
        left.discard(new Card(9));
        left.discard(new Card(2));
        left.discard(new Card(4));

        Thread t = new Thread(p2);
        t.start();

        // Let the player take at least one atomic turn.
        Thread.sleep(60);

        // End the game to stop the loop.
        CardGame.trySetWinner(99);
        t.join();

        // Hand size should remain 4 (no transient 5 sticking around).
        Field handF = Player.class.getDeclaredField("hand");
        handF.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Card> hand = (List<Card>) handF.get(p2);
        assertEquals(4, hand.size());

        // Right deck should not contain the preferred value 2 (discard rule).
        for (int v : right.snapshotValues()) {
            assertNotEquals(id, v, "discard should avoid the preferred value");
        }
    }
}

