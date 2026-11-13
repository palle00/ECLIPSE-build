package eclipse.content;

import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import eclipse.augment.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static mindustry.Vars.*;

/**
 * Feel for every ECLIPSE machine, by kind of work: a looping working sound (vanilla sounds, only while working),
 * a light on dark maps while working, an effect when a batch finishes in the colour of the product, and a light
 * running effect. Runs after {@link Animations}. Effects or sounds a machine set itself are kept.
 */
public class Polish{
    /** Kinds of work. Each machine name is listed under exactly one. */
    public enum Kind{furnace, electric, chemical, steam, combustion, reactor, cryo, press, grind, electronics, bio, assembler}

    static final ObjectMap<String, Kind> kinds = new ObjectMap<>();

    static void kind(Kind k, String... names){
        for(String n : names) kinds.put("eclipse-" + n, k);
    }

    static{
        kind(Kind.furnace, "alloy-furnace", "sintering-furnace", "glass-kiln", "soda-glass-furnace", "silicon-reduction-furnace", "frame-forge",
            "casing-forge", "carbon-fiber-furnace", "thermal-waste-processor", "chloride-titanium-plant", "alumina-refinery", "methane-pyrolysis-reactor",
            "fuel-rod-fabricator");
        kind(Kind.electric, "electrolysis-plant", "chlor-alkali-cell", "electrowinning-cell", "induction-furnace", "aluminium-smelter", "arc-furnace", "electric-heater");
        kind(Kind.chemical, "acid-synthesizer", "acid-leaching-vat", "sulfuric-acid-plant", "nitric-acid-plant", "aqua-regia-mixer", "platinum-refinery",
            "ammonium-nitrate-plant", "catalyst-plant", "polymerization-reactor", "rubber-plant", "acrylonitrile-reactor", "epoxy-plant", "olefin-reactor",
            "methanol-synthesizer", "high-pressure-reactor", "industrial-neutralizer", "industrial-scrubber", "fertilizer-plant", "reprocessing-plant",
            "chemical-synthesis-complex", "lithium-extractor", "mineral-separator", "water-treatment-plant", "wastewater-treatment-plant", "distillation-column");
        kind(Kind.steam, "industrial-boiler", "steam-turbine", "cooling-tower", "steam-cracker", "geothermal-heat-tap");
        kind(Kind.combustion, "industrial-heater", "methane-turbine", "biofuel-engine");
        kind(Kind.reactor, "fission-reactor");
        kind(Kind.cryo, "hydrogen-fuel-cell", "air-separation-unit", "cryogenic-condenser", "gas-separator");
        kind(Kind.press, "gear-press", "heavy-gear-press", "metal-extruder", "wire-drawing-machine", "bearing-press", "composite-press",
            "battery-assembly-plant", "explosives-plant");
        kind(Kind.grind, "ore-crusher", "ore-washer");
        kind(Kind.electronics, "circuit-board-printer", "chip-fabricator", "control-unit-assembler", "precision-assembler", "experimental-core-assembler", "silicon-refinery");
        kind(Kind.bio, "biomass-cultivator", "algae-photobioreactor", "enzyme-extractor", "biochemical-fermenter", "anaerobic-digester");
        kind(Kind.assembler, "basic-module-assembler", "advanced-module-assembler", "experimental-augmentation-facility", "aetherite-module-forge", "module-recycling-facility");
    }

    public static @Nullable Kind kindOf(Block b){
        return kinds.get(b.name);
    }

    /** Machines whose waste heat Thermal Recovery can use; fixed before this pass changes any effect (see AugmentContent.hotMachines). */
    public static final ObjectSet<Block> smelters = new ObjectSet<>();

    public static void load(){
        //remember the smelters by their original smelting smoke before effects change
        for(Block b : content.blocks()) if(b instanceof GenericCrafter g && g.craftEffect == Fx.smeltsmoke) smelters.add(b);

        for(Block b : content.blocks()){
            Kind k = kindOf(b);
            if(k == null) continue;
            Color product = b instanceof GenericCrafter g ? Animations.productColor(g) : Pal.accent;

            if(b.ambientSound == Sounds.none){
                b.ambientSound = sound(k);
                b.ambientSoundVolume = k == Kind.reactor ? 0.12f : 0.06f;
            }

            Color light = lightColor(k, product);
            if(light != null && addLight(b, light, 18f + b.size * 10f)){
                b.emitLight = true;
                b.lightColor = light;
                b.lightRadius = 18f + b.size * 10f;
            }

            if(b instanceof GenericCrafter g){
                Effect craft = craftEffect(k, product);
                if(craft != null) g.craftEffect = craft;
                if(g.updateEffect == Fx.none){
                    Effect running = runningEffect(k, product);
                    if(running != null){
                        g.updateEffect = running;
                        g.updateEffectChance = 0.03f;
                        g.updateEffectSpread = b.size * 3f;
                    }
                }
            }
        }
    }

    static Sound sound(Kind k){
        return switch(k){
            case furnace -> Sounds.loopSmelter;
            case electric -> Sounds.loopElectricHum;
            case chemical -> Sounds.loopSpray;
            case steam -> Sounds.loopSteam;
            case combustion -> Sounds.loopCombustion;
            case reactor -> Sounds.loopThoriumReactor;
            case cryo -> Sounds.loopHum;
            case press -> Sounds.loopMachine;
            case grind -> Sounds.loopGrind;
            case electronics -> Sounds.loopCircuit;
            case bio -> Sounds.loopBio;
            case assembler -> Sounds.loopTech;
        };
    }

