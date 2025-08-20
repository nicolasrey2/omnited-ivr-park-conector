package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;


/**
 * Métodos auxiliares para navegación y búsqueda de nodos en el JSON del bot.
 */
public final class DiagramaUtils {

    private DiagramaUtils() {
        /* no instanciable */ }

    /**
     * Encuentra el nodo inicial: 1) Busca uno de tipo "inInicio" 2) Si no
     * existe, devuelve el primer nodo que no tenga incoming edges
     */
    public static String encontrarNodoInicial(JsonNode ivrLimpio) {
        if (ivrLimpio.has("nodes")) {
            for (JsonNode nodo : ivrLimpio.get("nodes")) {
                if ("inInicio".equalsIgnoreCase(nodo.get("type").asText())) {
                    return nodo.get("id").asText();
                }
            }
            Set<String> todos = new HashSet<>();
            Set<String> targets = new HashSet<>();
            ivrLimpio.get("nodes").forEach(n -> todos.add(n.get("id").asText()));
            if (ivrLimpio.has("edges")) {
                ivrLimpio.get("edges").forEach(e -> targets.add(e.get("target").asText()));
            }
            for (String id : todos) {
                if (!targets.contains(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * Busca un nodo por su ID dentro de "nodes".
     */
    public static JsonNode encontrarNodoPorId(JsonNode ivrLimpio, String id) {
        if (!ivrLimpio.has("nodes")) {
            return null;
        }
        for (JsonNode n : ivrLimpio.get("nodes")) {
            if (n.get("id").asText().equals(id)) {
                return n;
            }
        }
        return null;
    }

    /**
     * Retorna el ID del siguiente nodo según el primer edge cuyo "source"
     * coincida con el nodo dado.
     *
     * @param ivrLimpio
     * @param nodo
     * @return
     */
    public static String obtenerTarget(JsonNode ivrLimpio, JsonNode nodo) {
        if (!ivrLimpio.has("edges")) {
            return null;
        }
        String id = nodo.get("id").asText();
        for (JsonNode e : ivrLimpio.get("edges")) {
            if (id.equals(e.get("source").asText())) {
                return e.get("target").asText();
            }
        }
        return null;
    }

    /**
     * Busca el target asociado a un sourceHandle específico.
     *
     * @param ivrLimpio
     * @param nodo
     * @param handle
     * @return
     */
    public static String buscarEdgePorHandle(JsonNode ivrLimpio, JsonNode nodo, String handle) {
        if (!ivrLimpio.has("edges")) {
            return null;
        }
        String id = nodo.get("id").asText();
        for (JsonNode e : ivrLimpio.get("edges")) {
            JsonNode sh = e.get("sourceHandle");
            if (id.equals(e.get("source").asText())
                && sh != null
                && handle.equalsIgnoreCase(sh.asText())) {
                return e.get("target").asText();
            }
        }
        return null;
    }

    /**
     * Búsqueda de siguiente nodo para:
     * Lista Interactiva
     */
    public static String listaInteractivaSiguienteNodo(JsonNode ivrLimpio, String seleccion, String nodoId) {
        for (JsonNode edge : ivrLimpio.get("edges")) {
            if (edge.get("source").asText().equals(nodoId) && edge.get("sourceHandle").asText().equals(seleccion)) {
                return edge.get("target").asText();
            }
        }
        // Si no se encuentra edge, retorna null (fin de flujo)
        return null;
    }

    /**
     * Valida formatos simples, p.ej. Integer vs texto no vacío.
     */
    public static boolean validarFormato(String texto, String dtype) {
        if ("Integer".equalsIgnoreCase(dtype)) {
            return texto != null && texto.matches("\\d+");
        }
        return texto != null && !texto.trim().isEmpty();
    }
}
