package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component("consultaHorario")
public class ConsultaHorarioNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(ConsultaHorarioNode.class);
  // formato actual "HH:MM:SS"

  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {
    try {
      JsonNode data = nodo.get("data");
      LocalTime horarioInicial = LocalTime.parse(data.get("horarioInicial").asText());
      LocalTime horarioFinal = LocalTime.parse(data.get("horarioFinal").asText());
      LocalTime horaActual = LocalTime.now();

      log.debug("[{}] ConsultaHorarioNode: inicio={}, fin={}, actual={}", channelId, horarioInicial, horarioFinal, horaActual);

      if (TimeUtils.isBetween(horaActual, horarioInicial, horarioFinal)) {
        log.info("[{}] Hora dentro de rango ({} - {}).", channelId, horarioInicial, horarioFinal);
        return DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "OK");
      }

      log.warn("[{}] Hora fuera de rango ({} - {}). Actual={}", channelId, horarioInicial, horarioFinal, horaActual);
      return DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "ERROR");
    }
    catch (Exception e) {
      log.error(e.toString());
      return null;
    }
  }

}
