package uk.ac.ebi.biostudies.exceptions;

/**
 * Thrown when the Indexer service is unreachable or returns a server error.
 *
 * <p>Indicates a deployment/runtime issue where RIBS cannot retrieve indexed data because the
 * Indexer service (mandatory dependency post-migration) is down, overloaded, or misconfigured.
 *
 * <p>Callers should treat this as a transient or permanent failure depending on context:
 *
 * <ul>
 *   <li>API → HTTP 503 Service Unavailable
 *   <li>Background jobs → Retry with backoff
 *   <li>UI → "Service temporarily unavailable"
 * </ul>
 */
public class IndexerServiceUnavailableException extends RuntimeException {

  public IndexerServiceUnavailableException(String message) {
    super(message);
  }

  public IndexerServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
