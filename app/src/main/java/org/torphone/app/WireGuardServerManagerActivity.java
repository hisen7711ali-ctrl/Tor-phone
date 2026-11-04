package org.torphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wireguard.config.Config;
import com.wireguard.config.InetEndpoint;
import org.torphone.app.vpn.WireGuardBackend;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WireGuardServerManagerActivity extends Activity {
    private static final int REQUEST_IMPORT = 8201;
    private static final int REQUEST_VPN_PERMISSION = 8202;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private String pendingProfile;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ThemePrefs.apply(this);
        render();
        refreshProfiles();
    }

    private File profileDir() {
        File dir = new File(getFilesDir(), "wireguard");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(28));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        scroll.addView(page);

        TextView title = label("WireGuard Server Manager", 23, true);
        page.addView(title);
        TextView hint = label("استورد ملف .conf الخاص بخادمك. لا توجد خوادم مجانية أو عناوين جاهزة داخل التطبيق.", 14, false);
        hint.setPadding(0, dp(8), 0, dp(16));
        page.addView(hint);

        Button importButton = new Button(this);
        importButton.setText("Import WireGuard .conf");
        importButton.setAllCaps(false);
        importButton.setMinHeight(dp(52));
        importButton.setOnClickListener(v -> openImporter());
        page.addView(importButton);

        TextView listTitle = label("Imported configurations", 17, true);
        listTitle.setPadding(0, dp(22), 0, dp(10));
        page.addView(listTitle);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        page.addView(list);
        setContentView(scroll);
    }

    private void openImporter() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "application/octet-stream"});
        startActivityForResult(pick, REQUEST_IMPORT);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQUEST_VPN_PERMISSION) {
            if (result == RESULT_OK && pendingProfile != null) connectProfile(pendingProfile);
            else Toast.makeText(this, "VPN permission was denied", Toast.LENGTH_LONG).show();
            return;
        }
        if (result != RESULT_OK || data == null) return;
        if (request == REQUEST_IMPORT && data.getData() != null) {
            try (java.io.InputStream input = getContentResolver().openInputStream(data.getData())) {
                if (input == null) throw new IllegalArgumentException("Cannot open selected file");
                byte[] bytes = readAll(input);
                Config parsed = Config.parse(new ByteArrayInputStream(bytes));
                String displayName = queryDisplayName(data.getData());
                String name = safeName(displayName);
                File target = uniqueFile(name);
                writeConfig(target, parsed.toWgQuickString());
                Toast.makeText(this, "Configuration imported and validated", Toast.LENGTH_LONG).show();
                refreshProfiles();
            } catch (Exception error) {
                Toast.makeText(this, "Invalid WireGuard config: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private byte[] readAll(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private String queryDisplayName(android.net.Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return "wireguard.conf";
    }

    private String safeName(String value) {
        String name = value == null ? "wireguard" : value.replaceFirst("(?i)\\.conf$", "");
        name = name.replaceAll("[^A-Za-z0-9_.-]", "_").replaceAll("^\\.+", "");
        if (name.isEmpty()) name = "wireguard";
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private File uniqueFile(String base) {
        File file = new File(profileDir(), base + ".conf");
        int suffix = 2;
        while (file.exists()) file = new File(profileDir(), base + "-" + suffix++ + ".conf");
        return file;
    }

    private void writeConfig(File file, String value) throws java.io.IOException {
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void refreshProfiles() {
        list.removeAllViews();
        File[] files = profileDir().listFiles((dir, name) -> name.endsWith(".conf"));
        if (files == null || files.length == 0) {
            TextView empty = label("لا توجد إعدادات بعد. استورد ملف WireGuard .conf من مزوّدك.", 14, false);
            empty.setPadding(0, dp(12), 0, 0);
            list.addView(empty);
            return;
        }
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : files) addProfileCard(file);
    }

    private void addProfileCard(File file) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0x141A2370);
        TextView name = label(file.getName().replaceFirst("(?i)\\.conf$", ""), 17, true);
        card.addView(name);

        String endpointText = "Endpoint: unavailable";
        try (FileInputStream input = new FileInputStream(file)) {
            Config config = Config.parse(input);
            if (!config.getPeers().isEmpty()) {
                InetEndpoint endpoint = config.getPeers().get(0).getEndpoint().orElse(null);
                if (endpoint != null) endpointText = "Endpoint: " + endpoint.getHost() + ":" + endpoint.getPort();
            }
        } catch (Exception ignored) { }
        TextView endpoint = label(endpointText, 12, false);
        endpoint.setPadding(0, dp(5), 0, dp(4));
        card.addView(endpoint);
        TextView latency = label("ICMP latency: checking…", 12, false);
        card.addView(latency);
        measureLatency(file, latency);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button connect = actionButton("Connect");
        connect.setOnClickListener(v -> requestConnect(file.getName()));
        Button edit = actionButton("Edit");
        edit.setOnClickListener(v -> editProfile(file));
        Button duplicate = actionButton("Duplicate");
        duplicate.setOnClickListener(v -> duplicateProfile(file));
        Button delete = actionButton("Delete");
        delete.setOnClickListener(v -> confirmDelete(file));
        actions.addView(connect, weightParams());
        actions.addView(edit, weightParams());
        actions.addView(duplicate, weightParams());
        actions.addView(delete, weightParams());
        card.addView(actions);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        list.addView(card, params);
    }

    private void measureLatency(File file, TextView target) {
        io.execute(() -> {
            String result = "ICMP: no reply";
            try (FileInputStream input = new FileInputStream(file)) {
                Config config = Config.parse(input);
                if (!config.getPeers().isEmpty()) {
                    InetEndpoint endpoint = config.getPeers().get(0).getEndpoint().orElse(null);
                    if (endpoint != null) {
                        long start = System.nanoTime();
                        boolean reachable = InetAddress.getByName(endpoint.getHost()).isReachable(1200);
                        long ms = (System.nanoTime() - start) / 1_000_000L;
                        result = reachable ? "ICMP latency: " + ms + " ms" : "ICMP: no reply (may be filtered)";
                    } else result = "Endpoint: not configured";
                }
            } catch (Exception ignored) { result = "ICMP: unavailable"; }
            final String display = result;
            runOnUiThread(() -> { if (!isFinishing()) target.setText(display); });
        });
    }

    private void requestConnect(String profile) {
        pendingProfile = profile;
        Intent permission = VpnService.prepare(this);
        if (permission == null) connectProfile(profile);
        else startActivityForResult(permission, REQUEST_VPN_PERMISSION);
    }

    private void connectProfile(String profile) {
        File file = new File(profileDir(), profile);
        io.execute(() -> {
            try {
                WireGuardBackend.connect(getApplicationContext(), file);
                runOnUiThread(() -> {
                    Toast.makeText(this, "WireGuard connected", Toast.LENGTH_LONG).show();
                    refreshProfiles();
                });
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        "WireGuard connection failed: " + error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void editProfile(File file) {
        EditText editor = new EditText(this);
        editor.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        editor.setMinLines(10);
        editor.setMaxLines(18);
        editor.setText(readText(file));
        new AlertDialog.Builder(this)
                .setTitle("Edit " + file.getName())
                .setView(editor)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        Config config = Config.parse(new ByteArrayInputStream(
                                editor.getText().toString().getBytes(StandardCharsets.UTF_8)));
                        if (file.getName().equals(getSharedPreferences("torphone_preferences", MODE_PRIVATE)
                                .getString("wireguard_profile", ""))) {
                            try { WireGuardBackend.disconnect(this); } catch (Exception ignored) { }
                        }
                        writeConfig(file, config.toWgQuickString());
                        refreshProfiles();
                        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) {
                        Toast.makeText(this, "Invalid config: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private String readText(File file) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception error) { return ""; }
    }

    private void duplicateProfile(File file) {
        try {
            File copy = uniqueFile(safeName(file.getName().replaceFirst("(?i)\\.conf$", "") + "-copy"));
            writeConfig(copy, readText(file));
            refreshProfiles();
        } catch (Exception error) {
            Toast.makeText(this, "Duplicate failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this).setTitle("Delete " + file.getName() + "?")
                .setMessage("The configuration is stored only in this app.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (file.getName().equals(getSharedPreferences("torphone_preferences", MODE_PRIVATE)
                            .getString("wireguard_profile", ""))) {
                        try { WireGuardBackend.disconnect(this); } catch (Exception ignored) { }
                    }
                    if (file.delete()) refreshProfiles();
                    else Toast.makeText(this, "Could not delete config", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private TextView label(String value, int size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTypeface(android.graphics.Typeface.DEFAULT, bold ? 1 : 0);
        text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return text;
    }

    private Button actionButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinHeight(dp(44));
        return button;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, -2, 1f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
