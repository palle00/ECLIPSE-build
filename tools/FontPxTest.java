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

        Set<Integer> distinct = new HashSet<>();
        for(float z = 0.4f; z <= 2.5f; z *= 1.15f){
            distinct.add(EclipseFonts.px(EclipseFonts.basePx() * 0.85f * z));
        }
        if(distinct.size() > 40) throw new AssertionError("too many distinct sizes: " + distinct.size() + " " + distinct);

        System.out.println("FontPxTest OK");
    }
}
