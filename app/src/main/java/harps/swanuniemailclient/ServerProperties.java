package harps.swanuniemailclient;

import java.util.Properties;

/**
 * Created by eghar on 01/04/2017.
 */

public class ServerProperties {
    Properties props;
    ImapSettings imapSettings;

    public ServerProperties(){
        this.props = new Properties();
        this.imapSettings = new ImapSettings();
    }

    public Properties getInboxProperties(){
        // Server settings
        props.put("mail.imaps.host", imapSettings.getServerAddress());
        props.put("mail.imaps.port", imapSettings.getInPort());
        // Set protocal
        props.setProperty("mail.store.protocol", imapSettings.getInProtocol());
        // SSL settings
        props.put("mail.imaps.ssl.enable", imapSettings.getIncSll());
        props.put("mail.imaps.timeout", 1000);
        return props;
    }

}
