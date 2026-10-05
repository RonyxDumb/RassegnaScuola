package com.ronyxdumb.rassegnascuola;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;

public final class WindowsApp extends JFrame {
    static final Color BG = new Color(0x171920), SURFACE = new Color(0x242731), INK = new Color(0xF3F2F7), MUTED = new Color(0xA4A5B6), ACCENT = new Color(0xC4B8ED);
    private final Preferences prefs = Preferences.userRoot().node("com/ronyxdumb/rassegnascuola");
    private final Map<Sector,JButton> sectors = new EnumMap<>(Sector.class);
    private final JPanel results = new NewsColumn();
    private final JLabel status = label("Scegli un argomento",14,MUTED), heading = label("La tua rassegna",32,INK);
    private final JButton create = button("Crea rassegna",true), open = button("Apri Word",false), folder = button("Apri cartella",false);
    private final JProgressBar progress = new JProgressBar();
    private final JScrollPane scroller;
    private Sector selected;
    private Path document;
    private boolean scanning, checking;
    private WindowsApp(boolean preview) {
        super("Rassegna Scuola");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setMinimumSize(new Dimension(760,620)); setSize(1100,820); setLocationRelativeTo(null);
        try { setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon.png"))).getImage()); } catch(Exception ignored) { }
        JPanel root = new JPanel(new BorderLayout(28,0)); root.setBackground(BG); root.setBorder(new EmptyBorder(28,28,28,28));
        JPanel sidebar = column(); sidebar.setPreferredSize(new Dimension(225,0));
        sidebar.add(label("Rassegna",29,INK)); sidebar.add(label("Scuola",29,INK)); space(sidebar,12);
        sidebar.add(label("Francesco Pio Pipino",12,MUTED)); space(sidebar,36);
        sidebar.add(label("ARGOMENTI",11,MUTED)); space(sidebar,14);
        for(Sector s : Sector.values()) {
            JButton b = button(s.label,false); b.setHorizontalAlignment(SwingConstants.LEFT); b.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
            b.addActionListener(e -> choose(s)); sectors.put(s,b); sidebar.add(b); space(sidebar,8);
        }
        sidebar.add(Box.createVerticalGlue());
        JButton settings = button("Impostazioni",false); settings.addActionListener(e -> settings()); sidebar.add(settings); space(sidebar,10);
        sidebar.add(label("Versione " + AppInfo.VERSION,12,MUTED));
        root.add(sidebar,BorderLayout.WEST);
        JPanel content = column();
        JLabel date = label(LocalDate.now(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.ITALY)),13,MUTED);
        content.add(date); space(content,14); content.add(heading); space(content,22);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); toolbar.setOpaque(false); toolbar.setAlignmentX(LEFT_ALIGNMENT);
        create.setEnabled(false); create.addActionListener(e -> scan()); toolbar.add(create);
        open.setVisible(false); open.addActionListener(e -> openDocument()); toolbar.add(open);
        folder.setVisible(false); folder.addActionListener(e -> browseFolder()); toolbar.add(folder);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE,48)); content.add(toolbar); space(content,16);
        content.add(status); space(content,8);
        progress.setIndeterminate(true); progress.setVisible(false); progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,4)); content.add(progress); space(content,16);
        results.setBorder(new EmptyBorder(0,0,8,10));
        scroller = new JScrollPane(results); scroller.setBorder(null); scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(20); scroller.setAlignmentX(LEFT_ALIGNMENT); scroller.getViewport().setBackground(BG);
        content.add(scroller); root.add(content,BorderLayout.CENTER); setContentPane(root);
        emptyState();
        if(preview) { choose(Sector.POLITICS); }
        else if(prefs.getBoolean("automatic",true) && System.currentTimeMillis()-prefs.getLong("last",0)>86400000L) checkUpdates(false);
    }
    private void choose(Sector s) {
        if(scanning) return; selected=s; document=null; open.setVisible(false); folder.setVisible(false);
        heading.setText(s.label); status.setText("5 notizie di oggi"); create.setEnabled(true);
        for(Map.Entry<Sector,JButton> e : sectors.entrySet()) {
            e.getValue().setBackground(e.getKey()==s ? new Color(0x3C364D) : SURFACE);
            e.getValue().setForeground(e.getKey()==s ? ACCENT : INK);
        }
        emptyState();
    }
    private void emptyState() {
        results.removeAll(); JPanel card=card();
        card.add(label("La rassegna di oggi",23,INK)); space(card,12);
        card.add(wrapped("Titoli, estratti e fonti. Il documento Word viene salvato in Download/RassegnaScuola.",15,MUTED));
        results.add(card); results.revalidate(); results.repaint();
    }
    private void scan() {
        if(scanning || selected==null) return; scanning=true; Sector s=selected; setBusy(true);
        document=null; open.setVisible(false); folder.setVisible(false); results.removeAll(); results.revalidate(); results.repaint(); status.setText("Leggo le fonti…");
        new SwingWorker<NewsEngine.Result,Void>() {
            Path saved;
            protected NewsEngine.Result doInBackground() throws Exception {
                NewsEngine.Result result = new NewsEngine().scan(s);
                Path dir=Paths.get(System.getProperty("user.home"),"Downloads","RassegnaScuola"); Files.createDirectories(dir);
                String stamp=ZonedDateTime.now(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmmss_SSS"));
                saved=dir.resolve("Rassegna_"+s.key+"_"+stamp+".docx");
                Files.write(saved,DocxExporter.build(result.items,result.notes,s),StandardOpenOption.CREATE_NEW);
                return result;
            }
            protected void done() {
                scanning=false; setBusy(false);
                try { NewsEngine.Result r=get(); document=saved; display(r); }
                catch(Exception e) { Throwable cause=e.getCause()==null ? e : e.getCause(); status.setText("Ricerca non riuscita"); JOptionPane.showMessageDialog(WindowsApp.this,cause.getMessage(),"Rassegna Scuola",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    private void setBusy(boolean busy) { create.setEnabled(!busy && selected!=null); progress.setVisible(busy); for(JButton b:sectors.values()) b.setEnabled(!busy); }
    private void display(NewsEngine.Result result) {
        status.setText(result.items.size()+" notizie · Word salvato"); open.setVisible(true); folder.setVisible(true); results.removeAll(); int index=1;
        for(NewsEngine.Item item : result.items) {
            JPanel card=card();
            String time=Instant.ofEpochMilli(item.published).atZone(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("HH:mm"));
            card.add(wrapped(String.format(Locale.ITALY,"%02d  ·  %s  /  %s",index++,item.source,time),12,ACCENT)); space(card,12);
            card.add(wrapped(item.title,22,INK)); space(card,12); card.add(wrapped(item.summary,15,MUTED)); space(card,16);
            JButton link=button("Leggi l’articolo ↗",false); link.addActionListener(e->browse(item.url)); card.add(link);
            results.add(card); space(results,14);
        }
        if(result.items.size()<5) results.add(wrapped("Disponibili " + result.items.size() + " notizie di oggi.",14,MUTED));
        results.revalidate(); results.repaint(); SwingUtilities.invokeLater(()->scroller.getVerticalScrollBar().setValue(0));
    }
    private void settings() {
        JPanel body=column(); JCheckBox automatic=new JCheckBox("Controlla aggiornamenti all’avvio",prefs.getBoolean("automatic",true));
        automatic.addActionListener(e->prefs.putBoolean("automatic",automatic.isSelected())); body.add(automatic); space(body,16);
        body.add(label("Versione " + AppInfo.VERSION,14,INK)); space(body,5); body.add(label("Francesco Pio Pipino",14,MUTED));
        int answer=JOptionPane.showOptionDialog(this,body,"Impostazioni",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[]{"Verifica ora","Chiudi"},"Chiudi");
        if(answer==0) checkUpdates(true);
    }
    private void checkUpdates(boolean manual) {
        if(checking) return; checking=true; prefs.putLong("last",System.currentTimeMillis());
        if(manual) status.setText("Controllo aggiornamenti…");
        new SwingWorker<UpdateChecker.Result,Void>() {
            protected UpdateChecker.Result doInBackground() throws Exception { return UpdateChecker.check(UpdateChecker.Target.WINDOWS); }
            protected void done() { checking=false;
                try { UpdateChecker.Result r=get();
                    if(r.newer) { int choice=JOptionPane.showConfirmDialog(WindowsApp.this,"È disponibile la versione "+r.version+".\nAprire il download?","Aggiornamento",JOptionPane.YES_NO_OPTION); if(choice==JOptionPane.YES_OPTION) browse(r.url); }
                    else if(manual) JOptionPane.showMessageDialog(WindowsApp.this,r.published ? "Hai la versione più recente." : "Nessuna release pubblicata.");
                    if(manual) status.setText("Controllo completato");
                } catch(Exception e) { if(manual) { status.setText("Controllo non riuscito"); JOptionPane.showMessageDialog(WindowsApp.this,(e.getCause()==null?e:e.getCause()).getMessage(),"Aggiornamenti",JOptionPane.ERROR_MESSAGE); } }
            }
        }.execute();
    }
    private void browse(String url) { try { Desktop.getDesktop().browse(new URI(url)); } catch(Exception e) { JOptionPane.showMessageDialog(this,"Impossibile aprire il browser."); } }
    private void openDocument() { try { if(document!=null) Desktop.getDesktop().open(document.toFile()); } catch(Exception e) { JOptionPane.showMessageDialog(this,"Installa un’app che apre documenti Word."); } }
    private void browseFolder() { try { if(document!=null) Desktop.getDesktop().open(document.getParent().toFile()); } catch(Exception e) { JOptionPane.showMessageDialog(this,"Impossibile aprire la cartella."); } }
    private static JPanel column() { JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setBackground(BG); p.setAlignmentX(LEFT_ALIGNMENT); return p; }
    private static JPanel card() {
        JPanel p=new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D gg=(Graphics2D)g.create(); gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                gg.setColor(SURFACE); gg.fillRoundRect(0,0,getWidth(),getHeight(),24,24); gg.dispose();
            }
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE,getPreferredSize().height); }
        };
        p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(24,24,24,24)); return p;
    }
    private static final class NewsColumn extends JPanel implements Scrollable {
        NewsColumn() { setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBackground(BG); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r,int o,int d) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r,int o,int d) { return r.height-20; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private static JLabel label(String text,int size,Color color) { JLabel l=new JLabel(text); l.setFont(new Font("Segoe UI",size>=22 ? Font.BOLD : Font.PLAIN,size)); l.setForeground(color); l.setAlignmentX(LEFT_ALIGNMENT); return l; }
    private static JButton button(String text,boolean primary) { JButton b=new JButton(text); b.setFont(new Font("Segoe UI",Font.PLAIN,14)); b.setBackground(primary?ACCENT:SURFACE); b.setForeground(primary?new Color(0x232031):INK); b.putClientProperty("JButton.buttonType","roundRect"); b.setMargin(new Insets(12,18,12,18)); b.setAlignmentX(LEFT_ALIGNMENT); return b; }
    private static JTextArea wrapped(String text,int size,Color color) {
        JTextArea pane=new JTextArea(text) {
            @Override public Dimension getPreferredSize() {
                if(getParent()!=null && getParent().getWidth()>0) {
                    Insets border=getParent().getInsets();
                    int width=Math.max(100,getParent().getWidth()-border.left-border.right);
                    if(getWidth()!=width) setSize(width,Short.MAX_VALUE);
                }
                return super.getPreferredSize();
            }
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE,getPreferredSize().height); }
        };
        pane.setFont(new Font("Segoe UI",size>=22?Font.BOLD:Font.PLAIN,size));pane.setForeground(color);
        pane.setLineWrap(true);pane.setWrapStyleWord(true);pane.setEditable(false);pane.setOpaque(false);
        pane.setBorder(null);pane.setAlignmentX(LEFT_ALIGNMENT);
        pane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> { pane.revalidate(); if(pane.getParent()!=null) pane.getParent().revalidate(); });
            }
        });
        return pane;
    }
    private static void space(JPanel p,int height) { p.add(Box.createRigidArea(new Dimension(1,height))); }
    public static void main(String[] args) {
        FlatDarkLaf.setup(); UIManager.put("defaultFont",new Font("Segoe UI",Font.PLAIN,14));
        UIManager.put("Panel.background",BG); UIManager.put("Component.arc",18); UIManager.put("Button.arc",18);
        UIManager.put("ScrollBar.width",10); UIManager.put("ScrollBar.showButtons",false);
        UIManager.put("Component.focusColor",ACCENT);
        SwingUtilities.invokeLater(()->new WindowsApp(Arrays.asList(args).contains("--preview")).setVisible(true));
    }
}
