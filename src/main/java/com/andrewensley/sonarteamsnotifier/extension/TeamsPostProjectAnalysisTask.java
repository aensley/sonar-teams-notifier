package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;

import java.util.Map;
import java.util.Optional;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Post Project Analysis Task that sends the WebEx Teams notification message.
 */
public class TeamsPostProjectAnalysisTask implements PostProjectAnalysisTask {

  /**
   * Logger.
   */
  private static final Logger LOG = Loggers.get(TeamsPostProjectAnalysisTask.class);

  /**
   * SonarQube settings.
   */
  private final Configuration settings;

  /**
   * Constructor.
   *
   * @param settings The SonarQube Configuration settings.
   */
  public TeamsPostProjectAnalysisTask(Configuration settings) {
    this.settings = settings;
  }

  /**
   * Post analysis task.
   *
   * @param analysis The project analysis results.
   */
  @Override
  public void finished(final ProjectAnalysis analysis) {
    if (!isPluginEnabled()) {
      LOG.info("Teams Notifier Plugin disabled.");
      return;
    }

    Map<String, String> properties = analysis.getScannerContext().getProperties();
    if (!properties.containsKey(Constants.HOOK)) {
      LOG.info("No hook URL found for Teams Notifier Plugin.");
      return;
    }

    LOG.debug("Analysis ScannerContext: [{}]", properties);
    String hook = properties.getOrDefault(Constants.HOOK, "").trim();
    boolean failOnly = !properties.getOrDefault(Constants.FAIL_ONLY, "").trim().isEmpty();
    if (failOnly && qualityGateOk(analysis)) {
      LOG.info("QualityGate passed and fail_only is enabled. Skipping notification.");
      return;
    }

    LOG.debug("Teams notification URL: " + hook);
    LOG.debug("Teams notification message: " + analysis.toString());
    sendNotification(hook, failOnly, analysis);
  }

  /**
   * Checks if the quality gate status is set and is OK.
   *
   * @param analysis Project Analysis object.
   *
   * @return True if quality gate is set and is OK. False if not.
   */
  private boolean qualityGateOk(ProjectAnalysis analysis) {
    QualityGate qualityGate = analysis.getQualityGate();
    return (qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus()));
  }

  /**
   * Sends the WebEx teams notification.
   *
   * @param hook     The hook URL.
   * @param failOnly The setting of the fail_only flag.
   * @param analysis The Project Analysis.
   */
  private void sendNotification(String hook, boolean failOnly, ProjectAnalysis analysis) {
    try {
      TeamsHttpClient httpClient = TeamsHttpClient
          .of(hook, PayloadBuilder.of(
                  analysis,
                  projectUrl(analysis.getProject().getKey()),
                  failOnly,
                  qualityGateOk(analysis)
              )
              .commitUrl(settings.get(Constants.COMMIT_URL).orElse(""))
              .changeAuthor(
                  settings.get(Constants.CHANGE_AUTHOR_EMAIL).orElse(""),
                  settings.get(Constants.CHANGE_AUTHOR_NAME).orElse("")
              )
              .build())
          .bypassHttpsValidation(isBypassEnabled())
          .proxy(settings.get(Constants.PROXY_IP), settings.getInt(Constants.PROXY_PORT))
          .proxyAuth(settings.get(Constants.PROXY_USER), settings.get(Constants.PROXY_PASS))
          .build();
      if (httpClient.post()) {
        LOG.info("Teams message posted");
      } else {
        LOG.error("Teams message failed");
      }
    } catch (Exception e) {
      LOG.error("Failed to send teams message", e);
    }
  }


  /**
   * Checks if the plugin is enabled globally.
   *
   * @return True if enabled. False if not.
   */
  private boolean isPluginEnabled() {
    return settings.getBoolean(Constants.ENABLED).orElse(false);
  }

  /**
   * Checks if the HTTPS Validation Bypass is enabled.
   *
   * @return True if enabled. False if not.
   */
  private boolean isBypassEnabled() {
    return settings.getBoolean(Constants.BYPASS_HTTPS_VALIDATION).orElse(false);
  }

  /**
   * Gets the project URL.
   *
   * @param projectKey The ID of this project.
   * @return The project URL.
   */
  private String projectUrl(String projectKey) {
    return getSonarServerUrl() + "dashboard?id=" + projectKey;
  }

  /**
   * Returns the sonar server url, with a trailing /.
   *
   * @return the sonar server URL
   */
  private String getSonarServerUrl() {
    Optional<String> urlOptional = settings.get("sonar.core.serverBaseURL");
    if (!urlOptional.isPresent()) {
      return "http://pleaseDefineSonarQubeUrl/";
    }

    String url = urlOptional.get();
    if (url.endsWith("/")) {
      return url;
    }

    return url + "/";
  }
}
