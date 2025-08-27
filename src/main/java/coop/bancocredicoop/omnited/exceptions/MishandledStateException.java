package coop.bancocredicoop.omnited.exceptions;

public class MishandledStateException extends RuntimeException {
  public MishandledStateException(String message) {
    super(message);
  }
}
