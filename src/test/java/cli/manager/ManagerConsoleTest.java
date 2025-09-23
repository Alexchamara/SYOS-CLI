package cli.manager;

import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cli.manager.ManagerConsole;

class ManagerConsoleTest {

    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    @DisplayName("prints menu and exits on 0")
    void printsMenuAndExitsOnZero() {
        // Given
        System.setIn(new ByteArrayInputStream("0\n".getBytes()));
        DataSource ds = mock(DataSource.class);
        ManagerConsole console = new ManagerConsole(ds);

        // When
        console.run();

        // Then
        String output = out.toString();
        assertTrue(output.contains("=== Manager Console ==="));
        assertTrue(output.contains("1) Daily Sales"));
        assertTrue(output.contains("2) End-of-Day Reshelve List"));
        assertTrue(output.contains("3) Reorder (< 50) Report"));
        assertTrue(output.contains("4) Stock Report (Batch-wise)"));
        assertTrue(output.contains("5) Bill Report (SHELF Sales)"));
        assertTrue(output.contains("6) Order Report (WEB Sales)"));
        assertTrue(output.contains("0) Back"));
        assertTrue(output.contains("Pick:"));
    }

    @Test
    @DisplayName("shows invalid message then exits")
    void showsInvalidMessageThenExits() {
        // Given: invalid choice, then 0 to exit
        System.setIn(new ByteArrayInputStream("x\n0\n".getBytes()));
        DataSource ds = mock(DataSource.class);
        ManagerConsole console = new ManagerConsole(ds);

        // When
        console.run();

        // Then
        String output = out.toString();
        assertTrue(output.contains("Invalid choice. Please select 0-6."));
    }

    @Test
    @DisplayName("trims input and exits on whitespace 0")
    void trimsInputAndExitsOnWhitespaceZero() {
        // Given: spaces around 0
        System.setIn(new ByteArrayInputStream("  0  \n".getBytes()));
        DataSource ds = mock(DataSource.class);
        ManagerConsole console = new ManagerConsole(ds);

        // When
        console.run();

        // Then
        String output = out.toString();
        assertTrue(output.contains("=== Manager Console ==="));
        // No invalid message should appear
        assertFalse(output.contains("Invalid choice"));
    }
}
