package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record GdprRequestedEvent(@NonNull String email, @NonNull String name) {}
