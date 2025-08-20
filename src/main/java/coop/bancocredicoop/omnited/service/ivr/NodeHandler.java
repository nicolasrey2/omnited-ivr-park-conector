package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;

public interface NodeHandler {
    // String getType(); deprecate

    /**
     * Procesa el nodo invocado.
     *
     * @param ivrLimpio     El JSON completo del bot.
     * @param nodo          El nodo actual.
     * @param from          El identificador de usuario.
     * @param textoUsuario  El texto que acaba de enviar el usuario.
     */
    String handle(JsonNode ivrLimpio, JsonNode nodo, String from, String textoUsuario);
}