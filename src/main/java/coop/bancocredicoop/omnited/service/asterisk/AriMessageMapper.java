package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.generated.models.*;
import coop.bancocredicoop.omnited.service.ivr.IvrService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AriMessageMapper {
  private final IvrService ivrService;
  private final Logger log = LoggerFactory.getLogger(AriMessageMapper.class);

  public AriMessageMapper(@Lazy IvrService ivrService) {
    this.ivrService = ivrService;
  }

  public void mapMessage(Message message) {
    if(message == null || message.getType() == null) {
      return;
    }
    log.info(message.getType());
    switch (message.getType()) {
      case "StasisStart":
        handleStasisStart((StasisStart) message);
        break;

      case "ChannelDtmfReceived":
        ChannelDtmfReceived dtmf = (ChannelDtmfReceived) message;
        log.info("ChannelDtmfReceived, digit: {}", dtmf.getDigit());
        handleChannelDtmfReceived(dtmf);
        break;

      case "PlaybackFinished":
        PlaybackFinished playbackFinished = (PlaybackFinished) message;
        log.info("Playback finished por evento: {}", playbackFinished.getPlayback().getId());
        handlePlaybackFinished(playbackFinished);
        break;

      // Agregá más casos según los eventos que uses
      default:
        log.warn("Evento ARI no manejado: {}", message.getType());

    }
  }
  private void handleStasisStart(StasisStart message) {
    ivrService.startFlow(message.getChannel());
  }

  private void handleChannelDtmfReceived(ChannelDtmfReceived dtmf) {
    ivrService.handleDtmf(dtmf.getChannel(), dtmf.getDigit());
  }

  private void handlePlaybackFinished(PlaybackFinished event) {
    String playbackId = event.getPlayback().getId();
    ivrService.playbackFinished(playbackId);
  }


}
