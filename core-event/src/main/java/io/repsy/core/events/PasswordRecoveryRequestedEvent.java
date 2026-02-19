package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record PasswordRecoveryRequestedEvent(
    @NonNull String email, @NonNull String fullName, @NonNull String recoveryCode) {}
