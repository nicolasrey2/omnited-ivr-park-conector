package coop.bancocredicoop.omnited.dto;

import java.util.List;

public class CandidatoResponse {
  private List<CandidatoDto> candidatos;

  public List<CandidatoDto> getCandidatos() {
    return candidatos;
  }

  public void setCandidatos(List<CandidatoDto> candidatos) {
    this.candidatos = candidatos;
  }
}
