package cardgame;

/**
 * Simple immutable card holding a non-negative integer.
 */
public class Card {
    private final int value;

    public Card(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Card value must be non-negative");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Print as the number itself to keep logs clean.
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
