package coop.bancocredicoop.omnited.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import coop.bancocredicoop.omnited.config.MessageOut;
import coop.bancocredicoop.omnited.service.rabbit.RabbitSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageToRabbit {
  private static final Logger log = LoggerFactory.getLogger(MessageToRabbit.class);
  private final ObjectMapper objectMapper;
  private final RabbitSenderService rabbitSenderService;

  public MessageToRabbit(ObjectMapper objectMapper, RabbitSenderService rabbitSenderService) {
    this.objectMapper = objectMapper;
    this.rabbitSenderService = rabbitSenderService;
  }

  public void processMessage(String idMensaje, String mensajeType, String mensajeJson) {

    try {
      MessageOut.MensajeJSON message = MessageOut.MensajeJSON.newBuilder()
          .setIdMensaje(idMensaje)
          .setMensajeType(mensajeType)
          .setMensajeJson(mensajeJson)
          .build();

      // Usar el servicio RabbitSenderService para enviar el mensaje
      rabbitSenderService.sendMessage(message);

    } catch (Exception e) {
      log.error("Error en el processMessage {}", e.getMessage());
    }
  }

}
