package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitMenu implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DigitMenu.class);
  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String from, String textoUsuario) {
    if (textoUsuario == null || textoUsuario.isEmpty()) {
      // se debe esperar el input (analizar si se debe hacer algo)
      return null;
    }

    String textoABuscar = findFetchHandler(nodo, textoUsuario);

    return DiagramaUtils.buscarEdgePorHandle(ivrLimpio, nodo, textoABuscar);
  }

  private String findFetchHandler(JsonNode nodo, String textoUsuario) {
    JsonNode options = nodo.get("optionsList");

    if (options == null || !options.isArray()) {
      log.error("Las opciones son nulas o estan vacias");
      return null;
    }

    for (JsonNode option : options) {
      if(option.get("id").asText().equals(textoUsuario)) {
        return option.get("text").asText();
      }
    }
    log.error("no se encontró opción para input={}", textoUsuario);
    return null;
  }
}
