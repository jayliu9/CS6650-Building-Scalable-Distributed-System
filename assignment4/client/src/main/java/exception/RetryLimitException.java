package exception;

public class RetryLimitException extends Exception{

  public RetryLimitException(String message) {
    super(message);
  }
}
