package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.models.Message;
import ch.loway.oss.ari4java.generated.models.Playback;
import ch.loway.oss.ari4java.tools.AriConnectionEvent;
import ch.loway.oss.ari4java.tools.AriWSCallback;
import ch.loway.oss.ari4java.tools.RestException;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AriConnector {

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
  private final RedisService redisService;
  private volatile boolean connected = false;

  public AriConnector(AriMessageMapper ariMessageMapper, RedisService redisService) {
    this.ariMessageMapper = ariMessageMapper;
    this.redisService = redisService;
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
      System.out.println("Conectando a ARI...");
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
              System.err.println("Error en ARI: " + e.getMessage());
              reconnectLater();
            }

            @Override
            public void onConnectionEvent(AriConnectionEvent event) {
              System.out.println("Estado WS: " + event);
              if (event == AriConnectionEvent.WS_CONNECTED) {
                connected = true;
              } else if (event == AriConnectionEvent.WS_DISCONNECTED) {
                connected = false;
                reconnectLater();
              }
            }
          });

    } catch (Exception e) {
      System.err.println("Error inicial conectando a ARI: " + e.getMessage());
      reconnectLater();
    }
  }

  private void reconnectLater() {
    if (!connected) {
      System.out.println("Reintentando conexión ARI en 5 segundos...");
      scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
    }
  }

  public void play(String channelId, String soundFilename) {
    String sound = FilenameUtils.removeExtension(soundFilename);
    String url = "sound:" + sound;
    try {
      System.out.println("Enviando audio a Asterisk: " + url);
      Playback playback = ari.channels().play(channelId, url).execute();
      redisService.set("playback:" + playback.getId(), channelId, 300);
      System.out.println("Playback ID: " + playback);
    } catch (RestException e) {
      System.err.println("Error enviando audio a Asterisk: " + e.getMessage());
    }
  }

  public void hangupChannel(String channelId) {
    try {
      ari.channels().hangup(channelId).execute();
      System.out.println("Hangup channel ID: " + channelId);
    } catch (RestException e) {
      System.out.println("Error enviando audio a Asterisk: " + e.getMessage());
    }
  }


  @PreDestroy
  public void shutdown() {
    System.out.println("Cerrando conexión ARI...");
    scheduler.shutdownNow();
    if (ari != null) {
      ari.cleanup();
    }
  }
}
