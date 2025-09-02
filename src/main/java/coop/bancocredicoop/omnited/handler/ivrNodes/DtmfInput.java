package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.*;

@Component("dtmfInput")
public class DtmfInput implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DtmfInput.class);
  private final RedisService redisService;
  private final DiagramaProcessor diagramaProcessor;
  private static final long TTL_SEC = 300; // 5 mins

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Map<String, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();


  public DtmfInput(RedisService redisService, @Lazy DiagramaProcessor diagramaProcessor) {
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String channelId, String textoUsuario) {
    if("finalizo".equalsIgnoreCase(textoUsuario)) {
      return DiagramaUtils.obtenerTarget(ivrLimpio, nodo);
    }
    JsonNode data             = nodo.get("data");
    int cantidadMinEsperada   = data.get("cantidadMinima").asInt();
    String variable           = data.get("variable").asText();
    String redisKey           = variable + ":" + channelId;

    recoverAccumulatedDuringSimpleExitIfExists(channelId, redisKey);
    String acumulado = redisService.getOrDefault(redisKey, "");
    String input = textoUsuario.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_SEC);

    // cancelamos el timer anterior si existía
    ScheduledFuture<?> previous = timeoutTasks.get(channelId);
    if (previous != null) previous.cancel(false);
    // programamos un nuevo timeout de 2 seg
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      String finalAcumulado = redisService.getOrDefault(redisKey, "");
      if (finalAcumulado.length() >= cantidadMinEsperada) {
        // avanzamos con el flujo
        log.info("Se avanza el flujo con el acumulado {} para la key {}", finalAcumulado, redisKey);
        diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "finalizo");
        timeoutTasks.remove(channelId);
      } else {
        log.error("No se pudo procesar la cantidad de datos correcta");
      }
    }, 2, TimeUnit.SECONDS);
    timeoutTasks.put(channelId, future);

    return null;
  }

  private void recoverAccumulatedDuringSimpleExitIfExists(String channelId, String redisKey) {
    String digit = redisService.get("dtmfAcc:" + channelId);
    if (digit != null) {
      redisService.set(redisKey, digit,  TTL_SEC);
      redisService.delete("dtmfAcc:" + channelId);
    }
  }

}
