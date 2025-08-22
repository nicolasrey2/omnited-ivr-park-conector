package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import org.springframework.stereotype.Component;

@Component("salidaSimple")
public class SalidaSimpleHandler implements NodeHandler {

    private final CanalMensajeria canalMensajeria;

    public SalidaSimpleHandler(
            CanalMensajeria canalMensajeria
    ) {
        this.canalMensajeria = canalMensajeria;
    }

    @Override
    public String handle(JsonNode botLimpio, JsonNode nodo, String from, String textoUsuario) {
        // 1) Lógica original: enviar mensaje
        String texto = nodo.get("data").get("text").asText();
        canalMensajeria.enviarMensaje(from, texto);

        // 2) Devolver el siguiente ID
        return DiagramaUtils.obtenerTarget(botLimpio, nodo);
    }
}
