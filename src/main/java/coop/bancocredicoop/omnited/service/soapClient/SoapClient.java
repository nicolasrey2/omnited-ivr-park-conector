package coop.bancocredicoop.omnited.service.soapClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SoapClient {

  private static final Logger log = LoggerFactory.getLogger(SoapClient.class);

  public SoapClient() {

  }

  /**
   * Envía un request SOAP
   *
   * @param url      URL del endpoint SOAP
   * @param soapBody XML completo del body como String
   * @param soapAction SOAP Action (puede ser null si no aplica)
   * @param headers  Headers extra si necesitas (opcional)
   * @return String con la respuesta SOAP
   */
  public String send(String url, String soapBody, String soapAction, Map<String, String> headers) {
    return "mockString";
  }
}
