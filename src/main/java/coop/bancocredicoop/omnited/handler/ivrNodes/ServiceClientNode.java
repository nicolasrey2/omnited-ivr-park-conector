package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.redis.RetryService;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import coop.bancocredicoop.omnited.service.restClient.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.*;


@Component("servCliente")
public class ServiceClientNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(ServiceClientNode.class);
  private final int TTS_REINTENTOS = 35;
  private final int TTS_VARIABLES = 300;

  private final RedisService redisService;
  private final RestClient restClient;
  private final VariableResolver variableResolver;
  private final RetryService retryService;

  public ServiceClientNode(RedisService redisService, RestClient restClient,
                           VariableResolver variableResolver,  RetryService retryService) {
    this.redisService = redisService;
    this.restClient = restClient;
    this.variableResolver = variableResolver;
    this.retryService = retryService;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    JsonNode data = node.get("data");
    if (data == null) {
      log.error("No hay data en el nodo");
      return "";
    }

    String method = data.has("method") ? data.get("method").asText() : null;

    String url = data.has("url") ? data.get("url").asText() : null;
    Map<String, String> pathParams = getPathParams(data, channelId);

    Map<String, String> queryParams = getQueryParams(data, channelId);
    Map<String, String> body        = getBodyParams(data, channelId);

    Map<String, String> headersMap = getHeaders(data);
    List<Integer> correctStatues = getCorrectStates(data);

    // Llamar RestClient
    ResponseEntity<String> response = restClient.send(url, method, queryParams, headersMap, pathParams, body);
    log.info("Respuesta del endpoint {}: {}", url, response.getBody());

    if (! correctStatues.contains(response.getStatusCodeValue())) { // error
      log.info("Error al consultar el endpoint {}: cod: {}; body: {}", url,
          response.getStatusCodeValue(), response.getBody());

      for (String variable : pathParams.keySet()) {
        redisService.delete(variable + ":" + channelId);
        log.info("Borrada variable {} de Redis para canal {}", variable, channelId);
      }

      retryService.handleRetries(channelId, TTS_REINTENTOS);
      String nodoError = DiagramaUtils.buscarEdgePorHandle(ivr, node, "error");
      if (nodoError == null) {
        log.error("No se encontro nodo con handler error");
        nodoError = DiagramaUtils.encontrarHangup(ivr);
      }
      return nodoError;
    }

    retryService.clearRetries(channelId);
    setVars(channelId, response.getBody(), data.get("variablesASetear"));
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, "ok");
  }


  /**
   * Convierte un JsonNode (objeto) en Map<String, String>, reemplazando las variables {var}.
   */
  private Map<String, String> getJsonNodeAsMapWithRedisVars(JsonNode node, String channelId) {
    Map<String, String> result = new HashMap<>();
    if (node != null && node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        String key = entry.getKey();
        String rawValue = entry.getValue().asText();
        String resolvedValue = variableResolver.resolve(rawValue, channelId);
        result.put(key, resolvedValue);
      });
    }
    return result;
  }

  /**
   * obtener queryParams resuelto.
   */
  private Map<String, String> getQueryParams(JsonNode data, String channelId) {
    return getJsonNodeAsMapWithRedisVars(data.get("queryParams"), channelId);
  }

  /**
   * obtener body resuelto.
   */
  private Map<String, String> getBodyParams(JsonNode data, String channelId) {
    return getJsonNodeAsMapWithRedisVars(data.get("body"), channelId);
  }


  private void setVars(String channelId, String responseBody, JsonNode varsNode) {
    if (varsNode == null || !varsNode.isObject()) {
      log.info("No hay variables en el nodo de variables");
      return;
    }

    Iterator<Map.Entry<String, JsonNode>> fields = varsNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();

      String nombreVariable = entry.getKey();              // ej: "nombre"
      String jsonPath = entry.getValue().asText();         // ej: "$.candidatos[0].nombre"

      try {
        // resolver el JSONPath sobre la respuesta del servicio
        Object valor = JsonPath.read(responseBody, jsonPath);

        if (valor != null) {
          redisService.set(nombreVariable + ":" + channelId, valor.toString(), TTS_VARIABLES);
          log.info("Se seteó la variable: {}, con el valor: {}", nombreVariable, valor);
        } else {
          log.error("No se encontró valor para la variable {} con JSONPath {}", nombreVariable, jsonPath);
        }

      } catch (Exception e) {
        log.error("Error resolviendo JSONPath {} para la variable {}", jsonPath, nombreVariable, e);
      }
    }
  }

  private Map<String, String> getPathParams(JsonNode data, String channelId) {
    JsonNode pathValuesNode = data.get("pathParams");
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
