# core-event

Shared Spring application events used across Repsy services.

## Purpose

This module defines a set of plain, serializable record types published via Spring's
`ApplicationEventPublisher` so that different Repsy services and modules can react to the same
domain events without depending on each other's internal types.

Events cover areas such as:

- **Account/tenant lifecycle** — `UserCreatedEvent`, `RegistrationCompletedEvent`,
  `UsernameUpdatedEvent`, `EmailUpdatedEvent`, `TenantDeleteRequestedEvent`,
  `TenantInactivityWarningEvent`
- **Auth & security** — `UserLoginEvent`, `PasswordRecoveryRequestedEvent`,
  `PasswordUpdatedEvent`, `EmailOtpRequestedEvent`, `EmailVerificationRequestedEvent`
- **Artifact/repository activity** — `ArtifactPushedEvent`, `ArtifactVersionDeletedEvent`,
  `ScanResultNotificationRequestedEvent`, `DeploymentScanResultNotificationRequestedEvent`,
  `SeverityCounts`
- **Notifications & comms** — `ContactRequestReceivedEvent`,
  `DeploymentNotificationRequestedEvent`, `MailjetUnsubRequestedEvent`
- **Storage & compliance** — `ClearStorageTrashRequestedEvent`, `GdprRequestedEvent`

Generic events (e.g. `UserCreatedEvent<T>`) implement `ResolvableTypeProvider` so Spring's event
listener matching resolves the correct generic parameter at runtime.

## Dependencies

- `spring-context` — for `ApplicationEvent` publishing/listening infrastructure.

## Usage

Add the dependency (version managed via `core-bom` or the parent POM), publish events with
`ApplicationEventPublisher`, and consume them with `@EventListener` / `@TransactionalEventListener`
in the service that owns the reaction.
