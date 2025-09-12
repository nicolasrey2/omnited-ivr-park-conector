package coop.bancocredicoop.omnited.handler.ivrNode;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.IvrTimerService;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("tiempoEspera")
public class TiempoEsperaNode implements NodeHandler {
  private final static Logger log = LoggerFactory.getLogger(TiempoEsperaNode.class);

  private final IvrTimerService timerService;
  private final DiagramaProcessor diagramaProcessor;

  public TiempoEsperaNode(IvrTimerService timerService, @Lazy DiagramaProcessor diagramaProcessor) {
    this.timerService = timerService;
    this.diagramaProcessor = diagramaProcessor;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {
    if("timeOutTiempoEsperaNode".equalsIgnoreCase(textoUsuario)) {
      log.info("[{}] - Se avanza el flujo en tiempoEspera", channelId);
      return DiagramaUtils.obtenerTarget(ivr, nodo);
    }
    String timerKey = channelId + ":tiempoEsperaNode";
    if (timerService.hasTimer(timerKey)) { // el timer esta corriendo
      log.info("[{}] - Se recibio un evento, pero aun se esta en la espera de tiempoEspera", channelId);
      return null;
    }
    JsonNode data   = nodo.get("data");
    int ttlTotal    = data.get("ttlTotal").asInt();

    timerService.setTimer(timerKey, Duration.ofSeconds(ttlTotal), () -> {
      log.info("[{}] - Timeout finalizado en tiempoEspera", channelId);
      diagramaProcessor.procesarMensaje(ivr, channelId, "timeOutTiempoEsperaNode");
    });
    log.info("[{}] - Se seteo un timer en tiempoEspera con ttl: {}", channelId, ttlTotal);
    return null;
  }
}
