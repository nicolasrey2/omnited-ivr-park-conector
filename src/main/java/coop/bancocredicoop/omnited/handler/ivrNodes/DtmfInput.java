package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Component("dtmfInput")
public class DtmfInput implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DtmfInput.class);
  private final RedisService redisService;
  private final DiagramaProcessor diagramaProcessor;
  private static final long TTL_VAR_SEC = 300; // 5 mins

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  private final Map<String, ScheduledFuture<?>> interDigitTimers = new ConcurrentHashMap<>();
  private final Map<String, ScheduledFuture<?>> totalTimers = new ConcurrentHashMap<>();

  public DtmfInput(RedisService redisService, @Lazy DiagramaProcessor diagramaProcessor) {
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
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
      setTimerTotal(ttlTotal, ivrLimpio, channelId);
    }
    String input = textoUsuario.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_VAR_SEC);

    // cancelamos el timer anterior si existía
    Optional.ofNullable(interDigitTimers.remove(channelId))
        .ifPresent(f -> f.cancel(false));

    // programamos un nuevo timeout de 2 seg
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      String finalAcumulado = redisService.getOrDefault(redisKey, "");
      if (finalAcumulado.length() >= cantidadMinEsperada) {
        // avanzamos con el flujo
        log.info("Se avanza el flujo con el acumulado {} para la key {}", finalAcumulado, redisKey);
        cancelAllTimers(channelId);
        log.info("Se limpian los timers");
        diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "finalizo");
      } else {
        log.error("No se pudo procesar la cantidad de datos correcta");
      }
    }, ttlInterDigit, TimeUnit.SECONDS);
    interDigitTimers.put(channelId, future);

    return null;
  }

  private void setTimerTotal(int ttlTotal, JsonNode ivrLimpio, String channelId) {
    Optional.ofNullable(totalTimers.remove(channelId))
        .ifPresent(f -> f.cancel(false));

    ScheduledFuture<?> futureTotal = scheduler.schedule(() -> {
      log.info("Se acabo el tiempo total en DtmfInput para: {}", channelId);
      cancelAllTimers(channelId);
      log.info("Se limpian los timers");
      diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "timeOut");
    }, ttlTotal, TimeUnit.SECONDS);

    log.info("Se setea timer total para: {}, con valor: {}", channelId, ttlTotal);

    totalTimers.put(channelId, futureTotal);
  }

  private void cancelAllTimers(String channelId) {
    Optional.ofNullable(interDigitTimers.remove(channelId))
        .ifPresent(f -> f.cancel(false));
    Optional.ofNullable(totalTimers.remove(channelId))
        .ifPresent(f -> f.cancel(false));
    log.info("Timers cancelados para canal {}", channelId);
  }

  private void recoverAccumulatedDuringSimpleExitIfExists(String channelId, String redisKey) {
    String digit = redisService.get("dtmfAcc:" + channelId);
    if (digit != null) {
      redisService.set(redisKey, digit,  TTL_VAR_SEC);
      redisService.delete("dtmfAcc:" + channelId);
    }
  }

}
