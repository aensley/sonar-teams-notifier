package com.andrewensley.sonarteamsnotifier.extension;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO for WebEx Teams Messages.
 */
class Payload {

  String type = "MessageCard";
  String context = "https://schema.org/extensions";
  /**
   * The message to send with markdown formatting.
   */
  String text = "";
  List<OpenUriAction> potentialAction = new ArrayList<>();
  String themeColor;

  /**
   * Constructor.
   *
   * @param text The message to send with markdown formatting.
   */
  Payload(String text) {
    if (text != null) {
      this.text = text;
    }
  }

  public void addOpenUriButton(String url) {
    OpenUriAction openUriAction = new OpenUriAction();
    openUriAction.addTarget(new OpenUriActionTarget(url));
    potentialAction.add(openUriAction);

  }

  public void setThemeColor(boolean qualityGateOk) {
    themeColor = qualityGateOk ? "0ff129" : "f10f0f";
  }

  private class OpenUriAction {

    @SerializedName("@type")
    private String type = "OpenUri";
    private String name = "Open sonar";
    private List<OpenUriActionTarget> targets = new ArrayList<>();

    public void addTarget(OpenUriActionTarget target) {
      targets.add(target);
    }
  }

  private class OpenUriActionTarget {

    private String os;
    private String uri;

    public OpenUriActionTarget(String uri) {
      this("default", uri);
    }

    public OpenUriActionTarget(String os, String uri) {
      this.os = os;
      this.uri = uri;
    }
  }
}
