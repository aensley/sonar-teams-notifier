package com.andrewensley.sonarteamsnotifier.extension;

import static java.lang.String.format;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Condition;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;


/**
 * Builds a payload for a WebEx Teams message.
 */
class PayloadBuilder {

  /**
   * Logger.
   */
  private static final Logger LOG = Loggers.get(PayloadBuilder.class);

  /**
   * Project Analysis.
   */
  private PostProjectAnalysisTask.ProjectAnalysis analysis;

  /**
   * Project URL.
   */
  private String projectUrl;

  /**
   * Whether to send notifications on only failures.
   */
  private boolean failOnly;

  /**
   * Decimal format for percentages.
   */
  private DecimalFormat percentageFormat;

  /**
   * Constructor.
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
  Payload build() {
    assertNotNull(projectUrl, "projectUrl");
    assertNotNull(failOnly, "failOnly");
    assertNotNull(analysis, "analysis");

    Payload payload = new Payload();
    payload.markdown = getMessage();
    LOG.info("WebEx Teams message: " + payload.markdown);

    return payload;
  }

  /**
   * Returns the message to send.
   *
   * @return The message.
   */
  @SuppressWarnings("deprecation")
  private String getMessage() {
    QualityGate qualityGate = analysis.getQualityGate();
    if (qualityGate == null) {
      return "";
    }

    StringBuilder message = new StringBuilder();
    message.append(format(
        "# %s %S - [%s]\n\n",
        qualityGate.getName(),
        qualityGate.getStatus(),
        analysis.getProject().getName()
    ));

    Optional<Branch> branch = analysis.getBranch();
    /* && !branch.get().isMain()*/
    branch.ifPresent(
        value -> message.append(format("* **Branch**: %s  \n", value.getName().orElse("")))
    );

    String commit = analysis.getScmRevisionId();
    message.append(format("* **Commit**: %s  \n", commit));
    Date date = analysis.getDate();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    message.append(format("* **Date**: %s  \n", simpleDateFormat.format(date)));
    message.append(getConditionsAppended(qualityGate, failOnly));
    message.append(format("\n\nSee %s", projectUrl));
    return message.toString();
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
  private boolean notOkOrNoValueCondition(Condition condition) {
    return !(QualityGate.EvaluationStatus.OK.equals(condition.getStatus())
      || QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus()));
  }

  /**
   * Translates individual conditions to formatted strings.
   *
   * @param condition The condition to translate.
   *
   * @return The translated condition.
   */
  private String translateCondition(Condition condition) {
    if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
      // No value for given metric
      return format("* **%s**: %s\n", condition.getMetricKey(), condition.getStatus().name());
    } else {
      return format(
          "* **%s**: %s\n  * %s\n",
          condition.getMetricKey(),
          condition.getStatus().name(),
          getConditionString(condition)
      );
    }
  }

  /**
   * Gets the condition string when there's more detailed information
   * about the quality gate condition.
   *
   * @param condition The Quality Gate Condition.
   *
   * @return The condition string.
   */
  @SuppressWarnings("deprecation")
  private String getConditionString(Condition condition) {
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

    return sb.toString();
  }

  /**
   * Appends a condition's value to a StringBuilder.
   *
   * @param condition The condition.
   * @param sb        The StringBuilder.
   */
  private void appendConditionValue(Condition condition, StringBuilder sb) {
    String value = condition.getValue();
    if (value.equals("")) {
      sb.append("-");
    } else {
      appendNonEmptyValue(condition, sb, value);
    }
  }

  /**
   * Appends a non-empty value to the condition string.
   *
   * @param condition The condition.
   * @param sb        The StringBuilder.
   * @param value     The value.
   */
  private void appendNonEmptyValue(Condition condition, StringBuilder sb, String value) {
    if (conditionValueIsPercentage(condition)) {
      appendPercentageValue(sb, value);
    } else {
      sb.append(value);
    }
  }

  /**
   * Appends a percentage value to the condition string.
   *
   * @param sb    The StringBuilder.
   * @param value The percentage value.
   */
  private void appendPercentageValue(StringBuilder sb, String value) {
    try {
      Double percent = Double.parseDouble(value);
      sb.append(percentageFormat.format(percent));
      sb.append("%");
    } catch (NumberFormatException e) {
      LOG.error("Failed to parse [{}] into a Double due to [{}]", value, e.getMessage());
      sb.append(value);
    }
  }

  /**
   * Appends a condition's comparison operator to a StringBuilder.
   *
   * @param condition The condition.
   * @param sb        The StringBuilder.
   */
  @SuppressWarnings("deprecation")
  private void appendConditionComparisonOperator(Condition condition, StringBuilder sb) {
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
  private boolean conditionValueIsPercentage(Condition condition) {
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
      throw new IllegalArgumentException(
          "[Assertion failed] - " + objectName + " argument is required; it must not be null"
      );
    }
  }
}
