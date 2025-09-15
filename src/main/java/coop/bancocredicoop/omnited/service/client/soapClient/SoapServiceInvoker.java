package coop.bancocredicoop.omnited.service.client.soapClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import coop.bancocredicoop.omnited.service.client.ServiceInvoker;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

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
      // 1️⃣ Resolver envelope dinámicamente
      String rawEnvelope = data.get("body").asText();
      String envelope = variableResolver.resolve(rawEnvelope, channelId);

      // 2️⃣ Construir headers HTTP
      HttpHeaders headers = buildHttpHeaders(data);

      log.info("SOAP invoke - URL: {}", url);
      log.debug("Envelope enviado: {}", envelope);
      log.debug("Headers: {}", headers);

      // 3️⃣ Llamada al cliente SOAP
      ResponseEntity<String> soapResponse = soapClient.send(url, envelope, headers);

      String soapBody = soapResponse.getBody();
      if (soapBody == null) {
        log.warn("Respuesta vacía desde el servicio SOAP");
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{}");
      }

      // 4️⃣ Manejo de SOAP Fault
      if (soapBody.contains("<Fault>")) {
        log.error("SOAP Fault detectado: {}", soapBody);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(soapBody);
      }

      // 5️⃣ Convertir XML → JSON robusto con Jackson
      XmlMapper xmlMapper = new XmlMapper();

      log.info("soapBody: {}", soapBody);

      JsonNode xmlNode = xmlMapper.readTree(soapBody.getBytes(StandardCharsets.UTF_8));

      String jsonResponse = xmlNode.toPrettyString();
      return ResponseEntity.ok(jsonResponse);

    } catch (HttpServerErrorException e) {
      log.error("SOAP server error: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getResponseBodyAsString());
    } catch (ResourceAccessException e) {
      log.error("SOAP timeout: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Timeout: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error inesperado en SOAP invoke: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
    }
  }

  private HttpHeaders buildHttpHeaders(JsonNode data) {
    HttpHeaders headers = new HttpHeaders();
    // headers.setContentType(MediaType.TEXT_XML);

    JsonNode headersNode = data.get("headers");
    if (headersNode != null && headersNode.isObject()) {
      headersNode.fields().forEachRemaining(entry -> {
        headers.add(entry.getKey(), entry.getValue().asText());
        log.debug("Header agregado: {}={}", entry.getKey(), entry.getValue().asText());
      });
    }
    return headers;
  }
}
