package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record EmailOtpRequestedEvent(
    @NonNull String email, @NonNull String username, @NonNull String code) {}
