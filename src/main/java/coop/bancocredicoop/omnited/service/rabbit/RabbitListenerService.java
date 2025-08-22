package coop.bancocredicoop.omnited.service.rabbit;

import java.util.HashMap;
import java.util.Map;

import coop.bancocredicoop.omnited.config.MessageOut;
import coop.bancocredicoop.omnited.handler.rabbit.IvrUpdaterHandler;
import coop.bancocredicoop.omnited.service.ivr.DiagramaStore;
import coop.bancocredicoop.omnited.utils.JsonCleaningService;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;


@Service
public class RabbitListenerService {

  private final Map<String, RabbitMessageHandler> handlers = new HashMap<>();

  /**
   * Servicios que se registran en la carga de la aplicación.
   *
   * @param jsonCleaningService
   */
  public RabbitListenerService(
      JsonCleaningService jsonCleaningService,
      DiagramaStore diagramaStore
  ) {

    // TODO definir bien estas keys

    // para actualizar el diagrama del ivr
    handlers.put("actualizarIVR", new IvrUpdaterHandler(jsonCleaningService, diagramaStore));

  }

  /**
   * Colas registradas en Rabbit. Esta aplicación solo escuchará mensajes
   * provinientes de esas colas.
   *
   */
  @RabbitListener(queues = {
      "#{@environment.getProperty('spring.rabbitmq.colaDB_IVR1')}"
  })
  public void receiveMessage(MessageOut.MensajeJSON message) {

    try {

      String idMensaje = message.getIdMensaje();
      String mensajeType = message.getMensajeType();
      String mensajeJson = message.getMensajeJson();
      long fechaEnvio = message.getFechaEnvio();

      // Identificar y procesar el mensaje según su tipo
      RabbitMessageHandler handler = handlers.get(mensajeType);

      if (handler != null) {
        handler.handle(idMensaje, mensajeJson, fechaEnvio);
      } else {
        System.err.println("No handler found for type: " + mensajeType);
      }
    } catch (Exception e) {
      System.err.println("Error handling message: " + e.getMessage());
    }
  }

}
