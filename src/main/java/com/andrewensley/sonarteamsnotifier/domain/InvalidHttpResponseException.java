package com.andrewensley.sonarteamsnotifier.domain;

public class InvalidHttpResponseException extends Exception {

  /**
   * Constructor.
   *
   * @param errorMessage The error message of the exception.
   */
  public InvalidHttpResponseException(String errorMessage) {
    super(errorMessage);
  }
}
