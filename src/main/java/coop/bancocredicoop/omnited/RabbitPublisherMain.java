package coop.bancocredicoop.omnited;

import coop.bancocredicoop.omnited.config.MessageOut;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.InputStream;
import java.util.Scanner;

public class RabbitPublisherMain {

  public static void main(String[] args) throws Exception {
    // 🔹 Configuración de RabbitMQ
    String host = "localhost";
    int port = 5672;
    String username = "guest";
    String password = "guest";

    String exchange = "exchange.messages";
    String routingKey = "routing.key.messages.db_ivr1";

    // 🔹 Crear conexión y RabbitTemplate
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);

    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

    // 🔹 Leer el JSON crudo desde resources (payload.json)
    InputStream is = RabbitPublisherMain.class.getClassLoader().getResourceAsStream("ivr/ivr-test.json");
    if (is == null) {
      throw new IllegalStateException("No se encontró payload.json en resources");
    }
    Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
    String mensajeJson = scanner.hasNext() ? scanner.next() : "";
    scanner.close();

    // 🔹 Construir Protobuf
    MessageOut.MensajeJSON protoMsg = MessageOut.MensajeJSON.newBuilder()
        .setIdMensaje("abc-123")
        .setMensajeType("actualizarIVR")
        .setMensajeJson(mensajeJson)
        .setIdSector(10)
        .setIdUsuario(99)
        .setFechaEnvio(System.currentTimeMillis())
        .build();

    byte[] payload = protoMsg.toByteArray();

    // 🔹 Publicar en RabbitMQ
    rabbitTemplate.convertAndSend(exchange, routingKey, payload);

    System.out.println("✅ Mensaje publicado en RabbitMQ con mensajeJson tomado desde resources");
    connectionFactory.destroy();
  }
}

