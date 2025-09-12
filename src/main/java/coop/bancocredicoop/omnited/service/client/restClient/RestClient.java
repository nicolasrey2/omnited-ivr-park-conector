package coop.bancocredicoop.omnited.service.client.restClient;

import coop.bancocredicoop.omnited.exception.NoThrowErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Map;

@Service
public class RestClient {
  private static final Logger log = LoggerFactory.getLogger(RestClient.class);

  private final RestTemplate restTemplate;

  public RestClient() {
    this.restTemplate = new RestTemplate();
    this.restTemplate.setErrorHandler(new NoThrowErrorHandler());
  }

  public ResponseEntity<String> send(String url, String method,
                                     Map<String, String> queryParams,
                                     Map<String, String> headersMap,
                                     Map<String, String> bodyParams) {

    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL no puede ser null o vacío");
    }
    if (method == null || method.isEmpty()) {
      throw new IllegalArgumentException("Método HTTP no puede ser null o vacío");
    }

    try {
      // 1️⃣ Construir URI con variables de ruta y query params
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

      if (queryParams != null) {
        queryParams.forEach(builder::queryParam);
      }

      URI uri = builder.buildAndExpand().toUri();

      // 2️⃣ Crear headers
      HttpHeaders headers = new HttpHeaders();
      if (headersMap != null) {
        headersMap.forEach(headers::add);
      }

      // 3️⃣ Crear HttpEntity según el métodohttp
      HttpMethod httpMethod = HttpMethod.resolve(method.toUpperCase());
      if (httpMethod == null) {
        throw new IllegalArgumentException("Método HTTP no soportado: " + method);
      }

      HttpEntity<String> entity = (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE)
          ? new HttpEntity<>(headers)
          : new HttpEntity<>(bodyParams != null ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(bodyParams) : null, headers);

      log.info("Ejecutando {} a {} con queryParams {} y body {}", httpMethod, uri, queryParams, bodyParams);

      // 4️⃣ Ejecutar request
      return restTemplate.exchange(uri, httpMethod, entity, String.class);

    } catch (Exception e) {
      log.error("Error ejecutando request a {}: {}", url, e.getMessage(), e);

      // Devolver un ResponseEntity "falso" con status 503 (Service Unavailable)
      return ResponseEntity
          .status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("{\"error\": \"No se pudo conectar a " + url + " - " + e.getMessage() + "\"}");
    }
  }

}
