# core-response

REST response envelope and factory for Repsy services.

## Purpose

Standardizes the shape of JSON responses returned by Repsy's REST APIs.

- **`ResponseType`** — `ERROR`, `SUCCESS`, or `WARNING`.
- **`RestResponse<T>`** — the response envelope: a message id, a `ResponseType`, an optional
  payload of type `T`, a resolved display `text`, and (for errors) a generated `errorCode` used to
  correlate a client-facing error with server-side logs.
- **`RestResponseFactory`** — a Spring `@Component` with `error(...)`, `success(...)`, and
  `warning(...)` builder methods. It resolves `msgId` against Spring's `MessageSource` (so
  responses are localizable) and, for `ERROR` responses, stamps a random `UUID` as the
  `errorCode`.

## Dependencies

- `spring-context` — `MessageSource` for message resolution.

## Usage

```java
@RestController
class ExampleController {
  private final RestResponseFactory responses;

  @PostMapping("/things")
  RestResponse<Thing> create() {
    return responses.success("thing.created", thing);
  }
}
```

Pair with `core-error-handling`: catch domain exceptions in a `@ControllerAdvice` and translate
them into `responses.error(...)` calls.
