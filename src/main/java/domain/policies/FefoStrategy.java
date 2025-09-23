package domain.policies;

import domain.inventory.Batch;
import domain.inventory.StockLocation;
import domain.repository.InventoryRepository;
import domain.shared.Code;

import java.sql.Connection;
import java.util.List;

public final class FefoStrategy extends AbstractBatchStrategy {
    public FefoStrategy(InventoryRepository inventory) { super(inventory); }

    @Override
    protected List<Batch> candidates(Connection con, Code productCode, StockLocation location) {
        // Same ordered query already prioritizes earlier expiry first, then oldest received.
        return inventory.findDeductionCandidates(con, productCode, location);
    }
}