package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.IvrTimerService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;


@Component("dtmfInput")
public class DtmfInputNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DtmfInputNode.class);

  private final IvrTimerService timerService;
  private final RedisService redisService;
  private final DiagramaProcessor diagramaProcessor;
  private static final long TTL_VAR_SEC = 300; // 5 mins

  public DtmfInputNode(RedisService redisService, @Lazy DiagramaProcessor diagramaProcessor, IvrTimerService ivrTimerService) {
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
    this.timerService = ivrTimerService;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String channelId, String textoUsuario) {
    if("timeOut".equalsIgnoreCase(textoUsuario)) {
      return DiagramaUtils.encontrarHangup(ivrLimpio);
    }
    if("finalizo".equalsIgnoreCase(textoUsuario)) {
      return DiagramaUtils.obtenerTarget(ivrLimpio, nodo);
    }
    JsonNode data             = nodo.get("data");
    int cantidadMinEsperada   = data.get("cantidadMinima").asInt();
    String variable           = data.get("variable").asText();
    int ttlTotal              = data.get("ttlTotal").asInt();
    int ttlInterDigit         = data.get("ttlInterDigit").asInt();

    String redisKey           = variable + ":" + channelId;

    recoverAccumulatedDuringSimpleExitIfExists(channelId, redisKey);

    String acumulado = redisService.getOrDefault(redisKey, "");
    if(acumulado.isEmpty()) {
      timerService.setTimer(channelId + ":total", Duration.ofSeconds(ttlTotal), () -> {
        log.info("Timeout total en DtmfInput {}", channelId);
        timerService.cancelAllForChannel(channelId);
        diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "timeOut");
      });
    }

    String input = textoUsuario.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_VAR_SEC);

    // Resetear el interDigit timer
    timerService.setTimer(channelId + ":interDigit", Duration.ofSeconds(ttlInterDigit), () -> {
      String finalAcumulado = redisService.getOrDefault(redisKey, "");
      if (finalAcumulado.length() >= cantidadMinEsperada) {
        log.info("Se avanza el flujo con el acumulado {} para la key {}", finalAcumulado, redisKey);
        timerService.cancelAllForChannel(channelId);
        diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "finalizo");
      } else {
        log.warn("No se pudo procesar la cantidad de datos correcta (len={}, esperado={})",
            finalAcumulado.length(), cantidadMinEsperada);
      }
    });

    return null;
  }


  private void recoverAccumulatedDuringSimpleExitIfExists(String channelId, String redisKey) {
    String digit = redisService.get("dtmfAcc:" + channelId);
    if (digit != null) {
      redisService.set(redisKey, digit,  TTL_VAR_SEC);
      redisService.delete("dtmfAcc:" + channelId);
    }
  }

}
