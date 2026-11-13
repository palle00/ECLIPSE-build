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
 * Sprite review sheet (see review.ps1). args: gameJar eclipseSpriteDir outPng names scale
 * names: comma list of v:<vanilla region> or e:<eclipse sprite file>; "/" starts a new row. names = --list <regex> prints vanilla regions.
 */
public class SpriteSheet{
    public static void main(String[] a) throws Exception{
        Path tmp = Files.createTempDirectory("atlas");
        try(ZipFile zip = new ZipFile(a[0])){
            for(ZipEntry e : Collections.list(zip.entries())){
                String n = e.getName();
                if(!n.startsWith("sprites/") || n.indexOf('/', 8) != -1 || !(n.endsWith(".aatls") || n.matches("sprites/sprites\\d*\\.png"))) continue;
                try(InputStream in = zip.getInputStream(e)){ Files.copy(in, tmp.resolve(n.substring(8))); }
            }
        }
        Fi dir = new Fi(tmp.toFile());
        TextureAtlasData data = new TextureAtlasData(dir.child("sprites.aatls"), dir, false);
        Map<String, BufferedImage> pages = new HashMap<>(), vanilla = new HashMap<>();
        for(TextureAtlasData.Region r : data.getRegions()){
            BufferedImage page = pages.computeIfAbsent(r.page.textureFile.path(), p -> { try{ return ImageIO.read(new File(p)); }catch(IOException ex){ throw new UncheckedIOException(ex); } });
            vanilla.put(r.name, page.getSubimage(r.left, r.top, r.width, r.height));
        }
        if(a[3].equals("--list")){ vanilla.keySet().stream().filter(k -> k.matches(a[4])).sorted().forEach(System.out::println); return; }
        if(a[3].equals("--dump")){
            File d = new File(a[2]); d.mkdirs();
            for(String k : vanilla.keySet()) if(k.matches(a[4])) ImageIO.write(vanilla.get(k), "png", new File(d, k + ".png"));
            return;
        }
        if(a[3].equals("--all")){
            //vanilla reference row, then every ECLIPSE block and item sprite (no layer parts), 14 per row
            StringBuilder names = new StringBuilder("v:graphite-press,v:silicon-smelter,v:kiln,v:separator,v:cryofluid-mixer,v:laser-drill,v:steam-generator,v:battery-large,v:vault,v:thorium-reactor,v:titanium-wall,v:titanium-wall-large");
            String[] files = new File(a[1]).list((d, n) -> n.endsWith(".png") && !n.matches(".*-(bottom|glow|top\\d?|heat|rotator|light|connector|output\\d|power(-empty|-full)?|team|liquid|cap|turbine|arrow|end|bridge|\\d+)\\.png"));
            Arrays.sort(files);
            for(int i = 0; i < files.length; i++){
                names.append(i % 14 == 0 ? "/" : ",").append("e:").append(files[i], 0, files[i].length() - 4);
            }
            a[3] = names.toString();
        }
        int scale = Integer.parseInt(a[4]);
        List<List<BufferedImage>> rows = new ArrayList<>();
        for(String row : a[3].split("/")){
            List<BufferedImage> imgs = new ArrayList<>();
            for(String n : row.split(",")){
                BufferedImage img = n.startsWith("v:") ? vanilla.get(n.substring(2)) : ImageIO.read(new File(a[1], n.substring(2) + ".png"));
                if(img == null) throw new RuntimeException("missing " + n);
                imgs.add(img);
            }
            rows.add(imgs);
        }
        int w = 0, h = 0;
        for(List<BufferedImage> row : rows){
            int rw = 0, rh = 0;
            for(BufferedImage i : row){ rw += i.getWidth() * scale + 8; rh = Math.max(rh, i.getHeight() * scale); }
            w = Math.max(w, rw); h += rh + 8;
        }
        BufferedImage sheet = new BufferedImage(w + 8, h + 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x404040)); g.fillRect(0, 0, w + 8, h + 8);
        int y = 4;
        for(List<BufferedImage> row : rows){
            int x = 4, rh = 0;
            for(BufferedImage i : row){ g.drawImage(i, x, y, i.getWidth() * scale, i.getHeight() * scale, null); x += i.getWidth() * scale + 8; rh = Math.max(rh, i.getHeight() * scale); }
            y += rh + 8;
        }
        ImageIO.write(sheet, "png", new File(a[2]));
    }
}
