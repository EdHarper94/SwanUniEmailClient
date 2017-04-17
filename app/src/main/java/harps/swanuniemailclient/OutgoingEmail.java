package harps.swanuniemailclient;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by eghar on 15/04/2017.
 */

public class OutgoingEmail extends Email {

    private ArrayList<String> recipients;
    private ArrayList<String> ccRecipients;
    private ArrayList<String> bccRecipients;

    public OutgoingEmail(Long UID, String subject, String message, Boolean attachment,
                         ArrayList<String> recipients, ArrayList<String> ccRecipients, ArrayList<String> bccRecipients){
        super(UID, subject, message, attachment);
        this.recipients = recipients;
        this.ccRecipients = ccRecipients;
        this.bccRecipients = bccRecipients;
    }

    public ArrayList<String> getRecipients(){
        return recipients;
    }

    public ArrayList<String> getCcRecipients(){
        return ccRecipients;
    }

    public ArrayList<String> getBccRecipients(){
        return bccRecipients;
    }
}
