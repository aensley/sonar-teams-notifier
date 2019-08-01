package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.InvalidHttpResponseException;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

class TeamsHttpClient {

  /**
   * Logger.
   */
  private static final Logger LOG = Loggers.get(TeamsHttpClient.class);

  /**
   * String value indicating a setting is not set.
   */
  private static final String NOT_SET = "NOT_SET";

  /**
   * The URL for the webhook.
   */
  private URL hook;

  /**
   * The port to be used for the connection.
   */
  private int port;

  /**
   * The full path of the URL including query string and anchor reference.
   */
  private String path;

  /**
   * The payload to send with the request.
   */
  private Payload payload;

  /**
   * Internal Apache HTTP Client.
   */
  private CloseableHttpClient httpClient;

  /**
   * Whether or not to bypass HTTPS validation.
   */
  private boolean bypassHttpsValidation = false;

  /**
   * The target host of the webhook.
   */
  private HttpHost target;

  /**
   * The HTTP POST request to send.
   */
  private HttpPost httpPost;

  /**
   * The proxy host name or IP.
   */
  private Optional<String> proxyIp;

  /**
   * The proxy port.
   */
  private Optional<Integer> proxyPort;

  /**
   * The username for proxy authentication.
   */
  private Optional<String> proxyUser;

  /**
   * The password for proxy authentication.
   */
  private Optional<String> proxyPass;

  /**
   * Constructor.
   *
   * @param url     The URL of the webhook.
   * @param payload The payload to send to the webhook.
   *
   * @throws MalformedURLException If the URL is malformed.
   */
  private TeamsHttpClient(String url, Payload payload) throws MalformedURLException {
    this.hook = new URL(url);
    this.payload = payload;
  }

  /**
   * Static pattern constructor.
   *
   * @param url     The URL of the webhook.
   * @param payload The payload to send to the webhook.
   *
   * @return The TeamsHttpClient
   *
   * @throws MalformedURLException If the URL is malformed.
   */
  static TeamsHttpClient of(String url, Payload payload) throws MalformedURLException {
    return new TeamsHttpClient(url, payload);
  }

  /**
   * Sets the bypass HTTPS validation state to enabled or disabled.
   *
   * @param bypass Set to true to enable the bypass. False to disable.
   *
   * @return The TeamsHttpClient
   */
  TeamsHttpClient bypassHttpsValidation(boolean bypass) {
    this.bypassHttpsValidation = bypass;
    return this;
  }

  /**
   * Sets proxy settings on the TeamsHttpClient.
   *
   * @param ip   The proxy host name or IP.
   * @param port The proxy port.
   *
   * @return The TeamsHttpClient.
   */
  TeamsHttpClient proxy(Optional<String> ip, Optional<Integer> port) {
    this.proxyIp = ip;
    this.proxyPort = port;
    return this;
  }

  /**
   * Sets proxy auth settings on the TeamsHttpClient.
   *
   * @param username The proxy username.
   * @param password The proxy password.
   *
   * @return The TeamsHttpClient
   */
  TeamsHttpClient proxyAuth(Optional<String> username, Optional<String> password) {
    this.proxyUser = username;
    this.proxyPass = password;
    return this;
  }

  /**
   * Builds the TeamsHttpClient, preparing it to make the request.
   *
   * @return The TeamsHttpClient
   *
   * @throws UnsupportedEncodingException If the payload is malformed.
   */
  TeamsHttpClient build() throws UnsupportedEncodingException {
    port = getPort();
    path = getPath();
    httpClient = getHttpClient();
    target = new HttpHost(hook.getHost(), port, hook.getProtocol());
    httpPost = getHttpPost();

    LOG.debug(
        "TeamsHttpClient BUILT"
        + " | Host: " + hook.getHost()
        + " | Port: " + port
        + " | Path: " + path
        + " | Bypass HTTPS Validation: " + bypassHttpsValidation
        + " | ProxyEnabled: " + proxyEnabled()
        + " | ProxyAuthEnabled: " + proxyAuthEnabled()
        + " | Proxy IP: " + proxyIp.orElse(NOT_SET)
        + " | Proxy Port: " + (proxyPort.isPresent() ? proxyPort.get() : NOT_SET)
        + " | Proxy User: " + proxyUser.orElse(NOT_SET)
        + " | Proxy Pass (length): " + proxyPass.orElse("").length()
    );

    return this;
  }

