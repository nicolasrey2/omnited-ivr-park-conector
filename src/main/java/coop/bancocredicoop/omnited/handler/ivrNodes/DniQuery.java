package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.dni.DniQueryClient;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Component("dniQuery")
public class DniQuery implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(DniQuery.class);
  private final MessageService messageService;
  private RedisService redisService;
  private DniQueryClient dniQueryClient;


  public DniQuery(RedisService redisService,
                  DniQueryClient dniQueryClient, MessageService messageService) {
    this.redisService = redisService;
    this.dniQueryClient = dniQueryClient;
    this.messageService = messageService;
  }


  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {
    if(textoUsuario.isEmpty()){ // primera iteracion
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
      messageService.sendMessage(channelId, mensaje);
      return null;
    }

    // llego un dtmf (la interaccion por ahora es solo saltar el audio)
    if(isDigit(textoUsuario)){
      messageService.stopPlayback(channelId);
      return null; // esperar por evento playbackFinished
    }

    // en otro caso es el playback finished
    messageService.clearPlaybackCache(channelId);

    return DiagramaUtils.obtenerTarget(ivr, nodo);
  }

  private boolean isDigit(String str) {
    try {
      Integer.parseInt(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
