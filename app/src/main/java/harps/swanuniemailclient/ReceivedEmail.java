package harps.swanuniemailclient;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * @file ReceivedEmail.java
 * @author Ed Harper
 * @date 31/03/2017
 *
 * Received email object for storing and maintaing received email data
 * Implements parcelable to allow objects to be passed through intents
 */

public class ReceivedEmail extends Email implements Parcelable {

    private String from;
    private Date receivedDate;
    private Boolean unread;

    public ReceivedEmail(){

    }

    /**
     * Initialises received email object
     * @param UID unique identifier of the email
     * @param subject email subject
     * @param message email message
     * @param attachment email attachment boolean
     * @param from email from email address
     * @param receivedDate date email was received
     * @param unread email unread boolean value
     */
    public ReceivedEmail(Long UID, String subject, String message, Boolean attachment, String from, Date receivedDate, Boolean unread){
        super(UID, subject, message, attachment);
        this.from = from;
        this.receivedDate = receivedDate;
        this.unread = unread;
    }

    public String getFrom(){
        return from;
    }

    public Date getReceivedDate(){
        return receivedDate;
    }

    public Boolean getUnread(){
        return unread;
    }

    /**
     * Sets unread value to the passed boolean value
     * @param unread the passed boolean value
     */
    public void setUnread(Boolean unread){
        this.unread = unread;
    }

    @Override
    public String toString(){
        return " From: " + getFrom() + ". Date: " + getReceivedDate() + ". Read: " + getUnread();
    }

    /**
     * Initialises parcel with data passed from in
     * @param in
     */
    protected ReceivedEmail(Parcel in) {
        super(in);
        from = in.readString();
        long tmpReceivedDate = in.readLong();
        receivedDate = tmpReceivedDate != -1 ? new Date(tmpReceivedDate) : null;
        byte unreadVal = in.readByte();
        unread = unreadVal == 0x02 ? null : unreadVal != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Packaged object into parcel
     * @param dest the destination parcel
     * @param flags flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(from);
        dest.writeLong(receivedDate != null ? receivedDate.getTime() : -1L);
        if (unread == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (unread ? 0x01 : 0x00));
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ReceivedEmail> CREATOR = new Parcelable.Creator<ReceivedEmail>() {
        @Override
        public ReceivedEmail createFromParcel(Parcel in) {
            return new ReceivedEmail(in);
        }

        @Override
        public ReceivedEmail[] newArray(int size) {
            return new ReceivedEmail[size];
        }
    };
}