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

The message of `BadRequestException`, `UnAuthorizedException`, `AccessNotAllowedException`,
`ItemNotFoundException`, `ItemAlreadyExistException`, `SignatureNotVerifiedException`,
`MfaException`, `DataExportRequestException` and `SubscriptionLimitReachedException` is a **msgId**:
a bare identifier (letters, digits and underscores) that names an entry in the consuming service's
message bundle. Their constructors throw `IllegalArgumentException` for anything else (free text, a
formatted sentence, a request path), because the error handler returns the message to the client as
it is. `null` is accepted and lets the handler fall back to its default id for the exception type.
Log the variable details instead of putting them in the id.

### Request and exception reports

`ErrorUtils` moved to the `core-web-error` module (`RequestReport`, `ExceptionReport`), so this jar
is pure JDK plus JSpecify and no longer pulls in spring-web, the servlet API or commons-lang3.
`RequestReport.of(request, traceId)` reports the path, method, query parameter names and an
allow-list of headers (every other header, such as `Authorization`, `Cookie`, `X-Api-Key`, `npm-otp`
and `X-NuGet-ApiKey`, is masked), never reads the body, and carries the caller's trace id so the log
entry matches the `errorCode` of the response. The `ErrorUtils` class remains in `core-web-error`,
deprecated, delegating to them.

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
