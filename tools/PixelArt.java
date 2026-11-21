import arc.files.*;
import arc.graphics.g2d.TextureAtlas.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/**
 * Hand-made pixel sprites (TEXTURE_QUALITY_PLAN.md method 1). Each art/pixel/NAME.txt is one sprite, one letter per pixel:
 *
 *   size 96 96          canvas in pixels
 *   mirror              the grid is the LEFT half only; the right half is its mirror image with every letter swapped
 *                       for its partner (dark <-> light), which gives the vanilla turret look: each plate dark on its
 *                       left, lit on its right
 *   color a 4d4e58 b    letter, colour, mirror partner (omit the partner for letters that stay the same)
 *   heat d e            letters that also go into NAME-heat.png (white mask, tinted by the game)
 *   mask glow d e       the same for any other layer: letters that go into NAME-glow.png
 *                       ("mask =jammer-glow d e" names the file outright: a dish's glow that the game loads as jammer-glow)
 *   mask cell e:ffffff f:dcc6c6   a mask letter may carry its own colour (a unit's team-colour cell is white and pale grey)
 *   outline 0           preview only: blocks that are not turrets get no game outline (default 4 #404049; units: 3 565666)
 *   weapon NAME X Y [flip] [under]  preview only: draw a weapon sprite (from art/pixel, or v:NAME from the game) at its
 *                       mount, X/Y in world units like Weapon.x/y; "under" draws it below the body (Weapon.top = false)
 *   team ffd37f         preview only: tint the "cell" mask with this team colour on top of the body
 *   on jammer           preview only: draw this sprite on top of another sprite from art/pixel (a dish on its base)
 *   base 3              preview only: draw on the vanilla block-3 turret base, with the game's 4px #404049 outline
 *   ref ripple          preview only: vanilla atlas sprites to show beside it
 *   grid                then one line per pixel row; '.' is transparent
 *
 * Usage: java -cp desktop.jar tools/PixelArt.java desktop.jar art/pixel assets/sprites [preview.png] [name...]
 * Writes NAME.png (and NAME-heat.png) into the sprite folder, where they replace the SpriteGen sprites at build time.
 */
public class PixelArt{
    static final Color outline = new Color(0x404049);

    record Mount(String name, float x, float y, boolean flip, boolean under){}
    record Sprite(String name, BufferedImage img, Map<String, BufferedImage> masks, int base, int outline, int outlineColor, String on,
                  List<String> refs, List<Mount> mounts, int team){}

    public static void main(String[] args) throws Exception{
        File jar = new File(args[0]), src = new File(args[1]), out = new File(args[2]);
        File preview = args.length > 3 ? new File(args[3]) : null;
        Set<String> only = new HashSet<>(Arrays.asList(args).subList(Math.min(4, args.length), args.length));
        List<Sprite> sprites = new ArrayList<>();
        File[] files = src.listFiles((d, n) -> n.endsWith(".txt"));
        Arrays.sort(files);
        for(File f : files){
            String name = f.getName().replace(".txt", "");
            if(!only.isEmpty() && !only.contains(name)) continue;
            Sprite s = read(name, Files.readAllLines(f.toPath()));
            ImageIO.write(s.img, "png", new File(out, name + ".png"));
            for(var m : s.masks.entrySet()) ImageIO.write(m.getValue(), "png", new File(out, maskFile(name, m.getKey()) + ".png"));
            System.out.println("wrote " + name + (s.masks.isEmpty() ? "" : " (+" + String.join(", +", s.masks.keySet()) + ")"));
            sprites.add(s);
        }
        if(preview != null) preview(jar, sprites, preview);
    }

