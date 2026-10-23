package com.jhonju.ps3netsrv;

import com.jhonju.ps3netsrv.app.Language;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LanguageTest {
    @Test
    public void ryukyuanAndJavaneseHaveSeparateCodes() {
        assertEquals(Language.RYUKYUAN, Language.fromCode("ryu"));
        assertEquals(Language.JAVANESE, Language.fromCode("jv"));
        assertEquals("ryu", Language.RYUKYUAN.getLocale().getLanguage());
        assertEquals("jv", Language.JAVANESE.getLocale().getLanguage());
        assertNotEquals(Language.RYUKYUAN.getCode(), Language.JAVANESE.getCode());
    }

    @Test
    public void portugueseUsesOneLocaleForAllRegions() {
        assertEquals(Language.PORTUGUESE, Language.fromCode("pt"));
        assertEquals(Language.PORTUGUESE, Language.fromCode("pt-BR"));
        assertEquals(Language.PORTUGUESE, Language.fromCode("pt_BR"));
        assertEquals(Language.PORTUGUESE, Language.fromCode("pt-PT"));
        assertEquals(Language.PORTUGUESE, Language.fromCode("pt_AO"));
        assertEquals("pt", Language.PORTUGUESE.getCode());
        assertEquals("pt", Language.PORTUGUESE.getLocale().getLanguage());
        assertEquals("", Language.PORTUGUESE.getLocale().getCountry());
    }

    @Test
    public void deviceRegionsAndUnsupportedLanguagesAreHandled() {
        assertEquals(Language.ENGLISH, Language.fromCode("en-US"));
        assertEquals(Language.SPANISH, Language.fromCode("es_MX"));
        assertEquals(Language.ITALIAN, Language.fromCode("IT-it"));
        assertEquals(Language.RYUKYUAN, Language.fromCode("ryu-JP"));
        assertEquals(Language.ENGLISH, Language.fromCode("ja"));
        assertEquals(Language.ENGLISH, Language.fromCode("fr"));
        assertEquals(Language.ENGLISH, Language.fromCode(""));
        assertEquals(Language.ENGLISH, Language.fromCode(null));
    }

    @Test
    public void allSixSpinnerPositionsRoundTrip() {
        assertEquals(6, Language.values().length);
        for (Language language : Language.values()) {
            assertEquals(language, Language.fromIndex(language.getIndex()));
            assertEquals(language, Language.fromCode(language.getCode()));
        }
        assertEquals(Language.ENGLISH, Language.fromIndex(-1));
        assertEquals(Language.ENGLISH, Language.fromIndex(6));
    }
}
