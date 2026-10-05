package com.ronyxdumb.rassegnascuola;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
public final class CoreChecks {
    private static int checks;
    private static void require(boolean value,String name) { checks++; if(!value) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        require(Sector.values().length==7,"seven sectors");
        require(UpdateChecker.compare("v3.10.0","3.9.9")>0,"numeric version ordering");
        require(UpdateChecker.compare("v3.0","3.0.0")==0,"equal versions");
        require(UpdateChecker.compare("v2.9.9","3.0.0")<0,"older version");
        try { UpdateChecker.compare("banana","3.0.0"); throw new AssertionError("invalid tag accepted"); } catch(IOException expected) { checks++; }
        String base="{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v3.1.0\",\"html_url\":\"https://github.com/"+AppInfo.REPOSITORY+"/releases/tag/v3.1.0\",\"assets\":[";
        String json=base+"{\"name\":\"RassegnaScuola-debug.apk\",\"browser_download_url\":\"https://github.com/"+AppInfo.REPOSITORY+"/releases/download/v3.1.0/debug.apk\"},{\"name\":\"RassegnaScuola-3.1.0-android.apk\",\"browser_download_url\":\"https://github.com/"+AppInfo.REPOSITORY+"/releases/download/v3.1.0/android.apk\"},{\"name\":\"RassegnaScuola-3.1.0-windows-x64.zip\",\"browser_download_url\":\"https://github.com/"+AppInfo.REPOSITORY+"/releases/download/v3.1.0/windows.zip\"}]}";
        UpdateChecker.Result android=UpdateChecker.parse(json,UpdateChecker.Target.ANDROID,"3.0.0");
        require(android.newer && android.asset.endsWith("android.apk"),"Android correct asset, skip debug");
        require(UpdateChecker.parse(json,UpdateChecker.Target.WINDOWS,"3.0.0").asset.contains("windows-x64"),"Windows correct asset");
        require(!UpdateChecker.parse(json,UpdateChecker.Target.ANDROID,"3.2.0").newer,"no downgrade");
        require(!UpdateChecker.parse(json.replace("\"prerelease\":false","\"prerelease\":true"),UpdateChecker.Target.ANDROID,"3.0.0").published,"ignore prerelease");
        require(UpdateChecker.parse(base+"]}",UpdateChecker.Target.ANDROID,"3.0.0").asset.isEmpty(),"release page fallback");
        try { UpdateChecker.parse(json.replace("github.com/","github.com.evil/"),UpdateChecker.Target.ANDROID,"3.0.0"); throw new AssertionError("untrusted URL accepted"); } catch(IOException expected) { checks++; }
        String rss="<rss><channel><item><title>A &amp; B</title><link>https://example.org/one</link><description><![CDATA[<p>Una descrizione con <b>testo</b> completo e informativo.</p>]]></description><pubDate>Mon, 05 Oct 2026 10:30:00 +0200</pubDate></item></channel></rss>";
        NewsEngine.Item parsed=NewsEngine.parseFeed(rss.getBytes(StandardCharsets.UTF_8),"Test","Italia").get(0);
        require(parsed.title.equals("A & B"),"decode RSS title");
        require(parsed.description.contains("testo completo"),"HTML cleanup");
        require(parsed.published==Instant.parse("2026-10-05T08:30:00Z").toEpochMilli(),"date timezone");
        require(NewsEngine.parseDate("unknown")==0,"reject undated news");
        String atom="<feed xmlns='http://www.w3.org/2005/Atom'><entry><title>Atom</title><link href='https://example.org/two'/><summary>Un estratto di almeno trentacinque caratteri.</summary><published>2026-10-05T08:00:00Z</published></entry></feed>";
        require(NewsEngine.parseFeed(atom.getBytes(StandardCharsets.UTF_8),"Test","Italia").get(0).url.endsWith("two"),"Atom links");
        require(NewsEngine.parseFeed(rss.getBytes(StandardCharsets.UTF_8),"Google News","Italia").get(0).description.isEmpty(),"Google link lists never treated as summary");
        require(NewsEngine.clean("&#x1F30D; &amp; &#232;").equals("🌍 & è"),"numeric HTML entities");
        require(NewsEngine.summarize("").equals("Estratto non disponibile."),"no invented summaries");
        long now=System.currentTimeMillis();
        List<NewsEngine.Item> items=new ArrayList<>();
        String[] titles={"Governo approva riforma scuola italiana","Parlamento discute bilancio e fisco","Elezioni regionali risultati e affluenza","Ministro presenta piano per lavoro","Partiti incontrano delegazione europea","Comune modifica trasporto pubblico","Senato vota decreto energia"};
        for(String title:titles) { NewsEngine.Item i=new NewsEngine.Item(); i.title=title; i.url="https://example.org/"+items.size(); i.published=now-1000; i.source="Test"; items.add(i); }
        NewsEngine.Item old=new NewsEngine.Item(); old.title="Articolo di ieri";old.url="https://example.org/old";old.published=now-86400000;items.add(old);
        NewsEngine.Item unknown=new NewsEngine.Item();unknown.title="Senza data";unknown.url="https://example.org/unknown";items.add(unknown);
        NewsEngine.Item duplicate=new NewsEngine.Item(); duplicate.title=items.get(0).title; duplicate.url=items.get(0).url+"?utm_source=x"; duplicate.published=now-1000; items.add(duplicate);
        NewsEngine.Item future=new NewsEngine.Item(); future.title="Dal futuro";future.url="https://example.org/future";future.published=now+3600000;items.add(future);
        List<NewsEngine.Item> selected=new NewsEngine().select(items,Sector.POLITICS);
        require(selected.size()==5,"five distinct eligible items");
        require(!selected.contains(old) && !selected.contains(unknown) && !selected.contains(future),"today only, no undated/future");
        Set<String> urls=new HashSet<>();for(NewsEngine.Item i:selected)urls.add(i.url);
        require(urls.size()==5,"deduplicate tracking URLs");
        require(new NewsEngine().select(Collections.singletonList(items.get(0)),Sector.POLITICS).size()==1,"do not fill with old articles");
        parsed.summary="Test < & >";
        byte[] doc=DocxExporter.build(Collections.singletonList(parsed),Collections.singletonList("Test"),Sector.POLITICS);
        int entries=0;boolean body=false;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(doc))) { ZipEntry entry;while((entry=zip.getNextEntry())!=null) { entries++;ByteArrayOutputStream data=new ByteArrayOutputStream(); byte[] b=new byte[4096];int n;while((n=zip.read(b))!=-1)data.write(b,0,n); DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(data.toByteArray())); if(entry.getName().equals("word/document.xml"))body=data.toString("UTF-8").contains("Test &lt; &amp; &gt;"); } }
        require(entries==3 && body,"valid DOCX package and XML escaping");
        require(!AppInfo.VERSION.isEmpty() && UpdateChecker.compare(AppInfo.VERSION,AppInfo.VERSION)==0,"packaged app configuration");
        System.out.println("Passed " + checks + " shared checks.");
    }
}
