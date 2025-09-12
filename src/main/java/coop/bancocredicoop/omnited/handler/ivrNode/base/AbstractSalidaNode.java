package coop.bancocredicoop.omnited.handler.ivrNode.base;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.exception.TimeoutNodeException;
import coop.bancocredicoop.omnited.message.MessageService;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractSalidaNode implements NodeHandler {
  private final Logger log =  LoggerFactory.getLogger(AbstractSalidaNode.class);

  private final MessageService messageService;
  protected final VariableResolver variableResolver;

  public AbstractSalidaNode(MessageService messageService, VariableResolver variableResolver) {
    this.messageService = messageService;
    this.variableResolver  = variableResolver;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    try {
      preHandleTasks(ivr, node, channelId, textoUsuario);
    }
    catch (TimeoutNodeException timeoutNodeException) {
      log.info(timeoutNodeException.getMessage());
      return timeoutNodeException.getTargetNode();
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

