package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.TimeoutNodeException;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.IvrTimerService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaProcessor;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.redis.RetryService;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.time.Duration;


@Component("opcionMenu")
public class OptionsMenuNode extends AbstractSalidaNode {
  private static final Logger log = LoggerFactory.getLogger(OptionsMenuNode.class);

  private static final String CHANNEL_SELECTED_HANDLER = "channelSelectedHandler:";


  private final IvrTimerService timerService;
  private final RedisService redisService;
  private final RetryService retryService;
  private final DiagramaProcessor diagramaProcessor;
  private final int TTS_RETRIES = 80;


  public OptionsMenuNode(MessageService messageService, RedisService redisService,
                         @Lazy DiagramaProcessor diagramaProcessor, VariableResolver variableResolver,
                         RetryService retryService, IvrTimerService ivrTimerService) {
    super(messageService, variableResolver);
    this.redisService = redisService;
    this.diagramaProcessor = diagramaProcessor;
    this.retryService = retryService;
    this.timerService = ivrTimerService;
  }

  @Override
  protected void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    if("timeOut".equalsIgnoreCase(textoUsuario)) {
      throw new TimeoutNodeException("Se acabo el tiempo del nodo salidaDigitMenu");
    }
    int ttlTotal = node.get("data").get("ttlTotal").asInt();

    timerService.setTimer(channelId + ":total", Duration.ofSeconds(ttlTotal), () -> {
      log.info("Timeout total en salidaDigitMenu {}", channelId);
      diagramaProcessor.procesarMensaje(ivr, channelId, "timeOut");
    });
  }

  @Override
  protected String primerOutput(JsonNode nodo, String channelId) {
    String textoSinVars = nodo.get("data").get("text").asText();
    String texto = variableResolver.resolve(textoSinVars, channelId);
    log.info("Texto: {}", texto);
    return texto;
  }

  @Override
  protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
    String safeDigit = digit == null ? "" : digit.trim();
    String selectedHandler = findFetchHandler(node, safeDigit, channelId);
    log.info("Se escribe el handler: {}", selectedHandler);
    redisService.set(CHANNEL_SELECTED_HANDLER + channelId, selectedHandler);
  }

  @Override
  protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
    timerService.cancelTimer(channelId + ":total");
    String safeDigit = digit == null ? "" : digit.trim();
    String selectedHandler = findFetchHandler(node, safeDigit, channelId);
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }


  @Override
  protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
    String selectedHandler = redisService.get(CHANNEL_SELECTED_HANDLER + channelId);
    redisService.delete(CHANNEL_SELECTED_HANDLER + channelId);
    log.info("se recupera y borra de la cache el handler: {} en evento onPlaybackfinished", selectedHandler);

    if (selectedHandler == null) {
      return null;
    }

    timerService.cancelTimer(channelId + ":total");

    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }

  private String findFetchHandler(JsonNode nodo, String textoUsuario, String channelId) {
    JsonNode options = nodo.get("optionsList");
    if (options == null || !options.isArray()) {
      log.error("Las opciones son nulas o estan vacias");
      return null;
    }

    for (JsonNode option : options) {
      if (option.get("id").asText().equals(textoUsuario)) {
        retryService.clearRetries(channelId);
        return option.get("text").asText();
      }
    }

    log.info("no se encontró opción para input = {}, retornando 'error' como handler", textoUsuario);
    retryService.handleRetries(channelId, TTS_RETRIES);
    return "error";
  }


}
