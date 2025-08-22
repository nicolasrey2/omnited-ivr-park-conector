package coop.bancocredicoop.omnited.service.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class PiperTtsService {

  @Value("${piper.tts.endpoint}")
  private String uriEndpointTTS;

  @Value("${piper.audio.output.path}")
  private String audioOutputPath;

  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Genera un archivo de audio a partir del texto recibido.
   * <p>
   * El texto es enviado al microservicio de TTS, que devuelve el nombre de archivo
   * del audio generado en formato WAV. Este método devuelve el path completo
   * del archivo de audio en el sistema de archivos.
   * </p>
   *
   * @param text Texto a convertir en audio.
   * @return Ruta completa del archivo WAV generado. (o filename)
   * @throws RuntimeException Si ocurre un error en la comunicación con el microservicio de TTS
   *                          o si la respuesta es inválida.
   */
  public String textToSpeech(String text) {
    // JSON body
    String requestJson = String.format("{\"texto\": \"%s\"}", text);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> httpEntity = new HttpEntity<>(requestJson, headers);

    // Hacemos el POST
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        uriEndpointTTS,
        HttpMethod.POST,
        httpEntity,
        JsonNode.class
    );

    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
      throw new RuntimeException("Error en TTS: " + response.getStatusCode());
    }

    String fileName = response.getBody().get("archivo").asText();
    //return audioOutputPath + "/" + fileName;
    return fileName;
  }
}
