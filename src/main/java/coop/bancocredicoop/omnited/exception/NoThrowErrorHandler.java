package coop.bancocredicoop.omnited.exception;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import java.io.IOException;

public class NoThrowErrorHandler extends DefaultResponseErrorHandler {
  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    // NO lanzar excepción, solo dejar que la respuesta pase
  }
}
