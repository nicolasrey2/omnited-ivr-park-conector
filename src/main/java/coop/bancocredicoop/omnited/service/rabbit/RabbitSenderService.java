package coop.bancocredicoop.omnited.service.rabbit;

import coop.bancocredicoop.omnited.config.MessageOut.MensajeJSON;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitSenderService {

    @Value("${spring.rabbitmq.routing-key}.dbu")
    private String routingKeyOutUnicast;
    
    @Value("${spring.rabbitmq.routing-key}.dbm")
    private String routingKeyOutMulticast;
    
    @Value("${spring.rabbitmq.routing-key}.dbstream")
    private String routingKeyOutUnicastToStream;

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
    public void sendMessageUnicast(MensajeJSON message) {
        
        rabbitTemplate.convertAndSend(exchangeName, routingKeyOutUnicast, message);
    }
    
    /**
     * Enviar un mensaje a RabbitMQ.
     *
     * @param message
     */
    public void sendMessageUnicastToStream(MensajeJSON message) {
        
        rabbitTemplate.convertAndSend(exchangeName, routingKeyOutUnicastToStream, message);
    }
    
    /**
     * Enviar un mensaje a RabbitMQ.
     *
     * @param message
     */
    public void sendMessageMulticast(MensajeJSON message) {
        
        rabbitTemplate.convertAndSend(exchangeName, routingKeyOutMulticast, message);
    }

}