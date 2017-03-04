package common.messages;

/**
 * A public enum for server commands
 *
 * - We can get the integer value of a command enum by obtaining its ordinal, 
 *      i.e. StatusType.PUT.ordinal() == 2
 *
 * - We can obtain the underlying enum from an int value by using its ordinal/position in the enum array
        i.e. StatusType putCommand = StatusType.values()[2];
 */
public enum StatusType {
    INVALID ("INVALID"), 
    SUCCESS ("SUCCESS"),
    ERROR ("ERROR"),
    GET_ERROR ("GET_ERROR"),
    REROUTE("REROUTE"),
    GET_SUCCESS ("GET_SUCCESS"),
    PUT_SUCCESS ("PUT_SUCCESS"),
    PUT_UPDATE ("PUT_UPDATE"),
    PUT_ERROR ("PUT_ERROR"),
    DELETE_SUCCESS ("DELETE_SUCCESS"),
    DELETE_ERROR ("DELETE_ERROR"),
    SERVER_STOPPED("SERVER_STOPPED"),
    SERVER_WRITE_LOCK("SERVER_WRITE_LOCK"),
    SERVER_NOT_REPSONSIBLE("SERVER_NOT_RESPONSIBLE");

    private final String status;
    StatusType(String status) {
        this.status = status;
    }

    public String getStringName() {
        return status;
    }
}
