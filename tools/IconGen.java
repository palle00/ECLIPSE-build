import javax.imageio.*;
import java.awt.image.*;
import java.io.*;

/**
 * Generates the mod icon (assets/icon.png, shown in Mindustry's mod list) as 64x64 pixel art, scaled 4x without
 * smoothing. Design: an industrial gear in Serpulo's greys with teal markers and bolts, a brass bezel, and an orange
 * sun being eclipsed by a dark octagon (vanilla's block shape) that leaves a bright crescent and corona.
 * Run: java tools/IconGen.java assets/icon.png
 */
public class IconGen{
    static final int N = 64, SCALE = 4;
    static final int[] px = new int[N * N];

    //palette: Serpulo greys, brass, teal, sun
    static final int bg = 0xff1d1f25, bgLine = 0xff262931, outline = 0xff15161b,
        light = 0xffb0b9c0, plate = 0xff989aa4, shadow = 0xff6e7080, recess = 0xff4a4b53,
        brass = 0xffc8b58a, brassDark = 0xff8a7a58, teal = 0xff4fc3c9, tealDark = 0xff2b7f86,
        sunDeep = 0xffd8641e, sun = 0xfff08a2c, sunLight = 0xffffb648, corona = 0xffffe08a,
        moon = 0xff2a2c34, moonEdge = 0xff3a3d47, moonLight = 0xff505461,
        text = 0xffe2d3a8, textDark = 0xff8a7a58;

    public static void main(String[] args) throws IOException{
        //background: dark panel with faint seams, rounded corners
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            boolean corner = (x < 2 || x > N - 3) && (y < 2 || y > N - 3);
            set(x, y, corner ? 0 : (x == 12 || y == 40 || x == 51) ? bgLine : bg);
        }

