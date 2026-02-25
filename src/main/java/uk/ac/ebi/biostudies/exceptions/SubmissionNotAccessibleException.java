package uk.ac.ebi.biostudies.exceptions;

/**
 * Thrown when a submission exists but the caller lacks permission to access it.
 *
 * <p>Typically occurs when:
 *
 * <ul>
 *   <li>Requesting a private/unreleased submission without valid {@code secretKey}
 *   <li>Insufficient user privileges for restricted content
 * </ul>
 *
 * <p>HTTP mapping: <b>403 Forbidden</b>
 *
 * <p><strong>Unchecked exception</strong> (extends RuntimeException) to avoid boilerplate {@code
 * throws} declarations in service layers.
 */
public class SubmissionNotAccessibleException extends RuntimeException {

  /** Creates an access exception with default message. */
  public SubmissionNotAccessibleException() {
    super("Submission is not accessible");
  }

  /** Creates an access exception with custom message. */
  public SubmissionNotAccessibleException(String message) {
    super(message);
  }

  /** Creates an access exception with message and cause. */
  public SubmissionNotAccessibleException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Creates an access exception with cause only. */
  public SubmissionNotAccessibleException(Throwable cause) {
    super(cause);
  }
}
