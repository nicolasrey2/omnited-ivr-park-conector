package coop.bancocredicoop.omnited.handler.ivrNode;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;


@Component("hangup")
public class HangupNode implements NodeHandler {
  private final RedisService redisService;
  private final AriConnector aricConnector;

  public HangupNode(RedisService redisService, AriConnector aricConnector) {
    this.redisService = redisService;
    this.aricConnector = aricConnector;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String chaannelId, String textoUsuario) {
    //TODO analizar si se quiere cambiar esto a 'ChannelHangupRequest' o 'StasisEnd'
    redisService.deleteAllFrom(chaannelId);

    aricConnector.hangupChannel(chaannelId);

    return null;
  }
}
