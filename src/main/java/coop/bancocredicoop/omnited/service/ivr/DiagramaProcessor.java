package coop.bancocredicoop.omnited.service.ivr;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.messages.CanalMensajeria;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.ext.ParamConverter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DiagramaProcessor {

    private static final Logger LOGGER = Logger.getLogger(DiagramaProcessor.class.getName());
    private static final long TTL_SEC = 300; // segundos (5 minutos)

    private final RedisService redisService;
    private final CanalMensajeria canalMensajeria;
    private final Map<String, NodeHandler> handlers;

    public DiagramaProcessor(
            RedisService redisService,
            CanalMensajeria canalMensajeria,
            Map<String, NodeHandler> handlers
    ) {
        this.redisService = redisService;
        this.canalMensajeria = canalMensajeria;
        this.handlers = handlers;
    }

    /**
     * Procesa un mensaje entrante para un diagrama dado.
     * @param ivrLimpio     JSON completo del ivr (nodes y edges)
     * @param from          Identificador del usuario
     * @param textoUsuario  Texto enviado por el usuario
     */
    public void procesarMensaje(JsonNode ivrLimpio, String from, String textoUsuario) {
        String posicionKey = "posicion:" + from;
        String sessionKey = "sessionStarted:" + from;

        // Recuperar posición actual en el flujo
        String nodoActualId = redisService.get(posicionKey);

        // Si no hay posición, puede ser inicio o sesión expirada
        if (nodoActualId == null) {
            if (redisService.get(sessionKey) != null) {
                canalMensajeria.enviarMensaje(from, "Se cerró la sesión. Gracias por comunicarse.");
                redisService.delete(sessionKey);
                return;
            }
            // Iniciar nueva sesión
            redisService.set(sessionKey, "true");
            nodoActualId = DiagramaUtils.encontrarNodoInicial(ivrLimpio);
        }

        // Obtener nodo actual
        JsonNode nodoActual = DiagramaUtils.encontrarNodoPorId(ivrLimpio, nodoActualId);
        if (nodoActual == null) {
            LOGGER.log(Level.SEVERE, "[" + from + "] Nodo no encontrado: id=" + nodoActualId);
            return;
        }

        // Delegar la lógica al handler de este tipo de nodo
        String tipo = nodoActual.get("type").asText();
        NodeHandler handler = handlers.get(tipo);
        if (handler == null) {
            LOGGER.log(Level.WARNING, "[" + from + "] Handler no registrado para tipo=" + tipo);
            return;
        }

        // Ejecutar handler y obtener el siguiente nodo
        String siguienteId = handler.handle(ivrLimpio, nodoActual, from, textoUsuario);

        // Si el handler devuelve un siguiente nodo, actualizamos Redis y continuamos
        if (siguienteId != null) {
            redisService.set(posicionKey, siguienteId, TTL_SEC);
            procesarMensaje(ivrLimpio, from, "");
        }
    }
}