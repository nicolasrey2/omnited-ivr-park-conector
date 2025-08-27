package coop.bancocredicoop.omnited.service.tts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.messages.AudioForChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PiperTtsService {
  private static final Logger log = LoggerFactory.getLogger(PiperTtsService.class);


  @Value("${piper.tts.endpoint}")
  private String uriEndpointTTS;

  private final RestTemplate restTemplate;

  public PiperTtsService() {
    this.restTemplate = new RestTemplate();
  }

  /**
   * Genera un archivo de audio a partir del texto recibido.
   *
   * @param textToSpeech Texto a convertir en audio.
   * @return Ruta completa del archivo WAV generado.
   */
  public String textToSpeech(String textToSpeech) {

    Map<String, Object> payload = new HashMap<>();
    payload.put("texto", textToSpeech);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(uriEndpointTTS, request, String.class);

    // Parseamos JSON y devolvemos solo el valor de "archivo"
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = null;
    try {
      json = mapper.readTree(response.getBody());
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }

    return json.get("archivo").asText();
  }
}
