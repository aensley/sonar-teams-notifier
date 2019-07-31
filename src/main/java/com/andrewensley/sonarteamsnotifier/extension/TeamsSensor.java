package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;

import java.util.Optional;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Plugin to check for this plugin's scanner properties.
 */
public class TeamsSensor implements Sensor {


  /**
   * Logger.
   */
  private static final Logger LOG = Loggers.get(TeamsSensor.class);

  /**
   * Describes this sensor.
   *
   * @param descriptor The SensorDescriptor
   */
  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name(getClass().getName());
  }

  /**
   * Executes this sensor.
   *
   * @param context The SensorContext
   */
  @Override
  public void execute(SensorContext context) {
    Optional<String> hook = context.config().get(Constants.HOOK);
    if (hook.isPresent()) {
      LOG.info("Sonar Teams Notifier Hook URL found.");
      context.addContextProperty(Constants.HOOK, hook.get());
    }

    Optional<String> failOnly = context.config().get(Constants.FAIL_ONLY);
    if (failOnly.isPresent()) {
      LOG.info("Sonar Teams Notifier fail_only found.");
      context.addContextProperty(Constants.FAIL_ONLY, failOnly.get());
    }
  }
}
