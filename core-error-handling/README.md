# core-error-handling

Common exceptions and error-reporting utilities shared across Repsy services.

## Purpose

Provides a base set of runtime exceptions that map to specific error conditions across Repsy's
services, plus a utility for turning an exception and its originating HTTP request into a
detailed, loggable string.

### Exceptions (`io.repsy.core.error_handling.exceptions`)

All exceptions extend the package-private `BaseException` (a thin `RuntimeException` wrapper).
Notable ones:

- `BadRequestException`, `UnAuthorizedException`, `AccessNotAllowedException` — HTTP-style error
  conditions
- `ItemNotFoundException`, `ItemAlreadyExistException` — resource lookup/creation failures
- `SubscriptionLimitReachedException` — plan/quota enforcement
- `MfaException`, `CryptoException`, `SignatureNotVerifiedException` — auth/security failures
- `ManifestParseException`, `ManifestSerializationException`, `ManifestListResolutionException` —
  package manifest handling errors
- `JsonParseException`, `EventResponseException`, `EventTimeoutException` — messaging/serialization
  failures
- `DataExportRequestException`, `RedirectToPathException`, `ErrorOccurredException`,
  `RetryableException`, `SslContextInitializationException` — misc. operational errors

### `ErrorUtils` (`io.repsy.core.error_handling.utils`)

A Lombok `@UtilityClass` with overloads of `exceptionToString(Throwable, HttpServletRequest)` that
render a diagnostic report combining the request (path, method, query string, headers with the
`Authorization` header redacted, and body) with the exception's stack trace and a randomly
generated error code. Specialized overloads add extra context for
`ConversionFailedException`, `HttpRequestMethodNotSupportedException`,
`MethodArgumentNotValidException`, and `MissingServletRequestParameterException`.

## Dependencies

- `spring-context`, `spring-web` — Spring exception types used by the specialized `ErrorUtils`
  overloads
- `jakarta.servlet-api` — `HttpServletRequest`
- `commons-lang3` — `ExceptionUtils` for stack trace rendering

## Usage

Throw the relevant exception from service/domain code, then catch it in a
`@ControllerAdvice`/`@ExceptionHandler` layer (owned by the consuming service) that uses
`ErrorUtils.exceptionToString(...)` to log a full diagnostic report before returning a client-safe
response (see `core-response`).
