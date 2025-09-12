package coop.bancocredicoop.omnited.handler.ivrNode;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.client.ServiceInvoker;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import coop.bancocredicoop.omnited.service.redis.RetryService;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.*;


@Component("servCliente")
public class ServiceClientNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(ServiceClientNode.class);
  private final int TTL_REINTENTOS = 35;
  private final int TTL_VARIABLES = 300;

  private final RedisService redisService;
  private final VariableResolver variableResolver;
  private final RetryService retryService;
  private final Map<String, ServiceInvoker> serviceInvokers;

  public ServiceClientNode(RedisService redisService, VariableResolver variableResolver,
                           RetryService retryService,
                           Map<String, ServiceInvoker> serviceInvokers) {
    this.redisService = redisService;
    this.variableResolver = variableResolver;
    this.retryService = retryService;
    this.serviceInvokers = serviceInvokers;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {
    JsonNode data = node.get("data");
    if (data == null) {
      log.error("No hay data en el nodo");
      return "";
    }

    String type = data.get("type").asText();
    String unsolvedUrl = data.has("url") ? data.get("url").asText() : null;
    String urlResolved = variableResolver.resolve(unsolvedUrl, channelId);

    // Llamar a estrategia correspondiente
    ResponseEntity<String> response = serviceInvokers.get(type).invoke(channelId, data, urlResolved);
    log.info("Respuesta del endpoint {}: {}", urlResolved, response.getBody());

    List<Integer> correctStates = getCorrectStates(data);
    if (! correctStates.contains(response.getStatusCodeValue())) { // error
      log.info("Error al consultar el endpoint {}: cod: {}; body: {}", urlResolved,
          response.getStatusCodeValue(), response.getBody());
      return handleError(channelId, unsolvedUrl, ivr, node);
    }

    retryService.clearRetries(channelId);
    setVars(channelId, response.getBody(), data.get("variablesASetear"));
    return DiagramaUtils.buscarEdgePorHandle(ivr, node, "OK");
  }

  private String handleError(String channelId, String unsolvedUrl, JsonNode ivr, JsonNode node) {
    for (String variable : variableResolver.getVariables(unsolvedUrl)) {
      redisService.delete(variable + ":" + channelId);
      log.info("Borrada variable {} de Redis para canal {}", variable, channelId);
    }

    retryService.handleRetries(channelId, TTL_REINTENTOS);
    String nodoError = DiagramaUtils.buscarEdgePorHandle(ivr, node, "ERROR");
    if (nodoError == null) {
      log.error("No se encontro nodo con handler error");
      nodoError = DiagramaUtils.encontrarHangup(ivr);
    }
    return nodoError;
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
          redisService.set(nombreVariable + ":" + channelId, valor.toString(), TTL_VARIABLES);
          log.info("Se seteó la variable: {}, con el valor: {}", nombreVariable, valor);
        } else {
          log.error("No se encontró valor para la variable {} con JSONPath {}", nombreVariable, jsonPath);
        }

      } catch (Exception e) {
        log.error("Error resolviendo JSONPath {} para la variable {}", jsonPath, nombreVariable, e);
      }
    }
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