    static @Nullable Color lightColor(Kind k, Color product){
        return switch(k){
            case furnace, combustion -> Color.valueOf("ff9a3c");
            case reactor -> Color.valueOf("ff6a3c");
            case electric -> Color.valueOf("8fd8ff");
            case electronics -> Color.valueOf("7fe0a0");
            case bio -> Color.valueOf("9fe07a");
            case assembler -> product;
            default -> null;
        };
    }

    static @Nullable Effect craftEffect(Kind k, Color product){
        return switch(k){
            case furnace -> sparks(Color.valueOf("ffb060"));
            case electric -> electric(Color.valueOf("8fd8ff"));
            case chemical -> bubbles(product);
            case steam -> Fx.steam;
            case cryo -> Fx.vapor;
            case press -> Fx.pulverizeSmall;
            case grind -> Fx.pulverizeMedium;
            case electronics -> electric(Color.valueOf("7fe0a0"));
            case bio -> spores(product);
            default -> null;
        };
    }

    static @Nullable Effect runningEffect(Kind k, Color product){
        return switch(k){
            case furnace, combustion -> Fx.smoke;
            case chemical -> bubbles(product);
            case steam -> Fx.steam;
            case bio -> spores(product);
            default -> null;
        };
    }

    /** Adds a light that follows the machine's warmup to its drawer (crafters, heaters and generators); false if it has no drawer. */
    static boolean addLight(Block b, Color color, float radius){
        DrawWorkLight light = new DrawWorkLight(color, radius);
        if(b instanceof GenericCrafter g) g.drawer = with(g.drawer, light);
        else if(b instanceof PowerGenerator p) p.drawer = with(p.drawer, light);
        else return false;
        return true;
    }

    /** The drawer plus one more part, kept as one flat list (other code looks at the parts in order). */
    static DrawBlock with(DrawBlock drawer, DrawBlock extra){
        Seq<DrawBlock> parts = drawer instanceof DrawMulti m ? Seq.with(m.drawers) : Seq.with(drawer);
        parts.add(extra);
        return new DrawMulti(parts.toArray(DrawBlock.class));
    }

    /** A light as bright as the machine is warmed up, so idle machines stay dark. */
    public static class DrawWorkLight extends DrawBlock{
        public Color color;
        public float radius;

        public DrawWorkLight(Color color, float radius){
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void drawLight(Building build){
            float w = build.warmup();
            if(w > 0.01f) Drawf.light(build.x, build.y, radius * (0.6f + 0.4f * w), color, 0.65f * w);
        }
    }

    // ---- effects in a machine's colour (one instance per colour: craft effects are spawned without one) ----

    static final ObjectMap<String, Effect> made = new ObjectMap<>();

    static Effect cached(String kind, Color c, arc.func.Prov<Effect> e){
        return made.get(kind + c, e);
    }

    /** Small rising bubbles in the liquid's colour. */
    static Effect bubbles(Color c){
        return cached("bubbles", c, () -> new Effect(55f, e -> {
            Draw.color(c, 0.85f * e.fout());
            Lines.stroke(0.6f);
            Angles.randLenVectors(e.id, 4, 1.5f + e.finpow() * 5f, (x, y) -> Lines.circle(e.x + x, e.y + y + e.fin() * 4f, 0.5f + e.fout() * 1.1f));
            Draw.reset();
        }));
    }

    /** Hot sparks flying out of a furnace. */
    static Effect sparks(Color c){
        return cached("sparks", c, () -> new Effect(28f, e -> {
            Draw.color(Color.white, c, e.fin());
            Lines.stroke(e.fout() * 1.1f);
            Angles.randLenVectors(e.id, 7, e.finpow() * 10f, (x, y) -> Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 2.6f));
            Draw.reset();
        }));
    }

    /** A few short arcs, as from electrodes. */
    static Effect electric(Color c){
        return cached("electric", c, () -> new Effect(18f, e -> {
            Draw.color(Color.white, c, e.fin());
            Lines.stroke(e.fout() * 0.9f);
            Rand rand = new Rand(e.id);
            for(int i = 0; i < 3; i++){
                float x = e.x + rand.range(4f), y = e.y + rand.range(4f);
                for(int s = 0; s < 3; s++){
                    float nx = x + rand.range(2.5f), ny = y + rand.range(2.5f);
                    Lines.line(x, y, nx, ny);
                    x = nx;
                    y = ny;
                }
            }
            Draw.reset();
        }));
    }

    /** Drifting motes for living cultures. */
    static Effect spores(Color c){
        return cached("spores", c, () -> new Effect(70f, e -> {
            Draw.color(c, 0.75f * e.fslope());
            Angles.randLenVectors(e.id, 5, 2f + e.fin() * 6f, (x, y) -> Fill.circle(e.x + x, e.y + y + e.fin() * 5f, 0.45f + e.fslope() * 0.5f));
            Draw.reset();
        }));
    }

    /** Corrosion: vapour rising and a drip falling, in the corroding liquid's colour (e.color). */
    public static final Effect corrode = new Effect(45f, e -> {
        Draw.color(e.color, 0.7f * e.fout());
        Angles.randLenVectors(e.id, 3, 1f + e.fin() * 3f, (x, y) -> Fill.circle(e.x + x, e.y + y + e.fin() * 4f, 0.4f + e.fout() * 0.9f));
        Draw.color(e.color, e.fout());
        Fill.circle(e.x, e.y - e.finpow() * 5f, 0.6f);
        Draw.reset();
    });
}
