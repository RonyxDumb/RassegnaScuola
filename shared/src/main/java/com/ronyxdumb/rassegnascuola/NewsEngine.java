package com.ronyxdumb.rassegnascuola;

import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.net.URI;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NewsEngine {
    public static final class Item {
        public String title = "", url = "", source = "", description = "", scope = "Da verificare", summary = "";
        public long published = 0;
        public double score = 0;
        public final List<String> signals = new ArrayList<>();
    }
    public static final class Result {
        public final List<Item> items = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        public int total;
        public Sector sector;
    }

    private static final Set<String> ITALY = wordsSet("italia italiano italiana roma governo istat inps mef fisco manovra");
    private static final Set<String> WORLD = wordsSet("mondo globale ue europa usa cina germania francia bce fed dazi export");
    private static final Set<String> STOP = wordsSet("della delle degli nella nelle sono come anche dopo alla allo agli sulla sulle per con che del dei una uno tra fra non piu suo sua dal dai nel");
    private static final Pattern WORD = Pattern.compile("[a-zà-öø-ÿ0-9]{3,}");
    private static final int LIMIT = 35;
    public NewsEngine() {}

    public Result scan(Sector sector) throws Exception {
        if (sector == null) throw new IllegalArgumentException("Scegli un argomento.");
        Result result = new Result();
        List<Callable<FeedResult>> jobs = new ArrayList<>();
        result.sector = sector;
        result.notes.add("Argomento: " + sector.label + "; notizie pubblicate oggi (Europe/Rome).");
        jobs.add(() -> feed(sector.rss(), "ANSA", "Da verificare"));
        String sectionQuery = sector == Sector.SCIENCE ? "site:ansa.it/canale_tecnologia" : sector.section == null ? "site:ansa.it " + sector.query : "site:ansa.it/sito/notizie/" + sector.section + "/";
        jobs.add(() -> feed(google(sectionQuery), "Google News", "Da verificare"));
        if (sector == Sector.SCIENCE)
            jobs.add(() -> feed("https://www.ansa.it/canale_scienza_tecnica/notizie/scienzaetecnica_rss.xml", "ANSA", "Da verificare"));
        String[] sections;
        switch (sector) {
            case ECONOMY: sections = new String[]{"site:ilsole24ore.com economia", "site:rainews.it economia"}; break;
            case POLITICS: sections = new String[]{"site:rainews.it politica italiana", "site:adnkronos.com/politica"}; break;
            case GEOPOLITICS: sections = new String[]{"site:rainews.it esteri diplomazia", "site:adnkronos.com/internazionale"}; break;
            case SCIENCE: sections = new String[]{"site:rainews.it scienza tecnologia", "site:ansa.it/canale_scienza_tecnica"}; break;
            case SPORT: sections = new String[]{"site:raisport.rai.it", "site:adnkronos.com/sport"}; break;
            case CRIME: sections = new String[]{"site:rainews.it cronaca", "site:adnkronos.com/cronaca"}; break;
            default: sections = new String[]{"site:rainews.it società scuola ambiente", "site:ansa.it attualità società"}; break;
        }
        for (String query : sections) jobs.add(() -> feed(google(query), "Google News", "Da verificare"));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<FeedResult>> futures = pool.invokeAll(jobs);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    FeedResult fetched = futures.get(i).get();
                    result.items.addAll(fetched.items);
                    result.notes.add(fetched.note);
                } catch (Exception e) {
                    result.notes.add("Fonte " + (i + 1) + ": errore (" + e.getClass().getSimpleName() + ")");
                }
            }
        } finally {
            pool.shutdownNow();
        }
        result.total = result.items.size();
        List<Item> chosen = select(result.items, sector);
        result.items.clear();
        result.items.addAll(chosen);
        if (chosen.isEmpty()) throw new IllegalStateException("Nessuna notizia di oggi trovata per " + sector.label + ". Verifica la connessione e riprova.");
        if (chosen.size() < 5) result.notes.add("Disponibili soltanto " + chosen.size() + " notizie di oggi: nessuna notizia vecchia aggiunta per arrivare a cinque.");
        for (Item item : chosen) item.summary = summarize(item.description);
        return result;
    }

    private static String google(String q) {
        try {
            return "https://news.google.com/rss/search?q=" + URLEncoder.encode(q + dateQuery(), "UTF-8") + "&hl=it&gl=IT&ceid=IT:it";
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String dateQuery() {
        java.util.Calendar today = java.util.Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(today.getTimeZone());
        String after = format.format(today.getTime());
        today.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return " after:" + after + " before:" + format.format(today.getTime());
    }
    private static class FeedResult {
        public final List<Item> items = new ArrayList<>();
        String note;
    }
    private FeedResult feed(String url, String source, String scope) {
        FeedResult out = new FeedResult();
        String kind = source + " " + scope;
        try {
            byte[] bytes = request(url);
            out.items.addAll(parseFeed(bytes, source, scope));
            out.note = kind + ": " + out.items.size() + " risultati";
        } catch (Exception e) {
            out.note = kind + ": errore (" + e.getClass().getSimpleName() + ")";
        }
        return out;
    }
    private static byte[] request(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RassegnaScuola/3.0");
        try {
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300)
                throw new java.io.IOException("HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = stream.read(buf)) != -1) {
                    if (out.size() + n > 3_000_000) throw new java.io.IOException("Risposta troppo grande");
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } finally { connection.disconnect(); }
    }
    public static List<Item> parseFeed(byte[] bytes, String source, String scope) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        List<Item> items = new ArrayList<>();
        javax.xml.parsers.SAXParser parser = factory.newSAXParser();
        org.xml.sax.XMLReader reader = parser.getXMLReader();
        reader.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
        reader.setContentHandler(new DefaultHandler() {
            Item item;
            String field = "";
            StringBuilder value = new StringBuilder();
            @Override public void startElement(String uri, String local, String qName, Attributes atts) {
                String tag = local.isEmpty() ? qName : local;
                if (tag.equals("item") || tag.equals("entry")) {
                    item = new Item(); item.source = source; item.scope = scope;
                } else if (item != null && field.isEmpty()) {
                    field = tag.toLowerCase(Locale.ROOT); value.setLength(0);
                    if (field.equals("link") && atts.getValue("href") != null) item.url = atts.getValue("href");
                }
            }
            @Override public void characters(char[] chars, int start, int length) {
                if (item != null && !field.isEmpty()) value.append(chars, start, length);
            }
            @Override public void endElement(String uri, String local, String qName) {
                String tag = (local.isEmpty() ? qName : local).toLowerCase(Locale.ROOT);
                if (item == null) return;
                if (tag.equals(field)) {
                    String text = value.toString();
                    switch (field) {
                        case "title": item.title = clean(text); break;
                        case "source": if (!clean(text).isEmpty()) item.source = clean(text); break;
                        case "link": if (item.url.isEmpty()) item.url = text.trim(); break;
                        case "description": case "summary": if (item.description.isEmpty()) item.description = clean(text); break;
                        case "pubdate": case "published": case "updated": if (item.published == 0) item.published = parseDate(text); break;
                    }
                    field = "";
                }
                if (tag.equals("item") || tag.equals("entry")) {
                    if (source.equals("Google News")) item.description = "";
                    if (items.size() < LIMIT && !item.title.isEmpty() && item.url.startsWith("https://")) items.add(item);
                    item = null; field = "";
                }
            }
        });
        reader.parse(new org.xml.sax.InputSource(new java.io.ByteArrayInputStream(bytes)));
        return items;
    }
    public static long parseDate(String raw) {
        for (DateTimeFormatter format : new DateTimeFormatter[]{DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME}) {
            try { return ZonedDateTime.parse(raw.trim(), format).toInstant().toEpochMilli(); }
            catch (Exception ignored) { }
        }
        return 0;
    }
    public static String clean(String raw) {
        String text = (raw == null ? "" : raw).replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", "")
                .replaceAll("(?s)<[^>]*>", " ");
        java.util.regex.Matcher m = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String token = m.group(1); String decoded = "";
            try { int cp = token.startsWith("x") ? Integer.parseInt(token.substring(1),16) : Integer.parseInt(token); decoded = new String(Character.toChars(cp)); } catch (Exception ignored) { }
            m.appendReplacement(out, Matcher.quoteReplacement(decoded));
        }
        m.appendTail(out);
        return out.toString().replace("&nbsp;", " ").replace("&quot;", "\"").replace("&apos;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replaceAll("\\s+", " ").trim();
    }
    private static List<String> words(String raw) {
        List<String> result = new ArrayList<>();
        Matcher matcher = WORD.matcher(raw.toLowerCase(Locale.ROOT));
        while (matcher.find()) result.add(matcher.group());
        return result;
    }
    private static Set<String> wordsSet(String text) { return new HashSet<>(Arrays.asList(text.split(" "))); }
    private static double similarity(String a, String b) {
        Set<String> x = new HashSet<>(words(a)), y = new HashSet<>(words(b));
        x.removeAll(STOP); y.removeAll(STOP);
        if (x.isEmpty() || y.isEmpty()) return 0;
        Set<String> union = new HashSet<>(x); union.addAll(y);
        x.retainAll(y);
        return (double) x.size() / union.size();
    }
    private static String canonical(String raw) {
        try {
            URI uri = new URI(raw);
            List<String> query = new ArrayList<>();
            if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&"))
                if (!part.toLowerCase(Locale.ROOT).startsWith("utm_")) query.add(part);
            String url = uri.getScheme() + "://" + uri.getRawAuthority() + (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (query.isEmpty() ? "" : "?" + String.join("&", query));
            return url.endsWith("/") ? url.substring(0, url.length()-1) : url;
        } catch (Exception e) { return raw; }
    }
    public List<Item> select(List<Item> items, Sector sector) {
        long now = System.currentTimeMillis();
        java.util.Calendar start = java.util.Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        start.set(java.util.Calendar.HOUR_OF_DAY, 0); start.set(java.util.Calendar.MINUTE, 0);
        start.set(java.util.Calendar.SECOND, 0); start.set(java.util.Calendar.MILLISECOND, 0);
        long cutoff = start.getTimeInMillis();
        List<Item> unique = new ArrayList<>();
        for (Item item : items) {
            item.url = canonical(item.url);
            // Undated entries cannot be presented as today's news.
            if (item.published < cutoff || item.published > now + 5 * 60_000L) continue;
            boolean duplicate = false;
            for (Item old : unique) if (item.url.equals(old.url) || similarity(item.title, old.title) >= .72) { duplicate = true; break; }
            if (duplicate) continue;
            Set<String> terms = new HashSet<>(words(item.title + " " + item.description));
            Set<String> italy = new HashSet<>(terms), world = new HashSet<>(terms);
            italy.retainAll(ITALY); world.retainAll(WORLD);
            if (italy.size() > world.size()) item.scope = "Italia";
            else if (world.size() > italy.size()) item.scope = "Internazionale";
            Set<String> relevant = new HashSet<>(terms); relevant.retainAll(wordsSet(sector.query));
            double age = item.published == 0 ? 18 : Math.max(0, (now - item.published) / 3600000.0);
            item.score = Math.max(0, 8 - age / 8) + Math.min(5, relevant.size() * .7) + (item.description.length() >= 35 ? 2.5 : 0);
            if (item.signals.contains("prima pagina")) item.score += 2;
            unique.add(item);
        }
        for (Item item : unique) {
            int corroborations = 0;
            for (Item other : unique) if (!item.source.equals(other.source) && similarity(item.title, other.title) >= .38) corroborations++;
            item.score += Math.min(4, corroborations * 2);
            if (corroborations > 0) item.signals.add("tema presente su " + (corroborations + 1) + " fonti");
        }
        unique.sort((a, b) -> Double.compare(b.score, a.score));
        List<Item> chosen = new ArrayList<>();
        for (String scope : new String[]{"Italia", "Internazionale"})
            for (Item item : unique) if (item.scope.equals(scope)) { if (!chosen.contains(item)) chosen.add(item); break; }
        for (Item item : unique) {
            if (chosen.size() >= 5) break;
            boolean similar = false;
            for (Item old : chosen) if (similarity(item.title, old.title) >= .55) { similar = true; break; }
            if (!chosen.contains(item) && !similar) chosen.add(item);
        }
        chosen.sort((a, b) -> Double.compare(b.score, a.score));
        return chosen;
    }
    public static String summarize(String raw) {
        String text = raw.replaceAll("\\s+", " ").trim();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> usable = new ArrayList<>();
        for (String sentence : sentences) if (sentence.length() >= 35) usable.add(sentence);
        if (usable.isEmpty()) return "Estratto non disponibile.";
        Map<String, Integer> freq = new HashMap<>();
        for (String word : words(text)) if (!STOP.contains(word)) freq.put(word, freq.getOrDefault(word, 0) + 1);
        List<Integer> ranking = new ArrayList<>();
        for (int i = 0; i < Math.min(12, usable.size()); i++) ranking.add(i);
        ranking.sort((a, b) -> Double.compare(sentenceScore(usable.get(b), freq), sentenceScore(usable.get(a), freq)));
        ranking = new ArrayList<>(ranking.subList(0, Math.min(3, ranking.size())));
        Collections.sort(ranking);
        StringBuilder summary = new StringBuilder();
        for (int idx : ranking) summary.append(usable.get(idx)).append(' ');
        String out = summary.toString().trim();
        if (out.length() <= 900) return out;
        int boundary = out.lastIndexOf(' ', 899);
        return out.substring(0, boundary > 0 ? boundary : 899) + "…";
    }
    private static double sentenceScore(String sentence, Map<String, Integer> freq) {
        List<String> tokens = words(sentence);
        double score = 0;
        for (String token : tokens) score += Math.log1p(freq.getOrDefault(token, 0));
        return score / Math.max(8, tokens.size());
    }
}

