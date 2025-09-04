package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("digitMenu")
public class SalidaDigitMenu extends AbstractSalidaHandler {
  private static final Logger log = LoggerFactory.getLogger(SalidaDigitMenu.class);
  private final RedisService redisService;

  public SalidaDigitMenu(MessageService messageService, RedisService redisService) {
    super(messageService, redisService);
    this.redisService = redisService;
  }

  @Override
  protected String primerOutput(JsonNode nodo, String channelId) {
    String texto = nodo.get("data").get("text").asText();
    log.info("Texto: {}", texto);
    return texto;
  }

  @Override
  protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
    String selectedHandler = findFetchHandler(node, digit);
    if (selectedHandler == null) {
      log.error("Seleccion de usuario incorrecta: {}", digit);
      return;
    }
    log.error("Se escribe el handler: {}", selectedHandler);
    redisService.set("channelSelectedHandler:" + channelId, selectedHandler);
  }

  @Override
  protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
    String selectedHandler = findFetchHandler(node, digit);
    if (selectedHandler == null) {
      log.error("Seleccion de usuario incorrecta: {}", digit);
      return null;
    }
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }


  @Override
  protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
    String selectedHandler = redisService.get("channelSelectedHandler:" + channelId);
    log.info("se recupera {} en evento onPlaybackfinished", selectedHandler);
    if (selectedHandler == null) {
      return null;
    }
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, selectedHandler);
  }

  private String findFetchHandler(JsonNode nodo, String textoUsuario) {
    JsonNode options = nodo.get("optionsList");

    if (options == null || !options.isArray()) {
      log.error("Las opciones son nulas o estan vacias");
      return null;
    }

    for (JsonNode option : options) {
      if (option.get("id").asText().equals(textoUsuario)) {
        return option.get("text").asText();
      }
    }

    log.error("no se encontró opción para input={}", textoUsuario);
    return null;
  }
}
