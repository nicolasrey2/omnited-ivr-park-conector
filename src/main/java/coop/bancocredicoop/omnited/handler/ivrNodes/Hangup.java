package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;


@Component("hangup")
public class Hangup implements NodeHandler {
  private RedisService redisService;
  private AriConnector aricConnector;

  public Hangup(RedisService redisService, AriConnector aricConnector) {
    this.redisService = redisService;
    this.aricConnector = aricConnector;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String from, String textoUsuario) {
    // Borrar la posición y todo el hash de la sesión
    redisService.delete("posicion:" + from);
    redisService.deleteHash(from);

    aricConnector.hangupChannel(from);

    return null;
  }
}
