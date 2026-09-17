# core-uuidv7

UUIDv7 (time-ordered) generation and Hibernate 7 integration for entity identifiers.

## Purpose

Lets JPA entities use time-ordered [UUIDv7](https://www.rfc-editor.org/rfc/rfc9562#section-5.7)
values as primary keys, backed by the
[`f4b6a3/uuid-creator`](https://github.com/f4b6a3/uuid-creator) library.

- **`@UuidV7`** — a field/method annotation combining Hibernate's `@IdGeneratorType` /
  `@ValueGenerationType` meta-annotations. Put it on an entity id field of type `UUID` to have
  Hibernate generate and persist it automatically.
- **`UuidV7Generator`** — the `BeforeExecutionGenerator` implementation backing `@UuidV7`.
  Generates a new time-ordered-epoch UUID (`UuidCreator.getTimeOrderedEpoch()`) on `INSERT`.

Unlike `core-ulid`, this module relies on the database's native `UUID` column type — no custom
`UserType`/converter is needed since Hibernate already understands `java.util.UUID`.

## Dependencies

- `spring-context`
- `com.github.f4b6a3:uuid-creator` — UUIDv7 generation
- `hibernate-core` — generator SPI

## Usage

```java
@Entity
class Thing {
  @Id
  @UuidV7
  private UUID id;
}
```

The entity's id is populated automatically as a time-ordered UUIDv7 on insert, giving
better index locality than random UUIDv4 while still storing as a standard `UUID` column.
