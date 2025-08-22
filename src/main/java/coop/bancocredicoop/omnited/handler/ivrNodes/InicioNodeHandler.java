package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import org.springframework.stereotype.Component;

/**
 * Handler para nodos de tipo "inicio". Inicia la sesión y avanza
 * inmediatamente al siguiente nodo.
 */
@Component("inicio")
public class InicioNodeHandler implements NodeHandler {
  /**
   * Imprime un log de inicio y devuelve el ID del siguiente nodo,
   * que será procesado por DiagramaMapper.
   *
   * @param botLimpio    JSON completo del diagrama
   * @param nodoIn       Nodo actual de tipo "inInicio"
   * @param from         Identificador del usuario
   * @param textoUsuario Texto ingresado por el usuario (no aplica aquí)
   * @return ID del siguiente nodo, o null si no hay target
   */
  @Override
  public String handle(JsonNode botLimpio, JsonNode nodoIn, String from, String textoUsuario) {
    // Log de inicio (puedes usar canalMensajeria si prefieres enviar un mensaje)
    System.out.println("INICIA BOT-IVR para canal=" + from);

    // Devuelve el nodo al que seguir
    return DiagramaUtils.obtenerTarget(botLimpio, nodoIn);
  }
}