        float c = 31.5f;
        //gear teeth (12), then the gear body ring, lit from the top-left
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            float dx = x - c, dy = y - c, r = (float)Math.sqrt(dx * dx + dy * dy);
            double a = Math.atan2(dy, dx);
            boolean tooth = r <= 30.5f && r > 25f && Math.cos(a * 12) > 0.35;
            if(r <= 26.5f || tooth) set(x, y, r > 25.5f && !tooth ? outline : lit(dx, dy));
        }
        //outline around teeth and body
        outline(outline);

        //recessed channel, brass bezel, then the sun
        disc(c, c, 21.5f, recess);
        ring(c, c, 21.5f, 19.5f, brassDark);
        ring(c, c, 21.5f, 20.5f, brass);
        disc(c, c, 19f, outline);
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            float dx = x - c, dy = y - c, r = (float)Math.sqrt(dx * dx + dy * dy);
            if(r > 18.5f) continue;
            //warm gradient, brighter toward the lower right where the crescent shows
            float t = (dx + dy) / 37f + 0.5f;
            set(x, y, t > 0.78f ? sunLight : t > 0.42f ? sun : sunDeep);
        }
        //corona: bright crescent rim on the lower right
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            float dx = x - c, dy = y - c, r = (float)Math.sqrt(dx * dx + dy * dy);
            if(r <= 18.5f && r > 16.8f && dx + dy > 6f) set(x, y, corona);
        }

        //the eclipsing body: a dark octagon, offset up-left, with a lit edge and a bolt hole
        float mx = c - 5.5f, my = c - 5f;
        octagon(mx, my, 13.5f, outline);
        octagon(mx, my, 12.5f, moonEdge);
        octagon(mx - 0.8f, my - 0.8f, 11.2f, moonLight);
        octagon(mx + 0.4f, my + 0.4f, 10.8f, moon);
        octagon(mx, my, 3.5f, outline);
        octagon(mx, my, 2.5f, recess);

        //bolts on the gear body and teal markers at the four compass points
        for(int i = 0; i < 8; i++){
            double a = Math.PI / 4 * i + Math.PI / 8;
            int bx = Math.round(c + (float)Math.cos(a) * 23.6f), by = Math.round(c + (float)Math.sin(a) * 23.6f);
            set(bx, by, light);
            set(bx + 1, by, shadow);
            set(bx, by + 1, shadow);
            set(bx + 1, by + 1, recess);
        }
        for(int[] m : new int[][]{{31, 3}, {31, 58}, {3, 31}, {58, 31}}){
            boolean vertical = m[1] == 3 || m[1] == 58;
            for(int i = 0; i < 4; i++){
                int x = m[0] + (vertical ? (i % 2) : i), y = m[1] + (vertical ? i : (i % 2));
                set(x, y, (i < 2) ? teal : tealDark);
            }
        }

        //wordmark across the lower part: brass letters with a dark outline and a lit top row
        wordmark("ECLIPSE", 44);

        BufferedImage img = new BufferedImage(N * SCALE, N * SCALE, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < N * SCALE; y++) for(int x = 0; x < N * SCALE; x++) img.setRGB(x, y, px[(y / SCALE) * N + x / SCALE]);
        File out = new File(args.length > 0 ? args[0] : "assets/icon.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    static int lit(float dx, float dy){
        float t = (dx + dy) / 52f;
        return t < -0.28f ? light : t < 0.22f ? plate : shadow;
    }

    static void set(int x, int y, int c){
        if(x >= 0 && y >= 0 && x < N && y < N) px[y * N + x] = c;
    }

    static int get(int x, int y){
        return x < 0 || y < 0 || x >= N || y >= N ? 0 : px[y * N + x];
    }

    static void disc(float cx, float cy, float r, int c){
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++) if(Math.hypot(x - cx, y - cy) <= r) set(x, y, c);
    }

    static void ring(float cx, float cy, float outer, float inner, int c){
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            double r = Math.hypot(x - cx, y - cy);
            if(r <= outer && r > inner) set(x, y, c);
        }
    }

    /** Regular octagon (flat sides on all four axes), "radius" = half its width. */
    static void octagon(float cx, float cy, float r, int c){
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            float dx = Math.abs(x - cx), dy = Math.abs(y - cy);
            if(dx <= r && dy <= r && dx + dy <= r * 1.42f) set(x, y, c);
        }
    }

    /** One-pixel outline around everything that is not background. */
    static void outline(int c){
        int[] copy = px.clone();
        for(int y = 0; y < N; y++) for(int x = 0; x < N; x++){
            int v = copy[y * N + x];
            if(v != bg && v != bgLine && v != 0) continue;
            boolean edge = false;
            for(int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}){
                int nx = x + d[0], ny = y + d[1];
                if(nx < 0 || ny < 0 || nx >= N || ny >= N) continue;
                int n = copy[ny * N + nx];
                if(n != bg && n != bgLine && n != 0) edge = true;
            }
            if(edge) set(x, y, c);
        }
    }

    // ---- 5x7 pixel font for the wordmark ----
    static final String[] E = {"#####", "#....", "#....", "####.", "#....", "#....", "#####"},
        C = {".####", "#....", "#....", "#....", "#....", "#....", ".####"},
        L = {"#....", "#....", "#....", "#....", "#....", "#....", "#####"},
        I = {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"},
        P = {"####.", "#...#", "#...#", "####.", "#....", "#....", "#...."},
        S = {".####", "#....", "#....", ".###.", "....#", "....#", "####."};

    static String[] glyph(char ch){
        return switch(ch){
            case 'E' -> E; case 'C' -> C; case 'L' -> L; case 'I' -> I; case 'P' -> P; default -> S;
        };
    }

    static void wordmark(String word, int top){
        int width = word.length() * 6 - 1, left = (N - width) / 2;
        //dark backing plate so the text reads over the gear
        for(int y = top - 2; y <= top + 8; y++) for(int x = left - 3; x <= left + width + 2; x++){
            boolean cut = (y == top - 2 || y == top + 8) && (x == left - 3 || x == left + width + 2);
            if(!cut) set(x, y, y == top - 2 ? recess : outline);
        }
        for(int i = 0; i < word.length(); i++){
            String[] g = glyph(word.charAt(i));
            for(int gy = 0; gy < 7; gy++) for(int gx = 0; gx < 5; gx++){
                if(g[gy].charAt(gx) != '#') continue;
                int x = left + i * 6 + gx, y = top + gy;
                set(x, y, gy == 0 ? 0xfffff0c8 : gy >= 5 ? textDark : text);
            }
        }
    }
}
