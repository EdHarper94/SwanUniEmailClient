package harps.swanuniemailclient;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Email Class
 * Created by eghar on 28/03/2017.
 */

public class Email implements Parcelable {

    private Long UID;
    private String subject;
    private String text;
    private Boolean attachment;

    public Email(){

    }

    public Email(Long UID, String subject, String text, Boolean attachment){
        this.UID = UID;
        this.subject = subject;
        this.text = text;
        this.attachment = attachment;
    }

    public long getUID(){
        return UID;
    }

    public String getSubject(){
        return subject;
    }

    public String getText(){
        return text;
    }

    public Boolean getAttachment(){
        return attachment;
    }

    public String toString(){
        return "ID: " + getUID() + ". Subject: " + getSubject() + ". Message: " + getText() + ". Attachment: " + getAttachment();
    }

    protected Email(Parcel in) {
        UID = in.readByte() == 0x00 ? null : in.readLong();
        subject = in.readString();
        text = in.readString();
        byte attachmentVal = in.readByte();
        attachment = attachmentVal == 0x02 ? null : attachmentVal != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (UID == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(UID);
        }
        dest.writeString(subject);
        dest.writeString(text);
        if (attachment == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (attachment ? 0x01 : 0x00));
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Email> CREATOR = new Parcelable.Creator<Email>() {
        @Override
        public Email createFromParcel(Parcel in) {
            return new Email(in);
        }

        @Override
        public Email[] newArray(int size) {
            return new Email[size];
        }
    };
}
