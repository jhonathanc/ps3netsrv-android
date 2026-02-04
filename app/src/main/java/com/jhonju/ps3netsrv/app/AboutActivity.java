package com.jhonju.ps3netsrv.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jhonju.ps3netsrv.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AboutActivity extends AppCompatActivity {

  private static final String GIT_PROPERTIES_FILE = "git.properties";
  private static final String GIT_COMMIT_ID = "git.commit.id";
  private static final String UNKNOWN = "Unknown";
  private static final int COMMIT_HASH_LENGTH = 7;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);

    TextView tvCommitHash = findViewById(R.id.tvCommitHash);
    tvCommitHash.setText(getCommitHash());
  }

  private String getCommitHash() {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = getAssets().open(GIT_PROPERTIES_FILE);
      properties.load(inputStream);
      String commit = properties.getProperty(GIT_COMMIT_ID);
      if (commit != null) {
        commit = commit.replaceAll("'", "");
        if (commit.length() >= COMMIT_HASH_LENGTH) {
          return commit.substring(0, COMMIT_HASH_LENGTH);
        }
      }
      return commit != null ? commit : UNKNOWN;
    } catch (IOException e) {
      return UNKNOWN;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }
}
