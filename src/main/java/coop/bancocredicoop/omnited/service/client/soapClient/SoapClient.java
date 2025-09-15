package coop.bancocredicoop.omnited.service.client.soapClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class SoapClient {

  private static final Logger log = LoggerFactory.getLogger(SoapClient.class);
  private final RestTemplate restTemplate;

  public SoapClient() {
    this.restTemplate = new RestTemplate();
    // Opcional: setErrorHandler si querés manejar errores manualmente
    // this.restTemplate.setErrorHandler(new NoThrowErrorHandler());
  }

  /**
   * Envía un request SOAP y devuelve ResponseEntity<String> igual que RestTemplate.
   *
   * @param url     URL del servicio SOAP
   * @param envelope Envelope SOAP como String
   * @param headers HttpHeaders opcionales (pueden incluir SOAPAction, Content-Type, etc.)
   */
  public ResponseEntity<String> send(String url, String envelope, HttpHeaders headers) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL no puede ser null o vacío");
    }

    try {
      URI uri = URI.create(url);


      if (headers == null) {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
      } else if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
        headers.setContentType(MediaType.TEXT_XML);
      }

      HttpEntity<String> entity = new HttpEntity<>(envelope, headers);

      log.info("Enviando SOAP a {} con headers {}", uri, headers);
      log.debug("Envelope enviado: {}", envelope);

      ResponseEntity<String> response = restTemplate.exchange(
          uri,
          HttpMethod.POST,
          entity,
          String.class
      );

      log.info("Respuesta SOAP status {}", response.getStatusCode());
      log.debug("Respuesta SOAP body: {}", response.getBody());

      return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

    } catch (Exception e) {
      log.error("Error enviando request SOAP a {}: {}", url, e.getMessage(), e);
      return ResponseEntity
          .status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("{\"error\": \"No se pudo conectar a " + url + " - " + e.getMessage() + "\"}");
    }
  }
}
