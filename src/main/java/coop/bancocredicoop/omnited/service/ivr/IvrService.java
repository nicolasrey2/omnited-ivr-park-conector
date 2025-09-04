package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaStore;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IvrService {
  private static final Logger log = LoggerFactory.getLogger(IvrService.class);
  private final DiagramaProcessor diagramaProcessor;
  private final DiagramaStore diagramaStore;
  private final RedisService redisService;

  public IvrService(DiagramaProcessor diagramaProcessor, DiagramaStore diagramaStore,
                    RedisService redisService) {
    this.diagramaStore = diagramaStore;
    this.diagramaProcessor = diagramaProcessor;
    this.redisService = redisService;
  }

  public void startFlow(Channel channel) {
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, "");
  }

  public void handleDtmf(Channel channel, String digit) {
    String channelId = channel.getId();

    JsonNode diagram = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagram, channelId, digit);
  }

  public void playbackFinished(String playbackId) {
    String channelId = redisService.get("playback:" + playbackId);

    JsonNode diagram = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagram, channelId, "playbackFinished");
  }

}
