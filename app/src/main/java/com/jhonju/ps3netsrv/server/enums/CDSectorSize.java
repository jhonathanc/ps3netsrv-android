package com.jhonju.ps3netsrv.server.enums;

public enum CDSectorSize {
    CD_SECTOR_2352(2352)
    , CD_SECTOR_2048(2048)
    , CD_SECTOR_2328(2328)
    , CD_SECTOR_2336(2336)
    , CD_SECTOR_2340(2340)
    , CD_SECTOR_2368(2368)
    , CD_SECTOR_2448(2448);

    public final int cdSectorSize;

    CDSectorSize(int cdSectorSize) {
        this.cdSectorSize = cdSectorSize;
    }

    public static CDSectorSize valueOf(int cdSectorSize) {
        for (CDSectorSize cdSecSize : CDSectorSize.values()) {
            if (cdSecSize.cdSectorSize == cdSectorSize) {
                return cdSecSize;
            }
        }
        return null;
    }

}
