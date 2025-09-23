package domain.repository;

import application.usecase.QuoteUseCase;
import domain.billing.BillLine;

public interface OrderRepository {
    /**
     * Get next bill serial number for the given type
     */
    long nextBillSerial(String type);

    /**
     * Save order as preview (not finalized)
     */
    long savePreview(String type, String location, Long userId, QuoteUseCase.Quote quote);

    /**
     * Save order lines
     */
    void saveLines(long orderId, java.util.List<BillLine> lines);

    /**
     * Mark order as final
     */
    void saveFinal(long orderId, QuoteUseCase.Quote quote);
}
