package com.ronyxdumb.rassegnascuola;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private Colors c;
    private ScrollView scroll;
    private LinearLayout page, results, actions;
    private TextView generate, status, resultHeading;
    private ProgressBar progress;
    private Uri documentUri;
    private boolean scanning, checkingUpdates;
    private Sector selected;
    private final java.util.List<TextView> sectorButtons = new java.util.ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        c = new Colors();
        if (state != null && state.containsKey("sector")) {
            try { selected = Sector.valueOf(state.getString("sector")); } catch (Exception ignored) { }
        }
        configureBars();
        buildScreen();
        android.content.SharedPreferences prefs = getSharedPreferences("updates", MODE_PRIVATE);
        if (prefs.getBoolean("automatic", true) && System.currentTimeMillis() - prefs.getLong("last", 0) > 86400000L) checkUpdates(false);
    }
    private void configureBars() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(c.background);
        getWindow().setNavigationBarColor(c.background);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (!c.night) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(c.night ? 0 : appearance, appearance);
        }
    }
    private void buildScreen() {
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(c.background);
        LinearLayout frame = column();
        frame.setBackgroundColor(c.background);
        View topBar = new View(this), bottomBar = new View(this);
        topBar.setBackgroundColor(c.background); bottomBar.setBackgroundColor(c.background);
        frame.addView(topBar, params(-1, 0));
        frame.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        frame.addView(bottomBar, params(-1, 0));
        scroll.setPadding(dp(22), dp(20), dp(22), dp(28));
        frame.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = system.top; bottom = system.bottom; left = system.left; right = system.right;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft(); right = insets.getSystemWindowInsetRight();
            }
            topBar.getLayoutParams().height = top; bottomBar.getLayoutParams().height = bottom;
            frame.setPadding(left, 0, right, 0); frame.requestLayout();
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });
        page = column();
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(frame);
        frame.requestApplyInsets();
        TextView eyebrow = text(new SimpleDateFormat("EEEE d MMMM", Locale.ITALY).format(new Date()).toUpperCase(Locale.ITALY), 12, true, c.muted);
        eyebrow.setLetterSpacing(.15f);
        page.addView(eyebrow);
        spacer(page, 14);
        page.addView(hero(), params(-1, -2));
        spacer(page, 18);
        page.addView(sectorPicker(), params(-1, -2));
        spacer(page, 16);
        page.addView(actionCard(), params(-1, -2));
        updateSectorButtons();
        spacer(page, 24);
        resultHeading = text("Le notizie selezionate", 23, true, c.ink);
        resultHeading.setVisibility(View.GONE);
        page.addView(resultHeading);
        results = column();
        page.addView(results);
        spacer(page, 8);
    }
    private View hero() {
        LinearLayout card = column();
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.ic_news_mark);
        header.addView(icon, params(dp(44), dp(44)));
        TextView name = text("Rassegna\nScuola", 32, true, c.ink);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,-2,1); nameParams.leftMargin=dp(14);
        header.addView(name, nameParams); card.addView(header);
        spacer(card, 16);
        TextView info = action("Impostazioni", c.surface, c.subtle);
        info.setOnClickListener(v -> settings()); card.addView(info, params(-1,-2));
        return card;
    }
    private void settings() {
        android.content.SharedPreferences prefs = getSharedPreferences("updates", MODE_PRIVATE);
        LinearLayout body = column(); body.setPadding(dp(24),dp(12),dp(24),dp(12));
        android.widget.Switch automatic = new android.widget.Switch(this);
        automatic.setText("Controlla aggiornamenti"); automatic.setTextColor(c.ink);
        automatic.setChecked(prefs.getBoolean("automatic", true));
        automatic.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean("automatic", checked).apply());
        body.addView(automatic); spacer(body,16);
        body.addView(text("Versione " + AppInfo.VERSION + "\nFrancesco Pio Pipino",14,false,c.muted));
        new android.app.AlertDialog.Builder(this).setTitle("Impostazioni").setView(body)
            .setPositiveButton("Verifica ora",(d,w)->checkUpdates(true)).setNegativeButton("Chiudi",null).show();
    }
    private void checkUpdates(boolean manual) {
        if (checkingUpdates) return;
        checkingUpdates = true;
        getSharedPreferences("updates",MODE_PRIVATE).edit().putLong("last",System.currentTimeMillis()).apply();
        if (manual) Toast.makeText(this,"Controllo aggiornamenti…",Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                UpdateChecker.Result result = UpdateChecker.check(UpdateChecker.Target.ANDROID);
                runOnUiThread(() -> {
                    checkingUpdates=false; if(isDestroyed()) return;
                    if (result.newer) new android.app.AlertDialog.Builder(this).setTitle("Versione " + result.version)
                        .setMessage("È disponibile un aggiornamento.")
                        .setPositiveButton(result.asset.isEmpty() ? "Apri release" : "Scarica", (d,w) -> {
                            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.url))); }
                            catch(ActivityNotFoundException e) { Toast.makeText(this,"Nessun browser disponibile",Toast.LENGTH_LONG).show(); }
                        }).setNegativeButton("Più tardi",null).show();
                    else if(manual) Toast.makeText(this,result.published ? "Hai la versione più recente." : "Nessuna release pubblicata.",Toast.LENGTH_LONG).show();
                });
            } catch(Exception e) { runOnUiThread(() -> { checkingUpdates=false; if(manual && !isDestroyed()) Toast.makeText(this,"Controllo non riuscito: " + e.getMessage(),Toast.LENGTH_LONG).show(); }); }
        },"RassegnaScuola-updates").start();
    }
    private View sectorPicker() {
        LinearLayout box = column();
        box.addView(text("Argomento", 19, true, c.ink));
        spacer(box, 12);
        LinearLayout line = null;
        int index = 0;
        for (Sector sector : Sector.values()) {
            if (index % 2 == 0) { line = row(); box.addView(line, params(-1,-2)); }
            TextView button = action(sector.label, c.surface, c.ink);
            button.setTextSize(14); button.setMinHeight(dp(56));
            button.setTag(sector);
            button.setOnClickListener(v -> {
                if (scanning) return;
                selected = sector;
                results.removeAllViews(); resultHeading.setVisibility(View.GONE);
                documentUri = null; actions.setVisibility(View.GONE);
                updateSectorButtons(); status.setText("");
            });
            sectorButtons.add(button);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,-2,1);
            lp.bottomMargin = dp(8); if(index % 2 == 0) lp.rightMargin=dp(8);
            line.addView(button,lp); index++;
        }
        return box;
    }
    private void updateSectorButtons() {
        for (TextView button : sectorButtons) {
            boolean active = button.getTag() == selected;
            button.setSelected(active);
            button.setTextColor(active ? c.onPrimary : c.ink);
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(c.ripple), shape(active ? c.primary : c.surface, 16), null));
            button.setEnabled(!scanning);
        }
        generate.setEnabled(selected != null && !scanning);
        generate.setAlpha(selected == null ? .45f : 1f);
        generate.setText(selected == null ? "Scegli un argomento" : "Crea · " + selected.label + "  →");
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (selected != null) out.putString("sector", selected.name());
    }
    private View actionCard() {
        LinearLayout card = column();
        card.setPadding(dp(20), dp(21), dp(20), dp(20));
        card.setBackground(shape(c.surface, 28));
        card.setElevation(dp(3));
        card.addView(text("Oggi", 12, true, c.muted));
        spacer(card, 6);
        card.addView(text("La tua rassegna", 22, true, c.ink));
        spacer(card, 5);
        spacer(card, 20);
        generate = action("Genera rassegna  →", c.primary, c.onPrimary);
        generate.setOnClickListener(v -> generate());
        card.addView(generate, params(-1, -2));
        progress = new ProgressBar(this);
        progress.setIndeterminateTintList(ColorStateList.valueOf(c.primary));
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams loader = params(dp(30), dp(30));
        loader.gravity = Gravity.CENTER_HORIZONTAL;
        loader.topMargin = dp(15);
        card.addView(progress, loader);
        status = text("", 13, false, c.muted);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = params(-1, -2);
        statusParams.topMargin = dp(11);
        card.addView(status, statusParams);
        actions = row();
        actions.setVisibility(View.GONE);
        LinearLayout.LayoutParams actionParams = params(-1, -2);
        actionParams.topMargin = dp(15);
        card.addView(actions, actionParams);
        TextView open = action("Apri Word", c.tag, c.primary);
        open.setOnClickListener(v -> viewDocument());
        TextView share = action("Condividi", c.tag, c.primary);
        share.setOnClickListener(v -> shareDocument());
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1);
        first.rightMargin = dp(8);
        actions.addView(open, first);
        actions.addView(share, new LinearLayout.LayoutParams(0, -2, 1));
        return card;
    }
    private void generate() {
        if (scanning || selected == null) return;
        final Sector sector = selected;
        scanning = true;
        for (TextView button : sectorButtons) button.setEnabled(false);
        results.removeAllViews(); resultHeading.setVisibility(View.GONE);
        generate.setEnabled(false);
        if (ValueAnimator.areAnimatorsEnabled()) generate.animate().alpha(.55f).setDuration(180).start();
        progress.setVisibility(View.VISIBLE);
        status.setText("Leggo le fonti pubbliche…");
        actions.setVisibility(View.GONE);
        documentUri = null;
        new Thread(() -> {
            try {
                NewsEngine engine = new NewsEngine();
                NewsEngine.Result result = engine.scan(sector);
                Uri uri = AndroidDocuments.save(this, result);
                runOnUiThread(() -> { if (!isDestroyed()) showResult(result, uri); });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    finishLoading();
                    status.setText("Scansione non riuscita: " + e.getMessage());
                });
            }
        }, "RassegnaScuola-scan").start();
    }
    private void finishLoading() {
        scanning = false;
        progress.setVisibility(View.GONE);
        updateSectorButtons();
        if (ValueAnimator.areAnimatorsEnabled()) generate.animate().alpha(1).setDuration(180).start();
        else generate.setAlpha(1);
    }
    private void showResult(NewsEngine.Result result, Uri uri) {
        finishLoading();
        documentUri = uri;
        status.setText(result.items.size() + " notizie selezionate · Word salvato in Download/RassegnaScuola");
        actions.setVisibility(View.VISIBLE);
        results.removeAllViews();
        resultHeading.setText(result.sector.label + " · " + result.items.size() + " notizie");
        resultHeading.setVisibility(View.VISIBLE);
        spacer(results, 12);
        if (result.items.size() < 5) {
            results.addView(text("Oggi sono disponibili soltanto " + result.items.size() + " notizie da queste fonti.", 13, false, c.muted));
            spacer(results, 12);
        }
        int index = 1;
        for (NewsEngine.Item item : result.items) {
            LinearLayout card = column();
            card.setPadding(dp(19), dp(19), dp(19), dp(18));
            card.setBackground(shape(c.surface, 24));
            card.setElevation(dp(2));
            TextView eyebrow = text(String.format(Locale.ITALY, "%02d", index) + "  ·  "
                    + item.source.toUpperCase(Locale.ITALY) + "  /  " + new SimpleDateFormat("HH:mm", Locale.ITALY).format(new Date(item.published)),
                    11, true, c.primary);
            eyebrow.setLetterSpacing(.06f);
            card.addView(eyebrow);
            spacer(card, 9);
            TextView title = text(item.title, 19, true, c.ink);
            title.setLineSpacing(dp(3), 1);
            card.addView(title);
            spacer(card, 10);
            TextView summary = text(item.summary, 14, false, c.subtle);
            summary.setLineSpacing(dp(4), 1);
            card.addView(summary);
            spacer(card, 13);
            card.addView(text("Leggi l'articolo  ↗", 13, true, c.primary));
            card.setClickable(true);
            card.setFocusable(true);
            card.setContentDescription("Apri articolo: " + item.title);
            card.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.url))); }
                catch (ActivityNotFoundException ex) { Toast.makeText(this, "Nessun browser disponibile", Toast.LENGTH_SHORT).show(); }
            });
            LinearLayout.LayoutParams cardParams = params(-1, -2);
            cardParams.bottomMargin = dp(12);
            results.addView(card, cardParams);
            if (ValueAnimator.areAnimatorsEnabled()) {
                card.setAlpha(0);
                card.setTranslationY(dp(18));
                card.animate().alpha(1).translationY(0).setStartDelay((index - 1) * 60L).setDuration(330).start();
            }
            index++;
        }
    }
    private void viewDocument() {
        if (documentUri == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(documentUri, DOCX)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Installa un'app che apre documenti Word.", Toast.LENGTH_LONG).show(); }
    }
    private void shareDocument() {
        if (documentUri == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND).setType(DOCX)
                .putExtra(Intent.EXTRA_STREAM, documentUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Condividi rassegna"));
    }
    private TextView action(String title, int background, int foreground) {
        TextView view = text(title, 15, true, foreground);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setMinHeight(dp(48));
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(c.ripple), shape(background, 18), null));
        return view;
    }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        view.setFontFeatureSettings("kern");
        view.setTypeface(Typeface.DEFAULT);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }
    private LinearLayout column() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); return box; }
    private LinearLayout row() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.HORIZONTAL); return box; }
    private LinearLayout.LayoutParams params(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private void spacer(LinearLayout parent, int h) { parent.addView(new View(this), params(1, dp(h))); }
    private GradientDrawable shape(int fill, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private final class Colors {
        final boolean night = true;
        final int background = Color.parseColor("#15171D");
        final int surface = Color.parseColor("#22252E");
        final int ink = Color.parseColor("#F3F2F7");
        final int subtle = Color.parseColor("#D2D1DC");
        final int muted = Color.parseColor("#A4A5B6");
        final int primary = Color.parseColor("#C4B8ED");
        final int onPrimary = Color.parseColor("#232031");
        final int tag = Color.parseColor("#343041");
        final int ripple = Color.parseColor("#544867");
    }
}
