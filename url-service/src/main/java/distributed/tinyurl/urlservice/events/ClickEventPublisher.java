package distributed.tinyurl.urlservice.events;

public interface ClickEventPublisher {
    void publish(ClickRecordedEvent event);
}
