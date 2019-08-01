package com.andrewensley.sonarteamsnotifier;

import com.andrewensley.sonarteamsnotifier.domain.Constants;
import com.andrewensley.sonarteamsnotifier.extension.TeamsPostProjectAnalysisTask;
import com.andrewensley.sonarteamsnotifier.extension.TeamsSensor;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;


/**
 * Base Plugin class.
 */
public class TeamsNotifierPlugin implements Plugin {

  /**
   * Defines this plugin's hooks.
   *
   * @param context The SonarQube context.
   */
  @Override
  public void define(Context context) {
    List<Object> extensions = pluginPropertyDefinitions();

    extensions.add(TeamsSensor.class);
    extensions.add(TeamsPostProjectAnalysisTask.class);

    context.addExtensions(extensions);
  }

  /**
   * Gets this plugin's property definitions.
   *
   * @return A list of this plugin's property definitions.
   */
  private List<Object> pluginPropertyDefinitions() {
    List<Object> extensions = new ArrayList<>();
    extensions.add(PropertyDefinition.builder(Constants.ENABLED)
        .name("Plugin enabled")
        .description("Are Teams notifications enabled in general?")
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(0)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.BYPASS_HTTPS_VALIDATION)
        .name("Bypass HTTPS Validation")
        .description("Bypass SSL/TLS certificate validation on HTTPS requests (useful for proxies)")
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(1)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.PROXY_IP)
        .name("Proxy Server")
        .description("Domain or IP address of proxy server to use")
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(2)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.PROXY_PORT)
        .name("Proxy port")
        .description("Port for proxy server")
        .defaultValue("8080")
        .type(PropertyType.INTEGER)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(3)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.PROXY_PROTOCOL)
        .name("Proxy protocol")
        .description("Protocol to use to connect to proxy server")
        .defaultValue("HTTP")
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options("DIRECT", "HTTP", "SOCKS")
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(4)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.PROXY_USER)
        .name("Proxy User")
        .description("User name for proxy authentication")
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(5)
        .build());
    extensions.add(PropertyDefinition.builder(Constants.PROXY_PASS)
        .name("Proxy Password")
        .description("Password for proxy authentication")
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(6)
        .build());
    return extensions;
  }
}
