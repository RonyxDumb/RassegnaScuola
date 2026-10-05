package com.ronyxdumb.rassegnascuola;
import com.google.gson.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
public final class UpdateChecker {
    public enum Target { ANDROID, WINDOWS }
    public static final class Result {
        public final boolean newer, published;
        public final String version, url, asset;
        Result(boolean newer, boolean published, String version, String url, String asset) {
            this.newer = newer; this.published = published; this.version = version; this.url = url; this.asset = asset;
        }
    }
    public static Result check(Target target) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL("https://api.github.com/repos/" + AppInfo.REPOSITORY + "/releases/latest").openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setRequestProperty("User-Agent", "RassegnaScuola/" + AppInfo.VERSION);
        try {
            int code = c.getResponseCode();
            if (code == 404) return new Result(false, false, "", AppInfo.RELEASES, "");
            if (code != 200) throw new IOException(code == 403 || code == 429 ? "Limite GitHub raggiunto. Riprova più tardi." : "GitHub: HTTP " + code);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try(InputStream in = c.getInputStream()) {
                byte[] b = new byte[4096]; int n;
                while ((n=in.read(b)) != -1) { if (bytes.size()+n > 1000000) throw new IOException("Risposta troppo grande"); bytes.write(b,0,n); }
            }
            return parse(bytes.toString(StandardCharsets.UTF_8.name()), target, AppInfo.VERSION);
        } finally { c.disconnect(); }
    }
    public static Result parse(String json, Target target, String installed) throws IOException {
        try {
            JsonObject release = JsonParser.parseString(json).getAsJsonObject();
            if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean())
                return new Result(false, false, "", AppInfo.RELEASES, "");
            String tag = release.get("tag_name").getAsString();
            int comparison = compare(tag, installed);
            String page = release.get("html_url").getAsString();
            if (!official(page)) throw new IOException("Indirizzo release non valido");
            String chosen = page, assetName = "";
            for (JsonElement row : release.getAsJsonArray("assets")) {
                JsonObject asset = row.getAsJsonObject(); String name = asset.get("name").getAsString();
                String lower = name.toLowerCase(Locale.ROOT);
                boolean match = target == Target.ANDROID ? lower.endsWith(".apk") && !lower.contains("debug") : lower.contains("windows") && lower.endsWith(".zip") && lower.contains("x64");
                String url = asset.get("browser_download_url").getAsString();
                if (match && official(url)) { chosen = url; assetName = name; break; }
            }
            return new Result(comparison > 0, true, tag, chosen, assetName);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Formato release non riconosciuto", e); }
    }
    private static boolean official(String address) {
        try { URI u = new URI(address); return "https".equals(u.getScheme()) && "github.com".equals(u.getHost())
            && u.getUserInfo() == null && (u.getPort() == -1 || u.getPort() == 443)
            && u.getPath().startsWith("/" + AppInfo.REPOSITORY + "/releases/"); }
        catch (Exception e) { return false; }
    }
    public static int compare(String left, String right) throws IOException {
        String a = left.replaceFirst("^[vV]", ""), b = right.replaceFirst("^[vV]", "");
        if (!a.matches("[0-9]+(?:\\.[0-9]+){1,3}") || !b.matches("[0-9]+(?:\\.[0-9]+){1,3}")) throw new IOException("Usa tag numerici, per esempio v3.0.1");
        String[] x=a.split("\\."), y=b.split("\\.");
        for(int i=0;i<Math.max(x.length,y.length);i++) {
            java.math.BigInteger p = new java.math.BigInteger(i<x.length ? x[i] : "0"), q = new java.math.BigInteger(i<y.length ? y[i] : "0");
            int c = p.compareTo(q); if(c != 0) return c;
        }
        return 0;
    }
    private UpdateChecker() {}
}
