package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("salidaSimple")
public class SalidaSimpleHandler implements NodeHandler {
    private final Logger log = LoggerFactory.getLogger(SalidaSimpleHandler.class);
    private RedisService redisService;
    private final CanalMensajeria canalMensajeria;
    private final int TTL = 300;

    public SalidaSimpleHandler(
            CanalMensajeria canalMensajeria,
            RedisService redisService
    ) {
        this.canalMensajeria = canalMensajeria;
        this.redisService = redisService;
    }

    @Override
    public String handle(JsonNode botLimpio, JsonNode nodo, String from, String textoUsuario) {
        String texto = nodo.get("data").get("text").asText();

        String siguienteId = DiagramaUtils.obtenerTarget(botLimpio, nodo);
        log.info("Texto: {}", texto);
        log.info("siguienteId: {}", siguienteId);
        redisService.set("siguiente:" + from, siguienteId, 300);


        canalMensajeria.enviarMensaje(from, texto);

        return null; // esperar PlaybackFinished
    }
}
