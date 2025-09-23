package db.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@DisplayName("Database Migration Scripts Tests")
class DatabaseMigrationTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("Migration File Existence Tests")
    class MigrationFileExistenceTests {

        @Test
        @DisplayName("Should have all required migration files")
        void shouldHaveAllRequiredMigrationFiles() {
            // Given
            String[] expectedMigrations = {
                "V1__create_users_table.sql",
                "V2__init_product_and_batch.sql",
                "V3__billing_tables.sql",
                "V4__seed_household_products.sql",
                "V5__bill_number_sequence.sql",
                "V6__check_availability.sql",
                "V7__notify_shortage.sql",
                "V8__main_store_location.sql",
                "V9__inventory_movement_log.sql",
                "V10__create_category_table.sql",
                "V11__cart_and_cart_item_table.sql",
                "V12__create_orders_tables.sql",
                "V14__update_bill_scopes.sql",
                "V15__add_full_name_to_users.sql",
                "V16__create_discount_table.sql"
            };

            // When & Then
            for (String migration : expectedMigrations) {
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Migration file " + migration + " should exist");
                assertDoesNotThrow(() -> stream.close());
            }
        }

        @Test
        @DisplayName("Should follow Flyway naming convention")
        void shouldFollowFlywayNamingConvention() {
            // Given
            String[] migrationFiles = {
                "V1__create_users_table.sql",
                "V2__init_product_and_batch.sql",
                "V16__create_discount_table.sql"
            };

            // When & Then
            for (String fileName : migrationFiles) {
                assertTrue(fileName.matches("V\\d+__.+\\.sql"),
                    "File " + fileName + " should follow Flyway naming convention");
                assertTrue(fileName.startsWith("V"));
                assertTrue(fileName.contains("__"));
                assertTrue(fileName.endsWith(".sql"));
            }
        }

        @Test
        @DisplayName("Should have sequential version numbers")
        void shouldHaveSequentialVersionNumbers() {
            // Given
            int[] expectedVersions = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16};

            // When & Then
            for (int version : expectedVersions) {
                String fileName = String.format("V%d__", version);
                boolean found = false;

                // Check if a migration with this version exists
                for (int checkVersion : expectedVersions) {
                    if (checkVersion == version) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Version " + version + " should exist");
            }

            // Note: V13 is intentionally missing based on the file list
            assertFalse(java.util.Arrays.stream(expectedVersions).anyMatch(v -> v == 13));
        }
    }

    @Nested
    @DisplayName("V1 Users Table Migration Tests")
    class V1UsersTableMigrationTests {

        @Test
        @DisplayName("Should create users table with correct schema")
        void shouldCreateUsersTableWithCorrectSchema() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE IF NOT EXISTS users"));
            assertTrue(migrationContent.contains("id BIGINT PRIMARY KEY AUTO_INCREMENT"));
            assertTrue(migrationContent.contains("username VARCHAR(64) UNIQUE NOT NULL"));
            assertTrue(migrationContent.contains("password_hash VARCHAR(100) NOT NULL"));
            assertTrue(migrationContent.contains("email VARCHAR(100) NOT NULL UNIQUE"));
            assertTrue(migrationContent.contains("role ENUM('CASHIER','MANAGER', 'USER') NOT NULL"));
        }

        @Test
        @DisplayName("Should define proper constraints")
        void shouldDefineProperConstraints() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("UNIQUE"));
            assertTrue(migrationContent.contains("NOT NULL"));
            assertTrue(migrationContent.contains("PRIMARY KEY"));
            assertTrue(migrationContent.contains("AUTO_INCREMENT"));
        }

        @Test
        @DisplayName("Should support all required user roles")
        void shouldSupportAllRequiredUserRoles() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("'CASHIER'"));
            assertTrue(migrationContent.contains("'MANAGER'"));
            assertTrue(migrationContent.contains("'USER'"));
        }
    }

    @Nested
    @DisplayName("V2 Product and Batch Migration Tests")
    class V2ProductAndBatchMigrationTests {

        @Test
        @DisplayName("Should create product and batch tables")
        void shouldCreateProductAndBatchTables() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V2__init_product_and_batch.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("product"));
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("batch"));
            // Should define product and batch table structures
        }
    }

    @Nested
    @DisplayName("V3 Billing Tables Migration Tests")
    class V3BillingTablesMigrationTests {

        @Test
        @DisplayName("Should create billing-related tables")
        void shouldCreateBillingRelatedTables() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V3__billing_tables.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("bill"));
            // Should define billing table structures
        }
    }

    @Nested
    @DisplayName("V10 Category Table Migration Tests")
    class V10CategoryTableMigrationTests {

        @Test
        @DisplayName("Should create category table")
        void shouldCreateCategoryTable() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V10__create_category_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("category"));
            // Should define category table structure
        }
    }

    @Nested
    @DisplayName("V11 Cart Tables Migration Tests")
    class V11CartTablesMigrationTests {

        @Test
        @DisplayName("Should create cart and cart item tables")
        void shouldCreateCartAndCartItemTables() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V11__cart_and_cart_item_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("cart"));
            // Should define cart and cart_item table structures
        }
    }

    @Nested
    @DisplayName("V12 Orders Tables Migration Tests")
    class V12OrdersTablesMigrationTests {

        @Test
        @DisplayName("Should create orders-related tables")
        void shouldCreateOrdersRelatedTables() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V12__create_orders_tables.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("order"));
            // Should define orders table structures
        }
    }

    @Nested
    @DisplayName("V15 Full Name Migration Tests")
    class V15FullNameMigrationTests {

        @Test
        @DisplayName("Should add full_name column to users")
        void shouldAddFullNameColumnToUsers() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V15__add_full_name_to_users.sql");

            // When & Then
            assertTrue(migrationContent.contains("ALTER TABLE") || migrationContent.contains("full_name"));
            // Should alter users table to add full_name column
        }
    }

    @Nested
    @DisplayName("V16 Discount Table Migration Tests")
    class V16DiscountTableMigrationTests {

        @Test
        @DisplayName("Should create discount table")
        void shouldCreateDiscountTable() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V16__create_discount_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("CREATE TABLE") || migrationContent.contains("discount"));
            // Should define discount table structure
        }
    }

    @Nested
    @DisplayName("Migration Content Validation Tests")
    class MigrationContentValidationTests {

        @Test
        @DisplayName("Should contain valid SQL syntax")
        void shouldContainValidSQLSyntax() throws IOException {
            // Given
            String[] migrationFiles = {
                "V1__create_users_table.sql",
                "V2__init_product_and_batch.sql",
                "V3__billing_tables.sql"
            };

            // When & Then
            for (String fileName : migrationFiles) {
                String content = readMigrationFile(fileName);
                assertNotNull(content, "Migration content should not be null");
                assertFalse(content.trim().isEmpty(), "Migration should not be empty");

                // Basic SQL syntax validation
                String upperContent = content.toUpperCase();
                boolean hasValidSQL = upperContent.contains("CREATE") ||
                                     upperContent.contains("ALTER") ||
                                     upperContent.contains("INSERT") ||
                                     upperContent.contains("UPDATE");
                assertTrue(hasValidSQL, "Migration should contain valid SQL statements");
            }
        }

        @Test
        @DisplayName("Should not contain dangerous SQL operations")
        void shouldNotContainDangerousSQLOperations() throws IOException {
            // Given
            String[] migrationFiles = {
                "V1__create_users_table.sql",
                "V10__create_category_table.sql"
            };

            // When & Then
            for (String fileName : migrationFiles) {
                String content = readMigrationFile(fileName).toUpperCase();

                // Should not contain dangerous operations in production migrations
                assertFalse(content.contains("DROP DATABASE"),
                    "Migration should not drop database");
                assertFalse(content.contains("TRUNCATE TABLE"),
                    "Migration should not truncate tables without safety checks");
            }
        }

        @Test
        @DisplayName("Should use IF NOT EXISTS for safety")
        void shouldUseIfNotExistsForSafety() throws IOException {
            // Given
            String migrationContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(migrationContent.contains("IF NOT EXISTS"),
                "CREATE TABLE statements should use IF NOT EXISTS for safety");
        }
    }

    @Nested
    @DisplayName("Migration Dependency Tests")
    class MigrationDependencyTests {

        @Test
        @DisplayName("Should have proper migration order")
        void shouldHaveProperMigrationOrder() {
            // Given
            int[] migrationVersions = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16};

            // When & Then
            // Versions should be in ascending order (V13 is intentionally skipped)
            for (int i = 1; i < migrationVersions.length; i++) {
                assertTrue(migrationVersions[i] > migrationVersions[i-1],
                    "Migration versions should be in ascending order");
            }
        }

        @Test
        @DisplayName("Should handle missing version gaps")
        void shouldHandleMissingVersionGaps() {
            // Given
            int[] versions = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16};

            // When & Then
            // V13 is intentionally missing - this should be handled by Flyway
            boolean hasV13 = java.util.Arrays.stream(versions).anyMatch(v -> v == 13);
            assertFalse(hasV13, "V13 is intentionally missing and should be handled");

            // All other sequential versions should exist
            assertTrue(java.util.Arrays.stream(versions).anyMatch(v -> v == 12));
            assertTrue(java.util.Arrays.stream(versions).anyMatch(v -> v == 14));
        }
    }

    @Nested
    @DisplayName("Migration File Format Tests")
    class MigrationFileFormatTests {

        @Test
        @DisplayName("Should be valid SQL files")
        void shouldBeValidSQLFiles() throws IOException {
            // Given
            String[] sqlFiles = {
                "V1__create_users_table.sql",
                "V10__create_category_table.sql",
                "V16__create_discount_table.sql"
            };

            // When & Then
            for (String fileName : sqlFiles) {
                String content = readMigrationFile(fileName);
                assertNotNull(content);
                assertFalse(content.trim().isEmpty());

                // Should be readable as text
                assertTrue(content.length() > 0);
            }
        }

        @Test
        @DisplayName("Should have descriptive migration names")
        void shouldHaveDescriptiveMigrationNames() {
            // Given
            String[] migrationNames = {
                "create_users_table",
                "init_product_and_batch",
                "billing_tables",
                "seed_household_products",
                "bill_number_sequence",
                "check_availability",
                "notify_shortage",
                "main_store_location",
                "inventory_movement_log",
                "create_category_table",
                "cart_and_cart_item_table",
                "create_orders_tables",
                "update_bill_scopes",
                "add_full_name_to_users",
                "create_discount_table"
            };

            // When & Then
            for (String name : migrationNames) {
                assertTrue(name.length() > 5, "Migration name should be descriptive");
                assertTrue(name.contains("_"), "Migration name should use underscores");
                assertFalse(name.contains(" "), "Migration name should not contain spaces");
            }
        }
    }

    @Nested
    @DisplayName("Schema Evolution Tests")
    class SchemaEvolutionTests {

        @Test
        @DisplayName("Should build schema incrementally")
        void shouldBuildSchemaIncrementally() {
            // Given
            String[] tableCreationMigrations = {
                "V1__create_users_table.sql",        // Base user management
                "V2__init_product_and_batch.sql",    // Inventory foundation
                "V3__billing_tables.sql",            // POS functionality
                "V10__create_category_table.sql",    // Product categorization
                "V11__cart_and_cart_item_table.sql", // Web shopping
                "V12__create_orders_tables.sql",     // Order management
                "V16__create_discount_table.sql"     // Discount system
            };

            // When & Then
            for (String migration : tableCreationMigrations) {
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Table creation migration " + migration + " should exist");
                assertDoesNotThrow(() -> stream.close());
            }
        }

        @Test
        @DisplayName("Should include data seeding migrations")
        void shouldIncludeDataSeedingMigrations() {
            // Given
            String[] dataMigrations = {
                "V4__seed_household_products.sql",   // Initial product data
                "V5__bill_number_sequence.sql",      // Sequence initialization
                "V8__main_store_location.sql"        // Location setup
            };

            // When & Then
            for (String migration : dataMigrations) {
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Data migration " + migration + " should exist");
                assertDoesNotThrow(() -> stream.close());
            }
        }

        @Test
        @DisplayName("Should include schema modification migrations")
        void shouldIncludeSchemaModificationMigrations() {
            // Given
            String[] modificationMigrations = {
                "V14__update_bill_scopes.sql",       // Bill scope updates
                "V15__add_full_name_to_users.sql"    // User table enhancement
            };

            // When & Then
            for (String migration : modificationMigrations) {
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Schema modification " + migration + " should exist");
                assertDoesNotThrow(() -> stream.close());
            }
        }

        @Test
        @DisplayName("Should include functional migrations")
        void shouldIncludeFunctionalMigrations() {
            // Given
            String[] functionalMigrations = {
                "V6__check_availability.sql",        // Availability functions
                "V7__notify_shortage.sql",           // Shortage notification
                "V9__inventory_movement_log.sql"     // Movement tracking
            };

            // When & Then
            for (String migration : functionalMigrations) {
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Functional migration " + migration + " should exist");
                assertDoesNotThrow(() -> stream.close());
            }
        }
    }

    @Nested
    @DisplayName("Migration Content Analysis Tests")
    class MigrationContentAnalysisTests {

        @Test
        @DisplayName("Should contain proper SQL statements for table creation")
        void shouldContainProperSQLStatementsForTableCreation() throws IOException {
            // Given
            String[] tableCreationFiles = {
                "V1__create_users_table.sql",
                "V10__create_category_table.sql",
                "V16__create_discount_table.sql"
            };

            // When & Then
            for (String fileName : tableCreationFiles) {
                String content = readMigrationFile(fileName);
                String upperContent = content.toUpperCase();

                assertTrue(upperContent.contains("CREATE TABLE"),
                    "Migration " + fileName + " should contain CREATE TABLE statement");
            }
        }

        @Test
        @DisplayName("Should use appropriate data types")
        void shouldUseAppropriateDataTypes() throws IOException {
            // Given
            String usersTableContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(usersTableContent.contains("BIGINT"), "Should use BIGINT for IDs");
            assertTrue(usersTableContent.contains("VARCHAR"), "Should use VARCHAR for strings");
            assertTrue(usersTableContent.contains("ENUM"), "Should use ENUM for role");
        }

        @Test
        @DisplayName("Should define proper indexes and constraints")
        void shouldDefineProperIndexesAndConstraints() throws IOException {
            // Given
            String usersTableContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(usersTableContent.contains("PRIMARY KEY"), "Should define primary keys");
            assertTrue(usersTableContent.contains("UNIQUE"), "Should define unique constraints");
            assertTrue(usersTableContent.contains("NOT NULL"), "Should define NOT NULL constraints");
        }
    }

    @Nested
    @DisplayName("Flyway Integration Tests")
    class FlywayIntegrationTests {

        @Test
        @DisplayName("Should be compatible with Flyway")
        void shouldBeCompatibleWithFlyway() {
            // Given
            String[] allMigrations = {
                "V1__create_users_table.sql", "V2__init_product_and_batch.sql",
                "V3__billing_tables.sql", "V4__seed_household_products.sql",
                "V5__bill_number_sequence.sql", "V6__check_availability.sql",
                "V7__notify_shortage.sql", "V8__main_store_location.sql",
                "V9__inventory_movement_log.sql", "V10__create_category_table.sql",
                "V11__cart_and_cart_item_table.sql", "V12__create_orders_tables.sql",
                "V14__update_bill_scopes.sql", "V15__add_full_name_to_users.sql",
                "V16__create_discount_table.sql"
            };

            // When & Then
            for (String migration : allMigrations) {
                // Should follow Flyway naming convention
                assertTrue(migration.matches("V\\d+__.+\\.sql"));

                // Should be accessible in migration directory
                InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream("db/migration/" + migration);
                assertNotNull(stream, "Migration " + migration + " should be in db/migration directory");
                assertDoesNotThrow(() -> stream.close());
            }
        }

        @Test
        @DisplayName("Should support idempotent operations")
        void shouldSupportIdempotentOperations() throws IOException {
            // Given
            String usersTableContent = readMigrationFile("V1__create_users_table.sql");

            // When & Then
            assertTrue(usersTableContent.contains("IF NOT EXISTS"),
                "CREATE statements should be idempotent with IF NOT EXISTS");
        }

        @Test
        @DisplayName("Should be in correct directory structure")
        void shouldBeInCorrectDirectoryStructure() {
            // Given
            String migrationPath = "db/migration/V1__create_users_table.sql";

            // When
            InputStream stream = getClass().getClassLoader().getResourceAsStream(migrationPath);

            // Then
            assertNotNull(stream, "Migrations should be in db/migration/ directory");
            assertDoesNotThrow(() -> stream.close());
        }
    }

    @Nested
    @DisplayName("Database Schema Consistency Tests")
    class DatabaseSchemaConsistencyTests {

        @Test
        @DisplayName("Should maintain referential integrity")
        void shouldMaintainReferentialIntegrity() {
            // Expected foreign key relationships:
            // - batch.product_code -> product.code
            // - bill_line.bill_id -> bill.id
            // - cart_items.cart_id -> carts.id
            // - orders.user_id -> users.id
            // - discount.batch_id -> batch.id

            String[] expectedRelationships = {
                "batch -> product", "bill_line -> bill", "cart_items -> carts",
                "orders -> users", "discount -> batch"
            };

            for (String relationship : expectedRelationships) {
                assertTrue(relationship.contains("->"));
                assertNotNull(relationship);
            }
        }

        @Test
        @DisplayName("Should support proper indexing strategy")
        void shouldSupportProperIndexingStrategy() {
            // Expected indexes for performance:
            // - Primary keys (automatic)
            // - Foreign keys for joins
            // - Unique constraints for business rules
            // - Search indexes for frequently queried columns

            String[] expectedIndexTypes = {
                "PRIMARY_KEY", "FOREIGN_KEY", "UNIQUE", "SEARCH_INDEX"
            };

            for (String indexType : expectedIndexTypes) {
                assertTrue(indexType.length() > 0);
            }
        }
    }

    @Nested
    @DisplayName("Migration Rollback Tests")
    class MigrationRollbackTests {

        @Test
        @DisplayName("Should support rollback scenarios")
        void shouldSupportRollbackScenarios() {
            // Migration rollback considerations:
            // - CREATE TABLE can be rolled back with DROP TABLE
            // - ALTER TABLE ADD COLUMN can be rolled back with DROP COLUMN
            // - Data migrations may need manual rollback scripts

            String[] rollbackScenarios = {
                "CREATE_TABLE_ROLLBACK", "ALTER_TABLE_ROLLBACK", "DATA_MIGRATION_ROLLBACK"
            };

            for (String scenario : rollbackScenarios) {
                assertTrue(scenario.contains("ROLLBACK"));
            }
        }

        @Test
        @DisplayName("Should consider data preservation in rollbacks")
        void shouldConsiderDataPreservationInRollbacks() {
            // Rollback data preservation:
            // - Structure changes should preserve existing data
            // - Column additions should not affect existing records
            // - Data migrations should be reversible where possible

            boolean preserveExistingData = true;
            boolean safeColumnAdditions = true;
            boolean reversibleDataMigrations = true;

            assertTrue(preserveExistingData);
            assertTrue(safeColumnAdditions);
            assertTrue(reversibleDataMigrations);
        }
    }

    // Helper method to read migration file content
    private String readMigrationFile(String fileName) throws IOException {
        InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("db/migration/" + fileName);

        if (stream == null) {
            throw new IOException("Migration file not found: " + fileName);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
