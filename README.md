# Sonar Teams Notifier

[![License](https://img.shields.io/github/license/aensley/sonar-teams-notifier)](LICENSE)
[![Build Status](https://travis-ci.com/aensley/sonar-teams-notifier.svg?branch=master)](https://travis-ci.com/aensley/sonar-teams-notifier)
[![Maintainability](https://api.codeclimate.com/v1/badges/29bdfe58f74e805ece51/maintainability)](https://codeclimate.com/github/aensley/sonar-teams-notifier/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/29bdfe58f74e805ece51/test_coverage)](https://codeclimate.com/github/aensley/sonar-teams-notifier/test_coverage)
[![Checkstyle](https://img.shields.io/badge/checkstyle-google-blue?style=flat&logoWidth=8&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAQBAMAAADZpCNOAAAAFVBMVEVvcm3/AAD/sLX/zAD/1Nf/6gD88cWfIbEzAAAAAXRSTlMAQObYZgAAACxJREFUCNdjcHFSUmJAEGAQGmxsTIBIDUs2Y2BLS0tgYEhLA2oRFAQSjAIMACYODUYHdu83AAAAAElFTkSuQmCC)](https://checkstyle.sourceforge.io/google_style.html)
[![Downloads](https://img.shields.io/github/downloads/aensley/sonar-teams-notifier/total)](https://github.com/aensley/sonar-teams-notifier/releases)


This SonarQube plugin notifies WebEx Teams of Scan Results.


## Usage


### Administration

Only one setting is required once the plugin is installed, and that's to enable the plugin.

[![Admin Screenshot](docs/sonar-teams-admin.png)](docs/sonar-teams-admin.png)


### Scanning


#### Basic Usage

To enable WebEx Teams notifications for scan results, supply the [**Incoming Webhook URL**](https://apphub.webex.com/integrations/incoming-webhooks-cisco-systems) to the sonar-scanner command using the custom `sonar.teams.hook` property. _This is the only property required to enable notifications._


##### Example

```ShellSession
mvn sonar:sonar \
  -Dsonar.teams.hook=https://api.ciscospark.com/v1/webhooks/incoming/1234
```


#### Advanced Usage

All custom properties pertaining to this plugin are specified under `sonar.teams`, e.g. `sonar.teams.hook`.

| Property name | Required | Description |
| ------------- | :------: | ----------- |
| `hook` | **YES** | The WebEx Teams [Incoming Webhook URL](https://apphub.webex.com/integrations/incoming-webhooks-cisco-systems). |
| `fail_only` | no | Specify any truthy value (e.g. `1` or `true`) to send notifications only when there is a failure. |
| `commit_url` | no | When specified, the commit in the notification links to the commit that triggered the build/scan. |
| `change_author_email` | no | When specified, the commit author is mentioned when there are any failures. |
| `change_author_name` | no | Sets the commit author's display name when mentioned. |


##### Example

```ShellSession
mvn sonar:sonar \
  -Dsonar.teams.hook=https://api.ciscospark.com/v1/webhooks/incoming/1234 \
  -Dsonar.teams.fail_only=1 \
  -Dsonar.teams.commit_url=https://github.com/owner/repo/commit/1234567 \
  -Dsonar.teams.change_author_email=author@email.com \
  -Dsonar.teams.change_author_name="Author Name"
```

## Documentation

Browse the Javadocs at https://github.andrewensley.com/sonar-teams-notifier/
