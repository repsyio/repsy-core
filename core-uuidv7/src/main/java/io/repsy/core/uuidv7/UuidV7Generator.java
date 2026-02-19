package io.repsy.core.uuidv7;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.EnumSet;
import java.util.UUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class UuidV7Generator implements BeforeExecutionGenerator {

  @Override
  public @NonNull UUID generate(
      final @NonNull SharedSessionContractImplementor session,
      final @NonNull Object owner,
      final @Nullable Object currentValue,
      final @NonNull EventType eventType) {
    return UuidCreator.getTimeOrderedEpoch();
  }

  @Override
  public @NonNull EnumSet<EventType> getEventTypes() {
    return EnumSet.of(EventType.INSERT);
  }
}
