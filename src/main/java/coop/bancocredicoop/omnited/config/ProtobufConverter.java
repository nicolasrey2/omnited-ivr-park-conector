package coop.bancocredicoop.omnited.config;

import coop.bancocredicoop.omnited.config.MessageOut.MensajeJSON;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class ProtobufConverter implements MessageConverter {

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) {
        if (object instanceof MensajeJSON) {
            byte[] bytes = ((MensajeJSON) object).toByteArray();
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
            return new Message(bytes, messageProperties);
        }
        throw new IllegalArgumentException("Unsupported message type");
    }

    @Override
    public Object fromMessage(Message message) {
        try {
            return MensajeJSON.parseFrom(message.getBody());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Error deserializing Protobuf message", e);
        }
    }
}
