package coop.bancocredicoop.omnited.service.rabbit;

import java.util.HashMap;
import java.util.Map;

import coop.bancocredicoop.omnited.config.MessageOut;
import coop.bancocredicoop.omnited.handler.rabbit.IVRHandler;
import coop.bancocredicoop.omnited.service.ivr.DiagramaService;
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
      DiagramaService diagramaService
  ) {
    // TODO definir bien esta key
    handlers.put("ivr", new IVRHandler(jsonCleaningService, diagramaService));
  }

  /**
   * Colas registradas en Rabbit. Esta aplicación solo escuchará mensajes
   * provinientes de esas colas.
   *
   */
  @RabbitListener(queues = {
      "#{@environment.getProperty('spring.rabbitmq.colaDB_WA')}"
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
