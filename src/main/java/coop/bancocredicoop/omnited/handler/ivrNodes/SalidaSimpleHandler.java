package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.PlaybackStateManager;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("salidaSimple")
public class SalidaSimpleHandler implements NodeHandler {
    private final Logger log = LoggerFactory.getLogger(SalidaSimpleHandler.class);
    private final CanalMensajeria canalMensajeria;
    private final PlaybackStateManager playbackStateManager;

    public SalidaSimpleHandler(
            CanalMensajeria canalMensajeria,
            PlaybackStateManager playbackStateManager
    ) {
        this.canalMensajeria = canalMensajeria;
        this.playbackStateManager =  playbackStateManager;
    }

    @Override
    public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
        String texto = node.get("data").get("text").asText();
        log.info("Texto: {}", texto);

        playbackStateManager.storeNextNode(ivr, node, channelId);

        canalMensajeria.enviarMensaje(channelId, texto);

        return null; // esperar PlaybackFinished
    }
}
