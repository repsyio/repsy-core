package io.repsy.core.events;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EmailUpdatedEvent<ID>(ID tenantId, String newEmail) { }
