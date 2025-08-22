package coop.bancocredicoop.omnited.handler.rabbit;

import coop.bancocredicoop.omnited.entity.IVR;
import coop.bancocredicoop.omnited.service.ivr.DiagramaStore;
import coop.bancocredicoop.omnited.service.rabbit.RabbitMessageHandler;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.utils.JsonCleaningService;

public class IvrUpdaterHandler implements RabbitMessageHandler {
  private static final Logger LOGGER = Logger.getLogger(IvrUpdaterHandler.class.getName());
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonCleaningService jsonCleaningService;
  private final DiagramaStore diagramaStore;

  public IvrUpdaterHandler(JsonCleaningService jsonCleaningService,
                           DiagramaStore diagramaStore) {
    this.jsonCleaningService = jsonCleaningService;
    this.diagramaStore = diagramaStore;
  }

  @Override
  public void handle(String idMensaje, String rawIVR, long fechaEnvioLocal) throws Exception {
    JsonNode ivrLimpio = this.parsearIvr(rawIVR);
    diagramaStore.saveDiagram(ivrLimpio);
  }

  private JsonNode parsearIvr(String rawIVR) {
    try {
      IVR ivr = objectMapper.readValue(rawIVR, IVR.class);
      return jsonCleaningService.clean(ivr.getIvrPayload());
    }
    catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error parsing IVR JSON:" + e.getMessage());
      return null;
    }
  }

}
