package coop.bancocredicoop.omnited.entity;

public class Playback {
  String id;
  String channelId;

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
}
