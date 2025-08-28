package coop.bancocredicoop.omnited.service.ivr;

import ch.loway.oss.ari4java.generated.models.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.entity.Playback;
import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IvrService {
  private static final Logger log = LoggerFactory.getLogger(IvrService.class);
  private final DiagramaProcessor diagramaProcessor;
  private final DiagramaStore diagramaStore;
  private final PlaybackStateManager playbackStateManager;

  @Autowired
  private AriConnector ariConnector;

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
    String channelId = channel.getId();
    if(! playbackStateManager.hasActivePlayback(channelId)) {
      JsonNode diagrama = diagramaStore.getDiagram();
      diagramaProcessor.procesarMensaje(diagrama, channelId, digit);
    }
    else { //tiene un playback activo
      Playback playback = playbackStateManager.stopPlaybackFor(channelId);
      playback.addDtmf(digit);
    }
  }

  public void playbackFinished(String playbackId) {
    Playback playback = playbackStateManager.advanceToNextNodeFromFinishedPlayback(playbackId);

    diagramaProcessor.procesarMensaje(diagramaStore.getDiagram(),
        playback.getChannelId(), playback.getDtmfAcumulated());
  }
}
