package cardgame;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CardTest {
    @Test
    void testCardValue() {
        Card c = new Card(5);
        assertEquals(5, c.getValue());
    }
}
