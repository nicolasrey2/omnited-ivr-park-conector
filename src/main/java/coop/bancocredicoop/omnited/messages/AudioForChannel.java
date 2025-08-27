package coop.bancocredicoop.omnited.messages;

import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import coop.bancocredicoop.omnited.service.tts.PiperTtsService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AudioForChannel implements  CanalMensajeria {
  private static final Logger log = LoggerFactory.getLogger(AudioForChannel.class);

  private PiperTtsService piperTtsService;
  private AriConnector ariConnector;

  public AudioForChannel(PiperTtsService piperTtsService, AriConnector ariConnector) {
    this.piperTtsService = piperTtsService;
    this.ariConnector = ariConnector;
  }

  @Override
  public void enviarMensaje(String channelId, String textToSendToChannel) {
    log.info("Se envia el mensaje: {} , sobre el canal: {}", textToSendToChannel, channelId);
    String audioFilename = piperTtsService.textToSpeech(textToSendToChannel);
    log.info("audio generado: {}", audioFilename);
    ariConnector.play(channelId, audioFilename);
  }
}
