package coop.bancocredicoop.omnited.handler.ivrNodes;

import ch.loway.oss.ari4java.generated.models.Playback;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
  private CanalMensajeria canalMensajeria;
  private RedisService redis;
  private int TTS = 300;

  public MessageService(CanalMensajeria canalMensajeria, RedisService redisService) {
    this.canalMensajeria = canalMensajeria;
    this.redis = redisService;
  }

  public void sendMessage(String channelId, String text) {
    Playback playback = canalMensajeria.enviarMensaje(channelId, text);
    redis.set("playback:" + playback.getId(), channelId, TTS);
    redis.set("channel:" + channelId, playback.getId(), TTS);
  }

  public void stopPlayback(String channelId) {
    String playbackId = redis.get("channel:" + channelId);
    canalMensajeria.stopPlayback(playbackId);
  }

  public void clearPlaybackCache(String channelId) {
    String playbackId = redis.get("channel:" + channelId);
    redis.delete("playback:" + playbackId);
    redis.delete("channel:" + channelId);
  }

  public boolean hasActivePlayback(String channelId) {
    String playbackId = redis.get("channel:" + channelId);
    return playbackId != null && !playbackId.isEmpty();
  }

}
