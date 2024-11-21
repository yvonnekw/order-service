package com.auction.order_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class WinningBidProducer {

    private final KafkaTemplate<String, WinningBidConfirmation> kafkaTemplate;

    public void sendWinningBidEvent(WinningBidConfirmation winningBidEvent) {
        log.info("Sending Winning Bid Event: {}", winningBidEvent);
        Message<WinningBidConfirmation> message = MessageBuilder
                .withPayload(winningBidEvent)
                .setHeader(KafkaHeaders.TOPIC, "winning-bid-topic")
                .build();
        kafkaTemplate.send(message);
    }
}