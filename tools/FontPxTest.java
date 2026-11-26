import arc.Core;
import arc.mock.*;

import java.util.*;

import eclipse.ui.EclipseFonts;

/** Plain-Java check of the bounded, even pixel-size helper. Run: java -cp <classes>:<Mindustry.jar> FontPxTest */
public class FontPxTest{
    static void eq(int expected, int actual){
        if(expected != actual) throw new AssertionError("expected " + expected + " but got " + actual);
    }

    public static void main(String[] args){
        //Scl.scl() reads Core.app/graphics; give the plain-Java run arc's own mocks
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();

        eq(18, EclipseFonts.px(18.4f));
        eq(10, EclipseFonts.px(9f));
        eq(8, EclipseFonts.px(4f));
        eq(128, EclipseFonts.px(1000f));
        eq(128, EclipseFonts.px(128f));
        eq(8, EclipseFonts.px(Float.NaN));
        eq(128, EclipseFonts.px(Float.POSITIVE_INFINITY));
        eq(8, EclipseFonts.px(Float.NEGATIVE_INFINITY));

        Set<Integer> distinct = new TreeSet<>();
        int samples = 0;
        for(float z = 0.4f; z <= 2.5f; z *= 1.15f){
            samples++;
            distinct.add(EclipseFonts.px(EclipseFonts.basePx() * 0.85f * z));
        }
        //14 distinct zoom targets must collapse onto this exact even-pixel ladder
        Set<Integer> expected = new TreeSet<>(Arrays.asList(8, 10, 12, 14, 16, 18, 22, 24, 28, 32, 38));
        if(!distinct.equals(expected)) throw new AssertionError("expected " + expected + " from " + samples + " samples but got " + distinct);
        if(distinct.size() >= samples) throw new AssertionError("no collapse across " + samples + " samples: " + distinct);

        System.out.println("FontPxTest OK");
    }
}
