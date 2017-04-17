package harps.swanuniemailclient;

/**
 * Created by eghar on 13/04/2017.
 */

public class EmailUser {

    private static final String firstName = "Edward";
    private static final String lastName = "Harper";
    private static final String emailAddress = "789968@swansea.ac.uk";
    private static final String password = "789968/17/04/1994";

    public static String getEmailAddress(){
        return emailAddress;
    }

    public static String getPassword(){
        return password;
    }

    public static String getFirstName(){
        return firstName;
    }

    public static String getLastName(){
        return lastName;
    }
}
