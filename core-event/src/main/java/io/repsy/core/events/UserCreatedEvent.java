package io.repsy.core.events;

import org.jspecify.annotations.NonNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public record UserCreatedEvent<T>(@NonNull T userId, @NonNull String username)
    implements ResolvableTypeProvider {

  @Override
  public @NonNull ResolvableType getResolvableType() {
    return ResolvableType.forClassWithGenerics(UserCreatedEvent.class, this.userId.getClass());
  }
}
