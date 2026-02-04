package com.jhonju.ps3netsrv.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jhonju.ps3netsrv.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AboutActivity extends AppCompatActivity {

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
      inputStream = getAssets().open("git.properties");
      properties.load(inputStream);
      String commit = properties.getProperty("git.commit.id");
      if (commit != null) {
        commit = commit.replaceAll("'", "");
        if (commit.length() >= 8) {
          return commit.substring(0, 8);
        }
      }
      return commit != null ? commit : "Unknown";
    } catch (IOException e) {
      return "Unknown";
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