    static Sprite read(String name, List<String> lines){
        int w = 0, h = 0, base = 0, outlineRadius = 4, outlineCol = 0x404049, team = -1;
        String on = null;
        List<Mount> mounts = new ArrayList<>();
        Map<Character, Integer> maskColor = new HashMap<>();
        boolean mirror = false;
        Map<Character, Integer> colors = new HashMap<>();
        Map<Character, Character> swap = new HashMap<>();
        Map<String, Set<Character>> maskLetters = new TreeMap<>();
        List<String> refs = new ArrayList<>(), grid = new ArrayList<>();
        boolean inGrid = false;
        for(String raw : lines){
            if(inGrid){
                if(!raw.isBlank()) grid.add(raw.strip());
                continue;
            }
            String line = raw.replaceAll("#.*", "").strip();
            if(line.isEmpty()) continue;
            String[] p = line.split("\\s+");
            switch(p[0]){
                case "size" -> { w = Integer.parseInt(p[1]); h = Integer.parseInt(p[2]); }
                case "mirror" -> mirror = true;
                case "color" -> {
                    colors.put(p[1].charAt(0), Integer.parseInt(p[2], 16));
                    swap.put(p[1].charAt(0), p.length > 3 ? p[3].charAt(0) : p[1].charAt(0));
                }
                case "heat" -> { for(int i = 1; i < p.length; i++) maskLetters.computeIfAbsent("heat", k -> new HashSet<>()).add(p[i].charAt(0)); }
                case "mask" -> {
                    for(int i = 2; i < p.length; i++){
                        maskLetters.computeIfAbsent(p[1], k -> new HashSet<>()).add(p[i].charAt(0));
                        if(p[i].contains(":")) maskColor.put(p[i].charAt(0), Integer.parseInt(p[i].substring(2), 16));
                    }
                }
                case "outline" -> { outlineRadius = Integer.parseInt(p[1]); if(p.length > 2) outlineCol = Integer.parseInt(p[2], 16); }
                case "weapon" -> mounts.add(new Mount(p[1], Float.parseFloat(p[2]), Float.parseFloat(p[3]),
                    Arrays.asList(p).contains("flip"), Arrays.asList(p).contains("under")));
                case "team" -> team = Integer.parseInt(p[1], 16);
                case "on" -> on = p[1];
                case "base" -> base = Integer.parseInt(p[1]);
                case "ref" -> refs.addAll(Arrays.asList(p).subList(1, p.length));
                case "grid" -> inGrid = true;
                default -> throw new IllegalArgumentException(name + ": unknown line '" + line + "'");
            }
        }
        int gw = mirror ? w / 2 : w;
        if(grid.size() != h) throw new IllegalArgumentException(name + ": " + grid.size() + " grid rows, expected " + h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Map<String, BufferedImage> masks = new TreeMap<>();
        for(String k : maskLetters.keySet()) masks.put(k, new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB));
        for(int y = 0; y < h; y++){
            String row = grid.get(y);
            if(row.length() != gw) throw new IllegalArgumentException(name + ": row " + (y + 1) + " is " + row.length() + " wide, expected " + gw);
            for(int x = 0; x < gw; x++){
                char c = row.charAt(x);
                put(name, img, masks, maskLetters, maskColor, colors, x, y, c);
                if(mirror) put(name, img, masks, maskLetters, maskColor, colors, w - 1 - x, y, swap.getOrDefault(c, c));
            }
        }
        return new Sprite(name, img, masks, base, outlineRadius, outlineCol, on, refs, mounts, team);
    }

    static String maskFile(String name, String suffix){
        return suffix.startsWith("=") ? suffix.substring(1) : name + "-" + suffix;
    }

    static void put(String name, BufferedImage img, Map<String, BufferedImage> masks, Map<String, Set<Character>> maskLetters,
                    Map<Character, Integer> maskColor, Map<Character, Integer> colors, int x, int y, char c){
        if(c == '.') return;
        Integer col = colors.get(c);
        if(col == null) throw new IllegalArgumentException(name + ": no colour for '" + c + "' at " + x + "," + y);
        img.setRGB(x, y, 0xff000000 | col);
        for(var m : maskLetters.entrySet()) if(m.getValue().contains(c)) masks.get(m.getKey()).setRGB(x, y, 0xff000000 | maskColor.getOrDefault(c, 0xffffff));
    }

