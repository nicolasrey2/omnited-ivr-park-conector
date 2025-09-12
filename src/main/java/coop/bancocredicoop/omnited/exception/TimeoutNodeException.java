package coop.bancocredicoop.omnited.exception;

public class TimeoutNodeException extends RuntimeException {
  private final String targetNode;

  public TimeoutNodeException(String message, String targetNode) {
    super(message);
    this.targetNode = targetNode;
  }

  public String getTargetNode() {
    return targetNode;
  }
}
