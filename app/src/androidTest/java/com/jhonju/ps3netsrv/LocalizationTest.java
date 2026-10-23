package com.jhonju.ps3netsrv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jhonju.ps3netsrv.app.Language;
import com.jhonju.ps3netsrv.app.utils.LocaleHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class LocalizationTest {
    @Test
    public void selectedLanguagesResolveTheirOwnResources() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = app.getSharedPreferences("language_settings", Context.MODE_PRIVATE);
        String key = "Locale.Helper.Selected.Language";
        String saved = preferences.getString(key, null);
        Locale originalLocale = Locale.getDefault();
        String originalLanguage = LocaleHelper.getLanguage(app);
        try {
            String[] expectedSave = {"Salva", "Save", "Guardar", "Salvar", "保存", "Simpen"};
            String[] expectedStart = {"Avvia server", "Start server", "Iniciar servidor",
                    "Iniciar servidor", "サーバー起動", "Wiwiti server"};
            for (Language language : Language.values()) {
                Context localized = LocaleHelper.setLocale(app, language.getCode());
                int index = language.getIndex();
                assertEquals(expectedSave[index], localized.getString(R.string.save));
                assertEquals(expectedStart[index], localized.getString(R.string.start_server));
                assertEquals(language.getCode(), LocaleHelper.getLanguage(localized));
                assertEquals(6, localized.getResources().getStringArray(R.array.languages).length);
                String running = localized.getString(R.string.server_running, "192.0.2.1", 38008, "/games");
                assertFalse(running.contains("%"));
                assertFalse(running.contains("\\n"));
                if (language != Language.ENGLISH) {
                    assertNotEquals("Starting server…", localized.getString(R.string.server_starting));
                    assertNotEquals("Allow background operation", localized.getString(R.string.allow_background_operation));
                }
                if (language == Language.RYUKYUAN) {
                    assertEquals("めー", localized.getString(R.string.previous));
                    String resourceLanguage = localized.getResources().getConfiguration().locale.getLanguage();
                    assertEquals(Build.VERSION.SDK_INT < 21 ? "ja" : "ryu", resourceLanguage);
                }
                if (language == Language.JAVANESE) {
                    assertEquals("Sadurunge", localized.getString(R.string.previous));
                }
            }
        } finally {
            LocaleHelper.setLocale(app, originalLanguage);
            if (saved == null) preferences.edit().remove(key).commit();
            else preferences.edit().putString(key, saved).commit();
            Locale.setDefault(originalLocale);
        }
    }
}
