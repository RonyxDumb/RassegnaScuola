package com.ronyxdumb.rassegnascuola;

/** A sector controls sources, search queries, history and document titles. */
public enum Sector {
    ECONOMY("Economia", "economia", "economia", "economia imprese lavoro finanza"),
    POLITICS("Politica", "politica", "politica", "politica italiana governo parlamento partiti elezioni"),
    GEOPOLITICS("Geo Politica", "geo_politica", "mondo", "geopolitica diplomazia relazioni internazionali conflitti"),
    SCIENCE("Scienze e tecnologia", "scienze_tecnologia", "tecnologia", "scienza tecnologia ricerca spazio innovazione"),
    SPORT("Sport", "sport", "sport", "sport calcio tennis basket atletica"),
    CRIME("Cronaca", "cronaca", "cronaca", "cronaca italiana indagini incidenti giustizia"),
    CURRENT("Attualità", "attualita", null, "attualità società scuola cultura ambiente salute");

    public final String label, key, section, query;
    Sector(String label, String key, String section, String query) {
        this.label = label; this.key = key; this.section = section; this.query = query;
    }
    public String rss() {
        if (this == SCIENCE) return "https://www.ansa.it/canale_tecnologia/notizie/tecnologia_rss.xml";
        return section == null ? "https://www.ansa.it/sito/ansait_rss.xml"
                : "https://www.ansa.it/sito/notizie/" + section + "/" + section + "_rss.xml";
    }
}
