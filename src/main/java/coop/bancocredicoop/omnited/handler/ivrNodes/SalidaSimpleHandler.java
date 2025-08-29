package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("salidaSimple")
public class SalidaSimpleHandler extends AbstractSalidaHandler {
    private final Logger log = LoggerFactory.getLogger(SalidaSimpleHandler.class);

    public SalidaSimpleHandler(MessageService messageService) {
      super(messageService);
    }

    @Override
    protected String primerOutput(JsonNode node) {
        String texto = node.get("data").get("text").asText();
        log.info("Texto: {}", texto);
        return texto;
    }

    @Override
    protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
    }

    @Override
    protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
        return null;
    }

    @Override
    protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
        return DiagramaUtils.obtenerTarget(ivr, node);
    }
}
