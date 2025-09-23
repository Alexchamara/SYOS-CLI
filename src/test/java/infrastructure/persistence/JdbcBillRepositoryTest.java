package infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import infrastructure.persistence.JdbcBillRepository;
import domain.billing.Bill;
import domain.billing.BillLine;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

@DisplayName("JdbcBillRepository Tests")
class JdbcBillRepositoryTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement billStatement;

    @Mock
    private PreparedStatement lineStatement;

    @Mock
    private ResultSet generatedKeys;

    private JdbcBillRepository billRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        billRepository = new JdbcBillRepository();

        when(connection.prepareStatement(contains("INSERT INTO bill"), eq(Statement.RETURN_GENERATED_KEYS)))
            .thenReturn(billStatement);
        when(connection.prepareStatement(contains("INSERT INTO bill_line")))
            .thenReturn(lineStatement);
        when(billStatement.getGeneratedKeys()).thenReturn(generatedKeys);
    }

    @Nested
    @DisplayName("Save Bill Tests")
    class SaveBillTests {

        @Test
        @DisplayName("Should save bill with lines successfully")
        void shouldSaveBillWithLinesSuccessfully() throws SQLException {
            // Given
            BillLine line1 = new BillLine(new Code("PROD001"), "Laptop", new Quantity(1), Money.of(new BigDecimal("999.99")));
            BillLine line2 = new BillLine(new Code("PROD002"), "Mouse", new Quantity(2), Money.of(new BigDecimal("25.00")));

            Bill bill = new Bill.Builder()
                .serial("C-000001")
                .addLine(line1)
                .addLine(line2)
                .cash(Money.of(new BigDecimal("1100.00")))
                .discount(Money.of(new BigDecimal("49.99")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(123L);

            // When
            long result = billRepository.save(connection, bill);

            // Then
            assertEquals(123L, result);

            // Verify bill insertion
            verify(billStatement).setString(1, "C-000001");
            verify(billStatement).setTimestamp(eq(2), any(Timestamp.class));
            verify(billStatement).setLong(3, 104999L); // subtotal in cents
            verify(billStatement).setLong(4, 4999L);   // discount in cents
            verify(billStatement).setLong(5, 100000L); // total in cents
            verify(billStatement).setLong(6, 110000L); // cash in cents
            verify(billStatement).setLong(7, 10000L);  // change in cents
            verify(billStatement).executeUpdate();

            // Verify line insertions
            verify(lineStatement, times(2)).addBatch();
            verify(lineStatement).executeBatch();
        }

        @Test
        @DisplayName("Should save bill with single line")
        void shouldSaveBillWithSingleLine() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Single Product", new Quantity(3), Money.of(new BigDecimal("10.00")));

            Bill bill = new Bill.Builder()
                .serial("C-000002")
                .addLine(line)
                .cash(Money.of(new BigDecimal("30.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(124L);

            // When
            long result = billRepository.save(connection, bill);

            // Then
            assertEquals(124L, result);
            verify(lineStatement, times(1)).addBatch();
            verify(lineStatement).executeBatch();
        }

        @Test
        @DisplayName("Should save bill with no discount")
        void shouldSaveBillWithNoDiscount() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("50.00")));

            Bill bill = new Bill.Builder()
                .serial("C-000003")
                .addLine(line)
                .cash(Money.of(new BigDecimal("50.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(125L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(billStatement).setLong(4, 0L); // discount = 0 cents
            verify(billStatement).setLong(7, 0L); // change = 0 cents
        }

        @Test
        @DisplayName("Should handle SQL exceptions during bill save")
        void shouldHandleSQLExceptionsDuringBillSave() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("25.00")));
            Bill bill = new Bill.Builder()
                .serial("C-000004")
                .addLine(line)
                .cash(Money.of(new BigDecimal("25.00")))
                .build();

            SQLException sqlException = new SQLException("Bill insert failed");
            when(billStatement.executeUpdate()).thenThrow(sqlException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.save(connection, bill));
            assertEquals(sqlException, exception.getCause());
        }

        @Test
        @DisplayName("Should handle failure to get generated keys")
        void shouldHandleFailureToGetGeneratedKeys() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("25.00")));
            Bill bill = new Bill.Builder()
                .serial("C-000005")
                .addLine(line)
                .cash(Money.of(new BigDecimal("25.00")))
                .build();

            when(generatedKeys.next()).thenReturn(false); // No generated keys

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.save(connection, bill));
            // Should fail when trying to get generated key
        }

        @Test
        @DisplayName("Should handle line insertion failures")
        void shouldHandleLineInsertionFailures() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("25.00")));
            Bill bill = new Bill.Builder()
                .serial("C-000006")
                .addLine(line)
                .cash(Money.of(new BigDecimal("25.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(126L);

            SQLException lineException = new SQLException("Line insert failed");
            when(lineStatement.executeBatch()).thenThrow(lineException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.save(connection, bill));
            assertEquals(lineException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Bill Line Processing Tests")
    class BillLineProcessingTests {

        @Test
        @DisplayName("Should process multiple bill lines correctly")
        void shouldProcessMultipleBillLinesCorrectly() throws SQLException {
            // Given
            BillLine line1 = new BillLine(new Code("PROD001"), "Product 1", new Quantity(2), Money.of(new BigDecimal("15.00")));
            BillLine line2 = new BillLine(new Code("PROD002"), "Product 2", new Quantity(1), Money.of(new BigDecimal("30.00")));
            BillLine line3 = new BillLine(new Code("PROD003"), "Product 3", new Quantity(5), Money.of(new BigDecimal("5.00")));

            Bill bill = new Bill.Builder()
                .serial("C-000007")
                .addLine(line1)
                .addLine(line2)
                .addLine(line3)
                .cash(Money.of(new BigDecimal("85.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(127L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(lineStatement, times(3)).addBatch();
            verify(lineStatement).executeBatch();

            // Verify line parameters for each line
            verify(lineStatement).setLong(1, 127L); // bill_id
            verify(lineStatement).setString(2, "PROD001");
            verify(lineStatement).setString(3, "Product 1");
            verify(lineStatement).setInt(4, 2);
            verify(lineStatement).setLong(5, 1500L); // unit_price_cents
            verify(lineStatement).setLong(6, 3000L); // line_total_cents
        }

        @Test
        @DisplayName("Should handle bill with no lines")
        void shouldHandleBillWithNoLines() throws SQLException {
            // Given
            Bill bill = new Bill.Builder()
                .serial("C-000008")
                .cash(Money.of(BigDecimal.ZERO))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(128L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(lineStatement, never()).addBatch();
            verify(lineStatement).executeBatch(); // Should still call executeBatch
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly on success")
        void shouldCloseResourcesProperlyOnSuccess() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("20.00")));
            Bill bill = new Bill.Builder()
                .serial("C-000009")
                .addLine(line)
                .cash(Money.of(new BigDecimal("20.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(129L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(generatedKeys).close();
            verify(billStatement).close();
            verify(lineStatement).close();
        }

        @Test
        @DisplayName("Should close resources properly on exception")
        void shouldCloseResourcesProperlyOnException() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("20.00")));
            Bill bill = new Bill.Builder()
                .serial("C-000010")
                .addLine(line)
                .cash(Money.of(new BigDecimal("20.00")))
                .build();

            when(billStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> billRepository.save(connection, bill));
            verify(billStatement).close();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large money amounts")
        void shouldHandleVeryLargeMoneyAmounts() throws SQLException {
            // Given
            BillLine line = new BillLine(new Code("PROD001"), "Expensive Item", new Quantity(1),
                Money.of(new BigDecimal("999999.99")));

            Bill bill = new Bill.Builder()
                .serial("C-000011")
                .addLine(line)
                .cash(Money.of(new BigDecimal("1000000.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(130L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(billStatement).setLong(5, 99999999L); // total in cents
            verify(billStatement).setLong(6, 100000000L); // cash in cents
        }

        @Test
        @DisplayName("Should handle bills with very long serials")
        void shouldHandleBillsWithVeryLongSerials() throws SQLException {
            // Given
            String longSerial = "COUNTER-TRANSACTION-" + "X".repeat(100);
            BillLine line = new BillLine(new Code("PROD001"), "Product", new Quantity(1), Money.of(new BigDecimal("10.00")));

            Bill bill = new Bill.Builder()
                .serial(longSerial)
                .addLine(line)
                .cash(Money.of(new BigDecimal("10.00")))
                .build();

            when(generatedKeys.next()).thenReturn(true);
            when(generatedKeys.getLong(1)).thenReturn(131L);

            // When
            billRepository.save(connection, bill);

            // Then
            verify(billStatement).setString(1, longSerial);
        }
    }
}
