package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class IvrService {
  private DiagramaProcessor diagramaProcessor;
  private DiagramaStore diagramaStore;

  public IvrService(DiagramaProcessor diagramaProcessor, DiagramaStore diagramaStore) {
    this.diagramaStore = diagramaStore;

    this.diagramaProcessor = diagramaProcessor;
  }

  public void startFlow(Channel channel) {
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, "");
  }

}
