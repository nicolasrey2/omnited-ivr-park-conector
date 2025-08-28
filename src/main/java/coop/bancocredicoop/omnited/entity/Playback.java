package coop.bancocredicoop.omnited.entity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Playback {
  String id;
  String channelId;
  private final Queue<String> dtmfQueue = new ConcurrentLinkedQueue<>();

  public Playback() {
  }

  public Playback(String id, String channelId) {
    this.id = id;
    this.channelId = channelId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  /**
   * Acumula un dígito DTMF recibido.
   */
  public void addDtmf(String digit) {
    if (digit != null && !digit.isEmpty()) {
      dtmfQueue.add(digit);
    }
  }

  /**
   * Devuelve todos los dígitos acumulados como un solo String
   * y limpia la cola.
   */
  public String getDtmfAcumulated() {
    StringBuilder sb = new StringBuilder();
    String d;
    while ((d = dtmfQueue.poll()) != null) {
      sb.append(d);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return "Playback[id=" + id + ", channel=" + channelId + ", dtmf=" + dtmfQueue + "]";
  }
}
