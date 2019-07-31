package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;

import java.util.Map;
import java.util.Optional;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
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
    ScannerContext scannerContext = analysis.getScannerContext();
    Map<String, String> properties = scannerContext.getProperties();
    if (properties.containsKey(Constants.HOOK)) {
      if (!isPluginEnabled()) {
        LOG.info("Teams Notifier Plugin disabled.");
        return;
      }

      LOG.debug("Analysis ScannerContext: [{}]", properties);
      String hook = properties.getOrDefault(Constants.HOOK, "").trim();
      boolean failOnly = !properties.getOrDefault(Constants.FAIL_ONLY, "").trim().isEmpty();

      QualityGate qualityGate = analysis.getQualityGate();
      if (
          failOnly
          && qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus())
      ) {
        LOG.info(
            "Skipping notification because "
            + "scan passed quality gate settings and fail_only is enabled."
        );
        return;
      }

      LOG.info("Teams notification URL: " + hook);
      LOG.info("Teams notification message: " + analysis.toString());

      Payload payload = PayloadBuilder.of(analysis)
          .failOnly(failOnly)
          .projectUrl(projectUrl(analysis.getProject().getKey()))
          .build();

      Optional<String> proxyIp = settings.get(Constants.PROXY_IP);
      Optional<Integer> proxyPort = settings.getInt(Constants.PROXY_PORT);
      Optional<String> proxyUser = settings.get(Constants.PROXY_USER);
      Optional<String> proxyPass = settings.get(Constants.PROXY_PASS);
      try {
        TeamsHttpClient httpClient = TeamsHttpClient.of(hook, payload)
            .proxy(proxyIp, proxyPort)
            .proxyAuth(proxyUser, proxyPass)
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
