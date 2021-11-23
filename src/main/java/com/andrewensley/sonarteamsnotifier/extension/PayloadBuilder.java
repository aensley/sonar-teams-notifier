package com.andrewensley.sonarteamsnotifier.extension;

import static java.lang.String.format;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Condition;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
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
   * Whether the overall QualityGate status is OK or not.
   */
  private boolean qualityGateOk;

  /**
   * The change author to mention on failures.
   */
  private String changeAuthor = "";

  /**
   * The URL of the commit.
   */
  private String commitUrl = "";

  /**
   * Decimal format for percentages.
   */
  private DecimalFormat percentageFormat;

  /**
   * Constructor.
   *
   * @param analysis      The Project's analysis.
   * @param projectUrl    The URL for the project.
   * @param failOnly      Whether to alert only on failures or not.
   * @param qualityGateOk Whether the overall quality gate status is OK or not.
   */
  private PayloadBuilder(
      PostProjectAnalysisTask.ProjectAnalysis analysis,
      String projectUrl,
      boolean failOnly,
      boolean qualityGateOk
  ) {
    this.analysis = analysis;
    this.projectUrl = projectUrl;
    this.failOnly = failOnly;
    this.qualityGateOk = qualityGateOk;
    // Round percentages to 2 decimal points.
    this.percentageFormat = new DecimalFormat();
    this.percentageFormat.setMaximumFractionDigits(2);
  }

  /**
   * Static pattern PayloadBuilder constructor.
   *
   * @param analysis      The Project's analysis.
   * @param projectUrl    The URL for the project.
   * @param failOnly      Whether to alert only on failures or not.
   * @param qualityGateOk Whether the overall quality gate status is OK or not.
   *
   * @return The PayloadBuilder
   */
  static PayloadBuilder of(
      PostProjectAnalysisTask.ProjectAnalysis analysis,
      String projectUrl,
      boolean failOnly,
      boolean qualityGateOk
  ) {
    return new PayloadBuilder(analysis, projectUrl, failOnly, qualityGateOk);
  }

  /**
   * Set changeAuthor in chained static builder.
   *
   * @param email The change author's email.
   * @param name  The change author's name.
   *
   * @return The PayloadBuilder
   */
  PayloadBuilder changeAuthor(String email, String name) {
    if (StringUtils.isNotEmpty(email)) {
      this.changeAuthor = "<@personEmail:" + email;
      if (StringUtils.isNotEmpty(name)) {
        this.changeAuthor += "|" + name;
      }

      this.changeAuthor += ">";
    } else if (StringUtils.isNotEmpty(name)) {
      this.changeAuthor = name;
    }

    return this;
  }

  /**
   * Set commitUrl in chained static builder.
   *
   * @param commitUrl The URL for the commit.
   *
   * @return The PayloadBuilder
   */
  PayloadBuilder commitUrl(String commitUrl) {
    if (commitUrl != null && !commitUrl.isEmpty()) {
      this.commitUrl = commitUrl;
    }

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
    assertNotNull(qualityGateOk, "qualityGateOk");
    assertNotNull(analysis, "analysis");

    QualityGate qualityGate = analysis.getQualityGate();
    Optional<Branch> branch = analysis.getBranch();
    StringBuilder message = new StringBuilder();
    if (qualityGate != null) {
      appendHeader(message, qualityGate, branch);
      appendCommit(message);
      appendBranch(message, branch);
      appendDate(message);
      appendConditions(message, qualityGate);
    }

    Payload payload = new Payload(message.toString());
    payload.addOpenUriButton(getProjectBranchUrl(branch));
    payload.setThemeColor(qualityGateOk);
    LOG.info("WebEx Teams message: " + payload.text);
    return payload;
  }

  /**
   * Appends the header to the message.
   *
   * @param message     The StringBuilder being used to build the message.
   * @param qualityGate The QualityGate object.
   * @param branch      The Branch object.
   */
  private void appendHeader(
    StringBuilder message,
    QualityGate qualityGate,
    Optional<Branch> branch
  ) {
    message.append(format(
      "### %s **%S** [[%s](%s)]\n\n",
      qualityGate.getName(),
      qualityGate.getStatus(),
      analysis.getProject().getName(),
      getProjectBranchUrl(branch)
    ));
  }

  /**
   * Appends commit information to the message.
   *
   * @param message The StringBuilder being used to build the message.
   */
  @SuppressWarnings("deprecation")
  private void appendCommit(StringBuilder message) {
    String commit = analysis.getScmRevisionId();
    message.append("**Commit**: ");
    if (commitUrl.isEmpty()) {
      message.append(commit);
    } else {
      message.append(format("[%s](%s)", commit, commitUrl));
    }

    if (!changeAuthor.isEmpty() && !qualityGateOk) {
      message.append(format(" by %s", changeAuthor));
    }

    message.append("  \n");
  }

  /**
   * Appends the analysis date to the message.
   *
   * @param message The StringBuilder being used to build the message.
   */
  @SuppressWarnings("deprecation")
  private void appendDate(StringBuilder message) {
    Date date = analysis.getAnalysis().get().getDate();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    message.append(format("**Date**: %s  \n", simpleDateFormat.format(date)));
  }

  /**
   * Appends the branch name to the message.
   *
   * @param message The StringBuilder being used to build the message.
   * @param branch  The Branch object.
   */
  private void appendBranch(StringBuilder message, Optional<Branch> branch) {
    if (branchIsNonMain(branch)) {
      //noinspection OptionalGetWithoutIsPresent
      message.append(format("**Branch**: %s  \n", branch.get().getName().orElse("default")));
    }
  }

  /**
   * Gets the URL for the project including the branch, if supplied.
   *
   * @param branch The branch that was analyzed.
   * @return The Project URL with optional branch.
   */
  private String getProjectBranchUrl(Optional<Branch> branch) {
    String projectBranchUrl = projectUrl;
    if (branchIsNonMain(branch)) {
      //noinspection OptionalGetWithoutIsPresent
      projectBranchUrl += format("&branch=%s", branch.get().getName().orElse(""));
    }

    return projectBranchUrl;
  }

  /**
   * Checks if the given branch is set and is not the main/master/default branch.
   *
   * @param branch The branch to check.
   * @return True if the branch is set and is not the main/master/default branch. Otherwise false.
   */
  private boolean branchIsNonMain(Optional<Branch> branch) {
    return branch.isPresent() && !branch.get().isMain();
  }

  /**
   * Appends Condition statuses to the message.
   *
   * @param message     The StringBuilder being used to build the message.
   * @param qualityGate The Quality Gate.
   */
  private void appendConditions(StringBuilder message, QualityGate qualityGate) {
    List<String> conditions = qualityGate.getConditions()
      .stream()
      .filter(condition -> !failOnly || notOkOrNoValueCondition(condition))
      .map(this::translateCondition)
      .collect(Collectors.toList());

    Collections.sort(conditions);
    for (String condition : conditions) {
      message.append(condition);
    }
  }

  /**
   * Checks that the condition value is not OK or NO_VALUE. Any other value indicates a failure.
   *
   * @param condition The condition to be checked.
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
   * @return The translated condition.
   */
  private String translateCondition(Condition condition) {
    if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
      // No value for given metric
      return format("  * **%s**: %s\n", condition.getMetricKey(), condition.getStatus().name());
    } else {
      return format(
        "  * **%s**: %s | %s\n",
        condition.getMetricKey(),
        condition.getStatus().name(),
        getConditionString(condition)
      );
    }
  }

  /**
   * Gets the condition string when there's more detailed information about the quality gate condition.
   *
   * @param condition The Quality Gate Condition.
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
      sb.append("**-**");
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
    sb.append("**");
    if (conditionValueIsPercentage(condition)) {
      appendPercentageValue(sb, value);
    } else {
      sb.append(value);
    }

    sb.append("**");
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
