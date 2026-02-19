package io.repsy.core.ulid;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.ValueGenerationType;

@IdGeneratorType(RandomUlidGenerator.class)
@ValueGenerationType(generatedBy = RandomUlidGenerator.class)
@Retention(RUNTIME)
@Target({FIELD, METHOD})
@Type(UlidUserType.class)
public @interface UlidGenerator {}
