package coop.bancocredicoop.omnited.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JsonCleaningService {

  private static final Set<String> KEYS_TO_REMOVE = Set.of(
      "measured", "position", "dragging", "selected", "viewport", "options", "style"
  );

  /**
   * Limpia recursivamente un JsonNode eliminando las claves "measured",
   * "position", "dragging" y "selected".
   *
   * @param root el JsonNode original
   * @return el mismo JsonNode, modificado in-place, sin esas propiedades
   */
  public JsonNode clean(JsonNode root) {
    removeKeys(root, KEYS_TO_REMOVE);
    return root;
  }

  private void removeKeys(JsonNode node, Set<String> keysToRemove) {
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      // Eliminamos las claves en este nivel
      keysToRemove.forEach(obj::remove);
      // Recorremos los hijos restantes
      obj.fields().forEachRemaining(entry -> removeKeys(entry.getValue(), keysToRemove));
    } else if (node.isArray()) {
      // Si es array, limpiamos cada elemento
      for (JsonNode element : node) {
        removeKeys(element, keysToRemove);
      }
    }
    // Nodos primitivos: no hay nada que hacer
  }
}
