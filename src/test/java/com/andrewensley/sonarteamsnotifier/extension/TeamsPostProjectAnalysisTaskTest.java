package com.andrewensley.sonarteamsnotifier.extension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.andrewensley.sonarteamsnotifier.domain.Constants;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Status;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;

@ExtendWith(MockitoExtension.class)
class TeamsPostProjectAnalysisTaskTest {

  @Mock
  private Configuration configuration;

  @Mock
  private ProjectAnalysis projectAnalysis;

  @Mock
  private Project project;

  @Mock
  private QualityGate qualityGate;

  @Mock
  private Branch branch;

  @Mock
  private Context context;

  @Mock
  private Analysis analysis;

  private Map<String, String> properties = new HashMap<>();

  @Test
  @Disabled("For demonstration/debugging purposes. This test push notification to your real teams.")
  void executePluginMainProcess() {
    String hook = "";
    boolean failOnly = true;

    enablePluginConfiguration();

    setPropertyToAnalysis();
    addProperty(Constants.HOOK, hook);
    addProperty(Constants.FAIL_ONLY, "");

    addProperty(Constants.COMMIT_URL, "");
    addProperty(Constants.CHANGE_AUTHOR_EMAIL, "");
    addProperty(Constants.CHANGE_AUTHOR_NAME, "");

    setProjectToAnalysis();
    setQualityGateToAnalysis();

    setMasterBranch();
    mockAnalysisData();

    TeamsPostProjectAnalysisTask task = new TeamsPostProjectAnalysisTask(configuration);
    setContext();
    task.finished(context);
  }

  private void enablePluginConfiguration() {
    when(configuration.getBoolean(Constants.ENABLED)).thenReturn(Optional.of(true));
  }

  private void setPropertyToAnalysis() {
    ScannerContext scannerContext = mock(ScannerContext.class);
    when(projectAnalysis.getScannerContext()).thenReturn(scannerContext);
    when(scannerContext.getProperties()).thenReturn(properties);
  }

  private void addProperty(String key, Object value) {
    properties.put(key, String.valueOf(value));
  }

  private void setProjectToAnalysis() {
    lenient().when(projectAnalysis.getProject()).thenReturn(project);
  }

  private void setQualityGateToAnalysis() {
    lenient().when(projectAnalysis.getQualityGate()).thenReturn(qualityGate);
  }

  private void setMasterBranch() {
    lenient().when(branch.isMain()).thenReturn(true);
  }

  private void setContext() {
    lenient().when(context.getProjectAnalysis()).thenReturn(projectAnalysis);
  }

  private void mockAnalysisData() {
    lenient().when(qualityGate.getName()).thenReturn("Test QG name");
    lenient().when(qualityGate.getStatus()).thenReturn(Status.OK);
    lenient().when(project.getKey()).thenReturn("test_project_key");
    lenient().when(project.getName()).thenReturn("Test project Name");
    lenient().when(projectAnalysis.getBranch()).thenReturn(Optional.of(branch));
    lenient().when(projectAnalysis.getAnalysis()).thenReturn(Optional.of(analysis));
    when(analysis.getDate()).thenReturn(new Date());
  }

}
