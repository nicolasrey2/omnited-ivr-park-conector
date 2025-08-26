package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.generated.models.*;
import coop.bancocredicoop.omnited.service.ivr.IvrService;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AriMessageMapper {
  private IvrService ivrService;
  private RedisService redisService;

  public AriMessageMapper(@Lazy IvrService ivrService, @Lazy RedisService redisService) {
    this.ivrService = ivrService;
    this.redisService = redisService;
  }

  public void mapMessage(Message message) {
    if(message == null || message.getType() == null) {
      return;
    }
    switch (message.getType()) {
      case "StasisStart":
        handleStasisStart((StasisStart) message);
        break;

      case "ChannelDtmfReceived":
        ChannelDtmfReceived dtmf = (ChannelDtmfReceived) message;
        System.out.println("Se recibio: " + dtmf.getDigit());
        handleChannelDtmfReceived(dtmf);
        break;

      case "PlaybackFinished":
        System.out.println("Playback finished");
        PlaybackFinished playbackFinished = (PlaybackFinished) message;
        handlePlaybackFinished(playbackFinished);
        break;

      // Agregá más casos según los eventos que uses
      default:
        System.out.println("Evento ARI no manejado: " + message.getType());

    }
  }
  private void handleStasisStart(StasisStart message) {
    ivrService.startFlow(message.getChannel());
  }

  private void handleChannelDtmfReceived(ChannelDtmfReceived message) {
    ivrService.handleDtmf(message.getChannel(), message.getDigit());
  }

  private void handlePlaybackFinished(PlaybackFinished event) {
    String playbackId = event.getPlayback().getId();
    String channelId = redisService.get("playback:" + playbackId);

    if (channelId == null) {
      System.out.println("Error en AriMessageMapper.handlePlaybackFinished no se " +
          "encontro canal para playback con id: " + playbackId);
      return;
    }

    redisService.delete("playback:" + playbackId);
    System.out.println("recupero siguiente:" + channelId);
    String siguienteId = redisService.get("siguiente:" + channelId);
    if (siguienteId == null) {
      System.out.println("No hay siguiente nodo cacheado AriMessageMapper.handlePlaybackFinished");
      return;
    }
    redisService.delete("siguiente:" + channelId);
    redisService.set("posicion:" + channelId, siguienteId, 300);
    System.out.println("Se setea la posicion en " + channelId);
    ivrService.playbackFinished(channelId, playbackId);
  }




}
