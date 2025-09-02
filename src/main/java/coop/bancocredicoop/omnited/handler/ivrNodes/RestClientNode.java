package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.restClient.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component("restClientNode")
public class RestClientNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(RestClientNode.class);

  private final RedisService redisService;
  private final RestClient restClient;

  public RestClientNode(RedisService redisService, RestClient restClient) {
    this.redisService = redisService;
    this.restClient = restClient;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    JsonNode data = node.get("data");
    if (data == null) {
      log.error("No hay data en el nodo");
      return "";
    }

    String url = data.has("url") ? data.get("url").asText() : null;
    String method = data.has("method") ? data.get("method").asText() : null;
    JsonNode queryParams = data.get("queryParams");
    JsonNode body = data.get("body");
    Map<String, String> values = getValues(data, channelId);
    Map<String, String> headersMap = getHeaders(data);
    List<Integer> correctStatues = getCorrectStates(data);

    // Llamar RestClient
    ResponseEntity<String> response = restClient.handle(url, method, queryParams, headersMap, values, body);
    log.info("Respuesta del endpoint {}: {}", url, response.getBody());

    if (! correctStatues.contains(response.getStatusCodeValue())) { // error
      log.error("Respuesta del endpoint {}: cod: {}; body: {}", url, response.getStatusCodeValue(), response.getBody());
      return DiagramaUtils.buscarEdgePorHandle(ivr, node, "error");
    }

    //TODO analizar setear variables redis para pasarselas al nodo ok
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, "ok");
  }

  private Map<String, String> getValues(JsonNode data, String channelId) {
    JsonNode pathValuesNode = data.get("PathValues");
    Map<String, String> values = new HashMap<>();

    if (pathValuesNode != null && pathValuesNode.isArray()) {
      for (JsonNode pv : pathValuesNode) {
        String key = pv.asText() + ":" + channelId;
        String value = redisService.get(key);
        values.put(pv.asText(), value);

        // Borrar key de Redis después de leer
        redisService.delete(key);
      }
    }

    return values;
  }

  private Map<String, String> getHeaders(JsonNode data) {
    Map<String, String> headersMap;
    JsonNode headersNode = data.get("headers");
    if (headersNode != null && headersNode.isObject()) {
      headersMap = new HashMap<>();
      headersNode.fields().forEachRemaining(entry -> headersMap.put(entry.getKey(), entry.getValue().asText()));
    }
    else {
      headersMap = null;
    }
    return headersMap;
  }

  private List<Integer> getCorrectStates(JsonNode data) {
    List<Integer> correctStatesList = new ArrayList<>();
    JsonNode correctStates = data.get("correctStates");

    if (correctStates != null && correctStates.isArray()) {
      for (JsonNode node : correctStates) {
        if (node.isInt()) {
          correctStatesList.add(node.asInt());
        }
      }
    }
    return correctStatesList;
  }

}
