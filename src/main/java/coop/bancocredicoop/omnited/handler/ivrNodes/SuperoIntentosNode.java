package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;

@Component("superoIntentos")
public class SuperoIntentosNode implements NodeHandler {
  private final RedisService redisService;

  public SuperoIntentosNode(RedisService redisService) {
    this.redisService = redisService;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String channelId, String textoUsuario) {
    int cantidadReintentosMax = nodo.get("data").get("cantidadReintentos").asInt();
    int ttl = nodo.get("data").get("ttl").asInt();

    int cantidadReintentosActual = getCantidadReintentosActual(channelId);
    if (cantidadReintentosActual < cantidadReintentosMax) {
      redisService.set("cantidadReintentos:" + channelId,
          String.valueOf(cantidadReintentosActual + 1), ttl);
      return DiagramaUtils.buscarEdgePorHandle(ivrLimpio, nodo, "noSupero");
    }
    redisService.delete("cantidadReintentos:" + channelId);
    return DiagramaUtils.buscarEdgePorHandle(ivrLimpio, nodo, "siSupero");
  }

  private int getCantidadReintentosActual(String channelId) {
    String cantidadString = redisService.get("cantidadReintentos:" + channelId);
    if(cantidadString != null) {
      return Integer.parseInt(cantidadString);
    }
    return 1;
  }
}
