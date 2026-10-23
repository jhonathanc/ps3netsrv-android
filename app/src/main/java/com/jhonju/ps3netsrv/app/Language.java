package com.jhonju.ps3netsrv.app;

import java.util.Locale;

/**
 * Enum representing supported languages in the application.
 * Each language has a display index and language code.
 */
public enum Language {
  ITALIAN(0, "it"),
  ENGLISH(1, "en"),
  SPANISH(2, "es"),
  PORTUGUESE(3, "pt"),
  RYUKYUAN(4, "ryu"),
  JAVANESE(5, "jv");

  private final int index;
  private final String code;

  Language(int index, String code) {
    this.index = index;
    this.code = code;
  }

  public int getIndex() {
    return index;
  }

  public String getCode() {
    return code;
  }

  public Locale getLocale() {
    return new Locale(code);
  }

  public static Language fromIndex(int index) {
    for (Language lang : values()) {
      if (lang.index == index) {
        return lang;
      }
    }
    return ENGLISH;
  }

  public static Language fromCode(String code) {
    if (code == null) {
      return ENGLISH;
    }
    String language = code.replace('_', '-').split("-", 2)[0];
    for (Language lang : values()) {
      if (lang.getLocale().getLanguage().equalsIgnoreCase(language)) {
        return lang;
      }
    }
    return ENGLISH;
  }
}
