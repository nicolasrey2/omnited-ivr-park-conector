package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.TimeoutNodeException;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Component("opcionMenu")
public class SalidaDigitMenu extends AbstractSalidaHandler {
  private static final Logger log = LoggerFactory.getLogger(SalidaDigitMenu.class);
  private final RedisService redisService;
  private final int TTS_RETRIES = 80;

  private final DiagramaProcessor diagramaProcessor;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  private final Map<String, ScheduledFuture<?>> totalTimers = new ConcurrentHashMap<>();

  public SalidaDigitMenu(MessageService messageService, RedisService redisService,
                         @Lazy DiagramaProcessor diagramaProcessor) {
    super(messageService, redisService);
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
  }

  @Override
  protected void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    if("timeOut".equalsIgnoreCase(textoUsuario)) {
      throw new TimeoutNodeException("Se acabo el tiempo del nodo salidaDigitMenu");
    }
    int ttlTotal = node.get("data").get("ttlTotal").asInt();
    setTimerTotal(ttlTotal, ivr, channelId);
  }

  @Override
  protected String primerOutput(JsonNode nodo, String channelId) {
    String textoSinVars = nodo.get("data").get("text").asText();
    String texto = resolveVars(textoSinVars, channelId);
    log.info("Texto: {}", texto);
    return texto;
  }

  @Override
  protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
    String selectedHandler = findFetchHandler(node, digit, channelId);
    log.info("Se escribe el handler: {}", selectedHandler);
    redisService.set("channelSelectedHandler:" + channelId, selectedHandler);
  }

  @Override
  protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
    removeTimerTotal(channelId);
    String selectedHandler = findFetchHandler(node, digit, channelId);
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }


  @Override
  protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
    String selectedHandler = redisService.get("channelSelectedHandler:" + channelId);
    redisService.delete("channelSelectedHandler:" + channelId);
    log.info("se recupera y borra de la cache el handler: {} en evento onPlaybackfinished", selectedHandler);
    if (selectedHandler == null) {
      return null;
    }
    removeTimerTotal(channelId);
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }

  private void removeTimerTotal(String channelId) {
    Optional.ofNullable(totalTimers.remove(channelId))
        .ifPresent(f -> f.cancel(false));
  }

  private void setTimerTotal(int ttlTotal, JsonNode ivrLimpio, String channelId) {
    ScheduledFuture<?> previousTotal = totalTimers.get(channelId);
    if (previousTotal != null) previousTotal.cancel(false);

    ScheduledFuture<?> futureTotal = scheduler.schedule(() -> {
      log.info("Se acabo el tiempo total en salidaDigitMenu para: {}", channelId);
      diagramaProcessor.procesarMensaje(ivrLimpio, channelId, "timeOut");
      totalTimers.remove(channelId);
    }, ttlTotal, TimeUnit.SECONDS);

    log.info("Se setea timer total para: {}, con valor: {}", channelId, ttlTotal);

    totalTimers.put(channelId, futureTotal);
  }

  private String findFetchHandler(JsonNode nodo, String textoUsuario, String channelId) {
    JsonNode options = nodo.get("optionsList");

    if (options == null || !options.isArray()) {
      log.error("Las opciones son nulas o estan vacias");
      return null;
    }

    for (JsonNode option : options) {
      if (option.get("id").asText().equals(textoUsuario)) {
        clearRetries(channelId);
        return option.get("text").asText();
      }
    }

    log.info("no se encontró opción para input = {}, retornando 'error' como handler", textoUsuario);
    handleRetries(channelId);
    return "error";
  }

  private void handleRetries(String channelId) {
    String cantActualStr = redisService.get("cantidadReintentos:" + channelId);
    if (cantActualStr == null) {
      cantActualStr = "0";
    }
    int cantActual = Integer.parseInt(cantActualStr) + 1 ;

    redisService.set("cantidadReintentos:" + channelId, String.valueOf(cantActual), TTS_RETRIES);
    log.info("Se seteo cantidadReintentos:{} en {}", channelId, cantActual);
  }

  private void clearRetries(String channelId) {
    redisService.delete("cantidadReintentos:" + channelId);
  }


}
