package com.jhonju.ps3netsrv.server.charset;

import java.nio.charset.Charset;

/** java.nio.charset.StandardCharsets requires API 19 and is not core-desugared. */
public final class StandardCharsets {
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private StandardCharsets() {
    }
}
