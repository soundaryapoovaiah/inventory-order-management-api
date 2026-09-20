package microservices.notification.service;

import microservices.notification.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(
            ProcessedEventRepository processedEventRepository
    ) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean processIfNew(
            UUID eventId,
            String eventType,
            String aggregateId,
            Runnable processingAction
    ) {

        int inserted = processedEventRepository.insertIfAbsent(
                eventId,
                eventType,
                aggregateId
        );

        if (inserted == 0) {
            return false;
        }

        processingAction.run();

        return true;
    }
}