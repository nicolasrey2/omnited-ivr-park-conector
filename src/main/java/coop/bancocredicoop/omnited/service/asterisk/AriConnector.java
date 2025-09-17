package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.ari_8_0_0.models.Bridge_impl_ari_8_0_0;
import ch.loway.oss.ari4java.generated.models.Bridge;
import ch.loway.oss.ari4java.generated.models.Message;
import ch.loway.oss.ari4java.generated.models.Playback;
import ch.loway.oss.ari4java.tools.AriConnectionEvent;
import ch.loway.oss.ari4java.tools.AriWSCallback;
import ch.loway.oss.ari4java.tools.RestException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AriConnector {
  private static final Logger log = LoggerFactory.getLogger(AriConnector.class);


  @Value("${ari.url}")
  private String ariUrl;
  @Value("${ari.username}")
  private String username;
  @Value("${ari.password}")
  private String password;
  @Value("${ari.app}")
  private String app;

  private ARI ari;
  private final AriMessageMapper ariMessageMapper;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private volatile boolean connected = false;

  public AriConnector(AriMessageMapper ariMessageMapper) {
    this.ariMessageMapper = ariMessageMapper;
  }

  @PostConstruct
  public void init() {
    connectWithRetry();
  }

  private void connectWithRetry() {
    scheduler.execute(this::connect);
  }

  private void connect() {
    try {
      log.info("Conectando a ARI...");
      ari = ARI.build(ariUrl, app, username, password, AriVersion.ARI_8_0_0);

      ari.events()
          .eventWebsocket(app)
          .execute(new AriWSCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
              ariMessageMapper.mapMessage(message);
            }

            @Override
            public void onFailure(RestException e) {
              log.error("Error al conectar a ARI", e);
              reconnectLater();
            }

            @Override
            public void onConnectionEvent(AriConnectionEvent event) {
              log.info("Estado WS: {}", event);
              if (event == AriConnectionEvent.WS_CONNECTED) {
                connected = true;
              } else if (event == AriConnectionEvent.WS_DISCONNECTED) {
                connected = false;
                reconnectLater();
              }
            }
          });

    } catch (Exception e) {
      log.error("Error inicial conectando a ARI: {}", e.getMessage());
      reconnectLater();
    }
  }

  private void reconnectLater() {
    if (!connected) {
      log.info("Reintentando conexión ARI en 5 segundos...");
      scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
    }
  }

  public void hangupChannel(String channelId) {
    try {
      ari.channels().hangup(channelId).execute();
      log.info("Hangup channel ID: {}", channelId);
    } catch (RestException e) {
      log.error("Error colgando el canal: {}. Error: {}", channelId, e.getMessage());
    }
  }

  public Playback play(String channelId, String soundFilename) {
    String sound = FilenameUtils.removeExtension(soundFilename);
    String url = "sound:" + sound;
    try {
      log.info("Enviando audio a Asterisk: {}", url);
      Playback playback = ari.channels().play(channelId, url).execute();
      log.info("Playback ID: {}", playback);
      return playback;
    } catch (RestException e) {
      log.error("Error enviando audio a Asterisk: {}", e.getMessage());
      throw new RuntimeException("Error enviando audio a Asterisk: " + e.getMessage());
    }
  }

  public void stopPlayback(String playbackId) {
    try {
      ari.playbacks().stop(playbackId).execute();
    } catch (RestException e) {
      log.error("Error deteniendo el playback: {}. Error: {}", playbackId, e.getMessage());
    }
  }

  public void addChannelToBridge(String channelId, String bridgeId) {
    try {
      ari.bridges().addChannel(bridgeId, channelId).execute();
    } catch (RestException e) {
      log.error("Error agregando el canal: {}, en el bridgeId: {}. Error: {}", channelId, bridgeId, e.getMessage());
    }
  }

  public void removeChannelToBridge(String channelId, String bridgeId) {
    try {
      ari.bridges().removeChannel(bridgeId, channelId).execute();
    } catch (RestException e) {
      log.error("Error removiendo el canal: {}, del bridgeId: {}. Error: {}", channelId, bridgeId, e.getMessage());
    }
  }

  public Bridge createBridge(String type) {
    try {
      return ari.bridges()
          .create()
          .setType(type)
          .execute();
    } catch (RestException e) {
      log.error("Error creando el parkingBridge. Error: {}", e.getMessage());
    }
    return null;
  }

  @PreDestroy
  public void shutdown() {
    log.info("Cerrando conexión ARI...");
    scheduler.shutdownNow();
    if (ari != null) {
      ari.cleanup();
    }
  }
}
