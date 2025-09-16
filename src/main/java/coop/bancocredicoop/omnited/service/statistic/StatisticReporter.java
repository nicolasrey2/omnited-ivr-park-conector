package coop.bancocredicoop.omnited.service.statistic;

import ch.loway.oss.ari4java.generated.models.Channel;
import ch.loway.oss.ari4java.generated.models.ChannelHangupRequest;
import ch.loway.oss.ari4java.generated.models.StasisEnd;
import ch.loway.oss.ari4java.generated.models.StasisStart;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import coop.bancocredicoop.omnited.exposition.IvrSession;
import coop.bancocredicoop.omnited.message.MessageToRabbit;
import coop.bancocredicoop.omnited.service.redis.RedisKeys;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;


@Service
public class StatisticReporter {
  private static final Logger log = LoggerFactory.getLogger(StatisticReporter.class);

  private final RedisService redisService;
  private final MessageToRabbit messageToRabbit;
  private final ObjectMapper objectMapper;

  public StatisticReporter(RedisService redisService, MessageToRabbit messageToRabbit,
                           ObjectMapper objectMapper) {
    this.redisService = redisService;
    this.messageToRabbit = messageToRabbit;
    this.objectMapper = objectMapper;
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  public void initSession(StasisStart stasisStart) {
    String channelId = stasisStart.getChannel().getId();
    String direction = "INBOUND";
    Instant startedAt = Instant.now();

    redisService.set(RedisKeys.directionOfSessionByChannel(channelId), direction);
    redisService.set(RedisKeys.instantOfStartSessionByChannel(channelId), startedAt.toString());
    log.debug("direction: {}; startedAt: {};", direction, startedAt);
  }

  public void endSession(StasisEnd stasisEnd) {
    Channel channel = stasisEnd.getChannel();

    String channelId = channel.getId();
    String accountCode = channel.getAccountcode();
    String dnis = channel.getDialplan().getExten();
    String context = channel.getDialplan().getContext();
    String ani = channel.getCaller().getNumber();
    String direction = redisService.get(RedisKeys.directionOfSessionByChannel(channelId));
    String entrypointNode = redisService.get(RedisKeys.entryPointNodeFor(channelId)); //TODO setear esto
    String hangupCause = redisService.get(RedisKeys.hangupCauseByChannel(channelId));
    String startedAt = redisService.get(RedisKeys.instantOfStartSessionByChannel(channelId));
    Instant endedAt = Instant.now();

    log.debug("channelId: {}; accountCode: {}; dnis: {}; context: {}; ani: {}; direction: {}; hangupCause: {}; startedAt: {}; endedAt: {};",
        channelId, accountCode, dnis, context, ani, direction, hangupCause, startedAt, endedAt);

    IvrSession ivrSession = new IvrSession()
        .setAriChannelId(channelId)
        .setDirection(IvrSession.CallDirection.valueOf(direction))
        .setAni(ani)
        .setDnis(dnis)
        .setContext(context)
        .setEntrypointNode(entrypointNode)
        .setStartedAt(Instant.parse(startedAt))
        .setEndedAt(endedAt)
        .setHangupCause(hangupCause);

    sendToRabbit(String.valueOf(UUID.randomUUID()), "IVR_SESSION", ivrSession);
  }

  public void hangUpRequest(ChannelHangupRequest channelHangupRequest) {
    String channelId = channelHangupRequest.getChannel().getId();
    int causeCode = channelHangupRequest.getCause();
    HangupCause hangupCause = HangupCause.fromCode(causeCode);
    redisService.set(RedisKeys.hangupCauseByChannel(channelId), hangupCause.getDescription());
    log.debug("hangupCause: {}", hangupCause.getDescription());
  }

  private void sendToRabbit(String uuid, String messageType,
                                         Object dto) {
    try {
      String dtoJson = objectMapper.writeValueAsString(dto);
      messageToRabbit.processMessage(uuid, messageType, dtoJson);

    } catch (JsonProcessingException ex) {
      java.util.logging.Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
    }
  }

}
