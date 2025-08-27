package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.dni.DniQueryClient;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.PlaybackStateManager;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Component("dniQuery")
public class DniQuery implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DniQuery.class);
  private RedisService redisService;
  private CanalMensajeria canalMensajeria;
  private DniQueryClient dniQueryClient;
  private PlaybackStateManager playbackStateManager;


  public DniQuery(RedisService redisService,  CanalMensajeria canalMensajeria,
                  DniQueryClient dniQueryClient, PlaybackStateManager playbackStateManager) {
    this.redisService = redisService;
    this.canalMensajeria = canalMensajeria;
    this.dniQueryClient = dniQueryClient;
    this.playbackStateManager = playbackStateManager;
  }


  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {
    String redisKey = "IVR:" + channelId + ":dtmfAcumulado";
    String dniAcumulado = redisService.get(redisKey);
    String fullName = dniQueryClient.getFullName(dniAcumulado);
    String mensaje;
    if (fullName == null) {
      mensaje = "No se ha encontrado un nombre y apellido para el dni " + dniAcumulado;
    }
    else {
      mensaje = "Su nombre completo es " + fullName.toLowerCase(Locale.ROOT);
    }
    log.info("Mensaje obtenido del DNI: {}", mensaje);

    playbackStateManager.storeNextNode(ivr, nodo, channelId);

    canalMensajeria.enviarMensaje(channelId, mensaje);

    return null;
  }
}
