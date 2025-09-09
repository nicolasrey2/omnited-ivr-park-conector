package coop.bancocredicoop.omnited.service.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VariableResolver {
  private static final Pattern VAR_PATTERN = Pattern.compile("\\{([^}]+)}");
  private final RedisService redisService;
  private final Logger log = LoggerFactory.getLogger(VariableResolver.class);

  public VariableResolver(RedisService redisService) {
    this.redisService = redisService;
  }

  public String resolve(String rawText, String channelId) {
    if (rawText == null) return null;
    Matcher matcher = VAR_PATTERN.matcher(rawText);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String varName = matcher.group(1);
      String value = redisService.get(varName + ":" + channelId);
      if (value == null) {
        log.warn("Variable {} no encontrada para canal {}, se mantiene placeholder", varName, channelId);
        value = matcher.group(0);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
    }

    matcher.appendTail(sb);
    return sb.toString();
  }
}

