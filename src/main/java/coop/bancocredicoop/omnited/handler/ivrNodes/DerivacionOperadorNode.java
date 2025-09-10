package coop.bancocredicoop.omnited.handler.ivrNodes;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.handler.ivrNodes.base.AbstractSalidaNode;
import coop.bancocredicoop.omnited.messages.MessageService;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("derivacionOperador")
public class DerivacionOperadorNode extends AbstractSalidaNode {
  private final static Logger log = LoggerFactory.getLogger(DerivacionOperadorNode.class);

  public DerivacionOperadorNode(MessageService messageService, VariableResolver variableResolver) {
    super(messageService, variableResolver);
  }

  @Override
  protected void preHandleTasks(JsonNode ivr, JsonNode node, String channelId, String textoUsuario) {

  }

  @Override
  protected String primerOutput(JsonNode node, String channelId) {
    String rawText = node.get("data").get("texto").asText();
    String texto = variableResolver.resolve(rawText, channelId);
    log.info("Texto: {}", texto);
    return texto;
  }

  @Override
  protected void logicaAnteDtmfMientrasPlayback(JsonNode node, String channelId, String digit) {

  }

  @Override
  protected String logicaAnteDtmfDespuesDePlayback(JsonNode ivr, JsonNode node, String channelId, String digit) {
    return null;
  }

  @Override
  protected String onPlaybackFinished(JsonNode ivr, JsonNode node, String channelId) {
    return null;
  }
}
