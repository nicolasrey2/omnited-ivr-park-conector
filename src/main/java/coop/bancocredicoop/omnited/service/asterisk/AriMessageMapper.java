package coop.bancocredicoop.omnited.service.asterisk;

import ch.loway.oss.ari4java.generated.models.Message;
import ch.loway.oss.ari4java.generated.models.StasisStart;
import coop.bancocredicoop.omnited.service.ivr.IvrService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AriMessageMapper {
  private IvrService ivrService;

  public AriMessageMapper(@Lazy IvrService ivrService) {
    this.ivrService = ivrService;
  }

  public void mapMessage(Message message) {
    if(message == null || message.getType() == null) {
      return;
    }
    switch (message.getType()) {
      case "StasisStart":
        handleStasisStart((StasisStart) message);
        break;

      case "StasisEnd":
        handleStasisEnd();
        break;

      // Agregá más casos según los eventos que uses
      default:
        System.out.println("Evento ARI no manejado: " + message.getType());

    }
  }
  private void handleStasisStart(StasisStart message) {
    ivrService.startFlow(message.getChannel());
  }

  private void handleStasisEnd() {

  }


}
