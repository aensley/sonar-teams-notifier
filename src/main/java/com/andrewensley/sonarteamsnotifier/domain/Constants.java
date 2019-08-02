package com.andrewensley.sonarteamsnotifier.domain;

/**
 * Constants to be used across the plugin.
 */
public class Constants {

  /**
   * The plugin's category.
   */
  public static final String CATEGORY = "Teams";

  /**
   * The plugin's sub category.
   */
  public static final String SUBCATEGORY = "Teams Notifier Plugin";

  /**
   * The name of the enabled property.
   */
  public static final String ENABLED = "sonar.teams.enabled";

  /**
   * The name of the webhook property, supplied to sonar-scanner.
   */
  public static final String HOOK = "sonar.teams.hook";

  /**
   * The name of the fail-only property, supplied to sonar-scanner.
   */
  public static final String FAIL_ONLY = "sonar.teams.fail_only";

  /**
   * The name of the change author email property, supplied to sonar-scanner.
   */
  public static final String CHANGE_AUTHOR_EMAIL = "sonar.teams.change_author_email";

  /**
   * The name of the change author name property, supplied to sonar-scanner.
   */
  public static final String CHANGE_AUTHOR_NAME = "sonar.teams.change_author_name";

  /**
   * The name of the commit URL property, supplied to sonar-scanner.
   */
  public static final String COMMIT_URL = "sonar.teams.commit_url";

  /**
   * The name of the bypass HTTPS validation property.
   */
  public static final String BYPASS_HTTPS_VALIDATION = "sonar.teams.bypass_https_validation";

  /**
   * The name of the Proxy IP property.
   */
  public static final String PROXY_IP = "sonar.teams.proxy_ip";

  /**
   * The name of the Proxy Port property.
   */
  public static final String PROXY_PORT = "sonar.teams.proxy_port";

  /**
   * The name of the Proxy User property.
   */
  public static final String PROXY_USER = "sonar.teams.proxy_user";

  /**
   * The name of the Proxy Password property.
   */
  public static final String PROXY_PASS = "sonar.teams.proxy_pass";

  private Constants() {
  }
}
