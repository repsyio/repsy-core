# core-ulid

ULID generation, conversion, and Hibernate 7 integration for entity identifiers.

## Purpose

Lets JPA entities use [ULIDs](https://github.com/ulid/spec) (Universally Unique Lexicographically
Sortable Identifiers) as primary keys, backed by the
[`f4b6a3/ulid-creator`](https://github.com/f4b6a3/ulid-creator) library.

- **`@UlidGenerator`** — a field/method annotation combining Hibernate's `@IdGeneratorType` and
  `@Type` meta-annotations. Put it on an entity id field of type `Ulid` to have Hibernate generate
  and persist it automatically.
- **`RandomUlidGenerator`** — the `BeforeExecutionGenerator` implementation backing
  `@UlidGenerator`. Generates a new random ULID on `INSERT` unless a value was already set.
- **`UlidUserType`** — a Hibernate `EnhancedUserType<Ulid>` that maps `Ulid` to/from a SQL
  `VARCHAR` column.
- **`UlidConverter`** — a Spring `Converter<Ulid, String>` for use in web/serialization layers
  (e.g. path variable binding).

## Dependencies

- `spring-context` — for the `Converter` component
- `com.github.f4b6a3:ulid-creator` — ULID generation/parsing
- `hibernate-core` — generator and user-type SPIs

## Usage

```java
@Entity
class Thing {
  @Id
  @UlidGenerator
  private Ulid id;
}
```

The entity's id is populated automatically as a lexicographically sortable, time-ordered ULID on
insert, and stored as a string column in the database.
