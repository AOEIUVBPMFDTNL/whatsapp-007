package com.whatsapp.android.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class MessagingMetrics {
    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    @Getter
    private static MessagingMetrics instance;

    public MessagingMetrics(MeterRegistry registry) {
        this.registry = registry;
        instance = this;
    }

    public void incrementSuccess(String senderCountry, String receiverCountry) {
        String counterKey = senderCountry + "_" + receiverCountry;
        Counter counter = counters.computeIfAbsent(counterKey, k -> createCounter(senderCountry, receiverCountry));
        counter.increment();
    }

    private Counter createCounter(String senderCountry, String receiverCountry) {
        return Counter.builder("messages.success")
                .tag("senderCountry", senderCountry)
                .tag("receiverCountry", receiverCountry)
                .description("Number of successful messages sent")
                .register(registry);
    }
}

