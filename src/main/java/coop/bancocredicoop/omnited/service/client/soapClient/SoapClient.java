package coop.bancocredicoop.omnited.service.client.soapClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Service
public class SoapClient {

  private static final Logger log = LoggerFactory.getLogger(SoapClient.class);

  private final RestTemplate restTemplate;

  public SoapClient() {
    this.restTemplate = new RestTemplate();
    // Opcional: setErrorHandler similar a RestClient
    // this.restTemplate.setErrorHandler(new NoThrowErrorHandler());
  }

  /**
   * Envía un request SOAP y devuelve ResponseEntity<String> igual que RestClient.
   */
  public ResponseEntity<String> send(String url, String envelope, Map<String, String> headers) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL no puede ser null o vacío");
    }

    try {
      URI uri = URI.create(url);

      // Headers
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentType(MediaType.TEXT_XML);
      if (headers != null) {
        headers.forEach(httpHeaders::add);
      }

      HttpEntity<String> entity = new HttpEntity<>(envelope, httpHeaders);

      log.info("Enviando SOAP a {} con headers {} y envelope {}", uri, headers, envelope);

      ResponseEntity<String> response = restTemplate.exchange(
          uri,
          HttpMethod.POST,
          entity,
          String.class
      );

      log.info("Respuesta SOAP status {}: {}", response.getStatusCode(), response.getBody());

      return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

    } catch (Exception e) {
      log.error("Error enviando request SOAP a {}: {}", url, e.getMessage(), e);

      return ResponseEntity
          .status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("{\"error\": \"No se pudo conectar a " + url + " - " + e.getMessage() + "\"}");
    }
  }
}
