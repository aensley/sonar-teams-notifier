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
    extensions.add(getProperty(Constants.ENABLED, "Plugin enabled",
        "Are Teams notifications enabled in general?",
        "false", PropertyType.BOOLEAN));
    extensions.add(getProperty(Constants.BYPASS_HTTPS_VALIDATION, "Bypass HTTPS Validation",
        "Bypass SSL/TLS certificate validation on HTTPS requests (useful for proxies)",
        "false", PropertyType.BOOLEAN));
    extensions.add(getProperty(Constants.PROXY_IP, "Proxy Server",
        "Domain or IP address of proxy server to use",
        "", PropertyType.STRING));
    extensions.add(getProperty(Constants.PROXY_PORT, "Proxy Port",
        "Port for the proxy server",
        "8080", PropertyType.INTEGER));
    extensions.add(getProperty(Constants.PROXY_USER, "Proxy User",
        "User name for proxy authentication",
        "", PropertyType.STRING));
    extensions.add(getProperty(Constants.PROXY_PASS, "Proxy Password",
        "Password for proxy authentication",
        "", PropertyType.STRING));
    return extensions;
  }

  /**
   * Gets a single property to add to Sonar plugins.
   *
   * @param property     Property ID.
   * @param name         Property name.
   * @param description  Property description.
   * @param defaultValue Default value of the property.
   * @param type         Property type.
   *
   * @return The property to add.
   */
  private PropertyDefinition getProperty(
      String property,
      String name,
      String description,
      String defaultValue,
      PropertyType type
  ) {
    return PropertyDefinition.builder(property)
      .name(name)
      .description(description)
      .defaultValue(defaultValue)
      .type(type)
      .category(Constants.CATEGORY)
      .subCategory(Constants.SUBCATEGORY)
      .index(propertyIndex++)
      .build();
  }
}
