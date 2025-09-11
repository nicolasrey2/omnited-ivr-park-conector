package coop.bancocredicoop.omnited.service.redis;

import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    /**
     * Guarda un valor simple (string) en Redis sin TTL.
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Guarda un valor con TTL (en segundos).
     */
    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * Obtiene un valor. Retorna null si no existe.
     */
    public String get(String key) {
      return redisTemplate.opsForValue().get(key);
    }

    /**
     * Obtiene un valor y si no existe retorna defaultValue.
     */
    public String getOrDefault(String key, String defaultValue) {
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    /**
     * Borra la clave indicada.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Borra todas las claves que contengan channelId
     * TODO se debe analizar si su uso es performante o si se debe cambiar por deletes explicitos
     */
    public void deleteAllFrom(String channelId) {
        String pattern = "*" + channelId + "*";

        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        try (Cursor<byte[]> cursor = redisTemplate.execute(
                (RedisCallback<Cursor<byte[]>>) connection -> connection.scan(options))) {

            List<String> keysToDelete = new ArrayList<>();
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                keysToDelete.add(redisTemplate.getStringSerializer().deserialize(rawKey));

                if (keysToDelete.size() >= 100) {
                    redisTemplate.unlink(keysToDelete);
                    keysToDelete.clear();
                }
            }

            if (!keysToDelete.isEmpty()) {
                redisTemplate.unlink(keysToDelete);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error borrando claves de Redis", e);
        }
    }

    /**
     * Retorna un conjunto de llaves que coincidan con el patrón. Ej:
     * keys("var:1234:*")
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * Retorna todas las variables guardadas para un usuario "from": Busca
     * llaves con prefijo "var:<from>:" y crea un Map<nombreVar, valor>.
     */
    public Map<String, String> getAllVars(String from) {
        String pattern = "var:" + from + ":*";
        Set<String> allKeys = redisTemplate.keys(pattern);
        Map<String, String> result = new HashMap<>();
        if (allKeys != null) {
            for (String key : allKeys) {
                String val = redisTemplate.opsForValue().get(key);
                // key tiene formato "var:<from>:<nombreVar>"
                String nombreVar = key.substring(("var:" + from + ":").length());
                result.put(nombreVar, val);
            }
        }
        return result;
    }

    /**
     * Guarda un campo en un Hash con TTL sobre todoel Hash.
     */
    public void hset(String from, String varName, String valor) {
        String hashKey = "vars:" + from;
        redisTemplate.opsForHash().put(hashKey, varName, valor);
    }

    /**
     * Recupera un campo del Hash.
     */
    public String hget(String from, String varName) {
        String hashKey = "vars:" + from;
        return (String) redisTemplate.opsForHash().get(hashKey, varName);
    }

    /**
     * BorraTODO el estado de ese usuario (todoel Hash).
     */
    public void deleteHash(String from) {
        redisTemplate.delete("vars:" + from);
    }

}
