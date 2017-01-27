package common.messages.commands;

/**
 * A public enum for server commands
 *
 * - We can get the integer value of a command enum by obtaining its ordinal, 
 *      i.e. CommandType.PUT.ordinal() == 2
 *
 * - We can obtain the underlying enum from an int value by using its ordinal/position in the enum array
        i.e. CommandType putCommand = CommandType.values()[2];
 */
public enum CommandType {INVALID, GET, PUT, DELETE}
