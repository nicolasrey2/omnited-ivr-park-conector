package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exceptions.TimeoutNodeException;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSalidaHandler implements NodeHandler {
  private final MessageService messageService;
  private final RedisService redisService;
  private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)\\}");
  private final Logger log =  LoggerFactory.getLogger(AbstractSalidaHandler.class);

  public AbstractSalidaHandler(MessageService messageService, RedisService redisService) {
    this.messageService = messageService;
    this.redisService = redisService;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    try {
      preHandleTasks(ivr, node, channelId, textoUsuario);
    }
    catch (TimeoutNodeException e) {
      /*
      String nodeErrorHandler = DiagramaUtils.buscarEdgePorHandle(ivr, node, "error");
      if (nodeErrorHandler != null) {
        return  nodeErrorHandler;
      }
      */

      return DiagramaUtils.encontrarHangup(ivr);
    }

    if (textoUsuario.isEmpty()) { // primera iteración
      String texto = primerOutput(node, channelId);
      if (texto.isEmpty()) {
        return null;
      }
      messageService.sendMessage(channelId, texto);
      return null; // esperar evento
    }

    if (isDigit(textoUsuario) && messageService.hasActivePlayback(channelId)) {
      messageService.stopPlayback(channelId);
      logicaAnteDtmfMientrasPlayback(node, channelId, textoUsuario);
      return null; // esperar evento playbackFinished
    }

    if (textoUsuario.equalsIgnoreCase("playbackFinished")) {
      messageService.clearPlaybackCache(channelId);
      return onPlaybackFinished(ivr, node, channelId);
    }

    if (isDigit(textoUsuario)) {
      return logicaAnteDtmfDespuesDePlayback(ivr, node, channelId, textoUsuario);
    }

    return null;
  }

  public String resolveVars(String textoSinVars, String channelId) {
    Matcher matcher = VAR_PATTERN.matcher(textoSinVars);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String varName = matcher.group(1);
      String value = redisService.get(varName + ":" + channelId);

      // si no está en Redis, dejo el placeholder tal cual
      if (value == null) {
        log.error("Variable {} no encontrada en Redis para canal {}", varName, channelId);
        value = matcher.group(0);
      }
      log.info("Se reemplaza la variable: {}; en el texto: {}", varName, textoSinVars);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
    }

    matcher.appendTail(sb);
    return sb.toString();
  }

  protected abstract void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario);

  protected abstract String primerOutput(JsonNode node, String channelId);

  /**
   * Qué hacer si llega un DTMF mientras suena el playback
   */
  protected abstract void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit);

  /**
   * Qué hacer si llega un DTMF después de que terminó el playback
   */
  protected abstract String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit);

  /**
   * Qué hacer cuando finaliza un playback.
   * - Ejemplo: en SalidaSimple podés devolver el siguiente nodo
   * - Ejemplo: en DigitMenu devolvés null para esperar al usuario
   */
  protected abstract String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId);

  private boolean isDigit(String str) {
    try {
      Integer.parseInt(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}

