# TIS Trainee Self-Service NDW Exporter

[![Build Status][build-badge]][build-href]
[![License][license-badge]][license-href]
[![Quality Gate Status][quality-gate-badge]][quality-gate-href]
[![Coverage Stats][coverage-badge]][coverage-href]

## About
This service handles the exporting of TIS Self-Service data to the National Data Warehouse (NDW).

TODO: details

## Developing

### Running

```shell
gradlew bootRun
```

#### Pre-Requisites
TODO

#### Environmental Variables

| Name                   | Description                                                 | Default |
|------------------------|-------------------------------------------------------------|---------|
| **Azure:**             |                                                             |
| AZURE_CLIENT_ID        | The client ID for connecting to the NDW Azure instance.     |         |
| AZURE_CLIENT_SECRET    | The client secret for connecting to the NDW Azure instance. |         |
| AZURE_TENANT_ID        | The tenant ID for connecting to the NDW Azure instance.     |         |
| AZURE_DATA_LAKE_NAME   | The name of the NDW data lake to export to.                 | local   |
| **Logging:**           |                                                             |         |
| SENTRY_DSN             | A Sentry error monitoring Data Source Name.                 |         |
| SENTRY_ENVIRONMENT     | The environment to log Sentry events against.               | local   |
| LOGGING_ROOT           | Root logging level.                                         | INFO    |
| LOGGING_EVENT          | NDW event logging level.                                    | DEBUG   |
| LOGGING_SERVICE        | NDW service logging level.                                  | DEBUG   |
| **Queues:**            |                                                             |         |
| ACTION_QUEUE_URL       | Queue to receive Action events.                             |         |
| NDW_FORM_QUEUE_URL     | Queue to receive AWS S3 Form events.                        |         |
| NOTIFICATION_QUEUE_URL | Queue to receive Notification events.                       |         |


### Usage Examples
TODO

### Testing

The Gradle `test` task can be used to run automated tests and produce coverage
reports.
```shell
gradlew test
```

The Gradle `check` lifecycle task can be used to run automated tests and also
verify formatting conforms to the code style guidelines.
```shell
gradlew check
```

### Building

```shell
gradlew bootBuildImage
```

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).

[coverage-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_tis-trainee-ndw-exporter&metric=coverage
[coverage-href]: https://sonarcloud.io/component_measures?metric=coverage&id=Health-Education-England_tis-trainee-ndw-exporter

[build-badge]: https://badgen.net/github/checks/health-education-england/tis-trainee-ndw-exporter?label=build&icon=github
[build-href]: https://github.com/Health-Education-England/tis-trainee-ndw-exporter/actions/workflows/ci-cd-workflow.yml

[license-badge]: https://badgen.net/github/license/health-education-england/tis-trainee-ndw-exporter
[license-href]: LICENSE

[quality-gate-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_tis-trainee-ndw-exporter&metric=alert_status
[quality-gate-href]: https://sonarcloud.io/summary/new_code?id=Health-Education-England_tis-trainee-ndw-exporter
