package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
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
    log.info("Mensaje obtenido del DNI: {}", mensaje);

    redisService.set("siguiente:" + from, siguienteId, 300);
    log.info("Seteo la key del siguiente nodo: {}", siguienteId);

    canalMensajeria.enviarMensaje(from, mensaje);

    return null;
  }
}
