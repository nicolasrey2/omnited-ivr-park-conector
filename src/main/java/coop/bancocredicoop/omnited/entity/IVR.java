package coop.bancocredicoop.omnited.entity;

import com.fasterxml.jackson.databind.JsonNode;

public class IVR {
  private Integer idIVR;
  private JsonNode ivrPayload;

  public IVR() {
  }

  public IVR(Integer idIVR, JsonNode ivrPayload) {
    this.idIVR = idIVR;
    this.ivrPayload = ivrPayload;
  }

  // getters and setters


  public Integer getIdIVR() {
    return idIVR;
  }

  public void setIdIVR(Integer idIVR) {
    this.idIVR = idIVR;
  }

  public JsonNode getIvrPayload() {
    return ivrPayload;
  }

  public void setIvrPayload(JsonNode ivrPayload) {
    this.ivrPayload = ivrPayload;
  }
}