package harps.swanuniemailclient;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by eghar on 15/04/2017.
 */

public class OutgoingEmail extends Email {

    private ArrayList<String> recipients;
    private ArrayList<String> ccRecipients;
    private ArrayList<String> bccRecipients;
    private ArrayList<File> attachments;
    private String originalSender;
    private String originalMessage;
    private Date originalDate;

    public OutgoingEmail(Long UID, String subject, String message, Boolean attachment,
                         ArrayList<String> recipients, ArrayList<String> ccRecipients,
                         ArrayList<String> bccRecipients, ArrayList<File> attachments,
                         String originalSender, String originalMessage, Date originalDate){
        super(UID, subject, message, attachment);
        this.recipients = recipients;
        this.ccRecipients = ccRecipients;
        this.bccRecipients = bccRecipients;
        this.attachments = attachments;
        this.originalSender = originalSender;
        this.originalMessage = originalMessage;
        this.originalDate = originalDate;
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

    public ArrayList<File> getAttachments(){
        return attachments;
    }

    public String getOriginalSender(){
        return originalSender;
    }

    public String getOriginalMessage(){
        return originalMessage;
    }

    public Date getOrignalDate(){
        return originalDate;
    }
}
