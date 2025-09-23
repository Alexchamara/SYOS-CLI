package domain.policies;

import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import domain.shared.Code;

import java.sql.Connection;
import java.util.List;

public abstract class AbstractBatchStrategy implements BatchSelectionStrategy {
    protected final InventoryRepository inventory;

    protected AbstractBatchStrategy(InventoryRepository inventory) {
        if (inventory == null) {
            throw new NullPointerException("Inventory repository cannot be null");
        }
        this.inventory = inventory;
    }

    @Override
    public void deduct(Connection con, Code productCode, int qtyNeeded, StockLocation location) {
        int remaining = qtyNeeded;
        for (Batch b : candidates(con, productCode, location)) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, b.quantity().value());
            if (take > 0) {
                inventory.deductFromBatch(con, b.id(), take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock for " + productCode.value() + " need=" + qtyNeeded);
        }
    }

    @Override
    public int deductUpTo(Connection con, Code productCode, int qtyNeeded, StockLocation location) {
        int remaining = qtyNeeded;
        int taken = 0;
        for (Batch b : candidates(con, productCode, location)) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, b.quantity().value());
            if (take > 0) {
                inventory.deductFromBatch(con, b.id(), take);
                remaining -= take;
                taken += take;
            }
        }
        return taken; // caller decides next steps
    }

    protected abstract List<Batch> candidates(Connection con, Code productCode, StockLocation location);
}