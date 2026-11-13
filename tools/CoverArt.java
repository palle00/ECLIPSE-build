import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

/**
 * Steam Workshop cover art (preview.png): the middle of a night screenshot, darkened toward the edges, an eclipse
 * (black disk, bright rim, warm corona) high in the frame, and the ECLIPSE logo with a tagline in the lower third.
 * Usage: java tools/CoverArt.java &lt;night screenshot&gt; &lt;logo.png&gt; &lt;out.png&gt; [size]
 */
public class CoverArt{
    public static void main(String[] args) throws Exception{
        BufferedImage shot = ImageIO.read(new File(args[0])), logo = ImageIO.read(new File(args[1]));
        int size = args.length > 3 ? Integer.parseInt(args[3]) : 800;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //the square middle of the screenshot
        int side = Math.min(shot.getWidth(), shot.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(shot, 0, 0, size, size, (shot.getWidth() - side) / 2, (shot.getHeight() - side) / 2,
            (shot.getWidth() + side) / 2, (shot.getHeight() + side) / 2, null);

        //darken toward the edges, and more toward the top (the eclipse) and the bottom (the title)
        g.setPaint(new RadialGradientPaint(size / 2f, size / 2f, size * 0.75f, new float[]{0.35f, 1f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}));
        g.fillRect(0, 0, size, size);
        g.setPaint(new LinearGradientPaint(0, 0, 0, size, new float[]{0f, 0.35f, 0.6f, 1f},
            new Color[]{new Color(4, 5, 10, 230), new Color(4, 5, 10, 60), new Color(4, 5, 10, 90), new Color(4, 5, 10, 235)}));
        g.fillRect(0, 0, size, size);

        //the eclipse: warm corona, black disk, bright rim
        float cx = size / 2f, cy = size * 0.3f, r = size * 0.15f;
        g.setPaint(new RadialGradientPaint(cx, cy, r * 2.6f, new float[]{0.38f, 0.5f, 1f},
            new Color[]{new Color(255, 200, 120, 230), new Color(255, 140, 50, 120), new Color(255, 120, 40, 0)}));
        g.fill(new Ellipse2D.Float(cx - r * 2.6f, cy - r * 2.6f, r * 5.2f, r * 5.2f));
        g.setColor(Color.black);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(255, 226, 168));
        g.setStroke(new BasicStroke(size / 220f));
        g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

        //the logo, pixel-sharp, with a soft shadow; the tagline under it
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        float lw = size * 0.86f, lh = lw * logo.getHeight() / logo.getWidth();
        float lx = (size - lw) / 2f, ly = size * 0.62f;
        BufferedImage shadow = new BufferedImage(logo.getWidth(), logo.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int x = 0; x < logo.getWidth(); x++) for(int y = 0; y < logo.getHeight(); y++){
            int a = (logo.getRGB(x, y) >>> 24) & 0xff;
            shadow.setRGB(x, y, (a * 170 / 255) << 24);
        }
        g.drawImage(shadow, Math.round(lx + size / 160f), Math.round(ly + size / 160f), Math.round(lw), Math.round(lh), null);
        g.drawImage(logo, Math.round(lx), Math.round(ly), Math.round(lw), Math.round(lh), null);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Segoe UI", Font.BOLD, Math.round(size / 26f)));
        String tagline = "A NEW ERA OF INDUSTRY";
        FontMetrics fm = g.getFontMetrics();
        int tx = (size - fm.stringWidth(tagline)) / 2, ty = Math.round(ly + lh + size * 0.075f);
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(tagline, tx + 2, ty + 2);
        g.setColor(new Color(255, 170, 90));
        g.drawString(tagline, tx, ty);
        g.dispose();

        ImageIO.write(out, "png", new File(args[2]));
        System.out.println("Wrote " + args[2] + " (" + new File(args[2]).length() / 1024 + " KB)");
    }
}
