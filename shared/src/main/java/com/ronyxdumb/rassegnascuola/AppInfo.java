package com.ronyxdumb.rassegnascuola;
import java.util.Properties;
public final class AppInfo {
    private static final Properties CONFIG = new Properties();
    static { try (java.io.InputStream in = AppInfo.class.getResourceAsStream("/app.properties")) {
        if (in == null) throw new IllegalStateException("app.properties assente"); CONFIG.load(in);
    } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    public static final String VERSION = CONFIG.getProperty("versionName");
    public static final String REPOSITORY = CONFIG.getProperty("updateRepository");
    public static final String RELEASES = "https://github.com/" + REPOSITORY + "/releases";
    private AppInfo() {}
}
