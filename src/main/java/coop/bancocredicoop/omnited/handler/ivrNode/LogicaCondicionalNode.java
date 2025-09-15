package coop.bancocredicoop.omnited.handler.ivrNode;

import com.fasterxml.jackson.databind.JsonNode;
import coop.bancocredicoop.omnited.service.ivr.NodeHandler;
import coop.bancocredicoop.omnited.service.ivr.diagram.DiagramaUtils;
import coop.bancocredicoop.omnited.service.redis.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Component("logicaCondicional")
public class LogicaCondicionalNode implements NodeHandler {
  private static final Logger log = LoggerFactory.getLogger(LogicaCondicionalNode.class);

  private final VariableResolver variableResolver;

  public LogicaCondicionalNode(VariableResolver variableResolver) {
    this.variableResolver = variableResolver;
  }

  @Override
  public String handle(JsonNode ivr, JsonNode nodo, String channelId, String textoUsuario) {
    JsonNode dataNode = nodo.get("data");
    if (dataNode == null || !dataNode.has("condicional")) {
      log.error("[{}] nodo IVR sin 'condicional'", channelId);
      return null;
    }

    String rawCondition = dataNode.get("condicional").asText();
    String condition = variableResolver.resolve(rawCondition, channelId);
    log.info("[{}] estructura condicional a evaluarse: {}", channelId, condition);

    ExpressionParser parser = new SpelExpressionParser();
    try {
      Expression expression = parser.parseExpression(condition);

      Boolean resultValue = expression.getValue(Boolean.class);
      if (resultValue == null) {
        log.error("[{}] la expresion no devolvio un booleano: {}", channelId, condition);
        return null;
      }

      return resultValue
          ? DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "TRUE")
          : DiagramaUtils.buscarEdgePorHandle(ivr, nodo, "FALSE");

    } catch (ParseException | EvaluationException e) {
      log.error("[{}] error evaluando la expresion: {}. Error: {}", channelId, condition, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("[{}] error inesperado evaluando la expresion: {}. Error: {}", channelId, condition, e.getMessage(), e);
      return null;
    }
  }
}
