package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.MishandledStateException;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import coop.bancocredicoop.omnited.entity.Playback;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlaybackStateManager {
  private final Logger log = LoggerFactory.getLogger(PlaybackStateManager.class);
  private final Integer TTL = 300;
  private final List<Playback> activePlaybacks;
  private final RedisService redisService;

  public PlaybackStateManager(RedisService redisService) {
    this.redisService = redisService;
    this.activePlaybacks = new ArrayList<>();
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
    this.activePlaybacks.add(playback);
    log.info("Playback: [{}, {}] fue almacenado para manejar estado", playback.getId(), playback.getChannelId());
  }


  public String advanceToNextNodeFromFinishedPlayback(String playbackFinalizated) {
    Playback playback = getPlayback(playbackFinalizated);
    if (playback == null) {
      log.error("playback con id: {} no encontrado", playbackFinalizated);
    }
    String channelId = playback.getChannelId();
    if (channelId == null) {
      log.error("Error no se encontro canal para playback con id: {}", playbackFinalizated);
      throw new MishandledStateException("Error no se encontro canal para playback con id: " + playbackFinalizated);
    }
    String nextNodeId = getNextNodeFor(channelId);
    if (nextNodeId == null) {
      log.error("No hay siguiente nodo cacheado");
      throw new MishandledStateException("No hay siguiente nodo cacheado");
    }
    applyNextPositionInCache(channelId, nextNodeId);

    log.info("PlaybackActivo con id: {} se finalizo, se continua con el ivr en {}", playback.getId(), nextNodeId);

    this.activePlaybacks.remove(playback);

    return channelId;
  }

  private Playback getPlayback(String playbackId) {
    //return redisService.get("playback:" + playbackId);
    return this.activePlaybacks.stream().filter(playback -> playback.getId().equals(playbackId))
        .findFirst().orElse(null);
  }

  private String getNextNodeFor(String channelId) {
    log.info("recupero siguiente:{}", channelId);
    return redisService.get("siguiente:" + channelId);
  }

  private void applyNextPositionInCache(String channelId, String nextNodeId) {
    redisService.set("posicion:" + channelId, nextNodeId, TTL);
    log.info("Se setea la posicion de {} en {}", channelId, nextNodeId);
  }

}
