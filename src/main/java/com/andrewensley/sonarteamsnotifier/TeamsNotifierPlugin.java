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
   * Index of properties added in the extension list.
   */
  private int propertyIndex = 0;

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
    extensions.add(getBooleanProperty(Constants.ENABLED, "Plugin enabled",
        "Are Teams notifications enabled in general?"));
    extensions.add(getBooleanProperty(Constants.BYPASS_HTTPS_VALIDATION, "Bypass HTTPS Validation",
        "Bypass SSL/TLS certificate validation on HTTPS requests (useful for proxies)"));
    extensions.add(getStringProperty(Constants.PROXY_IP, "Proxy Server",
        "Domain or IP address of proxy server to use"));
    extensions.add(getProxyPortProperty());
    extensions.add(getStringProperty(Constants.PROXY_USER, "Proxy User",
        "User name for proxy authentication"));
    extensions.add(getStringProperty(Constants.PROXY_PASS, "Proxy Password",
        "Password for proxy authentication"));
    return extensions;
  }

  /**
   * Gets a single string property to add to Sonar plugins.
   *
   * @param property     Property ID.
   * @param name         Property name.
   * @param description  Property description.
   *
   * @return The property to add.
   */
  private PropertyDefinition getStringProperty(String property, String name, String description) {
    return PropertyDefinition.builder(property)
        .name(name)
        .description(description)
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(propertyIndex++)
        .build();
  }

  /**
   * Gets a single boolean property to add to Sonar plugins.
   *
   * @param property     Property ID.
   * @param name         Property name.
   * @param description  Property description.
   *
   * @return The property to add.
   */
  private PropertyDefinition getBooleanProperty(
      String property,
      String name,
      String description
  ) {
    return PropertyDefinition.builder(property)
        .name(name)
        .description(description)
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(propertyIndex++)
        .build();
  }

  /**
   * Gets a single boolean property to add to Sonar plugins.
   *
   * @return The property to add.
   */
  private PropertyDefinition getProxyPortProperty() {
    return PropertyDefinition.builder(Constants.PROXY_PORT)
        .name("Proxy Port")
        .description("Port for the proxy server")
        .defaultValue("8080")
        .type(PropertyType.INTEGER)
        .category(Constants.CATEGORY)
        .subCategory(Constants.SUBCATEGORY)
        .index(propertyIndex++)
        .build();
  }
}
