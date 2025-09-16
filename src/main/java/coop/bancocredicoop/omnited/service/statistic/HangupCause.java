package coop.bancocredicoop.omnited.service.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum HangupCause {

  NORMAL_CLEARING(16, "Normal clearing"),
  USER_BUSY(17, "User busy"),
  NO_USER_RESPONDING(18, "No user responding"),
  NO_ANSWER(19, "No answer"),
  CALL_REJECTED(21, "Call rejected"),
  NO_CIRCUIT_AVAILABLE(34, "No circuit/channel available"),
  TEMPORARY_FAILURE(41, "Temporary failure"),
  SWITCH_CONGESTION(42, "Switch congestion"),
  RESOURCE_UNAVAILABLE(47, "Resource unavailable"),
  INTERWORKING(127, "Interworking / Unknown cause"),
  UNKNOWN(-1, "Unknown");

  private static final Logger log = LoggerFactory.getLogger(HangupCause.class);

  private final int code;
  private final String description;

  HangupCause(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public int getCode() { return code; }
  public String getDescription() { return description; }

  public static HangupCause fromCode(int code) {
    for (HangupCause c : values()) {
      if (c.code == code) {
        return c;
      }
    }
    // Código desconocido: loguear error
    log.error("HangupCause desconocido recibido: code={}", code);
    return UNKNOWN;
  }
}

