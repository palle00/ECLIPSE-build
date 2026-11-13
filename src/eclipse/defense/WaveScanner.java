package eclipse.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.LogicDisplay.*;

import static mindustry.Vars.*;

/**
 * Wave Intelligence Scanner: while powered, reports the next wave's composition (unit types, counts, shields, bosses)
 * and the time left. Tap it to open the report. Information only: it never changes waves or reveals the map.
 * It can also drive up to {@link #maxLinks} vanilla logic displays: select the scanner, then tap a display in range.
 * Each linked screen shows the next wave as lines like "1 Horizon (air)" with the unit's icon, drawn with the same
 * display commands a logic processor uses. Displays are drawn on each client, like processor graphics.
 */
public class WaveScanner extends Block{
    public int maxLinks = 4;
    public float linkRange = 12f * 8f;

    public WaveScanner(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        saveConfig = false;
        config(Integer.class, (ScannerBuild b, Integer pos) -> b.toggleLink(pos));
    }

    /** One report line per unit type: "count Name (type)", plus BOSS. Used by the screens and the tests. */
    public static Seq<String> reportLines(){
        Seq<String> out = new Seq<>();
        for(WaveInfo.Entry e : WaveInfo.nextWave()){
            String kind = Core.bundle.get(e.type.flying ? "eclipse-scanner.air" : e.type.naval ? "eclipse-scanner.naval" : "eclipse-scanner.ground");
            out.add((WaveInfo.approximate() ? "~" : "") + e.count + " " + e.type.localizedName + " (" + kind + ")" + (e.boss ? " " + Core.bundle.get("eclipse-scanner.boss") : ""));
        }
        return out;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-next-wave", (ScannerBuild e) -> new Bar(
            () -> !WaveInfo.timedWaves() ? Core.bundle.get("eclipse-scanner.no-waves")
                : Core.bundle.format("eclipse-scanner.next", state.wave, Strings.fixed(state.wavetime / 60f, 0)),
            () -> Pal.accent,
            () -> WaveInfo.timedWaves() && state.rules.waveSpacing > 0 ? 1f - Math.min(state.wavetime / state.rules.waveSpacing, 1f) : 0f));
    }

    public class ScannerBuild extends Building{
        public IntSeq links = new IntSeq();
        float refresh;

        void toggleLink(int pos){
            int i = links.indexOf(pos);
            if(i >= 0) links.removeIndex(i);
            else if(links.size < maxLinks) links.add(pos);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(other instanceof LogicDisplayBuild && within(other, linkRange + other.block.size * tilesize / 2f)){
                configure(other.pos());
                return false;
            }
            return other != this;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();
            Drawf.dashCircle(x, y, linkRange, Pal.accent);
            for(int i = 0; i < links.size; i++){
                Building b = world.build(links.get(i));
                if(b != null){
                    Drawf.select(b.x, b.y, b.block.size * tilesize / 2f + 2f, Pal.place);
                    Drawf.dashLine(Pal.place, x, y, b.x, b.y);
                }
            }
        }

        @Override
        public void updateTile(){
            //screens are drawn locally on each client, like logic processor graphics
            if(headless || links.isEmpty()) return;
            refresh -= Time.delta;
            if(refresh > 0f) return;
            refresh = 60f;
            for(int i = 0; i < links.size; i++){
                if(world.build(links.get(i)) instanceof LogicDisplayBuild d && d.isValid()) drawScreen(d);
            }
        }

        /** Writes the report into a logic display: clear, title, then one icon + text line per unit type. */
        void drawScreen(LogicDisplayBuild d){
            int size = ((LogicDisplay)d.block).displaySize;
            Font.FontData font = Fonts.logic.getData();
            int adv = Math.max(1, (int)font.spaceXadvance), line = (int)font.lineHeight + 2;
            LongSeq c = new LongSeq();
            c.add(DisplayCmd.get(LogicDisplay.commandClear, 34, 36, 46, 0, 0, 0));
            int y = size - line;
            String title = !online() ? Core.bundle.get("eclipse-scanner.screen-offline")
                : !state.rules.waves ? Core.bundle.get("eclipse-scanner.no-waves")
                : Core.bundle.format("eclipse-scanner.screen-title", state.wave, WaveInfo.timedWaves() ? Strings.fixed(state.wavetime / 60f, 0) + "s" : "--");
            color(c, 255, 211, 127);
            print(c, font, title, 3, y, adv, (size - 6) / adv);
            if(online() && state.rules.waves){
                Seq<WaveInfo.Entry> wave = WaveInfo.nextWave();
                Seq<String> lines = reportLines();
                int maxLines = Math.max(1, (y - 2) / line), chars = (size - 16) / adv;
                for(int i = 0; i < lines.size && i < maxLines; i++){
                    y -= line;
                    boolean more = i == maxLines - 1 && lines.size > maxLines;
                    if(!more){
                        UnlockableContent type = wave.get(i).type;
                        int v = (type.id << 5) | (type.getContentType().ordinal() & 31);
                        c.add(DisplayCmd.get(LogicDisplay.commandImage, sign(7), sign(y + line / 2 - 1), v & 1023, sign(9), sign(0), v >> 10));
                    }
                    if(wave.get(i).boss) color(c, 255, 110, 110); else color(c, 220, 224, 232);
                    print(c, font, more ? "+" + (lines.size - i) + " " + Core.bundle.get("eclipse-scanner.more") : lines.get(i), 14, y, adv, chars);
                }
                if(lines.isEmpty()){
                    color(c, 160, 164, 176);
                    print(c, font, Core.bundle.get("eclipse-scanner.empty-short"), 3, y - line, adv, (size - 6) / adv);
                }
            }
            d.draw(c);
        }

        void color(LongSeq c, int r, int g, int b){
            c.add(DisplayCmd.get(LogicDisplay.commandColor, r & 511, g & 511, b & 511, 255, 0, 0));
        }

        void print(LongSeq c, Font.FontData font, String text, int x, int y, int adv, int maxChars){
            String t = Strings.stripColors(text);
            if(t.length() > maxChars) t = t.substring(0, Math.max(0, maxChars - 1)) + ".";
            for(int i = 0; i < t.length(); i++){
                char ch = t.charAt(i);
                if(ch != ' ' && font.hasGlyph(ch)) c.add(DisplayCmd.get(LogicDisplay.commandPrint, sign(x), sign(y), ch, 0, 0, 0));
                x += adv;
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(links.size);
            for(int i = 0; i < links.size; i++) write.i(links.get(i));
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            links.clear();
            if(revision >= 1){
                int n = read.b();
                for(int i = 0; i < n; i++) links.add(read.i());
            }
        }

        public boolean online(){
            return efficiency > 0f;
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.table(t -> {
                t.left().defaults().left();
                t.update(() -> {
                    //refresh the report about twice a second while open
                    if(Time.time % 30f < Time.delta) rebuildReport(t);
                });
                rebuildReport(t);
            }).minWidth(260f);
        }

        void rebuildReport(Table t){
            t.clear();
            t.left().defaults().left();
            if(!online()){
                t.add(Core.bundle.get("eclipse-scanner.offline")).color(Pal.remove);
                return;
            }
            if(!state.rules.waves){
                t.add(Core.bundle.get("eclipse-scanner.no-waves"));
                return;
            }
            t.add(WaveInfo.timedWaves() ? Core.bundle.format("eclipse-scanner.next", state.wave, Strings.fixed(state.wavetime / 60f, 0))
                : Core.bundle.format("eclipse-scanner.next-manual", state.wave)).color(Pal.accent).row();
            Seq<WaveInfo.Entry> wave = WaveInfo.nextWave();
            if(wave.isEmpty()){
                t.add(Core.bundle.get("eclipse-scanner.empty")).row();
                return;
            }
            int ground = 0, air = 0;
            for(WaveInfo.Entry e : wave){
                if(e.type.flying) air += e.count; else ground += e.count;
                String approx = WaveInfo.approximate() ? "~" : "";
                t.table(row -> {
                    row.left();
                    row.image(e.type.uiIcon).size(24f).padRight(6f);
                    row.add(e.type.localizedName + "  x" + approx + e.count
                        + (e.shields > 0f ? "  [lightgray]" + Core.bundle.format("eclipse-scanner.shield", (int)e.shields) + "[]" : "")
                        + (e.boss ? "  [scarlet]" + Core.bundle.get("eclipse-scanner.boss") + "[]" : ""));
                }).row();
            }
            t.add("[lightgray]" + Core.bundle.format("eclipse-scanner.totals", ground, air)).padTop(4f).row();
            if(WaveInfo.approximate()) t.add("[lightgray]" + Core.bundle.get("eclipse-scanner.approx")).row();
        }
    }

    /** Same sign packing as the logic draw instruction. */
    static int sign(int v){
        return (Math.abs(v) & 511) | (v < 0 ? 512 : 0);
    }
}
