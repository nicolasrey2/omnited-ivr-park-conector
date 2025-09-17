package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.generated.models.*;
import coop.bancocredicoop.omnited.service.statistic.StatisticReporter;
import coop.bancocredicoop.omnited.service.ivr.IvrService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AriMessageMapper {
  private static final Logger log = LoggerFactory.getLogger(AriMessageMapper.class);
  private final IvrService ivrService;
  private final StatisticReporter statisticReporter;

  public AriMessageMapper(@Lazy IvrService ivrService, StatisticReporter statisticReporter) {
    this.ivrService = ivrService;
    this.statisticReporter = statisticReporter;
  }

  public void mapMessage(Message message) {
    if(message == null || message.getType() == null) {
      return;
    }
    log.info(message.getType());
    switch (message.getType()) {
      case "StasisStart":
        StasisStart stasisStartMessage = (StasisStart) message;
        statisticReporter.initSession(stasisStartMessage);
        handleStasisStart(stasisStartMessage);
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

      case "StasisEnd":
        StasisEnd stasisEndMessage = (StasisEnd) message;
        statisticReporter.endSession(stasisEndMessage);
        break;

      case "ChannelHangupRequest":
        ChannelHangupRequest channelHangupMessage = (ChannelHangupRequest) message;
        statisticReporter.hangUpRequest(channelHangupMessage);
        break;

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
