package coop.bancocredicoop.omnited.service.ivr.diagram;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.message.CanalMensajeria;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.redis.RedisService;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DiagramaProcessor {
    private static final Logger log = LoggerFactory.getLogger(DiagramaProcessor.class);

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
     * @param channelId          Identificador del usuario
     * @param textoUsuario  Texto enviado por el usuario
     */
    public void procesarMensaje(JsonNode ivrLimpio, String channelId, String textoUsuario) {
        String posicionKey = "posicion:" + channelId;
        String sessionKey = "sessionStarted:" + channelId;

        // Recuperar posición actual en el flujo
        String nodoActualId = redisService.get(posicionKey);

        // Si no hay posición, puede ser inicio o sesión expirada
        if (nodoActualId == null) {
            if (redisService.get(sessionKey) != null) {
                canalMensajeria.enviarMensaje(channelId, "Se cerró la sesión. Gracias por comunicarse.");
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
            log.error("[{}] Nodo no encontrado: id={}", channelId, nodoActualId);
            return;
        }

        // Delegar la lógica al handler de este tipo de nodo
        String tipo = nodoActual.get("type").asText();
        NodeHandler handler = handlers.get(tipo);
        if (handler == null) {
            log.warn("[{}] Handler no registrado para tipo={}", channelId, tipo);
            return;
        }


        // Ejecutar handler y obtener el siguiente nodo
        String siguienteId = handler.handle(ivrLimpio, nodoActual, channelId, textoUsuario);

        // Si el handler devuelve un siguiente nodo, actualizamos Redis y continuamos
        if (siguienteId != null) {
            redisService.set(posicionKey, siguienteId, TTL_SEC);
            procesarMensaje(ivrLimpio, channelId, "");
        }
    }


}