package com.andrewensley.sonarteamsnotifier.extension;

import com.google.gson.Gson;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import static java.lang.String.format;

/**
 * Builds a payload for a WebEx Teams message.
 */
class PayloadBuilder {

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(PayloadBuilder.class);

    /**
     * Project Analysis.
     */
    private PostProjectAnalysisTask.ProjectAnalysis analysis;

    /**
     * Project URL
     */
    private String projectUrl;

    /**
     * Whether to send notifications on only failures.
     */
    private boolean failOnly;

    /**
     * Decimal format for percentages
     */
    private DecimalFormat percentageFormat;

    /**
     * Constructor
     *
     * @param analysis The Project's analysis.
     */
    private PayloadBuilder(PostProjectAnalysisTask.ProjectAnalysis analysis) {
        this.analysis = analysis;
        // Round percentages to 2 decimal points.
        this.percentageFormat = new DecimalFormat();
        this.percentageFormat.setMaximumFractionDigits(2);
    }

    /**
     * Static pattern PayloadBuilder constructor.
     *
     * @param analysis The Project's analysis.
     *
     * @return The PayloadBuilder
     */
    static PayloadBuilder of(PostProjectAnalysisTask.ProjectAnalysis analysis) {
        return new PayloadBuilder(analysis);
    }

    /**
     * Set failOnly in chained static builder.
     *
     * @param failOnly Whether to alert only on failures or not.
     *
     * @return The PayloadBuilder
     */
    PayloadBuilder failOnly(boolean failOnly) {
        this.failOnly = failOnly;
        return this;
    }

    /**
     * Set projectUrl in chained static builder.
     *
     * @param projectUrl The URL for the project.
     *
     * @return The PayloadBuilder
     */
    PayloadBuilder projectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
        return this;
    }

    /**
     * Builds the payload.
     *
     * @return The payload as a JSON-encoded string.
     */
    String build() {
        assertNotNull(projectUrl, "projectUrl");
        assertNotNull(failOnly, "failOnly");
        assertNotNull(analysis, "analysis");

        QualityGate qualityGate = analysis.getQualityGate();
        StringBuilder message = new StringBuilder();
        message.append(format("# %s %S - [%s]\n\n", qualityGate.getName(), qualityGate.getStatus(), analysis.getProject().getName()));

        Optional<Branch> branch = analysis.getBranch();
        if (branch.isPresent()/* && !branch.get().isMain()*/) {
            message.append(format("* **Branch**: %s  \n", branch.get().getName().orElse("")));
        }

        String commit = analysis.getScmRevisionId();
        message.append(format("* **Commit**: %s  \n", commit));

        Date date = analysis.getDate();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        message.append(format("* **Date**: %s  \n", simpleDateFormat.format(date)));

        message.append(getConditionsAppended(qualityGate, failOnly));

        message.append(format("\n\nSee %s", projectUrl));

        Payload payload = new Payload();
        payload.markdown = message.toString();
        LOG.info("WebEx Teams message: " + payload.markdown);

        Gson gson = new Gson();

        return gson.toJson(payload);
    }

    /**
     * Appends Condition statuses to the message.
     *
     * @param qualityGate The Quality Gate.
     * @param failOnly    Whether to notify only on failures.
     *
     * @return The conditions string to append to the message.
     */
    private String getConditionsAppended(QualityGate qualityGate, boolean failOnly) {

        StringBuilder sb = new StringBuilder();

        List<String> conditions = qualityGate.getConditions()
                .stream()
                .filter(condition -> !failOnly || notOkOrNoValueCondition(condition))
                .map(this::translateCondition)
                .collect(Collectors.toList());

        for (String condition : conditions) {
            sb.append(condition);
        }

        return sb.toString();
    }

    /**
     * Checks that the condition value is not OK or NO_VALUE. Any other value indicates a failure.
     *
     * @param condition The condition to be checked.
     *
     * @return True if the condition is not OK or NO_VALUE. False if it is.
     */
    private boolean notOkOrNoValueCondition(QualityGate.Condition condition) {
        return !( QualityGate.EvaluationStatus.OK.equals(condition.getStatus())
            || QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus()) );
    }

    /**
     * Translates individual conditions to formatted strings.
     *
     * @param condition The condition to translate.
     *
     * @return The translated condition.
     */
    private String translateCondition(QualityGate.Condition condition) {
        String conditionName = condition.getMetricKey();

        if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
            // No value for given metric
            return format("* **%s**: %s\n", conditionName, condition.getStatus().name());
        } else {
            StringBuilder sb = new StringBuilder();
            appendConditionValue(condition, sb);
            if (condition.getWarningThreshold() != null) {
                sb.append(", warning if ");
                appendConditionComparisonOperator(condition, sb);
                sb.append(condition.getWarningThreshold());
            }

            if (condition.getErrorThreshold() != null) {
                sb.append(", error if ");
                appendConditionComparisonOperator(condition, sb);
                sb.append(condition.getErrorThreshold());
            }

            return format("* **%s**: %s\n  * %s\n", conditionName, condition.getStatus().name(), sb.toString());
        }
    }

    /**
     * Appends a condition's value to a StringBuilder.
     *
     * @param condition The condition.
     * @param sb        The StringBuilder.
     */
    private void appendConditionValue(QualityGate.Condition condition, StringBuilder sb) {
        String value = condition.getValue();
        if (value.equals("")) {
            sb.append("-");
        } else {
            if (conditionValueIsPercentage(condition)) {
                try {
                    Double d = Double.parseDouble(value);
                    sb.append(percentageFormat.format(d));
                    sb.append("%");
                } catch (NumberFormatException e) {
                    LOG.error("Failed to parse [{}] into a Double due to [{}]", value, e.getMessage());
                    sb.append(value);
                }
            } else {
                sb.append(value);
            }
        }
    }

    /**
     * Appends a condition's comparison operator to a StringBuilder.
     *
     * @param condition The condition.
     * @param sb        The StringBuilder.
     */
    private void appendConditionComparisonOperator(QualityGate.Condition condition, StringBuilder sb) {
        switch (condition.getOperator()) {
            case EQUALS:
                sb.append("==");
                break;
            case NOT_EQUALS:
                sb.append("!=");
                break;
            case GREATER_THAN:
                sb.append(">");
                break;
            case LESS_THAN:
                sb.append("<");
                break;
            default:
                break;
        }
    }

    /**
     * Checks if a condition's value is a percentage by checking the metric key.
     *
     * @param condition The condition to check.
     *
     * @return True if the condition's value is a percentage. False if not.
     */
    private boolean conditionValueIsPercentage(QualityGate.Condition condition) {
        switch (condition.getMetricKey()) {
            case CoreMetrics.NEW_COVERAGE_KEY:
            case CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY:
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * Asserts that an object is not null. Throws an exception if it is.
     *
     * @param object     The object to check.
     * @param objectName The name of the object.
     */
    private void assertNotNull(Object object, String objectName) {
        if (object == null) {
            throw new IllegalArgumentException("[Assertion failed] - " + objectName + " argument is required; it must not be null");
        }
    }
}
