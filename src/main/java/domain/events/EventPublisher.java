package domain.events;
public interface EventPublisher { void publish(DomainEvent e); }