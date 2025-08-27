package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IvrService {
  private static final Logger log = LoggerFactory.getLogger(IvrService.class);
  private final DiagramaProcessor diagramaProcessor;
  private final DiagramaStore diagramaStore;
  private final PlaybackStateManager playbackStateManager;

  public IvrService(DiagramaProcessor diagramaProcessor, DiagramaStore diagramaStore,
                    PlaybackStateManager playbackStateManager) {
    this.diagramaStore = diagramaStore;
    this.playbackStateManager = playbackStateManager;
    this.diagramaProcessor = diagramaProcessor;
  }

  public void startFlow(Channel channel) {
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, "");
  }

  public void handleDtmf(Channel channel, String digit) {
    //TODO se deberia pausar el playback del canal y pasar al siguiente nodo (se pasa solo si es nodo de salida)
    String from = channel.getId();
    JsonNode diagrama = diagramaStore.getDiagram();
    diagramaProcessor.procesarMensaje(diagrama, from, digit);
  }

  public void playbackFinished(String playbackId) {
    String channelId = playbackStateManager.advanceToNextNodeFromFinishedPlayback(playbackId);

    diagramaProcessor.procesarMensaje(diagramaStore.getDiagram(), channelId, "");
  }
}
