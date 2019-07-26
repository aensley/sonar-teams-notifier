package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

/**
 * Post Project Analysis Task that sends the WebEx Teams notification message.
 */
public class TeamsPostProjectAnalysisTask implements PostProjectAnalysisTask {

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(TeamsPostProjectAnalysisTask.class);

    /**
     * SonarQube settings
     */
    private final Configuration settings;

    /**
     * Constructor
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

            LOG.info("Analysis ScannerContext: [{}]", properties);
            String projectKey = analysis.getProject().getKey();
            String hook = properties.getOrDefault(Constants.HOOK, "").trim();
            boolean failOnly = !properties.getOrDefault(Constants.FAIL_ONLY, "").trim().isEmpty();

            QualityGate qualityGate = analysis.getQualityGate();
            if (failOnly && qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus())) {
                LOG.info("Skipping notification because scan passed quality gate settings and fail_only is enabled.");
                return;
            }

            LOG.info("Teams notification will be sent: " + analysis.toString());

            String payload = PayloadBuilder.of(analysis)
                .failOnly(failOnly)
                .projectUrl(projectUrl(projectKey))
                .build();

            try {
                HttpClient client = getHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(hook))
                    .header("Content-Type", "application/json")
                    .POST(ofString(payload))
                    .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(LOG::info)
                    .join();
            } catch (Exception e) {
                LOG.error("Failed to send teams message", e);
            }
        }
    }

    /**
     * Builds the HTTP Client for web requests.
     *
     * @return The HTTP Client for the request.
     */
    private HttpClient getHttpClient() {
        Optional<String> proxyIp = settings.get(Constants.PROXY_IP);
        Optional<Integer> proxyPort = settings.getInt(Constants.PROXY_PORT);
        Optional<String> proxyUser = settings.get(Constants.PROXY_USER);
        Optional<String> proxyPass = settings.get(Constants.PROXY_PASS);
        if (proxyIp.isPresent() && proxyPort.isPresent()) {
            if (proxyUser.isPresent() && proxyPass.isPresent()) {
                // TODO: Add proxy authentication.
                return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.of(new InetSocketAddress(proxyIp.get(), proxyPort.get())))
                    .build();
            } else {
                return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.of(new InetSocketAddress(proxyIp.get(), proxyPort.get())))
                    .build();
            }
        } else {
            return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
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
     *
     * @return The project URL.
     */
    private String projectUrl(String projectKey) {
        return getSonarServerUrl() + "dashboard?id=" + projectKey;
    }

    /**
     * Returns the sonar server url, with a trailing /
     *
     * @return the sonar server URL
     */
    private String getSonarServerUrl() {
        Optional<String> urlOptional = settings.get("sonar.core.serverBaseURL");
        if (urlOptional.isEmpty()) {
            return "http://pleaseDefineSonarQubeUrl/";
        }

        String url = urlOptional.get();
        if (url.endsWith("/")) {
            return url;
        }

        return url + "/";
    }
}
