package coop.bancocredicoop.omnited.service.client.soapClient;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.client.ServiceInvoker;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

@Component("SOAP")
public class SoapServiceInvoker implements ServiceInvoker {
  private static final Logger log = LoggerFactory.getLogger(SoapServiceInvoker.class);

  private final SoapClient soapClient;
  private final VariableResolver variableResolver;

  public SoapServiceInvoker(SoapClient soapClient, VariableResolver variableResolver) {
    this.soapClient = soapClient;
    this.variableResolver = variableResolver;
  }

  @Override
  public ResponseEntity<String> invoke(String channelId, JsonNode data, String url) {
    try {
      String rawEnvelope = data.get("body").asText();
      String envelope = variableResolver.resolve(rawEnvelope, channelId);
      Map<String,String> headers = getHeaders(data); // opcional

      log.info("SOAP invoke - URL: {}, Envelope: {}, Headers: {}", url, envelope, headers);

      // 1️⃣ Llamada al SoapClient que devuelve ResponseEntity
      ResponseEntity<String> soapResponseEntity = soapClient.send(url, envelope, headers);

      String soapBody = soapResponseEntity.getBody();

      // 2️⃣ Convertir XML a JSON
      JSONObject jsonObj = org.json.XML.toJSONObject(soapBody);

      if (jsonObj.has("Fault")) {
        log.error("SOAP Fault detected: {}", jsonObj.get("Fault"));
        return ResponseEntity.status(503).body(jsonObj.toString());
      }

      // 3️⃣ Devolver 200 OK si no hay fault
      return ResponseEntity.ok(jsonObj.toString());

    } catch (Exception e) {
      log.error("Error enviando SOAP a {}: {}", url, e.getMessage(), e);
      JSONObject errorJson = new JSONObject();
      errorJson.put("error", e.getMessage());
      return ResponseEntity.status(503).body(errorJson.toString());
    }
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


}
