package application.usecase;

import domain.inventory.StockLocation;
import domain.repository.InventoryAdminRepository;
import infrastructure.concurrency.Tx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReceiveFromSupplierUseCase {
    private final Tx tx;
    private final InventoryAdminRepository repo;

    public ReceiveFromSupplierUseCase(Tx tx, InventoryAdminRepository repo) {
        this.tx = tx; this.repo = repo;
    }

    /** Receive a fresh batch into MAIN store. */
    public long receive(String productCode, int qty, LocalDate expiry) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        return tx.inTx(con ->
                repo.insertBatch(con, productCode, StockLocation.MAIN_STORE.name(),
                        LocalDateTime.now(), expiry, qty));
    }

    /**
     * Return recent batches received. Default implementation returns empty list; tests may mock this.
     */
    public List<BatchManagementUseCase.BatchInfo> getRecentBatches(int limit) {
        return List.of();
    }
}