  /**
   * Posts the message to the webhook.
   *
   * @return True on success. False on failure.
   */
  boolean post() {
    boolean success = false;
    try {
      CloseableHttpResponse response = httpClient.execute(target, httpPost);
      int responseCode = response.getStatusLine().getStatusCode();
      if (responseCode != 200) {
        throw new InvalidHttpResponseException("Invalid HTTP Response Code: " + responseCode);
      }

      LOG.info("POST Successful!");
      success = true;
    } catch (Exception e) {
      LOG.error("Failed to send teams message", e);
    } finally {
      try {
        httpClient.close();
      } catch (Exception e) {
        LOG.error("Unable to close HTTP Client", e);
      }
    }

    return success;
  }

  /**
   * Gets the HttpPost request object.
   *
   * @return The HttpPost.
   *
   * @throws UnsupportedEncodingException If the payload is malformed.
   */
  private HttpPost getHttpPost() throws UnsupportedEncodingException {
    Gson gson = new Gson();
    HttpPost tempHttpPost = new HttpPost(path);
    tempHttpPost.setEntity(new StringEntity(gson.toJson(payload)));
    tempHttpPost.setHeader("Accept", "application/json");
    tempHttpPost.setHeader("Content-type", "application/json");

    if (proxyEnabled()) {
      //noinspection OptionalGetWithoutIsPresent
      HttpHost proxy = new HttpHost(proxyIp.get(), proxyPort.get());
      RequestConfig config = RequestConfig.custom()
          .setProxy(proxy)
          .build();
      tempHttpPost.setConfig(config);
    }

    return tempHttpPost;
  }

  /**
   * Checks if the HttpClient should use a proxy.
   *
   * @return True if proxy is enabled. False if not.
   */
  private boolean proxyEnabled() {
    return (proxyIp.isPresent() && proxyPort.isPresent());
  }

  /**
   * Checks if the HttpClient should use proxy authentication.
   *
   * @return True if proxy auth is enabled. False if not.
   */
  private boolean proxyAuthEnabled() {
    return (proxyEnabled() && proxyUser.isPresent() && proxyPass.isPresent());
  }

  /**
   * Gets the internal HTTP Client to be used for the request.
   *
   * @return The HTTP Client.
   */
  private CloseableHttpClient getHttpClient() {
    CloseableHttpClient tempHttpClient = HttpClients.createDefault();
    if (proxyAuthEnabled() || bypassHttpsValidation) {
      HttpClientBuilder httpClientBuilder = HttpClients.custom();
      if (bypassHttpsValidation) {
        try {
          httpClientBuilder
            .setSSLContext(
                new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build()
            )
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
          LOG.error("Error bypassing HTTPS Validation", e);
        }
      }
      if (proxyAuthEnabled()) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        //noinspection OptionalGetWithoutIsPresent
        credsProvider.setCredentials(
            new AuthScope(proxyIp.get(), proxyPort.get()),
            new UsernamePasswordCredentials(proxyUser.get(), proxyPass.get()));
        credsProvider.setCredentials(
            new AuthScope(hook.getHost(), port),
            new UsernamePasswordCredentials(proxyUser.get(), proxyPass.get()));
        httpClientBuilder
            .setDefaultCredentialsProvider(credsProvider);
      }

      tempHttpClient = httpClientBuilder.build();
    }

    return tempHttpClient;
  }

  /**
   * Gets the port of the webhook URL.
   *
   * @return The port of the webhook URL.
   */
  private int getPort() {
    int tempPort = hook.getPort();
    if (tempPort == -1) {
      tempPort = (hook.getProtocol().equals("https") ? 443 : 80);
    }

    return tempPort;
  }

  /**
   * Gets the full path of the webhook URL, including query string and anchor reference.
   *
   * @return The full path of the webhook URL.
   */
  private String getPath() {
    String tempPath = hook.getPath();
    String query = hook.getQuery();
    if (query != null && !query.isEmpty()) {
      tempPath += "?" + query;
    }

    String ref = hook.getRef();
    if (ref != null && !ref.isEmpty()) {
      tempPath += "#" + ref;
    }

    return tempPath;
  }

}
