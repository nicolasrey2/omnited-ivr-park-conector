package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component("respuestaCompuesta")
public class RespuestaCompuestaNode extends AbstractSalidaNode {
    private final Logger log = LoggerFactory.getLogger(RespuestaCompuestaNode.class);

    public RespuestaCompuestaNode(MessageService messageService,
                                  VariableResolver variableResolver) {
      super(messageService, variableResolver);
    }

    @Override
    protected void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {

    }

    @Override
    protected String primerOutput(JsonNode node, String channelId) {
        String textoSinVars = node.get("data").get("texto").asText();
        String texto = variableResolver.resolve(textoSinVars, channelId);
        log.info("Texto: {}", texto);
        return texto;
    }


    @Override
    protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {  }

    @Override
    protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
        // nunca deberia pasar
        log.error("Llego un DTMF dsp del playback en una RespuestaCompuestaNode");
        return null;
    }

    @Override
    protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
        return DiagramaUtils.obtenerTarget(ivr, node);
    }
}
