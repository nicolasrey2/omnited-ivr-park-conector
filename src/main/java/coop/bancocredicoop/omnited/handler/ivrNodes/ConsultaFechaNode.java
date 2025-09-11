package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component("consultaFecha")
public class ConsultaFechaNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(ConsultaFechaNode.class);
  public static final DateTimeFormatter FECHA_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {

    JsonNode data = nodo.get("data");
    LocalDateTime fechaInicial = LocalDateTime.parse(data.get("diaInicial").asText(), FECHA_FORMATTER);
    LocalDateTime fechaFinal = LocalDateTime.parse(data.get("diaFinal").asText(), FECHA_FORMATTER);
    LocalDateTime fechaActual = LocalDateTime.now();

    log.debug("[{}] ConsultaFechaNode: inicio={}, fin={}, actual={}", channelId, fechaInicial, fechaFinal, fechaActual);

    if (TimeUtils.isBetween(fechaActual, fechaInicial, fechaFinal)) {
      log.info("[{}] Fecha dentro de rango ({} - {}).", channelId, fechaInicial, fechaFinal);
      return DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "OK");
    }

    log.warn("[{}] Fecha fuera de rango ({} - {}). Actual={}", channelId, fechaInicial, fechaFinal, fechaActual);
    return DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "ERROR");

  }

}
