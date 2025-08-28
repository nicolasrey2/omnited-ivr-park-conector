package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.MishandledStateException;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PlaybackStateManager {
  private final Logger log = LoggerFactory.getLogger(PlaybackStateManager.class);
  private final Integer TTL = 300;

  private final RedisService redisService;

  public PlaybackStateManager(RedisService redisService) {
    this.redisService = redisService;
  }

  public void storeNextNode(JsonNode ivr, JsonNode actualNode, String channelId) {
    String siguienteId = DiagramaUtils.obtenerTarget(ivr, actualNode);
    redisService.set("siguiente:" + channelId, siguienteId, TTL);
    log.info("El siguiente nodo para el channelId: {} es el nodo con id: {}", channelId, siguienteId);
  }

  public void storePlayback(String playbackId, String channelId) {
    redisService.set("playback:" + playbackId, channelId, TTL);
    log.info("Playback con ID: {} y channelId: {} fue almacenado en redis para manejar estado", playbackId, channelId);
  }


  public String advanceToNextNodeFromFinishedPlayback(String playbackFinalizated) {
    String channelId = redisService.get("playback:" + playbackFinalizated);

    if (channelId == null) {
      log.error("Error no se encontro canal para playback con id: {}", playbackFinalizated);
      throw new MishandledStateException("Error no se encontro canal para playback con id: " + playbackFinalizated);
    }

    redisService.delete("playback:" + playbackFinalizated);


    log.info("recupero siguiente:{}", channelId);
    String nextNodeId = redisService.get("siguiente:" + channelId);
    if (nextNodeId == null) {
      log.error("No hay siguiente nodo cacheado");
      throw new MishandledStateException("No hay siguiente nodo cacheado");
    }
    redisService.delete("siguiente:" + channelId);
    redisService.set("posicion:" + channelId, nextNodeId, TTL);

    log.info("Se setea la posicion de {} en {}", channelId, nextNodeId);
    log.info("Playback con id: {} se finalizo, se continua con el ivr", nextNodeId);

    return channelId;
  }

}
