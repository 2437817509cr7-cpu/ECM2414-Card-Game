package cardgame;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end style checks using small temp packs and captured stdin/stdout.
 * Keeps the flow simple: one valid run, one invalid-then-valid run.
 */
public class CardGameTest {

    private PrintStream originalOut;
    private InputStream originalIn;
    private ByteArrayOutputStream outBuffer;

    @BeforeEach
    void setUpIO() {
        originalOut = System.out;
        originalIn = System.in;
        outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreIO() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @BeforeEach
    @AfterEach
    void resetCardGameStatics() throws Exception {
        var gameOver = CardGame.class.getDeclaredField("GAME_OVER");
        gameOver.setAccessible(true);
        Object atomic = gameOver.get(null);
        atomic.getClass().getMethod("set", boolean.class).invoke(atomic, false);

        var winner = CardGame.class.getDeclaredField("WINNER_ID");
        winner.setAccessible(true);
        winner.setInt(null, -1);
    }

    @Test
    void validPack_twoPlayers_producesOutputs_andConsoleWin() throws Exception {
        // n = 2 -> need 16 integers. Make player 1 win immediately: first 8 distribute so p1 gets four 1s.
        String packContent = String.join("\n",
                // 4 to players (round-robin: p1,p2,p1,p2,...)
                "1","2","1","2","1","9","1","8",
                // 8 cards for decks (any non-negative)
                "3","3","3","3","4","4","4","4"
        );
        Path pack = Files.createTempFile("pack_n2_", ".txt");
        Files.writeString(pack, packContent);

        // Provide stdin: n=2 then the temp pack path
        String input = "2\n" + pack.toAbsolutePath() + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        CardGame.main(new String[0]);

        // console should announce a winner
        String console = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("player 1 wins") || console.contains("player 2 wins"),
                "console should print a winner line");

        // output files exist for two players and two decks
        assertTrue(Files.exists(Path.of("player1_output.txt")));
        assertTrue(Files.exists(Path.of("player2_output.txt")));
        assertTrue(Files.exists(Path.of("deck1_output.txt")));
        assertTrue(Files.exists(Path.of("deck2_output.txt")));

        // deck file format: no brackets, space-separated numbers
        String deck1 = Files.readString(Path.of("deck1_output.txt"));
        assertTrue(deck1.startsWith("deck 1 contents: "));
        assertFalse(deck1.contains("["));
        assertFalse(deck1.contains("]"));
    }

    @Test
    void invalidThenValidPack_programKeepsPrompting_andEventuallyRuns() throws Exception {
        // Create a bad pack (wrong line count) and a good pack.
        Path bad = Files.createTempFile("badpack_", ".txt");
        Files.writeString(bad, "1\n2\n3\n"); // too short for any n>1

        // Valid for n=2 (16 ints)
        Path good = Files.createTempFile("goodpack_", ".txt");
        String content = String.join("\n",
                "0","0","1","1","2","2","3","3",  // to players
                "4","4","5","5","6","6","7","7"   // to decks
        );
        Files.writeString(good, content);

        // stdin: n=2, first give bad filename, then give good filename
        String input = "2\n" + bad.toAbsolutePath() + "\n" + good.toAbsolutePath() + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        CardGame.main(new String[0]); // should not throw; should eventually finish with the good pack

        // end-state: winner decided and files produced
        assertTrue(CardGame.getWinnerId() > 0, "someone should eventually win");
        assertTrue(Files.exists(Path.of("player1_output.txt")));
        assertTrue(Files.exists(Path.of("player2_output.txt")));
        assertTrue(Files.exists(Path.of("deck1_output.txt")));
        assertTrue(Files.exists(Path.of("deck2_output.txt")));
    }
}

