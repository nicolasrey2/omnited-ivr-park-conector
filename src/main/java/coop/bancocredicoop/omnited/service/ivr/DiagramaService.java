package coop.bancocredicoop.omnited.service.ivr;

import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class DiagramaService {

  private static final Logger LOGGER = Logger.getLogger(DiagramaService.class.getName());
  private volatile JsonNode currentDiagram;

  /**
   * Guarda (o reemplaza) el diagrama actual.
   */
  public void saveDiagram(JsonNode d) {
    System.out.println("Se guardo el diagrama");
    this.currentDiagram = d;
  }

  /**
   * Retorna el diagrama actual. Puede ser null si nunca se guardó ninguno.
   */
  public JsonNode getDiagram() {
    return currentDiagram;
  }
}
