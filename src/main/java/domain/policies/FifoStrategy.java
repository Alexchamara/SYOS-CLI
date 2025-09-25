package domain.policies;

import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import domain.shared.Code;

import java.sql.Connection;
import java.util.List;

public final class FifoStrategy extends AbstractBatchStrategy {
    public FifoStrategy(InventoryRepository inventory) { super(inventory); }

    @Override
    protected List<Batch> candidates(Connection con, Code productCode, StockLocation location) {
        // FIFO means ignore expiry prioritization: treat null expiry as “last” which is fine here.
        return inventory.findDeductionCandidates(con, productCode, location);
    }
}