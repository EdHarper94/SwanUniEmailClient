package harps.swanuniemailclient;

import java.util.Properties;

/**
 * @file ServerProperties.java
 * @author Ed Harper
 * @date 01/04/2017
 *
 *  Server property settings
 */

public class ServerProperties {

    private Properties props;
    private ServerSettings serverSettings;

    public ServerProperties(){
        this.props = new Properties();
        this.serverSettings = new ServerSettings();
    }

    /**
     * Inbox properties settings
     * @return IMAP properties
     */
    public Properties getInboxProperties(){
        // Server settings
        props.put("mail.imaps.host", serverSettings.getServerAddress());
        props.put("mail.imaps.port", serverSettings.getInPort());
        // Set protocal
        props.setProperty("mail.store.protocol", serverSettings.getInProtocol());
        // SSL settings
        props.put("mail.imaps.ssl.enable", serverSettings.getIncSll());
        props.put("mail.imaps.timeout", 1000);
        return props;
    }

    /**
     * Outgoin properties settings
     * @return SMPTP properties settings
     */
    public Properties getSMTPProperties(){
        props.put("mail.smtp.from", EmailUser.getEmailAddress());

        // Server settings
        props.put("mail.smtp.host", serverSettings.getServerAddress());
        props.put("mail.smtp.port", serverSettings.getOutPort());
        props.put("mail.smtp.auth", "true");

        // SSL/STARTTLS settings
        props.put("mail.smtp.socketFactory.port", serverSettings.getOutPort());
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactor.fallback", "false");
        props.put("mail.smtp.starttls.enable", true);

        props.put("mail.smtp.connectiontimeout", "5000");

        return props;
    }

}
