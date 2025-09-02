package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;

public abstract class AbstractSalidaHandler implements NodeHandler {
  private final MessageService messageService;

  public AbstractSalidaHandler(MessageService messageService) {
    this.messageService = messageService;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {

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

