package coop.bancocredicoop.omnited.service.client.restClient;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.client.ServiceInvoker;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("REST")
public class RestServiceInvoker implements ServiceInvoker {
  private static final Logger log = LoggerFactory.getLogger(RestServiceInvoker.class);

  private final RestClient restClient;
  private final VariableResolver variableResolver;

  public RestServiceInvoker(RestClient restClient, VariableResolver variableResolver) {
    this.restClient = restClient;
    this.variableResolver = variableResolver;
  }

  @Override
  public ResponseEntity<String> invoke(String channelId, JsonNode data, String url) {
    String method = data.get("method").asText();
    Map<String, String> headers = getHeaders(data);
    Map<String, String> queryParams = getQueryParams(data, channelId);
    Map<String, String> body = getBodyParams(data, channelId);

    log.info("REST invoke - URL: {}, Method: {}", url, method);
    log.info("Headers: {}", headers);
    log.info("Query Params: {}", queryParams);
    log.info("Body: {}", body);

    ResponseEntity<String> response = restClient.send(url, method, queryParams, headers, body);

    log.info("Response Status: {}, Body: {}", response.getStatusCodeValue(), response.getBody());
    return response;
  }

  private Map<String, String> getHeaders(JsonNode data) {
    Map<String, String> headersMap;
    JsonNode headersNode = data.get("headers");
    if (headersNode != null && headersNode.isObject()) {
      headersMap = new HashMap<>();
      headersNode.fields().forEachRemaining(entry -> {
        headersMap.put(entry.getKey(), entry.getValue().asText());
        log.debug("Resolved header: {}={}", entry.getKey(), entry.getValue().asText());
      });
    } else {
      headersMap = null;
    }
    return headersMap;
  }

  private Map<String, String> getQueryParams(JsonNode data, String channelId) {
    Map<String, String> queryParams = getJsonNodeAsMapWithRedisVars(data.get("queryParams"), channelId);
    log.debug("Resolved query params: {}", queryParams);
    return queryParams;
  }

  private Map<String, String> getBodyParams(JsonNode data, String channelId) {
    Map<String, String> bodyParams = getJsonNodeAsMapWithRedisVars(data.get("body"), channelId);
    log.debug("Resolved body params: {}", bodyParams);
    return bodyParams;
  }

  private Map<String, String> getJsonNodeAsMapWithRedisVars(JsonNode node, String channelId) {
    Map<String, String> result = new HashMap<>();
    if (node != null && node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        String key = entry.getKey();
        String rawValue = entry.getValue().asText();
        String resolvedValue = variableResolver.resolve(rawValue, channelId);
        result.put(key, resolvedValue);
        log.debug("Resolved variable {} -> {}", key, resolvedValue);
      });
    }
    return result;
  }
}
