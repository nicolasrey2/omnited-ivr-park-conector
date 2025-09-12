package coop.bancocredicoop.omnited.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ServiceInvoker {
  ResponseEntity<String> invoke(String channelId, JsonNode data, String url);
}
