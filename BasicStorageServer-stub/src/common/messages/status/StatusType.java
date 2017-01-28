package common.messages.status;

/**
 * A public enum for server commands
 *
 * - We can get the integer value of a command enum by obtaining its ordinal, 
 *      i.e. CommandType.PUT.ordinal() == 2
 *
 * - We can obtain the underlying enum from an int value by using its ordinal/position in the enum array
        i.e. CommandType putCommand = CommandType.values()[2];
 */
public enum StatusType {
    INVALID ("INVALID"), 
    SUCCESS ("SUCCESS"),
    UPDATE ("UPDATE"),
    ERROR ("ERROR"),
    GET ("GET"),
    GET_ERROR ("GET_ERROR"),
    GET_SUCCESS ("GET_SUCCESS"),
    PUT ("PUT"),
    PUT_SUCCESS ("PUT_SUCCESS"),
    PUT_UPDATE ("PUT_UPDATE"),
    PUT_ERROR ("PUT_ERROR"),
    DELETE_SUCCESS ("DELETE_SUCCESS"),
    DELETE_ERROR ("DELETE_ERROR");

    private final String status;
    StatusType(String status) {
        this.status = status;
    }

    public String getStringName() {
        return status;
    }
}
