package coop.bancocredicoop.omnited.exposition;

import java.time.Instant;
import java.util.UUID;

public class IvrSession {
  private UUID id;                   // uuid
  private String ariChannelId;       // varchar(64), NOT NULL
  private String astUniqueid;        // varchar(64), NULL
  private CallDirection direction;   // enum, NOT NULL
  private String ani;                // varchar(40), NULL
  private String dnis;               // varchar(40), NULL
  private String context;            // varchar(80), NULL
  private String entrypointNode;     // varchar(100), NULL
  private Instant startedAt;         // timestamptz, NOT NULL
  private Instant endedAt;           // timestamptz, NULL
  private String hangupCause;        // varchar(60), NULL
  private Long tenantId;             // int8, NULL
  private Long deptId;               // int8, NULL
  private Long sectorId;             // int8, NULL

  // Enum para direction
  public enum CallDirection {
    INCOMING,
    OUTGOING
  }

  public UUID getId() {
    return id;
  }

  public IvrSession setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getAriChannelId() {
    return ariChannelId;
  }

  public IvrSession setAriChannelId(String ariChannelId) {
    this.ariChannelId = ariChannelId;
    return this;
  }

  public String getAstUniqueid() {
    return astUniqueid;
  }

  public IvrSession setAstUniqueid(String astUniqueid) {
    this.astUniqueid = astUniqueid;
    return this;
  }

  public CallDirection getDirection() {
    return direction;
  }

  public IvrSession setDirection(CallDirection direction) {
    this.direction = direction;
    return this;
  }

  public String getAni() {
    return ani;
  }

  public IvrSession setAni(String ani) {
    this.ani = ani;
    return this;
  }

  public String getDnis() {
    return dnis;
  }

  public IvrSession setDnis(String dnis) {
    this.dnis = dnis;
    return this;
  }

  public String getContext() {
    return context;
  }

  public IvrSession setContext(String context) {
    this.context = context;
    return this;
  }

  public String getEntrypointNode() {
    return entrypointNode;
  }

  public IvrSession setEntrypointNode(String entrypointNode) {
    this.entrypointNode = entrypointNode;
    return this;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public IvrSession setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public IvrSession setEndedAt(Instant endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  public String getHangupCause() {
    return hangupCause;
  }

  public IvrSession setHangupCause(String hangupCause) {
    this.hangupCause = hangupCause;
    return this;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public IvrSession setTenantId(Long tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public Long getDeptId() {
    return deptId;
  }

  public IvrSession setDeptId(Long deptId) {
    this.deptId = deptId;
    return this;
  }

  public Long getSectorId() {
    return sectorId;
  }

  public IvrSession setSectorId(Long sectorId) {
    this.sectorId = sectorId;
    return this;
  }
}