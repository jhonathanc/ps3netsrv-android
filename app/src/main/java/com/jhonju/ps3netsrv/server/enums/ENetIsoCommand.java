package com.jhonju.ps3netsrv.server.enums;

public enum ENetIsoCommand {
    /* Closes the active ro file (if any) and open/stat a new one */
    NETISO_CMD_OPEN_FILE(0x1224),
    /* Reads the active ro file. Offsets and sizes in bytes. If file read fails, client is exited. Only read data is returned. */
    NETISO_CMD_READ_FILE_CRITICAL(0x1225),
    /* Reads 2048 sectors in a 2352 sectors iso.
     * Offsets and sizes in sectors. If file read fails, client is exited */
    NETISO_CMD_READ_CD_2048_CRITICAL(0x1226),
    /* Reads the active ro file. Offsets and sizes in bytes. It returns number of bytes read to client, -1 on error, and after that, the data read (if any)
       Only up to the BUFFER_SIZE used by server can be red at one time*/
    NETISO_CMD_READ_FILE(0x1227),
    /* Closes the active wo file (if any) and opens+truncates or creates a new one */
    NETISO_CMD_CREATE_FILE(0x1228),
    /* Writes to the active wo file. After command, data is sent. It returns number of bytes written to client, -1 on erro.
       If more than BUFFER_SIZE used by server is specified in command, connection is aborted. */
    NETISO_CMD_WRITE_FILE(0x1229),
    /* Closes the active directory (if any) and opens a new one */
    NETISO_CMD_OPEN_DIR(0x122A),
    /* Reads a directory entry. and returns result. If no more entries or an error happens, the directory is automatically closed. . and .. are automatically ignored */
    NETISO_CMD_READ_DIR_ENTRY(0x122B),
    /* Deletes a file. */
    NETISO_CMD_DELETE_FILE(0x122C),
    /* Creates a directory */
    NETISO_CMD_MKDIR(0x122D),
    /* Removes a directory (if empty) */
    NETISO_CMD_RMDIR(0x122E),
    /* Reads a directory entry (v2). and returns result. If no more entries or an error happens, the directory is automatically closed. . and .. are automatically ignored */
    NETISO_CMD_READ_DIR_ENTRY_V2(0x122F),
    /* Stats a file or directory */
    NETISO_CMD_STAT_FILE(0x1230),
    /* Gets a directory size */
    NETISO_CMD_GET_DIR_SIZE(0x1231),

    /* Get complete directory contents */
    NETISO_CMD_READ_DIR(0x1232),

    /* Replace this with any custom command */
    NETISO_CMD_CUSTOM_0(0x2412);

    public final int value;

    ENetIsoCommand(int value) {
        this.value = value;
    }

    public static ENetIsoCommand valueOf(int command) {
        for (ENetIsoCommand comm : ENetIsoCommand.values()) {
            if (comm.value == command) {
                return comm;
            }
        }
        return null;
    }
}
