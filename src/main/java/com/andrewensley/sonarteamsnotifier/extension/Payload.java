package com.andrewensley.sonarteamsnotifier.extension;

/**
 * POJO for WebEx Teams Messages.
 */
class Payload {

  /**
   * The message to send with markdown formatting.
   */
  String markdown = "";

  /**
   * Constructor.
   *
   * @param markdown The message to send with markdown formatting.
   */
  Payload(String markdown) {
    if (markdown != null) {
      this.markdown = markdown;
    }
  }
}
