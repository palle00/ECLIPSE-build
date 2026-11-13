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
 * Art-pass review sheet (art.md §23 step 9): puts ECLIPSE sprites beside vanilla references at native size and 3x,
 * on a dark and a light ground, so a batch can be judged against vanilla before it goes in game.
 * Usage: java -cp desktop.jar tools/ArtCompare.java &lt;desktop.jar&gt; &lt;eclipse sprites dir&gt; &lt;out.png&gt; name...
 * A name is an ECLIPSE sprite file name (without .png), or "v:" + a vanilla atlas region (v:graphite-press).
 * "|" starts a new row.
 */
public class ArtCompare{
    public static void main(String[] args) throws Exception{
        File jar = new File(args[0]), dir = new File(args[1]);
        List<String> names = Arrays.asList(args).subList(3, args.length);
        Set<String> wanted = new HashSet<>();
        for(String n : names) if(n.startsWith("v:")) wanted.add(n.substring(2));
        Map<String, BufferedImage> vanilla = wanted.isEmpty() ? Map.of() : loadVanilla(jar, wanted);

        List<List<Object[]>> rows = new ArrayList<>();
        rows.add(new ArrayList<>());
        for(String n : names){
            if(n.equals("|")){
                rows.add(new ArrayList<>());
                continue;
            }
            BufferedImage img = n.startsWith("v:") ? vanilla.get(n.substring(2)) : ImageIO.read(new File(dir, n + ".png"));
            if(img == null) throw new IOException("not found: " + n);
            rows.get(rows.size() - 1).add(new Object[]{n, img});
        }

        int scale = 3, pad = 16, label = 18;
        int width = 0, height = pad;
        int[] rowH = new int[rows.size()];
        for(int r = 0; r < rows.size(); r++){
            int w = pad, h = 0;
            for(Object[] e : rows.get(r)){
                BufferedImage img = (BufferedImage)e[1];
                w += img.getWidth() * (scale + 1) + pad * 2;
                h = Math.max(h, img.getHeight() * scale);
            }
            width = Math.max(width, w);
            rowH[r] = h * 2 + label + pad * 2;
            height += rowH[r];
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(0x26262e));
        g.fillRect(0, 0, width, height);
        int y = pad;
        for(int r = 0; r < rows.size(); r++){
            int x = pad;
            int half = (rowH[r] - label - pad * 2) / 2;
            //light ground behind the lower half
            g.setColor(new Color(0x8c8577));
            g.fillRect(0, y + label + half, width, half + pad);
            for(Object[] e : rows.get(r)){
                String n = (String)e[0];
                BufferedImage img = (BufferedImage)e[1];
                g.setColor(n.startsWith("v:") ? new Color(0x9fd0ff) : new Color(0xffd37f));
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString(n + " " + img.getWidth() + "x" + img.getHeight(), x, y + 12);
                for(int k = 0; k < 2; k++){
                    int yy = y + label + k * half;
                    g.drawImage(img, x, yy, null);
                    g.drawImage(img, x + img.getWidth() + pad / 2, yy, img.getWidth() * scale, img.getHeight() * scale, null);
                }
                x += img.getWidth() * (scale + 1) + pad * 2;
            }
            y += rowH[r];
        }
        g.dispose();
        ImageIO.write(out, "png", new File(args[2]));
        System.out.println("Wrote " + args[2]);
    }

    static Map<String, BufferedImage> loadVanilla(File gameJar, Set<String> names) throws IOException{
        Path tmp = Files.createTempDirectory("eclipse-atlas");
        try(ZipFile zip = new ZipFile(gameJar)){
            for(ZipEntry e : Collections.list(zip.entries())){
                String n = e.getName();
                if(!n.startsWith("sprites/") || n.indexOf('/', 8) != -1 || !(n.endsWith(".aatls") || n.matches("sprites/sprites\\d*\\.png"))) continue;
                try(InputStream in = zip.getInputStream(e)){
                    Files.copy(in, tmp.resolve(n.substring(8)));
                }
            }
        }
        Fi dir = new Fi(tmp.toFile());
        TextureAtlasData data = new TextureAtlasData(dir.child("sprites.aatls"), dir, false);
        Map<String, BufferedImage> pages = new HashMap<>(), result = new HashMap<>();
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
