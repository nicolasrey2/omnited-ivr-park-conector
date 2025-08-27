package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("dtmfInput")
public class DtmfInput implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DtmfInput.class);
  private RedisService redisService;
  private static final long TTL_SEC = 30; // 30 segs

  public DtmfInput(RedisService redisService) {
    this.redisService = redisService;
  }

  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String from, String textoUsuario) {
    int cantidadEsperada = nodo.get("data").get("cantidad").asInt();

    String redisKey = "IVR:" + from + ":dtmfAcumulado";
    String acumulado = redisService.getOrDefault(redisKey, "");

    String input = textoUsuario.trim();
    acumulado += input;
    redisService.set(redisKey, acumulado,  TTL_SEC);

    if(acumulado.length() != cantidadEsperada) {
      return null;
    }

    return DiagramaUtils.obtenerTarget(ivrLimpio, nodo);
  }

  Integer castAsInteger(String valor) {
    Integer valorAsInteger = null;
    try {
      valorAsInteger = Integer.parseInt(valor);
    }
    catch (NumberFormatException e) {
      log.error("Error convertir valor integer: " + valor);
    }
    return valorAsInteger;
  }

}
