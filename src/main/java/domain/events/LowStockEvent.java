package domain.events;

import domain.shared.Code;

public record LowStockEvent(Code productCode, int remaining) implements DomainEvent {}