package coop.bancocredicoop.omnited.service.rabbit;

public interface RabbitMessageHandler {
  void handle(String id, String jsonPayload, long fechaEnvioLocal) throws Exception;
}
