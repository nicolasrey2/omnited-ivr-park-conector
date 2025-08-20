package coop.bancocredicoop.omnited.entity;

import com.fasterxml.jackson.databind.JsonNode;

public class IVR {
  private Integer idIVR;
  private JsonNode IVRPayload;

  public IVR() {
  }

  public IVR(Integer idIVR, JsonNode IVRPayload) {
    this.idIVR = idIVR;
    this.IVRPayload = IVRPayload;
  }

  // getters and setters


  public Integer getIdIVR() {
    return idIVR;
  }

  public void setIdIVR(Integer idIVR) {
    this.idIVR = idIVR;
  }

  public JsonNode getIVRPayload() {
    return IVRPayload;
  }

  public void setIVRPayload(JsonNode IVRPayload) {
    this.IVRPayload = IVRPayload;
  }
}