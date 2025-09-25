package main.java;

import application.usecase.AuthenticationUseCase;
import application.usecase.ProductManagementUseCase;
import application.usecase.CategoryManagementUseCase;
import application.usecase.BatchManagementUseCase;
import application.usecase.DiscountManagementUseCase;
import application.services.BillNumberService;
import application.services.AvailabilityService;
import application.services.MainStoreService;
import application.services.ShortageEventService;
import application.services.DiscountService;
import application.usecase.QuoteUseCase;
import application.usecase.ReceiveFromSupplierUseCase;
import application.usecase.TransferStockUseCase;
import application.usecase.SearchProductUseCase;
import application.usecase.OnlineCartUseCase;
import application.usecase.CheckoutUseCase;
import cli.cashier.checkout.CliCheckout;
import cli.manager.ReceiveToMainCLI;
import cli.manager.TransferFromMainCLI;
import cli.manager.batch.BatchManagementCLI;
import cli.manager.category.CategoryManagementCLI;
import cli.manager.product.ProductManagementCLI;
import cli.signin.LoginScreen;
import cli.cashier.CashierMenu;
import cli.manager.ManagerMenu;
import config.Db;
import domain.policies.FefoStrategy;
import infrastructure.concurrency.Tx;
import infrastructure.persistence.*;
import infrastructure.security.PasswordEncoder;
import infrastructure.events.SimpleBus;
import infrastructure.events.LowStockPrinter;
import cli.webshop.WebShopMenu;
import cli.SeedUsers;

public class App {
    public static void main(String[] args) {

        try (var db = new Db()) {
            var ds = db.getDataSource();
            var tx = new Tx(ds);

            // Repos
            var products   = new JdbcProductRepository(ds);
            var categories = new JdbcCategoryRepository(ds);
            var bills      = new JdbcBillRepository();
            var inventory  = new JdbcInventoryRepository(ds);
            var users      = new JdbcUserRepository(ds);
            var shortageRepo = new JdbcShortageEventRepository(ds);
            var bus = new SimpleBus();
            bus.subscribe(new LowStockPrinter());

            // Strategy / use cases
            var availabilitySvc = new AvailabilityService(tx, inventory);
            var mainStoreSvc = new MainStoreService(tx, inventory);
            var strategy   = new FefoStrategy(inventory);
            var billNums   = new BillNumberService(tx);
            var shortageSvc = new ShortageEventService(tx, shortageRepo);

            // WEB e-commerce repos
            var carts = new JdbcCartRepository(ds);
            var orders = new JdbcOrderRepository(ds);
            var payments = new JdbcPaymentRepository(ds);

            var quoteUC    = new QuoteUseCase(products);

            // usecase dependencies
            var invAdmin = new JdbcInventoryAdminRepository();
            var receiveUC = new ReceiveFromSupplierUseCase(tx, invAdmin);
            var transferUC = new TransferStockUseCase(tx, inventory, invAdmin, strategy);
            var categoryManagementUC = new CategoryManagementUseCase(categories);
            var productManagementUC = new ProductManagementUseCase(products, categoryManagementUC);
            var batchManagementUC = new BatchManagementUseCase(ds, inventory, products);
            var checkoutUC = new CheckoutUseCase(tx, products, bills, strategy, billNums, inventory, bus);
            var checkoutUCFull = new CheckoutUseCase(tx, products, inventory, availabilitySvc, quoteUC, shortageSvc, bills, strategy, billNums, bus, carts, orders, payments, strategy);

            // CLI units
            var receiveCLI  = new ReceiveToMainCLI(receiveUC);
            var transferCLI = new TransferFromMainCLI(transferUC, availabilitySvc, quoteUC, inventory, tx);
            var productManagementCLI = new ProductManagementCLI(productManagementUC, categoryManagementUC);
            var categoryManagementCLI = new CategoryManagementCLI(categoryManagementUC);
            var batchManagementCLI = new BatchManagementCLI(batchManagementUC);

            // Auth
            var encoder = new PasswordEncoder();
            var authUseCase = new AuthenticationUseCase(users, encoder);
            var searchUC = new SearchProductUseCase(products, availabilitySvc);

            // Discount system
            var discountRepository = new SqlDiscountRepository(ds);
            var discountManagementUC = new DiscountManagementUseCase(discountRepository);
            var discountService = new DiscountService(discountManagementUC);

            // Create checkout CLI with discount service
            var checkoutCLI = new CliCheckout(checkoutUC, strategy, quoteUC, availabilitySvc, mainStoreSvc, shortageSvc, discountService);
            var cashierMenu = new CashierMenu(checkoutCLI::run, ds);

            var onlineCartUC = new OnlineCartUseCase(carts, products, discountService);

            // Seed users
            SeedUsers.ensure(users, encoder);

            // Main application entry point
            var scanner = new java.util.Scanner(System.in);
            System.out.println("=== SYOS POS System ===");
            System.out.println("1. CLI Interface (Cashier/Manager)");
            System.out.println("2. Web Shop Interface");
            System.out.print("Choose interface (1 or 2): ");

            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                var login = new LoginScreen(authUseCase);
                AuthenticationUseCase.Session session = null;

                while (session == null) {
                    session = login.prompt();
                    if (session == null) {
                        System.out.println("\n1. Try again");
                        System.out.println("2. Exit");
                        System.out.print("Choose option (1 or 2): ");
                        String retryChoice = scanner.nextLine().trim();

                        if ("2".equals(retryChoice)) {
                            System.out.println("Exiting application...");
                            return;
                        }
                    }
                }

                final var finalSession = session;
                final var finalUsers = users;
                final var finalDs = ds;
                final var finalCheckoutCLI = checkoutCLI;
                final var finalShortageSvc = shortageSvc;
                final var finalReceiveCLI = receiveCLI;
                final var finalTransferCLI = transferCLI;
                final var finalProductManagementCLI = productManagementCLI;
                final var finalBatchManagementCLI = batchManagementCLI;
                final var finalCategoryManagementCLI = categoryManagementCLI;
                final var finalDiscountManagementUC = discountManagementUC;

                LoginScreen.route(session, cashierMenu::run, () -> {
                    // Get the actual User object for managers
                    var managerUser = finalUsers.findByUsername(finalSession.identifier())
                        .orElseThrow(() -> new RuntimeException("Manager user not found"));
                    var managerMenuWithUser = new ManagerMenu(finalDs, finalCheckoutCLI::run, finalShortageSvc, finalReceiveCLI::run, finalTransferCLI::run, finalProductManagementCLI, finalBatchManagementCLI, finalCategoryManagementCLI, managerUser, finalDiscountManagementUC);
                    managerMenuWithUser.run();
                });
            } else if ("2".equals(choice)) {
                // WEB shop login flow
                var webShop = new WebShopMenu(
                        scanner, authUseCase, authUseCase, searchUC,
                        onlineCartUC, checkoutUCFull, discountService);
                webShop.start();
            } else {
                System.out.println("Invalid choice. Exiting...");
            }
        }
    }
}
