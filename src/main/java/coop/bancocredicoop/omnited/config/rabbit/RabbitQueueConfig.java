package coop.bancocredicoop.omnited.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitQueueConfig {

    @Value("${spring.rabbitmq.exchange}")
    private String exchangeName;
    
    @Value("${spring.rabbitmq.routing-key}")
    private String routingKey;
    
    @Value("${spring.rabbitmq.colaDB_WA}")
    private String colaEntranteDB_WA;

    /**
     * Bean para el envio Broadcast
     * @return 
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName);
    }
    
    @Bean
    public Queue colaEntranteDB_WA() {
        return new Queue(colaEntranteDB_WA, true); // Cola de entrada DB (duradera)
    }

    /**
     * Binding de la cola de entrada WA al exchange con una clave de
     * enrutamiento específica.
     *
     * @param colaEntranteDB_WA
     * @param exchange
     * @return
     */
    @Bean
    public Binding bindingColaEntranteDB_WA(Queue colaEntranteDB_WA, TopicExchange exchange) {
        return BindingBuilder.bind(colaEntranteDB_WA).to(exchange).with(routingKey + ".db_wa");
    }
}