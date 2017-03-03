package common.messages;

/**
 * A public enum for server commands
 *
 * - We can get the integer value of a command enum by obtaining its ordinal, 
 *      i.e. CommandType.PUT.ordinal() == 2
 *
 * - We can obtain the underlying enum from an int value by using its ordinal/position in the enum array
        i.e. CommandType putCommand = CommandType.values()[2];
 */
public enum CommandType {
    // Commands for regular clients
    CHAT ("CHAT"), 
    GET ("GET"), 
    PUT ("PUT"), 
    DELETE ("DELETE"),
    // Commands for ECS client
    INIT("INIT"),
    START("START"),
    STOP("STOP"),
    SHUT_DOWN("SHUT_DOWN"),
    LOCK_WRITE("LOCK_WRITE"),
    UNLOCK_WRITE("UNLOCK_WRITE"),
    MOVE_DATA("MOVE_DATA"),
    UPDATE_METADATA("UPDATE_METADATA");

    private final String command;
    CommandType(String command) {
        this.command = command;
    } 

    public String getStringName() {
        return command;
    }   
}
