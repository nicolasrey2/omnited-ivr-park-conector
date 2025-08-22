package coop.bancocredicoop.omnited.messages;

import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import coop.bancocredicoop.omnited.service.tts.PiperTtsService;
import org.springframework.stereotype.Service;

@Service
public class AudioForChannel implements  CanalMensajeria {
  private PiperTtsService piperTtsService;
  private AriConnector ariConnector;

  public AudioForChannel(PiperTtsService piperTtsService, AriConnector ariConnector) {
    this.piperTtsService = piperTtsService;
    this.ariConnector = ariConnector;
  }

  @Override
  public void enviarMensaje(String channelId, String textToSendToChannel) {
    String audioFilename = piperTtsService.textToSpeech(textToSendToChannel);
    ariConnector.play(channelId, audioFilename);
  }
}
