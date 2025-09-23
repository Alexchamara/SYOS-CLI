package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.usecase.BatchManagementUseCase;
import application.usecase.BatchManagementUseCase.CreateBatchRequest;
import application.usecase.BatchManagementUseCase.UpdateBatchRequest;
import application.usecase.BatchManagementUseCase.BatchInfo;
import application.usecase.BatchManagementUseCase.CreateResult;
import application.usecase.BatchManagementUseCase.UpdateResult;
import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.product.Product;
import domain.repository.InventoryRepository;
import domain.repository.ProductRepository;
import domain.shared.Code;
import domain.shared.Money;
import domain.shared.Quantity;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@DisplayName("BatchManagementUseCase Tests")
class BatchManagementUseCaseTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    private BatchManagementUseCase batchManagementUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchManagementUseCase = new BatchManagementUseCase(dataSource, inventoryRepository, productRepository);
    }

    @Nested
    @DisplayName("Create Batch Request Tests")
    class CreateBatchRequestTests {

        @Test
        @DisplayName("Should create CreateBatchRequest correctly")
        void shouldCreateCreateBatchRequestCorrectly() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 100;

            // When
            CreateBatchRequest request = new CreateBatchRequest(productCode, location, expiry, quantity);

            // Then
            assertEquals(productCode, request.productCode());
            assertEquals(location, request.location());
            assertEquals(expiry, request.expiry());
            assertEquals(quantity, request.quantity());
        }

        @Test
        @DisplayName("Should handle different stock locations")
        void shouldHandleDifferentStockLocations() {
            // Given
            String productCode = "PROD001";
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 100;

            // When
            CreateBatchRequest mainStoreRequest = new CreateBatchRequest(productCode, StockLocation.MAIN_STORE, expiry, quantity);
            CreateBatchRequest shelfRequest = new CreateBatchRequest(productCode, StockLocation.SHELF, expiry, quantity);
            CreateBatchRequest webRequest = new CreateBatchRequest(productCode, StockLocation.WEB, expiry, quantity);

            // Then
            assertEquals(StockLocation.MAIN_STORE, mainStoreRequest.location());
            assertEquals(StockLocation.SHELF, shelfRequest.location());
            assertEquals(StockLocation.WEB, webRequest.location());
        }
    }

    @Nested
    @DisplayName("Update Batch Request Tests")
    class UpdateBatchRequestTests {

        @Test
        @DisplayName("Should create UpdateBatchRequest correctly")
        void shouldCreateUpdateBatchRequestCorrectly() {
            // Given
            long batchId = 123L;
            LocalDate expiry = LocalDate.now().plusDays(60);
            int quantity = 200;

            // When
            UpdateBatchRequest request = new UpdateBatchRequest(batchId, expiry, quantity);

            // Then
            assertEquals(batchId, request.batchId());
            assertEquals(expiry, request.expiry());
            assertEquals(quantity, request.quantity());
        }
    }

    @Nested
    @DisplayName("Batch Info Tests")
    class BatchInfoTests {

        @Test
        @DisplayName("Should create BatchInfo from Batch correctly")
        void shouldCreateBatchInfoFromBatchCorrectly() {
            // Given
            long id = 456L;
            Code productCode = new Code("PROD001");
            StockLocation location = StockLocation.MAIN_STORE;
            LocalDateTime receivedAt = LocalDateTime.now();
            LocalDate expiry = LocalDate.now().plusDays(30);
            Quantity quantity = new Quantity(100);

            Batch batch = new Batch(id, productCode, location, receivedAt, expiry, quantity);

            // When
            BatchInfo batchInfo = new BatchInfo(batch);

            // Then
            assertEquals(id, batchInfo.id());
            assertEquals("PROD001", batchInfo.productCode());
            assertEquals(location, batchInfo.location());
            assertEquals(receivedAt, batchInfo.receivedAt());
            assertEquals(expiry, batchInfo.expiry());
            assertEquals(100, batchInfo.quantity());
        }

        @Test
        @DisplayName("Should handle different batch properties")
        void shouldHandleDifferentBatchProperties() {
            // Given
            Batch batch1 = new Batch(1L, new Code("PROD001"), StockLocation.SHELF,
                LocalDateTime.now().minusDays(1), LocalDate.now().plusDays(10), new Quantity(50));
            Batch batch2 = new Batch(2L, new Code("PROD002"), StockLocation.WEB,
                LocalDateTime.now().minusDays(2), LocalDate.now().plusDays(20), new Quantity(75));

            // When
            BatchInfo info1 = new BatchInfo(batch1);
            BatchInfo info2 = new BatchInfo(batch2);

            // Then
            assertNotEquals(info1.id(), info2.id());
            assertNotEquals(info1.productCode(), info2.productCode());
            assertNotEquals(info1.location(), info2.location());
            assertNotEquals(info1.quantity(), info2.quantity());
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should have correct CreateResult values")
        void shouldHaveCorrectCreateResultValues() {
            // Then
            assertEquals(3, CreateResult.values().length);
            assertEquals(CreateResult.SUCCESS, CreateResult.valueOf("SUCCESS"));
            assertEquals(CreateResult.PRODUCT_NOT_EXISTS, CreateResult.valueOf("PRODUCT_NOT_EXISTS"));
            assertEquals(CreateResult.INVALID_INPUT, CreateResult.valueOf("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Should have correct UpdateResult values")
        void shouldHaveCorrectUpdateResultValues() {
            // Then
            assertEquals(3, UpdateResult.values().length);
            assertEquals(UpdateResult.SUCCESS, UpdateResult.valueOf("SUCCESS"));
            assertEquals(UpdateResult.NOT_FOUND, UpdateResult.valueOf("NOT_FOUND"));
            assertEquals(UpdateResult.INVALID_INPUT, UpdateResult.valueOf("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create BatchManagementUseCase with required dependencies")
        void shouldCreateBatchManagementUseCaseWithRequiredDependencies() {
            // When
            BatchManagementUseCase useCase = new BatchManagementUseCase(dataSource, inventoryRepository, productRepository);

            // Then
            assertNotNull(useCase);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero quantity in CreateBatchRequest")
        void shouldHandleZeroQuantityInCreateBatchRequest() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;
            LocalDate expiry = LocalDate.now().plusDays(30);
            int quantity = 0;

            // When
            CreateBatchRequest request = new CreateBatchRequest(productCode, location, expiry, quantity);

            // Then
            assertEquals(0, request.quantity());
        }

        @Test
        @DisplayName("Should handle past expiry date in CreateBatchRequest")
        void shouldHandlePastExpiryDateInCreateBatchRequest() {
            // Given
            String productCode = "PROD001";
            StockLocation location = StockLocation.MAIN_STORE;
            LocalDate expiry = LocalDate.now().minusDays(10); // Past date
            int quantity = 100;

            // When
            CreateBatchRequest request = new CreateBatchRequest(productCode, location, expiry, quantity);

            // Then
            assertTrue(request.expiry().isBefore(LocalDate.now()));
        }

        @Test
        @DisplayName("Should handle negative quantity in UpdateBatchRequest")
        void shouldHandleNegativeQuantityInUpdateBatchRequest() {
            // Given
            long batchId = 123L;
            LocalDate expiry = LocalDate.now().plusDays(60);
            int quantity = -50; // Negative quantity

            // When
            UpdateBatchRequest request = new UpdateBatchRequest(batchId, expiry, quantity);

            // Then
            assertEquals(-50, request.quantity());
        }
    }
}
