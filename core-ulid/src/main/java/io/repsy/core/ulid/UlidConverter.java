package io.repsy.core.ulid;

import com.github.f4b6a3.ulid.Ulid;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UlidConverter implements Converter<Ulid, String> {

  @Override
  public @NonNull String convert(final @NonNull Ulid source) {

    return source.toString();
  }
}
