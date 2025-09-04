package coop.bancocredicoop.omnited.handler.rabbit;

import coop.bancocredicoop.omnited.entity.IVR;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaStore;
import coop.bancocredicoop.omnited.service.rabbit.RabbitMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.utils.JsonCleaningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IvrUpdaterHandler implements RabbitMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(IvrUpdaterHandler.class);
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
      log.error("Error parsing IVR JSON", e);
      return null;
    }
  }

}
