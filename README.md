# Sonar Teams Notifier

[![License](https://img.shields.io/github/license/aensley/sonar-teams-notifier)](LICENSE)
[![Build Status](https://travis-ci.com/aensley/sonar-teams-notifier.svg?branch=master)](https://travis-ci.com/aensley/sonar-teams-notifier)
[![Checkstyle](https://img.shields.io/badge/checkstyle-google-blue?style=flat&logoWidth=8&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAQBAMAAADZpCNOAAAAFVBMVEVvcm3/AAD/sLX/zAD/1Nf/6gD88cWfIbEzAAAAAXRSTlMAQObYZgAAACxJREFUCNdjcHFSUmJAEGAQGmxsTIBIDUs2Y2BLS0tgYEhLA2oRFAQSjAIMACYODUYHdu83AAAAAElFTkSuQmCC)](https://checkstyle.sourceforge.io/google_style.html)
[![Downloads](https://img.shields.io/github/downloads/aensley/sonar-teams-notifier/total)](https://github.com/aensley/sonar-teams-notifier/releases)

[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/aensley/sonar-teams-notifier.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/aensley/sonar-teams-notifier/context:java)
[![Maintainability](https://api.codeclimate.com/v1/badges/29bdfe58f74e805ece51/maintainability)](https://codeclimate.com/github/aensley/sonar-teams-notifier/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/29bdfe58f74e805ece51/test_coverage)](https://codeclimate.com/github/aensley/sonar-teams-notifier/test_coverage)

This SonarQube plugin notifies WebEx Teams of Scan Results.


## Usage


### Administration

Only one setting is required once the plugin is installed, and that's to enable the plugin.

[![Admin Screenshot](docs/sonar-teams-admin.png)](docs/sonar-teams-admin.png)


### Scanning

To enable WebEx Teams notifications for scan results, supply the [**Incoming Webhook URL**](https://apphub.webex.com/integrations/incoming-webhooks-cisco-systems) to the sonar-scanner command using the custom `sonar.teams.hook` property.


```ShellSession
mvn sonar:sonar \
  -Dsonar.teams.hook=https://api.ciscospark.com/v1/webhooks/incoming/1234
```

If you want notifications only of failures, specify any truthy value to the custom `sonar.teams.fail_only` property.

```ShellSession
mvn sonar:sonar \
  -Dsonar.teams.hook=https://api.ciscospark.com/v1/webhooks/incoming/1234 \
  -Dsonar.teams.fail_only=1
```
