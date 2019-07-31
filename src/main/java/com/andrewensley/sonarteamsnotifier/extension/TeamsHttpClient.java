package com.andrewensley.sonarteamsnotifier.extension;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

class TeamsHttpClient {

  /**
   * Logger.
   */
  private static final Logger LOG = Loggers.get(PayloadBuilder.class);

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
    target = new HttpHost(hook.getHost(), port);
    httpPost = getHttpPost();

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
        throw new Exception("Invalid HTTP Response Code: " + responseCode);
      }

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
    HttpPost httpPost = new HttpPost(path);
    httpPost.setEntity(new StringEntity(gson.toJson(payload)));
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");

    if (proxyEnabled()) {
      HttpHost proxy = new HttpHost(proxyIp.get(), proxyPort.get());
      RequestConfig config = RequestConfig.custom()
          .setProxy(proxy)
          .build();
      httpPost.setConfig(config);
    }

    return httpPost;
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
    CloseableHttpClient httpClient;
    if (proxyAuthEnabled()) {
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

  /**
   * Gets the port of the webhook URL.
   *
   * @return The port of the webhook URL.
   */
  private int getPort() {
    int port = hook.getPort();
    if (port == -1) {
      port = (hook.getProtocol().equals("https") ? 443 : 80);
    }

    return port;
  }

  /**
   * Gets the full path of the webhook URL, including query string and anchor reference.
   *
   * @return The full path of the webhook URL.
   */
  private String getPath() {
    String path = hook.getPath();
    if (!hook.getQuery().isEmpty()) {
      path += "?" + hook.getQuery();
    }

    if (!hook.getRef().isEmpty()) {
      path += "#" + hook.getRef();
    }

    return path;
  }

}
