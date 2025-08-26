package coop.bancocredicoop.omnited.service.dni;

import coop.bancocredicoop.omnited.dto.CandidatoDto;
import coop.bancocredicoop.omnited.dto.CandidatoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;

/*
* curl -X GET "http://sesb2303ax/listacandidatos-creditos-txt/rest/api/v1/listacandidatos/F/nrodoc/24930047" \
* -H "accept: application/json"
 */
@Service
public class DniQueryClient {
  private final RestTemplate restTemplate;
  private String baseUrl;

  public DniQueryClient(@Value("${dni.api.base-url}") String baseUrl) {
    this.restTemplate = new RestTemplate();
    this.baseUrl = baseUrl;
  }

  public Optional<CandidatoResponse> consultarPorDni(String dni) {
    String url = String.format("%s/F/nrodoc/%s", baseUrl, dni);

    try {
      ResponseEntity<CandidatoResponse> response =
          restTemplate.getForEntity(url, CandidatoResponse.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        return Optional.ofNullable(response.getBody());
      }
    } catch (RestClientException e) {
      // loggear error
      System.err.println("Error al consultar DNI " + dni + ": " + e.getMessage());
    }
    return Optional.empty();
  }

  public String getFullName(String dni) {
    return this.consultarPorDni(dni)
        .map(resp -> {
          if (resp.getCandidatos() != null && !resp.getCandidatos().isEmpty()) {
            CandidatoDto c = resp.getCandidatos().get(0);
            return (c.getApellidoRazonSocial() + " " + c.getNombre()).trim();
          }
          return null; // no hay candidatos
        })
        .orElse(null); // no hubo respuesta del servicio
  }

}
