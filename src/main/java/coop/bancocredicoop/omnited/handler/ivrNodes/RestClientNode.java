package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.restClient.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.*;


@Component("restClientNode")
public class RestClientNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(RestClientNode.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final int tts = 20;

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
      log.info("Error al consultar el endpoint {}: cod: {}; body: {}", url,
          response.getStatusCodeValue(), response.getBody());

      //TODO analizar: Si es error funcional (ej: 400 o 404) -> borro variables de redis
      if (response.getStatusCodeValue() == 400 || response.getStatusCodeValue() == 404) {
        for (String variable : values.keySet()) {
          redisService.delete(variable + ":" + channelId);
          log.info("Borrada variable {} de Redis para canal {}", variable, channelId);
        }
      }

      handleRetries(channelId);
      return DiagramaUtils.buscarEdgePorHandle(ivr, node, "error");
    }

    clearRetries(channelId);
    setVars(channelId, response.getBody(), data.get("variablesASetear"));
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, "ok");
  }

  private void handleRetries(String channelId) {
    String cantActualStr = redisService.get("cantidadReintentos:" + channelId);
    if (cantActualStr == null) {
      cantActualStr = "0";
    }
    int cantActual = Integer.parseInt(cantActualStr);

    redisService.set("cantidadReintentos:" + channelId, String.valueOf(cantActual+1), tts);
  }

  private void clearRetries(String channelId) {
    redisService.delete("cantidadReintentos:" + channelId);
  }

  private void setVars(String channelId, String responseBody, JsonNode variablesASetear) {
    if (variablesASetear == null || !variablesASetear.isArray()) {
      return;
    }

    try {
      JsonNode responseJson = objectMapper.readTree(responseBody);

      for (JsonNode varNode : variablesASetear) {
        String variable = varNode.asText();

        // buscamos el valor en el JSON de respuesta
        JsonNode valorNode = responseJson.get(variable);
        if (valorNode != null && !valorNode.isNull()) {
          String valor = valorNode.asText();

          // lo guardamos en Redis con la key var:channelId
          redisService.set(variable + ":" + channelId, valor, tts);
          log.info("Seteada variable {}={} para canal {}", variable, valor, channelId);
        } else {
          log.error("Variable {} no encontrada en la respuesta del endpoint", variable);
        }
      }
    } catch (Exception e) {
      log.error("Error procesando responseBody en setVars: {}", e.getMessage(), e);
    }

  }

  private Map<String, String> getValues(JsonNode data, String channelId) {
    JsonNode pathValuesNode = data.get("PathValues");
    Map<String, String> values = new HashMap<>();

    if (pathValuesNode != null && pathValuesNode.isArray()) {
      for (JsonNode pv : pathValuesNode) {
        String key = pv.asText() + ":" + channelId;
        String value = redisService.get(key);
        values.put(pv.asText(), value);
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
