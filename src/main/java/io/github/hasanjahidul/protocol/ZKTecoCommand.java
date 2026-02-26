package io.github.hasanjahidul.protocol;

/**
 * ZKTeco device command constants
 */
public class ZKTecoCommand {

    // Connection commands
    /** Command to connect to device */
    public static final int CMD_CONNECT = 1000;
    /** Command to exit/disconnect from device */
    public static final int CMD_EXIT = 1001;
    /** Command to enable device (unlock) */
    public static final int CMD_ENABLE_DEVICE = 1002;
    /** Command to disable device (lock) */
    public static final int CMD_DISABLE_DEVICE = 1003;

    // Device information commands
    /** Command to get device info with parameter string */
    public static final int CMD_DEVICE = 11;
    /** Command to get firmware version */
    public static final int CMD_GET_VERSION = 1100;
    /** Command to get device name */
    public static final int CMD_GET_DEVICE_NAME = 1101;
    /** Command to get serial number */
    public static final int CMD_GET_SERIAL_NUMBER = 1102;
    /** Command to get platform */
    public static final int CMD_GET_PLATFORM = 1103;
    /** Command to get OS version */
    public static final int CMD_GET_OS_VERSION = 1104;
    /** Command to get device time */
    public static final int CMD_GET_TIME = 201;
    /** Command to set device time */
    public static final int CMD_SET_TIME = 202;
    /** Command to change communication speed */
    public static final int CMD_CHANGE_SPEED = 1101;
    /** Command to test temperature sensor */
    public static final int CMD_TEST_TEMP = 1011;
    /** Command to test voice (plays "Thank you") */
    public static final int CMD_TESTVOICE = 1017;

    // LCD commands
    /** Command to write text to LCD screen */
    public static final int CMD_WRITE_LCD = 66;
    /** Command to clear LCD screen */
    public static final int CMD_CLEAR_LCD = 67;

    // User management commands
    /** Command to set/add user */
    public static final int CMD_SET_USER = 8;
    /** Command to request user template data */
    public static final int CMD_USER_TEMP_RRQ = 9;
    /** Command to write user template data */
    public static final int CMD_USER_TEMP_WRQ = 10;
    /** Command to delete user */
    public static final int CMD_DELETE_USER = 18;
    /** Command to delete user template */
    public static final int CMD_DELETE_USER_TEMP = 19;
    /** Command to clear all data */
    public static final int CMD_CLEAR_DATA = 14;
    /** Command to clear admin privileges */
    public static final int CMD_CLEAR_ADMIN = 20;

    // Attendance commands
    /** Command to request attendance log */
    public static final int CMD_ATT_LOG_RRQ = 13;
    /** Command to clear attendance log */
    public static final int CMD_CLEAR_ATT_LOG = 15;
    /** Command to get free memory sizes */
    public static final int CMD_GET_FREE_SIZES = 50;

    // Device control commands
    /** Command to restart device */
    public static final int CMD_RESTART = 1004;
    /** Command to power off device */
    public static final int CMD_POWEROFF = 1005;
    /** Command to put device to sleep */
    public static final int CMD_SLEEP = 1006;
    /** Command to resume device from sleep */
    public static final int CMD_RESUME = 1007;
    /** Command to unlock the door */
    public static final int CMD_UNLOCK = 31;
    
    // Function codes
    /** Function code for attendance log */
    public static final int FCT_ATTLOG = 1;
    /** Function code for fingerprint template */
    public static final int FCT_FINGERTMP = 2;
    /** Function code for operation log */
    public static final int FCT_OPLOG = 4;
    /** Function code for user data */
    public static final int FCT_USER = 5;
    /** Function code for SMS */
    public static final int FCT_SMS = 6;
    /** Function code for user data */
    public static final int FCT_UDATA = 7;
    /** Function code for work code */
    public static final int FCT_WORKCODE = 8;
    
    // User levels
    /** User level: regular user */
    public static final int LEVEL_USER = 0;
    /** User level: administrator */
    public static final int LEVEL_ADMIN = 14;

    // Command types
    /** Command type: general command */
    public static final int COMMAND_TYPE_GENERAL = 1;
    /** Command type: data transfer command */
    public static final int COMMAND_TYPE_DATA = 2;

    // Response codes
    /** Response: command successful */
    public static final int CMD_ACK_OK = 2000;
    /** Response: command error */
    public static final int CMD_ACK_ERROR = 2001;
    /** Response: data ready */
    public static final int CMD_ACK_DATA = 2002;
    /** Response: unauthorized */
    public static final int CMD_ACK_UNAUTH = 2005;
    
    // Data commands
    /** Command: device is preparing to send data */
    public static final int CMD_PREPARE_DATA = 1500;
    /** Command: data packet */
    public static final int CMD_DATA = 1501;
    /** Command: free data buffer */
    public static final int CMD_FREE_DATA = 1502;

    private ZKTecoCommand() {
        // Utility class
    }
}
