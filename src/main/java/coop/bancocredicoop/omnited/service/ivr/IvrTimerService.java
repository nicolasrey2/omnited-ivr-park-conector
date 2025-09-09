package coop.bancocredicoop.omnited.service.ivr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class IvrTimerService {
  private static final Logger log = LoggerFactory.getLogger(IvrTimerService.class);

  private final ThreadPoolTaskScheduler scheduler;
  private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

  public IvrTimerService() {
    this.scheduler = new ThreadPoolTaskScheduler();
    this.scheduler.setPoolSize(4);
    this.scheduler.setThreadNamePrefix("ivr-timer-");
    this.scheduler.initialize();
  }

  public void setTimer(String key, Duration duration, Runnable task) {
    cancelTimer(key); // cancela si ya existía
    ScheduledFuture<?> future = scheduler.schedule(task,
        java.util.Date.from(java.time.Instant.now().plus(duration)));
    timers.put(key, future);
    log.debug("Timer seteado para {} con duración {}", key, duration);
  }

  public void cancelTimer(String key) {
    Optional.ofNullable(timers.remove(key))
        .ifPresent(f -> {
          f.cancel(false);
          log.debug("Timer cancelado para {}", key);
        });
  }

  public void cancelAllForChannel(String channelId) {
    timers.keySet().removeIf(k -> {
      if (k.startsWith(channelId + ":")) {
        cancelTimer(k);
        return true;
      }
      return false;
    });
  }
}
