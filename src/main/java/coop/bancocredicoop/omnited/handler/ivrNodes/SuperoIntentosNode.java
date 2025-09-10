package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("superoIntentos")
public class SuperoIntentosNode implements NodeHandler {
  private final Logger log = LoggerFactory.getLogger(SuperoIntentosNode.class);
  private final RedisService redisService;

  public SuperoIntentosNode(RedisService redisService) {
    this.redisService = redisService;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String channelId, String textoUsuario) {
    int cantidadReintentosMax = nodo.get("data").get("cantReintentosMax").asInt();

    int cantidadReintentosActual = getCantidadReintentosActual(channelId);

    log.info("la cantidad de reintentos del canal {} actualmente es: {} y la maxima es: {}",
        channelId, cantidadReintentosActual, cantidadReintentosMax);

    if (cantidadReintentosActual < cantidadReintentosMax) {
      log.info("aun no supero la cantidad de reintentos el canal: {}", channelId);
      return DiagramaUtils.buscarEdgePorHandle(ivrLimpio, nodo, "noSupero");
    }

    log.info("el canal: {} supero la cantidad de reintentos", channelId);
    return DiagramaUtils.buscarEdgePorHandle(ivrLimpio, nodo, "siSupero");
  }

  private int getCantidadReintentosActual(String channelId) {
    String cantidadString = redisService.get("cantidadReintentos:" + channelId);
    if(cantidadString != null) {
      return Integer.parseInt(cantidadString);
    }

    log.error("la cantidad de reintentos del canal {} es null", channelId);
    throw new RuntimeException("cantidadReintentos:" + channelId);
  }
}
