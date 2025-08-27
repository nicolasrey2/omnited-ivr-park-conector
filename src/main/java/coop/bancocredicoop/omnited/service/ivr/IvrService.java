package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IvrService {
  private static final Logger log = LoggerFactory.getLogger(IvrService.class);
  private DiagramaProcessor diagramaProcessor;
  private DiagramaStore diagramaStore;
  private RedisService redisService;

  public IvrService(DiagramaProcessor diagramaProcessor, DiagramaStore diagramaStore,
                    RedisService redisService) {
    this.diagramaStore = diagramaStore;
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
  }

  public void startFlow(Channel channel) {
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, "");
  }

  public void handleDtmf(Channel channel, String digit) {
    //TODO se deberia pausar el playback del canal y pasar al siguiente nodo (se pasa solo si es nodo de salida)
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, digit);
  }

  public void playbackFinished(String playbackId) {
    String channelId = redisService.get("playback:" + playbackId);

    if (channelId == null) {
      log.error("Error no se encontro canal para playback con id: {}", playbackId);
      return;
    }

    redisService.delete("playback:" + playbackId);
    log.info("recupero siguiente:{}", channelId);
    String siguienteId = redisService.get("siguiente:" + channelId);
    if (siguienteId == null) {
      log.error("No hay siguiente nodo cacheado");
      return;
    }
    redisService.delete("siguiente:" + channelId);
    redisService.set("posicion:" + channelId, siguienteId, 300);

    log.info("Se setea la posicion en {}", channelId);
    log.info("Playback con id: {} se finalizo, se continua con el ivr", playbackId);

    diagramaProcessor.procesarMensaje(diagramaStore.getDiagram(), channelId, "");
  }
}
