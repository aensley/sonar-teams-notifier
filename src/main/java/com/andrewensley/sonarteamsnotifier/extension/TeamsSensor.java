package com.andrewensley.sonarteamsnotifier.extension;

import com.andrewensley.sonarteamsnotifier.domain.Constants;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import java.util.Optional;

/**
 * Plugin to check for this plugin's scanner properties.
 */
public class TeamsSensor implements Sensor {

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
            context.addContextProperty(Constants.HOOK, hook.get());
        }

        Optional<String> fail_only = context.config().get(Constants.FAIL_ONLY);
        if (fail_only.isPresent()) {
            context.addContextProperty(Constants.FAIL_ONLY, fail_only.get());
        }
    }
}
