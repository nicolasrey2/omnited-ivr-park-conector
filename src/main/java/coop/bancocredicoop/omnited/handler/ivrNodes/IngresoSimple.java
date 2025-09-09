package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.TimeoutNodeException;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.IvrTimerService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("ingresoSimple")
public class IngresoSimple extends AbstractSalidaNode {
  private static final Logger log = LoggerFactory.getLogger(IngresoSimple.class);

  private final RedisService redisService;
  private final IvrTimerService timerService;
  private final DiagramaProcessor  diagramaProcessor;

  private static final int TTL_VAR_SEC = 300; // 5 mins

  public IngresoSimple(MessageService messageService, VariableResolver variableResolver,
                       RedisService redisService, IvrTimerService timerService,
                       @Lazy DiagramaProcessor diagramaProcessor) {
    super(messageService, variableResolver);
    this.redisService = redisService;
    this.timerService = timerService;
    this.diagramaProcessor = diagramaProcessor;
  }

  @Override
  protected void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    if("noDigit".equalsIgnoreCase(textoUsuario)) {
      log.error("No se ingreso digito");
      throw new TimeoutNodeException(
          "Se acabo el tiempo del nodo IngresoSimple para el canal: " + channelId,
          DiagramaUtils.encontrarHangup(ivr));
    }

    if("timeOut".equalsIgnoreCase(textoUsuario)) {
      throw new TimeoutNodeException(
          "Se acabo el tiempo del nodo IngresoSimple para el canal: " + channelId,
          DiagramaUtils.obtenerTarget(ivr, node));
    }
  }

  @Override
  protected String primerOutput(JsonNode node, String channelId) {
    String textoSinVars = node.get("data").get("texto").asText();
    String texto = variableResolver.resolve(textoSinVars, channelId);
    log.info("Texto: {}", texto);
    return texto;
  }

  @Override
  protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
    JsonNode data             = node.get("data");
    String variable           = data.get("ingreso").asText();

    String redisKey           = variable + ":" + channelId;
    String acumulado = redisService.getOrDefault(redisKey, "");

    String input = digit.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_VAR_SEC);
  }

  @Override
  protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
    JsonNode data             = node.get("data");
    int cantMaxDigitos        = data.get("cantMaxDigitos").asInt();
    String variable           = data.get("ingreso").asText();
    int ttlInterDigit         = data.get("ttlInterDigito").asInt();

    timerService.cancelTimer(channelId + ":ttlPrimerDigito");

    String redisKey           = variable + ":" + channelId;

    String acumulado = redisService.getOrDefault(redisKey, "");

    String input = digit.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_VAR_SEC);

    if (acumulado.length() == cantMaxDigitos) {
      timerService.cancelAllForChannel(channelId);
      return DiagramaUtils.obtenerTarget(ivr, node);
    }

    timerService.cancelTimer(channelId + ":firstDigit");

    // Resetear el interDigit timer
    setInterDigitTimer(ivr, channelId, redisKey, ttlInterDigit);

    return null;
  }

  @Override
  protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
    JsonNode data             = node.get("data");
    int cantMaxDigitos        = data.get("cantMaxDigitos").asInt();
    String variable           = data.get("ingreso").asText();
    int ttlPrimerDigito       = data.get("ttlPrimerDigito").asInt();
    int ttlInterDigit         = data.get("ttlInterDigito").asInt();

    String redisKey           = variable + ":" + channelId;

    String acumulado = redisService.getOrDefault(redisKey, "");

    if(acumulado.isEmpty()) { // si no hay acumulado seteo el timer para el primer digito
      timerService.setTimer(channelId + ":firstDigit", Duration.ofSeconds(ttlPrimerDigito), () -> {
        log.error("no se ha ingresado primer digito para la key {}", redisKey);
        timerService.cancelAllForChannel(channelId);
        diagramaProcessor.procesarMensaje(ivr, channelId, "noDigit");
      });
    }
    else if (acumulado.length() == cantMaxDigitos) { // avanzo (esto solo sucede si cantMaxDigitos es = 1)
      timerService.cancelAllForChannel(channelId);
      return DiagramaUtils.obtenerTarget(ivr, node);
    }
    else {
      setInterDigitTimer(ivr, channelId, redisKey, ttlInterDigit);
    }

    return null;
  }


  private void setInterDigitTimer(JsonNode ivr, String channelId, String redisKey, int ttlInterDigit) {
    timerService.setTimer(channelId + ":interDigit", Duration.ofSeconds(ttlInterDigit), () -> {
      String finalAcumulado = redisService.getOrDefault(redisKey, "");
      log.info("Se avanza el flujo por timeOut interDIgit con el acumulado {} para la key {}",
          finalAcumulado, redisKey);
      timerService.cancelAllForChannel(channelId);
      diagramaProcessor.procesarMensaje(ivr, channelId, "timeOut");
    });
  }

}
