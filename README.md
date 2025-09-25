# SYOS POS System

A comprehensive Point of Sale (POS) system built with Java 21, featuring both CLI and Web Shop interfaces for retail operations management.

## Overview

SYOS POS is an enterprise-grade point of sale system designed for retail businesses. It provides comprehensive inventory management, sales processing, user authentication, and reporting capabilities through both command-line and web interfaces.

## Features

### 🏪 Core POS Functions
- **Multi-Interface Support**: CLI interface for cashiers/managers and web shop for customers
- **User Authentication**: Role-based access control (Cashier/Manager)
- **Checkout System**: Full featured checkout with discount support
- **Bill Generation**: Automated bill numbering and printing
- **Payment Processing**: Multiple payment methods support

### 📦 Inventory Management
- **Product Management**: Create, update, delete products with categories
- **Batch Management**: Track product batches with expiration dates
- **Stock Location Management**: Main store, shelf, and web inventory tracking
- **FEFO Strategy**: First Expired, First Out inventory rotation
- **Stock Transfer**: Transfer inventory between locations (Main → Shelf/Web)
- **Receiving**: Receive stock from suppliers
- **Low Stock Alerts**: Automated shortage event notifications

### 💰 Pricing & Discounts
- **Dynamic Pricing**: Flexible pricing system
- **Discount Management**: Create and manage various discount types
- **Quote System**: Generate price quotes for customers

### 📊 Reporting & Analytics
- **Reorder Reports**: Identify products with stock below 50 units
- **Shortage Events**: Track and report inventory shortages
- **Sales Reports**: Comprehensive sales analytics
- **Inventory Reports**: Stock level and movement reports

### 🌐 E-commerce Integration
- **Web Shop Interface**: Customer-facing online store
- **Shopping Cart**: Online cart management
- **Order Processing**: Web order fulfillment
- **Payment Gateway**: Integrated payment processing

## Technology Stack

- **Java**: 21 (Latest LTS)
- **Database**: PostgreSQL (Production), H2 (Testing), MySQL (Current config)
- **Testing**: JUnit 5, Mockito, TestContainers
- **Logging**: SLF4J + Logback
- **Build Tool**: Maven
- **Architecture**: Clean Architecture with Domain-Driven Design

## Project Structure

```
src/
├── main/java/
│   ├── App.java                    # Main application entry point
│   ├── application/                # Application layer (Use Cases & Services)
│   │   ├── reports/               # Reporting functionality
│   │   ├── services/              # Application services
│   │   └── usecase/               # Business use cases
│   ├── cli/                       # Command Line Interface
│   │   ├── bill/                  # Bill printing
│   │   ├── cashier/               # Cashier interface
│   │   ├── manager/               # Manager interface
│   │   ├── signin/                # Authentication
│   │   └── webshop/               # Web shop CLI
│   ├── config/                    # Configuration
│   ├── domain/                    # Domain layer (Business Logic)
│   │   ├── billing/               # Billing domain
│   │   ├── events/                # Domain events
│   │   ├── inventory/             # Inventory management
│   │   ├── model/                 # Domain models
│   │   ├── policies/              # Business policies (FEFO)
│   │   ├── pricing/               # Pricing logic
│   │   ├── product/               # Product management
│   │   ├── repository/            # Repository interfaces
│   │   ├── shared/                # Shared domain objects
│   │   └── user/                  # User management
│   └── infrastructure/            # Infrastructure layer
│       ├── concurrency/           # Transaction management
│       ├── events/                # Event handling
│       ├── persistence/           # Database repositories
│       └── security/              # Security utilities
└── test/                          # Test files
```

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- MySQL/PostgreSQL database server
- IDE with Java support (IntelliJ IDEA recommended)

## Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd syos_pos
   ```

2. **Database Setup**
   - Create a MySQL database named `SYOS`
   - Import the provided `SYOS.sql` schema file
   - Update database credentials in `src/main/resources/application.properties`

3. **Configure Database Connection**
   ```properties
   db.url=jdbc:mysql://localhost:3306/SYOS?useSSL=false&allowPublicKeyRetrieval=true
   db.user=your_username
   db.pass=your_password
   ```

4. **Build the Project**
   ```bash
   mvn clean compile
   ```

5. **Run Tests**
   ```bash
   mvn test
   ```

6. **Start the Application**
   ```bash
   mvn exec:java -Dexec.mainClass="main.java.App"
   ```

## Usage

### Starting the Application

When you run the application, you'll be presented with two interface options:

1. **CLI Interface** (Cashier/Manager)
2. **Web Shop Interface** (Customer)

### CLI Interface

#### Cashier Functions
- Login with cashier credentials
- Process customer checkouts
- Apply discounts
- Generate bills

#### Manager Functions
- All cashier functions plus:
- Product management (CRUD operations)
- Category management
- Batch management
- Inventory transfers
- Stock receiving from suppliers
- View reorder reports (items < 50 units)
- Monitor shortage events
- Discount management
- Access manager console

### Web Shop Interface

Customers can:
- Browse products
- Search inventory
- Add items to cart
- Apply discounts
- Complete purchases
- Process payments

## Key Components

### Domain Models
- **Product**: Core product entity with code, name, price, and category
- **Category**: Product categorization
- **Batch**: Inventory batches with expiration tracking
- **User**: System users with role-based access
- **Money**: Value object for monetary amounts

### Use Cases
- **Authentication**: User login and session management
- **Product Management**: CRUD operations for products
- **Checkout**: Complete sales transaction processing
- **Inventory Transfer**: Move stock between locations
- **Discount Management**: Handle promotional pricing

### Repositories
- JDBC-based repositories for all entities
- Transaction management with `Tx` wrapper
- Support for multiple database backends

## Testing

The project includes comprehensive test coverage:

- Unit tests for domain logic
- Integration tests for repositories
- CLI interface tests
- Use case tests

Run tests with: `mvn test`

## Architecture

The system follows Clean Architecture principles:

- **Domain Layer**: Pure business logic, no external dependencies
- **Application Layer**: Use cases and application services
- **Infrastructure Layer**: Database, external services, framework code
- **Presentation Layer**: CLI and web interfaces

### Design Patterns Used
- Repository Pattern for data access
- Strategy Pattern for inventory policies (FEFO)
- Event-Driven Architecture for notifications
- Dependency Injection for loose coupling

## Configuration

Key configuration files:
- `application.properties`: Database configuration
- `pom.xml`: Maven dependencies and build configuration
- `SYOS.sql`: Database schema

## Contributing

1. Follow Java coding standards
2. Write tests for new functionality
3. Maintain clean architecture principles
4. Update documentation for new features

## Database Schema

The system uses the following key tables:
- `products`: Product catalog
- `categories`: Product categories
- `batch`: Inventory batches
- `users`: System users
- `bills`: Sales transactions
- `inventory`: Stock tracking
- `discounts`: Promotional pricing

## Future Enhancements

- REST API for external integrations
- Mobile application support
- Advanced reporting dashboard
- Multi-location support
- Barcode scanning integration
- Real-time inventory synchronization

## License

[License information to be added]

## Support

For technical support or questions about the SYOS POS system, please contact the development team.

---

*Last updated: September 25, 2025*