    /** The game outlines turret sprites when it loads them: every empty pixel within 4px of the sprite turns #404049. */
    static BufferedImage outlined(BufferedImage src, int r){
        return outlined(src, r, outline.getRGB());
    }

    static BufferedImage outlined(BufferedImage src, int r, int color){
        BufferedImage o = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < src.getHeight(); y++) for(int x = 0; x < src.getWidth(); x++){
            if((src.getRGB(x, y) >>> 24) > 0){ o.setRGB(x, y, src.getRGB(x, y)); continue; }
            search:
            for(int dy = -r; dy <= r; dy++) for(int dx = -r; dx <= r; dx++){
                int nx = x + dx, ny = y + dy;
                if(dx * dx + dy * dy > r * r || nx < 0 || ny < 0 || nx >= src.getWidth() || ny >= src.getHeight()) continue;
                if((src.getRGB(nx, ny) >>> 24) > 0){ o.setRGB(x, y, 0xff000000 | color); break search; }
            }
        }
        return o;
    }

    /** One row per sprite: on its base at 1x and 3x, the heat layer, then the vanilla references on their bases. */
    static void preview(File jar, List<Sprite> sprites, File file) throws IOException{
        Set<String> want = new HashSet<>();
        for(Sprite s : sprites){
            want.addAll(s.refs);
            if(s.base > 0) want.add("block-" + s.base);
            for(Mount m : s.mounts) if(m.name.startsWith("v:")) want.add(m.name.substring(2));
        }
        Map<String, BufferedImage> vanilla = loadVanilla(jar, want);
        List<List<BufferedImage>> rows = new ArrayList<>();
        Map<String, Sprite> byName = new HashMap<>();
        for(Sprite s : sprites) byName.put(s.name, s);
        for(Sprite s : sprites){
            BufferedImage base = vanilla.get("block-" + s.base);
            if(s.on != null && byName.containsKey(s.on)) base = byName.get(s.on).img;
            BufferedImage body = s.img;
            if(s.team >= 0 && s.masks.containsKey("cell")) body = compose(body, multiply(s.masks.get("cell"), s.team));
            BufferedImage mine = compose(base, s.outline > 0 ? outlined(body, s.outline, s.outlineColor) : body);
            if(!s.mounts.isEmpty()) mine = mounted(mine, s, byName, vanilla);
            List<BufferedImage> row = new ArrayList<>(List.of(mine, scale(mine, 3), scale(s.img, 3)));
            for(BufferedImage m : s.masks.values()) row.add(scale(compose(mine, tint(m, new Color(0xff8a3d))), 3));
            for(String r : s.refs) if(vanilla.containsKey(r)) row.add(scale(compose(base, vanilla.get(r)), 3));
            rows.add(row);
        }
        int pad = 12, w = 0, h = pad;
        for(List<BufferedImage> row : rows){
            int rw = pad, rh = 0;
            for(BufferedImage i : row){ rw += i.getWidth() + pad; rh = Math.max(rh, i.getHeight()); }
            w = Math.max(w, rw); h += rh + pad;
        }
        BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x2b2c33)); g.fillRect(0, 0, w, h);
        int y = pad;
        for(List<BufferedImage> row : rows){
            int x = pad, rh = 0;
            for(BufferedImage i : row){ g.drawImage(i, x, y, null); x += i.getWidth() + pad; rh = Math.max(rh, i.getHeight()); }
            y += rh + pad;
        }
        ImageIO.write(sheet, "png", file);
        System.out.println("preview " + file);
    }

    /** The unit with its weapons at their mounts (weapons outlined like the game does), on a canvas with room for them. */
    static BufferedImage mounted(BufferedImage unit, Sprite s, Map<String, Sprite> byName, Map<String, BufferedImage> vanilla){
        int pad = 24, cw = unit.getWidth() + pad * 2, ch = unit.getHeight() + pad * 2;
        BufferedImage o = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        for(int pass = 0; pass < 2; pass++){
            if(pass == 1) g.drawImage(unit, pad, pad, null);
            for(Mount m : s.mounts){
                if(m.under != (pass == 0)) continue;
                BufferedImage wi = m.name.startsWith("v:") ? vanilla.get(m.name.substring(2))
                    : byName.containsKey(m.name) ? outlined(byName.get(m.name).img, 3, s.outlineColor) : null;
                if(wi == null) continue;
                int mx = Math.round(cw / 2f + m.x * 4), my = Math.round(ch / 2f - m.y * 4);
                g.drawImage(wi, mx + (m.flip ? wi.getWidth() / 2 : -wi.getWidth() / 2), my - wi.getHeight() / 2,
                    m.flip ? -wi.getWidth() : wi.getWidth(), wi.getHeight(), null);
            }
        }
        return o;
    }

    static BufferedImage multiply(BufferedImage mask, int team){
        BufferedImage o = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < mask.getHeight(); y++) for(int x = 0; x < mask.getWidth(); x++){
            int c = mask.getRGB(x, y);
            if((c >>> 24) == 0) continue;
            int r = ((c >> 16) & 255) * ((team >> 16) & 255) / 255, gg = ((c >> 8) & 255) * ((team >> 8) & 255) / 255, b = (c & 255) * (team & 255) / 255;
            o.setRGB(x, y, 0xff000000 | r << 16 | gg << 8 | b);
        }
        return o;
    }

    static BufferedImage compose(BufferedImage under, BufferedImage over){
        int w = Math.max(under == null ? 0 : under.getWidth(), over.getWidth()), h = Math.max(under == null ? 0 : under.getHeight(), over.getHeight());
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        if(under != null) g.drawImage(under, (w - under.getWidth()) / 2, (h - under.getHeight()) / 2, null);
        g.drawImage(over, (w - over.getWidth()) / 2, (h - over.getHeight()) / 2, null);
        return o;
    }

    static BufferedImage tint(BufferedImage mask, Color c){
        BufferedImage o = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < mask.getHeight(); y++) for(int x = 0; x < mask.getWidth(); x++)
            if((mask.getRGB(x, y) >>> 24) > 0) o.setRGB(x, y, 0xff000000 | c.getRGB());
        return o;
    }

    static BufferedImage scale(BufferedImage i, int s){
        BufferedImage o = new BufferedImage(i.getWidth() * s, i.getHeight() * s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(i, 0, 0, o.getWidth(), o.getHeight(), null);
        return o;
    }

    static Map<String, BufferedImage> loadVanilla(File gameJar, Set<String> names) throws IOException{
        Map<String, BufferedImage> result = new HashMap<>();
        if(names.isEmpty()) return result;
        Path tmp = Files.createTempDirectory("eclipse-atlas");
        try(ZipFile zip = new ZipFile(gameJar)){
            for(ZipEntry e : Collections.list(zip.entries())){
                String n = e.getName();
                if(!n.startsWith("sprites/") || n.indexOf('/', 8) != -1 || !(n.endsWith(".aatls") || n.matches("sprites/sprites\\d*\\.png"))) continue;
                try(InputStream in = zip.getInputStream(e)){ Files.copy(in, tmp.resolve(n.substring(8))); }
            }
        }
        Fi dir = new Fi(tmp.toFile());
        TextureAtlasData data = new TextureAtlasData(dir.child("sprites.aatls"), dir, false);
        Map<String, BufferedImage> pages = new HashMap<>();
        for(TextureAtlasData.Region r : data.getRegions()){
            if(!names.contains(r.name)) continue;
            BufferedImage page = pages.computeIfAbsent(r.page.textureFile.path(), p -> {
                try{ return ImageIO.read(new File(p)); }catch(IOException ex){ throw new UncheckedIOException(ex); }
            });
            result.put(r.name, page.getSubimage(r.left, r.top, r.width, r.height));
        }
        return result;
    }
}
