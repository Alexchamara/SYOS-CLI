package application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import application.services.AvailabilityService;
import application.services.BillNumberService;
import application.services.ShortageEventService;
import domain.billing.Bill;
import domain.events.EventPublisher;
import domain.inventory.StockLocation;
import domain.policies.BatchSelectionStrategy;
import domain.policies.FefoStrategy;
import domain.pricing.DiscountPolicy;
import domain.product.Product;
import domain.repository.BillRepository;
import domain.repository.InventoryRepository;
import domain.repository.OrderRepository;
import domain.repository.PaymentRepository;
import domain.repository.ProductRepository;
import domain.shared.Code;
import domain.shared.Money;
import infrastructure.concurrency.Tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@DisplayName("CheckoutUseCase Tests")
class CheckoutUseCaseTest {

    @Mock private Tx tx;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private QuoteUseCase quoteUseCase;
    @Mock private ShortageEventService shortageService;
    @Mock private BillRepository billRepository;
    @Mock private BatchSelectionStrategy batchStrategy;
    @Mock private BillNumberService billNumberService;
    @Mock private EventPublisher eventPublisher;
    @Mock private domain.repository.CartRepository cartRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private FefoStrategy fefoStrategy;
    @Mock private DiscountPolicy discountPolicy;
    @Mock private Connection connection;

    private CheckoutUseCase checkoutUseCase;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        checkoutUseCase = new CheckoutUseCase(
            tx, productRepository, inventoryRepository, availabilityService,
            quoteUseCase, shortageService, billRepository, batchStrategy,
            billNumberService, eventPublisher, cartRepository,
            orderRepository, paymentRepository, fefoStrategy
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Cash Checkout Tests")
    class CashCheckoutTests {

        @Test
        @DisplayName("Should perform cash checkout successfully")
        void shouldPerformCashCheckoutSuccessfully() {
            // Given
            CheckoutUseCase.CashItem item1 = new CheckoutUseCase.CashItem("PROD001", 2);
            CheckoutUseCase.CashItem item2 = new CheckoutUseCase.CashItem("PROD002", 1);
            List<CheckoutUseCase.CashItem> cart = List.of(item1, item2);

            Product product1 = new Product(new Code("PROD001"), "Product 1", Money.of(new BigDecimal("10.00")));
            Product product2 = new Product(new Code("PROD002"), "Product 2", Money.of(new BigDecimal("20.00")));

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, Bill> function = invocation.getArgument(0);
                return function.apply(connection);
            });

            when(billNumberService.next("COUNTER")).thenReturn("BILL001");
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.of(product1));
            when(productRepository.findByCode(new Code("PROD002"))).thenReturn(Optional.of(product2));
            when(discountPolicy.discountFor(any())).thenReturn(Money.of(new BigDecimal("2.00")));

            // When
            Bill result = checkoutUseCase.checkoutCash(cart, 5000L, StockLocation.SHELF, discountPolicy, "COUNTER");

            // Then
            assertNotNull(result);
            verify(billRepository).save(eq(connection), any(Bill.class));
            verify(batchStrategy).deductUpTo(eq(connection), eq(new Code("PROD001")), eq(2), eq(StockLocation.SHELF));
            verify(batchStrategy).deductUpTo(eq(connection), eq(new Code("PROD002")), eq(1), eq(StockLocation.SHELF));
        }

        @Test
        @DisplayName("Should throw exception when product not found in cash checkout")
        void shouldThrowExceptionWhenProductNotFoundInCashCheckout() {
            // Given
            CheckoutUseCase.CashItem item = new CheckoutUseCase.CashItem("NONEXISTENT", 1);
            List<CheckoutUseCase.CashItem> cart = List.of(item);

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, Bill> function = invocation.getArgument(0);
                return function.apply(connection);
            });

            when(productRepository.findByCode(new Code("NONEXISTENT"))).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                checkoutUseCase.checkoutCash(cart, 1000L, StockLocation.SHELF, discountPolicy, "COUNTER"));
        }

        @Test
        @DisplayName("Should publish low stock event when threshold reached")
        void shouldPublishLowStockEventWhenThresholdReached() {
            // Given
            CheckoutUseCase.CashItem item = new CheckoutUseCase.CashItem("PROD001", 1);
            List<CheckoutUseCase.CashItem> cart = List.of(item);

            Product product = new Product(new Code("PROD001"), "Product 1", Money.of(new BigDecimal("10.00")));

            when(tx.inTx(any())).thenAnswer(invocation -> {
                Function<Connection, Bill> function = invocation.getArgument(0);
                return function.apply(connection);
            });

            when(billNumberService.next("COUNTER")).thenReturn("BILL001");
            when(productRepository.findByCode(new Code("PROD001"))).thenReturn(Optional.of(product));
            when(discountPolicy.discountFor(any())).thenReturn(Money.of(BigDecimal.ZERO));
            when(inventoryRepository.remainingQuantity(connection, "PROD001", "SHELF")).thenReturn(30); // Below threshold of 50

            // When
            checkoutUseCase.checkoutCash(cart, 1000L, StockLocation.SHELF, discountPolicy, "COUNTER");

            // Then
            verify(eventPublisher).publish(any());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CheckoutUseCase with full constructor")
        void shouldCreateCheckoutUseCaseWithFullConstructor() {
            // When
            CheckoutUseCase useCase = new CheckoutUseCase(
                tx, productRepository, inventoryRepository, availabilityService,
                quoteUseCase, shortageService, billRepository, batchStrategy,
                billNumberService, eventPublisher, cartRepository,
                orderRepository, paymentRepository, fefoStrategy
            );

            // Then
            assertNotNull(useCase);
        }

        @Test
        @DisplayName("Should create CheckoutUseCase with cash-only constructor")
        void shouldCreateCheckoutUseCaseWithCashOnlyConstructor() {
            // When
            CheckoutUseCase useCase = new CheckoutUseCase(
                tx, productRepository, billRepository, batchStrategy,
                billNumberService, inventoryRepository, eventPublisher
            );

            // Then
            assertNotNull(useCase);
        }
    }

    @Nested
    @DisplayName("Cash Item Tests")
    class CashItemTests {

        @Test
        @DisplayName("Should create CashItem correctly")
        void shouldCreateCashItemCorrectly() {
            // When
            CheckoutUseCase.CashItem item = new CheckoutUseCase.CashItem("PROD001", 5);

            // Then
            assertEquals("PROD001", item.code());
            assertEquals(5, item.qty());
        }

        @Test
        @DisplayName("Should support CashItem equality")
        void shouldSupportCashItemEquality() {
            // Given
            CheckoutUseCase.CashItem item1 = new CheckoutUseCase.CashItem("PROD001", 5);
            CheckoutUseCase.CashItem item2 = new CheckoutUseCase.CashItem("PROD001", 5);
            CheckoutUseCase.CashItem item3 = new CheckoutUseCase.CashItem("PROD002", 5);

            // Then
            assertEquals(item1, item2);
            assertNotEquals(item1, item3);
            assertEquals(item1.hashCode(), item2.hashCode());
        }
    }
}
