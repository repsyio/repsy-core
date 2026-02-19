package io.repsy.core.events;

import org.jspecify.annotations.NonNull;

public record UserLoginEvent(@NonNull String username) {}
