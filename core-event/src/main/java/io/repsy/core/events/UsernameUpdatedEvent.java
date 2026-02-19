package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record UsernameUpdatedEvent(@NonNull String currentUsername, @NonNull String newUsername) {}
