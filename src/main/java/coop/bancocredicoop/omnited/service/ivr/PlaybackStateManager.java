package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.MishandledStateException;
import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import coop.bancocredicoop.omnited.entity.Playback;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PlaybackStateManager {
  private final Logger log = LoggerFactory.getLogger(PlaybackStateManager.class);
  private final Integer TTL = 300;
  private final ConcurrentMap<String, Playback> activePlaybacks = new ConcurrentHashMap<>();
  private final RedisService redisService;
  private final AriConnector ariConnector;

  public PlaybackStateManager(RedisService redisService, @Lazy AriConnector ariConnector) {
    this.redisService = redisService;
    this.ariConnector = ariConnector;
  }

  public void storeNextNode(JsonNode ivr, JsonNode actualNode, String channelId) {
    String siguienteNodoId = DiagramaUtils.obtenerTarget(ivr, actualNode);
    storeNextNodeFromChannel(siguienteNodoId, channelId);
  }

  private void storeNextNodeFromChannel(String nextNode, String channelId) {
    redisService.set("siguiente:" + channelId, nextNode, TTL);
    log.info("El siguiente nodo para el channelId: {} es el nodo con id: {}", channelId, nextNode);
  }


  public void storePlayback(String channelId, String playbackId) {
    Playback playback = new Playback(playbackId, channelId);
    activePlaybacks.put(channelId, playback);
    log.info("Playback: [{}, {}] fue almacenado para manejar estado", playback.getId(), playback.getChannelId());
  }


  public Playback advanceToNextNodeFromFinishedPlayback(String playbackId) {
    // buscamos playback por ID recorriendo el map
    log.error("llego a 1");
    Playback playback = activePlaybacks.values().stream()
        .filter(p -> p.getId().equals(playbackId))
        .findFirst()
        .orElseThrow(() -> new MishandledStateException("Playback con id " + playbackId + " no encontrado"));
    log.error("llego a 2");
    String channelId = playback.getChannelId();
    String nextNodeId = getNextNodeFor(channelId);
    log.error("llego a 3");
    if (nextNodeId == null) {
      throw new MishandledStateException("No hay siguiente nodo cacheado");
    }
    log.error("llego a 4");
    applyNextPositionInCache(channelId, nextNodeId);

    log.info("PlaybackActivo con id: {} se finalizo, se continua con el ivr en {}", playback.getId(), nextNodeId);

    activePlaybacks.remove(channelId); // aseguramos eliminarlo
    return playback;
  }

  private String getNextNodeFor(String channelId) {
    log.info("recupero siguiente:{}", channelId);
    return redisService.get("siguiente:" + channelId);
  }

  private void applyNextPositionInCache(String channelId, String nextNodeId) {
    redisService.set("posicion:" + channelId, nextNodeId, TTL);
    log.info("Se setea la posicion de {} en {}", channelId, nextNodeId);
  }

  public boolean hasActivePlayback(String channelId) {
    return activePlaybacks.containsKey(channelId);
  }

  public Playback stopPlaybackFor(String channelId) {
    Playback playback = activePlaybacks.get(channelId);
    if (playback != null) {
      ariConnector.stopPlayback(playback.getId());
      return playback;
    }
    log.error("No se encontró playback activo para el canal: {}", channelId);
    return null;
  }
}
