package coop.bancocredicoop.omnited.service.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RetryService {
  private static final Logger log = LoggerFactory.getLogger(RetryService.class);
  private final RedisService redisService;

  public RetryService(RedisService redisService) {
    this.redisService = redisService;
  }

  public void handleRetries(String channelId, int ttl) {
    String cantActualStr = redisService.get("cantidadReintentos:" + channelId);
    if (cantActualStr == null) {
      cantActualStr = "0";
    }

    int cantActual;
    try {
      cantActual = Integer.parseInt(cantActualStr);
    } catch (NumberFormatException e) {
      log.error("Valor inválido en retries para canal {}, reseteando a 0", channelId);
      cantActual = 0;
    }
    cantActual++;

    redisService.set("cantidadReintentos:" + channelId, String.valueOf(cantActual), ttl);
    log.info("Se seteo cantidadReintentos:{} en {}", cantActual, channelId);
  }

  public void clearRetries(String channelId) {
    redisService.delete("cantidadReintentos:" + channelId);
    log.info("Se limpiaron retries para canal {}", channelId);
  }
}
