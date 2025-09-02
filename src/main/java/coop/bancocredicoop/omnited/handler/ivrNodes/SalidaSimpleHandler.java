package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("salidaSimple")
public class SalidaSimpleHandler extends AbstractSalidaHandler {
    private final Logger log = LoggerFactory.getLogger(SalidaSimpleHandler.class);
    private final RedisService redisService;

    public SalidaSimpleHandler(MessageService messageService, RedisService redisService) {
      super(messageService);
      this.redisService = redisService;
    }

    @Override
    protected String primerOutput(JsonNode node, String channelId) {
        String texto = node.get("data").get("text").asText();
        log.info("Texto: {}", texto);
        return texto;
    }

    @Override
    protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {
        redisService.set("dtmfAcc:" + channelId, digit, 30);
    }

    @Override
    protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
        // nunca deberia pasar
        return null;
    }

    @Override
    protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
        return DiagramaUtils.obtenerTarget(ivr, node);
    }
}
