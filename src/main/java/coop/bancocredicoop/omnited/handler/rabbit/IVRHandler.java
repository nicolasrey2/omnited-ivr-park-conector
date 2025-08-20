package coop.bancocredicoop.omnited.handler.rabbit;

import coop.bancocredicoop.omnited.entity.IVR;
import coop.bancocredicoop.omnited.service.ivr.DiagramaService;
import coop.bancocredicoop.omnited.service.rabbit.RabbitMessageHandler;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.utils.JsonCleaningService;

// nombre dudoso
public class IVRHandler implements RabbitMessageHandler {
  private static final Logger LOGGER = Logger.getLogger(IVRHandler.class.getName());
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonCleaningService jsonCleaningService;
  private final DiagramaService diagramaService;

  public IVRHandler(JsonCleaningService jsonCleaningService,
                    DiagramaService diagramaService) {
    this.jsonCleaningService = jsonCleaningService;
    this.diagramaService = diagramaService;
  }

  @Override
  public void handle(String idMensaje, String rawIVR, long fechaEnvioLocal) throws Exception {
    JsonNode ivrLimpio = this.parsearIvr(rawIVR);
    diagramaService.saveDiagram(ivrLimpio);
  }

  private JsonNode parsearIvr(String rawIVR) {
    try {
      IVR ivr = objectMapper.readValue(rawIVR, IVR.class);
      return jsonCleaningService.clean(ivr.getIVRPayload());
    }
    catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error parsing IVR JSON");
      return null;
    }
  }

}
