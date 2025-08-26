package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import ch.loway.oss.ari4java.generated.models.Playback;
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

  public void handleDtmf(Channel channel, String digit) {
    //TODO validar si no habria que guardar/comprobar estado (para asegurar que se esta esperando un dtmf)
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, digit);
  }

  public void playbackFinished(String channel, String playbackId) {
    System.out.println("Playback con id: " + playbackId + " se finalizo, se continua con el ivr");
    diagramaProcessor.procesarMensaje(diagramaStore.getDiagram(), channel, "");
  }
}
