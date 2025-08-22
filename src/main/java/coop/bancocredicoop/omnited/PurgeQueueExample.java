package coop.bancocredicoop.omnited;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

public class PurgeQueueExample {
  public static void main(String[] args) {
    CachingConnectionFactory connectionFactory =
        new CachingConnectionFactory("localhost", 5672);
    connectionFactory.setUsername("guest");
    connectionFactory.setPassword("guest");

    RabbitAdmin admin = new RabbitAdmin(connectionFactory);

    // 🔹 Purga la cola
    admin.purgeQueue("cola.DB_IVR1", false);

    System.out.println("✅ Cola vaciada");
    connectionFactory.destroy();
  }
}

