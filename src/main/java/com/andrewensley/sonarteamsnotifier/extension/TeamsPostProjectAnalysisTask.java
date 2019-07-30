package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;
import java.util.Optional;

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
               postJson(hook, payload);
            } catch (Exception e) {
                LOG.error("Failed to send teams message", e);
            }
        }
    }

    private void postJson (String uri, String payload) throws MalformedURLException {
        Optional<String> proxyIp = settings.get(Constants.PROXY_IP);
        Optional<Integer> proxyPort = settings.getInt(Constants.PROXY_PORT);
        Optional<String> proxyUser = settings.get(Constants.PROXY_USER);
        Optional<String> proxyPass = settings.get(Constants.PROXY_PASS);
        URL hook = new URL(uri);
        int port = getPort(hook);
        String path = getPath(hook);
        boolean proxyEnabled = (proxyIp.isPresent() && proxyPort.isPresent());
        CloseableHttpClient httpClient = getHttpClient(proxyEnabled, proxyIp, proxyPort, proxyUser, proxyPass, hook, port);
        try {
            HttpHost target = new HttpHost(hook.getHost(), port);
            HttpPost httpPost = getHttpPost(payload, path, proxyEnabled, proxyIp, proxyPort);
            CloseableHttpResponse response = httpClient.execute(target, httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                throw new Exception("Invalid HTTP Response Code: " + responseCode);
            }

            LOG.info("HTTP Response: " + response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            LOG.error("Failed to send teams message", e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
            }
        }
    }

    private HttpPost getHttpPost(
        String payload,
        String path,
        boolean proxyEnabled,
        Optional<String> proxyIp,
        Optional<Integer> proxyPort
    ) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(path);
        httpPost.setEntity(new StringEntity(payload));
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        if (proxyEnabled) {
            HttpHost proxy = new HttpHost(proxyIp.get(), proxyPort.get());
            RequestConfig config = RequestConfig.custom()
                .setProxy(proxy)
                .build();
            httpPost.setConfig(config);
        }

        return httpPost;
    }

    private CloseableHttpClient getHttpClient(
        boolean proxyEnabled,
        Optional<String> proxyIp,
        Optional<Integer> proxyPort,
        Optional<String> proxyUser,
        Optional<String> proxyPass,
        URL hook,
        int port
    ) {
        CloseableHttpClient httpClient;
        if (proxyEnabled && proxyUser.isPresent() && proxyPass.isPresent()) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                new AuthScope(proxyIp.get(), proxyPort.get()),
                new UsernamePasswordCredentials(proxyUser.get(), proxyPass.get()));
            credsProvider.setCredentials(
                new AuthScope(hook.getHost(), port),
                new UsernamePasswordCredentials(proxyUser.get(), proxyPass.get()));
            httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        } else {
            httpClient = HttpClients.createDefault();
        }

        return httpClient;
    }

    private int getPort(URL url) {
        int port = url.getPort();
        if (port == -1) {
            port = (url.getProtocol().equals("https") ? 443 : 80);
        }

        return port;
    }

    private String getPath(URL url) {
        String path = url.getPath();
        if (!url.getQuery().isEmpty()) {
            path += "?" + url.getQuery();
        }

        if (!url.getRef().isEmpty()) {
            path += "#" + url.getRef();
        }

        return path;
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
