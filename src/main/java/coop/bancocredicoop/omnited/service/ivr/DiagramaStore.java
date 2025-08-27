package coop.bancocredicoop.omnited.service.ivr;

import coop.bancocredicoop.omnited.handler.rabbit.IvrUpdaterHandler;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class DiagramaStore {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(DiagramaStore.class);
  private volatile JsonNode currentDiagram;

  /**
   * Guarda (o reemplaza) el diagrama actual.
   */
  public void saveDiagram(JsonNode d) {
    this.currentDiagram = d;
    log.info("Se guardo el diagrama");
  }

  /**
   * Retorna el diagrama actual. Puede ser null si nunca se guardó ninguno.
   */
  public JsonNode getDiagram() {
    return currentDiagram;
  }
}
