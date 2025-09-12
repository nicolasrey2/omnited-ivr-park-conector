package coop.bancocredicoop.omnited.message;

import ch.loway.oss.ari4java.generated.models.Playback;

public interface CanalMensajeria {
    /**
     * Envía un mensaje de texto al destinatario.
     * @param destinatario identificador de usuario (generalmente channel id por ahora)
     * @param texto       cuerpo del mensaje
     */
    Playback enviarMensaje(String destinatario, String texto);

    void stopPlayback(String playbackId);
}