package coop.bancocredicoop.omnited.exception;

public class MishandledStateException extends RuntimeException {
  public MishandledStateException(String message) {
    super(message);
  }
}
