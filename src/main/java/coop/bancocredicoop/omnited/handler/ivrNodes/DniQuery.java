package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.dni.DniQueryClient;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component("dniQuery")
public class DniQuery implements NodeHandler {
  private RedisService redisService;
  private CanalMensajeria canalMensajeria;
  private DniQueryClient dniQueryClient;


  public DniQuery(RedisService redisService,  CanalMensajeria canalMensajeria,  DniQueryClient dniQueryClient) {
    this.redisService = redisService;
    this.canalMensajeria = canalMensajeria;
    this.dniQueryClient = dniQueryClient;
  }


  @Override
  public String handle(JsonNode ivrLimpio, JsonNode nodo, String from, String textoUsuario) {
    String redisKey = "IVR:" + from + ":dtmfAcumulado";
    String dniAcumulado = redisService.get(redisKey);
    String fullName = dniQueryClient.getFullName(dniAcumulado);
    String mensaje;
    if (fullName == null) {
      mensaje = "No se ha encontrado un nombre y apellido para el dni " + dniAcumulado;
    }
    else {
      mensaje = "Su nombre completo es " + fullName.toLowerCase(Locale.ROOT);
    }


    String siguienteId = DiagramaUtils.obtenerTarget(ivrLimpio, nodo);
    System.out.println("Texto: " + mensaje);
    System.out.println("siguienteId: " + siguienteId);
    redisService.set("siguiente:" + from, siguienteId, 300);

    canalMensajeria.enviarMensaje(from, mensaje);

    return null;
  }
}
