package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record PasswordUpdatedEvent(
    @NonNull String username, @NonNull String hash, @NonNull String salt) {}
