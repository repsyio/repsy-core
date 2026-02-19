package io.repsy.core.events;

import java.util.Map;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record ContactRequestReceivedEvent(
    long templateId,
    @NonNull String name,
    @NonNull String email,
    String message,
    Map<String, String> variables) {}
