package com.xit.voidapp;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import java.util.*;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(9, 9, 13);
    private final int PANEL = Color.rgb(22, 22, 30);
    private final int RED = Color.rgb(255, 48, 79);
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private String deviceId, role = "MEMBER", userKey = "";
    private LinearLayout root, body;
    private EditText keyInput;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null) deviceId = "unknown-device";
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance("us-central1");
        showLoading();
        auth.signInAnonymously().addOnSuccessListener(result -> restoreSession())
            .addOnFailureListener(e -> showError("Firebase Auth gagal: " + e.getMessage()));
    }

    private void showLoading() {
        setupRoot();
        TextView t = text("XIT VOID", 28, RED, true);
        t.setGravity(Gravity.CENTER);
        root.addView(t, new LinearLayout.LayoutParams(-1, 180));
        TextView s = text("INITIALIZING SECURE SESSION...", 12, Color.LTGRAY, false);
        s.setGravity(Gravity.CENTER);
        root.addView(s);
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(18));
        root.setBackgroundColor(BG);
        setContentView(root);
    }

    private void restoreSession() {
        Map<String,Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        functions.getHttpsCallable("getProfile").call(data)
            .addOnSuccessListener(r -> {
                Map<?,?> p = (Map<?,?>) r.getData();
                if (p != null && Boolean.TRUE.equals(p.get("active"))) {
                    role = String.valueOf(p.get("role"));
                    userKey = String.valueOf(p.get("key"));
                    showDashboard("HOME");
                } else showLogin();
            })
            .addOnFailureListener(e -> showLogin());
    }

    private void showLogin() {
        setupRoot();
        addBrand();
        TextView sub = text("ACCESS CONTROL // SECURE ENTRY", 11, Color.LTGRAY, false);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, lp( -1, 32));
        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint("ENTER ACCESS KEY");
        keyInput.setHintTextColor(Color.GRAY);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setTextSize(14);
        keyInput.setLetterSpacing(.08f);
        keyInput.setPadding(dp(15), 0, dp(15), 0);
        keyInput.setBackground(round(PANEL, 12, Color.rgb(60,60,70)));
        root.addView(keyInput, lp(-1, 56));
        space(root, 14);
        Button login = button("LOGIN", true);
        root.addView(login, lp(-1, 52));
        login.setOnClickListener(v -> redeemKey(keyInput.getText().toString().trim()));
        space(root, 10);
        Button buy = button("BUY ACCESS KEY  ↗", false);
        root.addView(buy, lp(-1, 50));
        buy.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6282155521748?text=BUY+ACCES+KEY"));
            startActivity(i);
        });
        space(root, 24);
        TextView foot = text("ONE KEY • ONE DEVICE  |  XIT VOID v1.0", 10, Color.GRAY, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, lp(-1, 30));
    }

    private void redeemKey(String key) {
        if (key.isEmpty()) { toast("Masukkan Access Key"); return; }
        setBusy(true);
        Map<String,Object> data = new HashMap<>();
        data.put("key", key.toUpperCase(Locale.US));
        data.put("deviceId", deviceId);
        functions.getHttpsCallable("redeemKey").call(data)
            .addOnSuccessListener(r -> {
                setBusy(false);
                Map<?,?> p = (Map<?,?>) r.getData();
                role = String.valueOf(p.get("role"));
                userKey = key.toUpperCase(Locale.US);
                showDashboard("HOME");
            })
            .addOnFailureListener(e -> {
                setBusy(false);
                toast(friendly(e));
            });
    }

    private void setBusy(boolean busy) {
        if (keyInput != null) keyInput.setEnabled(!busy);
        toast(busy ? "Memverifikasi key..." : "Selesai");
    }

    private void showDashboard(String page) {
        setupRoot();
        addBrand();
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("PROFILE".equals(page)) profilePage();
        else if ("GENERATE".equals(page) && "OWNER".equals(role)) generatePage();
        else homePage();
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(10), 0, 0);
        Button home = button("HOME", !"PROFILE".equals(page), false);
        Button prof = button("PROFILE", "PROFILE".equals(page), false);
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(46), 1));
        nav.addView(prof, new LinearLayout.LayoutParams(0, dp(46), 1));
        home.setOnClickListener(v -> showDashboard("HOME"));
        prof.setOnClickListener(v -> showDashboard("PROFILE"));
        if ("OWNER".equals(role)) {
            Button gen = button("KEYS", "GENERATE".equals(page), false);
            nav.addView(gen, new LinearLayout.LayoutParams(0, dp(46), 1));
            gen.setOnClickListener(v -> showDashboard("GENERATE"));
        }
        root.addView(nav);
    }

    private void homePage() {
        cardTitle("CONTROL CENTER", "MEMBER".equals(role) ? "MEMBER ACCESS" : "OWNER ACCESS");
        addSwitch("AIMNING", "Panel preset saja — tidak mengubah input game.");
        addSwitch("DRAG HS 60%", "Preset tampilan sensitivitas; atur manual di game.");
        addSwitch("OPTIMIZE FPS", "Tips performa aman; tidak memodifikasi game.");
        Button clear = button("CLEAR APP CACHE", true);
        body.addView(clear, lp(-1, 50));
        clear.setOnClickListener(v -> {
            long before = folderSize(getCacheDir());
            deleteContents(getCacheDir());
            toast("Cache aplikasi dibersihkan: " + formatBytes(before));
        });
        space(body, 18);
        cardTitle("GAME LAUNCHER", "Buka game yang sudah terpasang");
        gameButton("FREE FIRE", "com.dts.freefireth");
        gameButton("FREE FIRE MAX", "com.dts.freefiremax");
    }

    private void profilePage() {
        cardTitle("MY PROFILE", "ACCOUNT INFORMATION");
        info("ROLE", role);
        info("ACCESS KEY", mask(userKey));
        info("DEVICE", deviceId.substring(0, Math.min(8, deviceId.length())) + "••••");
        info("STATUS", "ACTIVE");
        if ("OWNER".equals(role)) {
            TextView owner = text("OWNER PRIVILEGES ENABLED", 12, RED, true);
            body.addView(owner, lp(-1, 42));
            Button gen = button("GENERATE ACCESS KEY", true);
            body.addView(gen, lp(-1, 50));
            gen.setOnClickListener(v -> showDashboard("GENERATE"));
        }
        Button logout = button("LOG OUT", false);
        body.addView(logout, lp(-1, 48));
        logout.setOnClickListener(v -> {
            // Keep the same anonymous Firebase UID so the same device can redeem
            // its already-bound key again after logging out.
            showLogin();
        });
    }

    private void generatePage() {
        cardTitle("KEY GENERATOR", "OWNER ONLY • 13 CHARACTERS");
        TextView note = text("Format: XXXX-XXXXX-XXXX", 12, Color.LTGRAY, false);
        body.addView(note, lp(-1, 32));
        Spinner roles = new Spinner(this);
        String[] choices = {"MEMBER", "OWNER"};
        roles.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices));
        body.addView(roles, lp(-1, 48));
        Button create = button("GENERATE KEY", true);
        body.addView(create, lp(-1, 52));
        TextView result = text("KEY RESULT WILL APPEAR HERE", 14, RED, true);
        result.setGravity(Gravity.CENTER);
        body.addView(result, lp(-1, 70));
        Button copy = button("COPY KEY", false);
        body.addView(copy, lp(-1, 46));
        final String[] generated = {""};
        create.setOnClickListener(v -> {
            Map<String,Object> data = new HashMap<>();
            data.put("role", roles.getSelectedItem().toString());
            functions.getHttpsCallable("generateKey").call(data)
                .addOnSuccessListener(r -> {
                    Map<?,?> m = (Map<?,?>) r.getData();
                    generated[0] = String.valueOf(m.get("key"));
                    result.setText(generated[0]);
                })
                .addOnFailureListener(e -> toast(friendly(e)));
        });
        copy.setOnClickListener(v -> {
            if (generated[0].isEmpty()) { toast("Generate key dahulu"); return; }
            android.content.ClipboardManager cm = (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("XIT VOID KEY", generated[0]));
            toast("Key disalin");
        });
    }

    private void addBrand() {
        TextView brand = text("XIT VOID", 30, RED, true);
        brand.setGravity(Gravity.CENTER);
        brand.setLetterSpacing(.12f);
        root.addView(brand, lp(-1, 66));
        TextView line = text("━━━━━━━━━━━━━━━━━━━━━━━━", 12, RED, true);
        line.setGravity(Gravity.CENTER);
        root.addView(line, lp(-1, 20));
    }

    private void cardTitle(String title, String subtitle) {
        space(body, 8);
        TextView t = text(title, 19, Color.WHITE, true);
        body.addView(t, lp(-1, 36));
        TextView s = text(subtitle, 10, RED, true);
        body.addView(s, lp(-1, 24));
        space(body, 8);
    }

    private void addSwitch(String title, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setPadding(dp(14), dp(8), dp(12), dp(8));
        box.setBackground(round(PANEL, 13, Color.rgb(45,45,55)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 14, Color.WHITE, true);
        TextView d = text(description, 9, Color.LTGRAY, false);
        labels.addView(t); labels.addView(d);
        box.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(this);
        sw.setButtonTintList(null);
        box.addView(sw);
        body.addView(box, lp(-1, 76));
        space(body, 9);
    }

    private void gameButton(String title, String packageName) {
        Button b = button("▶  START " + title, true);
        body.addView(b, lp(-1, 52));
        space(body, 8);
        b.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) toast(title + " belum terpasang");
            else startActivity(launch);
        });
    }

    private void info(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(13), dp(12), dp(13), dp(12));
        row.setBackground(round(PANEL, 10, Color.TRANSPARENT));
        TextView a = text(label, 11, Color.LTGRAY, true);
        TextView b = text(value, 12, Color.WHITE, true);
        row.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(b);
        body.addView(row, lp(-1, 48));
        space(body, 7);
    }

    private Button button(String label, boolean primary) {
        return button(label, primary, true);
    }
    private Button button(String label, boolean primary, boolean full) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(round(primary ? RED : PANEL, 12, primary ? RED : Color.rgb(70,70,80)));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }
    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }
    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }
    private void space(LinearLayout parent, int h) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void showError(String s) {
        setupRoot(); addBrand();
        TextView t = text(s, 12, Color.WHITE, false); root.addView(t);
        Button retry = button("RETRY", true); root.addView(retry, lp(-1, 50));
        retry.setOnClickListener(v -> auth.signInAnonymously().addOnSuccessListener(x -> restoreSession()).addOnFailureListener(e -> showError(e.getMessage())));
    }
    private String friendly(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException f = (FirebaseFunctionsException)e;
            if (f.getCode() == FirebaseFunctionsException.Code.PERMISSION_DENIED) return "Key tidak valid, sudah dipakai, atau akses ditolak.";
            return "Server: " + f.getMessage();
        }
        return "Koneksi gagal: " + e.getMessage();
    }
    private String mask(String s) {
        if (s == null || s.length() < 5) return "••••••••";
        return s.substring(0, 4) + "-•••••-••••";
    }
    private long folderSize(java.io.File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long n = 0; java.io.File[] list = f.listFiles();
        if (list != null) for (java.io.File x : list) n += folderSize(x);
        return n;
    }
    private void deleteContents(java.io.File f) {
        java.io.File[] list = f.listFiles();
        if (list != null) for (java.io.File x : list) {
            if (x.isDirectory()) deleteContents(x);
            x.delete();
        }
    }
    private String formatBytes(long n) {
        return String.format(Locale.US, "%.1f KB", n / 1024.0);
    }
  }
          
