
This application supports Envoy to Ambassador authentication by validating with an Identity
Provider (IdP) and issuing a client TLS certificate.

![Auth Service Overview](docs/auth-service-overview.drawio.png)

# Specify Vault credentials

You'll need to set the `secret-id` and `role-id` in src/main/resources/application-dev.yml
with the values that were presented when [setting up Vault for development usage](https://github.com/racker/salus-telemetry-bundle/#setting-up-vault-for-development-usage).

Alternatively, you can set the two properties in the IntelliJ run configuration or pass them
via Maven with `-Dspring-boot.run.arguments=...`:

- `vault.app-role.role-id`
- `vault.app-role.secret-id`

# Maven usage

Maven usage is described in the [Salus App Base](https://github.com/racker/salus-app-base)