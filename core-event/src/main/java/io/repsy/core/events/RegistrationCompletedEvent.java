package io.repsy.core.events;

import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Builder
public record RegistrationCompletedEvent(
    @NonNull String username,
    @Nullable String hash,
    @NonNull String salt,
    @NonNull UUID tenantUuid,
    @NonNull String fullName,
    @NonNull String email,
    @NonNull String verificationCode) {}
