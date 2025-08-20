package coop.bancocredicoop.omnited.messages;

public interface CanalMensajeria {
    /**
     * Envía un mensaje de texto al destinatario.
     * @param destinatario identificador de usuario (p.ej. número de WhatsApp, o "console-user")
     * @param texto       cuerpo del mensaje
     */
    void enviarMensaje(String destinatario, String texto);
}