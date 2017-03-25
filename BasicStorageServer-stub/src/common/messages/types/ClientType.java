package common.messages;

/**
 * A public enum for server commands
 *
 * - We can get the integer value of a command enum by obtaining its ordinal, 
 *      i.e. ClientType.PUT.ordinal() == 2
 *
 * - We can obtain the underlying enum from an int value by using its ordinal/position in the enum array
        i.e. ClientType putCommand = ClientType.values()[2];
 */
public enum ClientType {
    // Invalid if not a client - e.g. message originates from server
    INVALID ("INVALID"),
    CLIENT ("CLIENT"), 
    COORDINATOR ("COORDINATOR"),
    ECS ("ECS");

    private final String client;
    ClientType(String client) {
        this.client = client;
    } 

    public String getStringName() {
        return client;
    }   
}
