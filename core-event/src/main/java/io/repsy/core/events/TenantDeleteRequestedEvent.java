package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record TenantDeleteRequestedEvent(@NonNull String owner) {}
