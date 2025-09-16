package coop.bancocredicoop.omnited.service.redis;

public final class RedisKeys {

  // Evitamos instanciación
  private RedisKeys() {}

  // Prefijos
  private static final String PREFIX_SESSION = "session:";
  private static final String PREFIX_STATS = "stats:";
  private static final String PREFIX_CHANNEL = "channel:";


  // -------------------------
  // Estadísticas
  // -------------------------
  public static String hangupCauseByChannel(String channelId) {
    return PREFIX_STATS + "channel:" + channelId + ":hangup_cause";
  }
  public static String directionOfSessionByChannel(String channelId) {
    return PREFIX_STATS + "channel:" + channelId + ":direction_of_session";
  }
  public static String instantOfStartSessionByChannel(String channelId) {
    return PREFIX_STATS + "channel:" + channelId + ":instant_of_start_session";
  }

  public static String entryPointNodeFor(String channelId) {
    return PREFIX_STATS + "channel:" + channelId + ":entry_point_node";
  }
}
