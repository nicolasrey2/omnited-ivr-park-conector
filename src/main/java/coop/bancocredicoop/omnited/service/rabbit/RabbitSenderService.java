package coop.bancocredicoop.omnited.service.rabbit;

import coop.bancocredicoop.omnited.config.MessageOut.MensajeJSON;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitSenderService {

    @Value("${spring.rabbitmq.routing-key}.ivr")
    private String routingKeyOut;

    @Value("${spring.rabbitmq.exchange}")
    private String exchangeName;

    private final RabbitTemplate rabbitTemplate;

    public RabbitSenderService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Enviar un mensaje a RabbitMQ.
     *
     * @param message
     */
    public void sendMessage(MensajeJSON message) {
        rabbitTemplate.convertAndSend(exchangeName, routingKeyOut, message);
    }


}