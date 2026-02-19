package io.repsy.core.ulid;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.EnumSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RandomUlidGenerator implements BeforeExecutionGenerator {

  @Override
  public Object generate(
      final @NonNull SharedSessionContractImplementor session,
      final @NonNull Object entityInstance,
      final @Nullable Object currentValue,
      final @NonNull EventType eventType) {

    if (currentValue instanceof final Ulid ulid) {
      return ulid;
    }

    return UlidCreator.getUlid();
  }

  @Override
  public @NonNull EnumSet<EventType> getEventTypes() {

    return EnumSet.of(EventType.INSERT);
  }
}
