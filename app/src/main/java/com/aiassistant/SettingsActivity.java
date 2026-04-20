package com.aiassistant;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager settingsManager;
    private TaskDatabase taskDatabase;
    private SwitchMaterial switchDigest;
    private SwitchMaterial switchWidget;
    private EditText etAllowedApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = new SettingsManager(this);
        taskDatabase = new TaskDatabase(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchDigest = findViewById(R.id.switchDigest);
        switchWidget = findViewById(R.id.switchWidget);
        etAllowedApps = findViewById(R.id.etAllowedApps);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);
        MaterialButton btnUseRecentApps = findViewById(R.id.btnUseRecentApps);
        MaterialButton btnOpenNotificationAccess = findViewById(R.id.btnOpenNotificationAccess);

        switchDigest.setChecked(settingsManager.isDailyDigestEnabled());
        switchWidget.setChecked(settingsManager.isWidgetEnabled());
        etAllowedApps.setText(settingsManager.getAllowedAppsRaw());

        btnSave.setOnClickListener(v -> saveSettings());
        btnUseRecentApps.setOnClickListener(v -> fillFromRecentApps());
        btnOpenNotificationAccess.setOnClickListener(v ->
                startActivity(new android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    }

    private void fillFromRecentApps() {
        List<String> recent = taskDatabase.getRecentAppSources();
        Set<String> normalized = new HashSet<>(recent);
        etAllowedApps.setText(settingsManager.mergeRecentApps(normalized));
    }

    private void saveSettings() {
        settingsManager.setDailyDigestEnabled(switchDigest.isChecked());
        settingsManager.setWidgetEnabled(switchWidget.isChecked());
        settingsManager.setAllowedAppsRaw(etAllowedApps.getText().toString());
        TaskWidgetProvider.updateAll(this);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
