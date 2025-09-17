package coop.bancocredicoop.omnited.service.parking;

import ch.loway.oss.ari4java.generated.models.Bridge;
import coop.bancocredicoop.omnited.service.asterisk.AriConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsteriskParking {
  private static final Logger log = LoggerFactory.getLogger(AsteriskParking.class);

  private final AriConnector ariConnector;
  String parkingBridgeId = null;

  public AsteriskParking(AriConnector ariConnector) {
    this.ariConnector = ariConnector;
  }

  public void parkChannel(String channelId) {
    ariConnector.addChannelToBridge(channelId, parkingBridgeId);
    log.info("Canal {} añadido al Parking bridge {}", channelId, parkingBridgeId);
  }

  public void unparkChannel(String channelId) {
    ariConnector.removeChannelToBridge(channelId, parkingBridgeId);
    log.info("Canal {} removido del Parking bridge {}", channelId, parkingBridgeId);

    /// todo ver si hay que llamar a agente ACA!
  }


  private synchronized String getParkingLotId() {
    if(parkingBridgeId == null) {
      Bridge bridge = ariConnector.createBridge("holding");
      if (bridge == null) {
        log.error("Parking lot not created");
        return null;
      }
      parkingBridgeId = bridge.getId();
      log.info("Parking lot created: {}", parkingBridgeId);
    }
    return parkingBridgeId;
  }
}
