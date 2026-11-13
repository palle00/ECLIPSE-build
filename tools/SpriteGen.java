import arc.files.*;
import arc.graphics.g2d.TextureAtlas.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/**
 * Generates every ECLIPSE sprite, following SpriteAndTextures.md.
 *
 * Each sprite is modelled as a small height field: housings, plates, recesses, pipes, tanks and domes are painted
 * with a material and a height, and the renderer shades every pixel from its slope under a top-right light, with
 * short cast shadows towards the lower left. That gives buildings, ores and items the raised/recessed depth of vanilla
 * sprites, in the vanilla block palette, without noise textures or hand-placed highlights.
 *
 * Crafters with a liquid tile get see-through windows over their "-bottom" layer, so the liquid level is visible.
 * Transport variants are recoloured vanilla sprites.
 *
 * Run: java -cp <desktop.jar> SpriteGen <outDir> <path-to-desktop.jar>
 */
public class SpriteGen{
    static final int T = 32; //pixels per tile

    static File out;
    static Map<String, BufferedImage> vanilla;

    public static void main(String[] args) throws Exception{
        if(args.length < 2){
            throw new IllegalArgumentException("Usage: java -cp <desktop.jar> SpriteGen <outDir> <path-to-desktop.jar>");
        }
        out = new File(args[0]);
        Files.createDirectories(out.toPath());

        // Vanilla assets are used only as a base for ECLIPSE transport variants.
        // ECLIPSE machine buildings, walls and storage sprites are drawn independently.
        vanilla = loadVanilla(new File(args[1]), "(conduit-top-\\d|conduit-bottom|titanium-conveyor-\\d-\\d|liquid-router(-bottom)?|liquid-tank(-bottom)?|bridge-conduit(-end|-bridge|-arrow)?)");

        liquid("purified-water", 0x8fd3ff);
        liquid("brine", 0xa7c9b4);
        liquid("hydrochloric-acid", 0xdfe882);
        gas("oxygen", 0xd8efff);
        gas("chlorine", 0xb6dc4c);

        waterTreatment("water-treatment-plant");
        nozzles("water-treatment-plant", 2, "eclipse-purified-water", 0x8fd3ff);
        nozzles("water-treatment-plant", 2, "eclipse-brine", 0xa7c9b4);

        electrolysisPlant("electrolysis-plant");
        nozzles("electrolysis-plant", 3, "hydrogen", 0x9ab7ff);
        nozzles("electrolysis-plant", 3, "eclipse-oxygen", 0xd8efff);

        chlorAlkali("chlor-alkali-cell");
        nozzles("chlor-alkali-cell", 3, "eclipse-chlorine", 0xb6dc4c);
        nozzles("chlor-alkali-cell", 3, "hydrogen", 0x9ab7ff);

        acidSynth("acid-synthesizer");
        leachingVat("acid-leaching-vat");

        fuelCellArt("hydrogen-fuel-cell");

        // ---- batch 2 ----
        sulfurIcon("sulfur", 0xe8d44d);
        pellets("polyethylene", 0xe3ecf0);
        cellIcon("lead-acid-cell", 0x7f9a63);

        gas("methane", 0xc4b3e6);
        liquid("naphtha", 0xd9a45a);
        gas("ethylene", 0xaee3cf);
        liquid("sulfuric-acid", 0xe8b84a);
        liquid("sodium-hydroxide", 0xd8dff0);
        liquid("acidic-waste", 0x8f8a3a);

        nozzles("chlor-alkali-cell", 3, "eclipse-sodium-hydroxide", 0xd8dff0);

        distillationColumn("distillation-column");
        nozzles("distillation-column", 3, "eclipse-naphtha", 0xd9a45a);
        nozzles("distillation-column", 3, "eclipse-methane", 0xc4b3e6);

        steamCracker("steam-cracker");
        edgeGlow("steam-cracker-heat", 3);

        polymerizationReactor("polymerization-reactor");
        sulfuricPlant("sulfuric-acid-plant");
        neutralizer("industrial-neutralizer");

        batteryPlant("battery-assembly-plant");
        methaneTurbine("methane-turbine");
        industrialBattery("industrial-battery");
        combustionHeater("industrial-heater");

        // ---- batch 3 ----
        sulfurIcon("polymer-catalyst", 0xb07ad6);
        pellets("ammonium-nitrate", 0xf2efe4);
        gas("ammonia", 0xc9e6f2);
        liquid("nitric-acid", 0xf0d98c);

        airSeparation("air-separation-unit");
        nozzles("air-separation-unit", 3, "nitrogen", 0xefefef);
        nozzles("air-separation-unit", 3, "eclipse-oxygen", 0xd8efff);
        pressureReactor("high-pressure-reactor");
        nitricPlant("nitric-acid-plant");
        nitratePlant("ammonium-nitrate-plant");
        catalystPlant("catalyst-plant");
        explosivesPlant("explosives-plant");

        // ---- polymer transport assets ----
        for(int i = 0; i < 5; i++) polymer("conduit-top-" + i, "polymer-conduit-top-" + i);
        polymer("conduit-bottom", "polymer-conduit-bottom");
        polymer("liquid-router", "polymer-liquid-router");
        polymer("liquid-router-bottom", "polymer-liquid-router-bottom");
        polymer("liquid-tank", "polymer-liquid-tank");
        polymer("liquid-tank-bottom", "polymer-liquid-tank-bottom");
        polymer("bridge-conduit", "polymer-bridge-conduit");
        polymer("bridge-conduit-end", "polymer-bridge-conduit-end");
        polymer("bridge-conduit-bridge", "polymer-bridge-conduit-bridge");
        polymer("bridge-conduit-arrow", "polymer-bridge-conduit-arrow");

        // ---- augmentation ----
        for(int t = 1; t <= 3; t++){
            moduleIcon("production-accelerator-" + t, 0xff9a3c, t);
            moduleIcon("power-optimizer-" + t, 0x5ab8ff, t);
        }
        for(int t = 1; t <= 3; t++){
            moduleIcon("output-amplifier-" + t, 0xffb35a, t);
            moduleIcon("precision-fabricator-" + t, 0xe0683a, t);
        }
        for(int t = 1; t <= 2; t++) moduleIcon("material-recycler-" + t, 0xffd84a, t);
        int[][] chem = {{0xb37aff, 3}, {0xff7a5a, 3}, {0x7ab8ff, 2}, {0xe4e6ea, 2}, {0x6fe0e0, 3}, {0x8f8a3a, 2}};
        String[] chemNames = {"reaction-accelerator", "thermal-controller", "pressure-stabilizer", "catalyst-optimizer", "purification-module", "waste-recovery"};
        for(int i = 0; i < chemNames.length; i++){
            for(int t = 1; t <= chem[i][1]; t++) moduleIcon(chemNames[i] + "-" + t, chem[i][0], t);
        }
        Object[][] moreModules = {
            {"thermal-recovery", 0x5ab8ff, 3}, {"energy-overcharger", 0x8fd0ff, 2}, {"capacitor-module", 0x3f86d6, 2},
            {"deep-mining", 0xc47a3c, 3}, {"advanced-drill-head", 0x8a5a2c, 2},
            {"rapid-fire-controller", 0xff5a5a, 3}, {"cooling-system", 0x9fd8ff, 2}, {"advanced-ammunition", 0xd94f4f, 3}, {"targeting-computer", 0xff8a8a, 2}
        };
        for(Object[] f : moreModules){
            for(int t = 1; t <= (int)f[2]; t++) moduleIcon(f[0] + "-" + t, (int)f[1], t);
        }
        moduleIcon("geological-analyzer", 0xc47a3c, 1);
        moduleIcon("ballistic-computer", 0xff5a5a, 2);
        for(String n : new String[]{"production-accelerator", "output-amplifier", "precision-fabricator"}) moduleIcon(n + "-4", 0xff9a3c, 4);
        moduleIcon("power-optimizer-4", 0x5ab8ff, 4);
        moduleIcon("reaction-accelerator-4", 0xb37aff, 4);
        moduleIcon("deep-mining-4", 0xc47a3c, 4);
        moduleIcon("mass-production", 0xffd84a, 3);
        moduleIcon("closed-loop-processing", 0xffd84a, 3);
        moduleIcon("emergency-overdrive", 0xffd84a, 3);
        for(String n : new String[]{"quantum-accelerator", "matter-replicator", "ancient-efficiency-core", "temporal-processing-core"}) moduleIcon(n, 0xe8d8ff, 5);
        militaryInterface("military-augmentation-interface");
        augmentationFacility("experimental-augmentation-facility");
        moduleRecycler("module-recycling-facility");
        chemicalInterface("chemical-augmentation-interface");
        augmentationCoupler("augmentation-coupler");
        advancedCoupler("advanced-coupler");
        augmentationHub("augmentation-hub");
        moduleAssembler("basic-module-assembler");
        advancedModuleAssembler("advanced-module-assembler");

        // ---- advanced logistics ----
        threeWaySorter("three-way-sorter");
        priorityMerger("priority-merger");
        controlledSplitter("controlled-splitter");
        itemFilter("advanced-item-filter");
        batchGate("batch-gate");
        flowMeter("flow-meter");
        logo("logo");
        distributionHub("distribution-hub");
        lineBalancer("line-balancer");
        logisticsBuffer("small-logistics-buffer", false);
        logisticsBuffer("industrial-logistics-buffer", true);
        smartUnloader("smart-unloader");
        overflowValve("liquid-overflow-valve");
        liquidBalancerArt("liquid-balancer");
        reserveValve("reserve-valve");

        // ---- high-speed belts and pipes ----
        String[] tiers = {"aluminium", "chromium", "superalloy", "carbide", "surge"};
        int[][] palettes = {{0x5d6670, 0xd6dde4}, {0x4f5a6e, 0xe2ebf5}, {0x6b5a3a, 0xe6d6a8}, {0x2c2d33, 0x8d909c}, {0x7a5a1a, 0xffe08a}};
        for(int t = 0; t < tiers.length; t++){
            int dark = palettes[t][0], light = palettes[t][1];
            for(int v = 0; v < 5; v++) for(int f = 0; f < 4; f++){
                recolor("titanium-conveyor-" + v + "-" + f, tiers[t] + "-conveyor-" + v + "-" + f, dark, light);
            }
            recolor("titanium-conveyor-0-0", tiers[t] + "-conveyor", dark, light);
            for(int i = 0; i < 5; i++) recolor("conduit-top-" + i, tiers[t] + "-conduit-top-" + i, dark, light);
            recolor("conduit-bottom", tiers[t] + "-conduit-bottom", dark, light);
        }

        // ---- metallurgy ----
        chunks("nickel", 0xb4c2a6);
        chunks("chromium", 0xc3cedb);
        chunks("cobalt", 0x4f6fd1);
        chunks("tungsten", 0x8a8c96);
        sulfurIcon("lithium", 0xd9cfe9);
        ingot("superalloy", 0xcdb98a);
        ingot("tungsten-carbide", 0x50525e);

        ore("ore-nickel", 0xa3b293);
        ore("ore-chromium", 0x7d8696);
        ore("ore-cobalt", 0x44609f);
        ore("ore-tungsten", 0x5a5c66);

        mineralSeparator("mineral-separator");
        lithiumExtractor("lithium-extractor");
        alloyFurnace("alloy-furnace");
        sinteringFurnace("sintering-furnace");
        carbideDrill("carbide-drill");
        latticeDrill("lattice-drill");
        lithiumBattery("lithium-battery");
        wallArt("superalloy-wall", 1, 0);
        wallArt("superalloy-wall-large", 2, 0);

        // ---- heat recovery, electronics, components ----
        pellets("alumina", 0xece8e2);
        ingot("aluminium", 0xb9c4cf);
        wafer("high-purity-silicon", 0x6d7a99);
        chip("microchip", 0x3f9a5c);
        pane("reinforced-glass", 0x9fc7d6);
        ingot("surge-superalloy", 0xf2c45a);
        gas("steam", 0xe8edf0);
        liquid("liquid-nitrogen", 0xbfe3ff);

        heatParts("sulfuric-acid-plant", 2, 0xe8b84a);
        heatExchanger("heat-exchanger");
        boilerArt("industrial-boiler");
        edgeGlow("industrial-boiler-heat", 3);
        steamTurbineArt("steam-turbine");

        aluminaRefinery("alumina-refinery");
        edgeGlow("alumina-refinery-heat", 3);
        aluminiumSmelter("aluminium-smelter");
        glassKiln("glass-kiln");
        arcFurnace("arc-furnace");
        wallArt("surge-superalloy-wall", 1, 2);
        wallArt("surge-superalloy-wall-large", 2, 2);

        cryoCondenser("cryogenic-condenser");
        siliconRefinery("silicon-refinery");
        chipFabricator("chip-fabricator");

        // ---- more metals & nuclear ----
        chunks("molybdenum", 0x8c9bb0);
        sulfurIcon("vanadium", 0x7a5fa8);
        chunks("platinum", 0xe4e6ea);
        rod("thorium-fuel-rod", 0xc96fa0);
        rod("spent-fuel-rod", 0x6d5a63);
        liquid("aqua-regia", 0xe8873a);
        ore("ore-molybdenum", 0x76808f);
        ore("ore-platinum", 0xe4e6ea);

        aquaRegiaMixer("aqua-regia-mixer");
        platinumRefinery("platinum-refinery");
        electricHeater("electric-heater");
        flowBattery("flow-battery");
        fuelRodFabricator("fuel-rod-fabricator");
        fissionReactor("fission-reactor");
        reprocessingPlant("reprocessing-plant");

        // ---- waste and emissions ----
        gas("flue-gas", 0x8a8580);
        gas("carbon-dioxide", 0xc8ccd0);
        liquid("wastewater", 0x6f7f5a);
        chunks("industrial-sludge", 0x5c5140);
        scrubber("industrial-scrubber");
        gasSeparator("gas-separator");
        nozzles("gas-separator", 3, "nitrogen", 0xefefef);
        nozzles("gas-separator", 3, "eclipse-carbon-dioxide", 0xc8ccd0);
        wastewaterPlant("wastewater-treatment-plant");
        thermalWasteProcessor("thermal-waste-processor");

        // ---- organic chemistry & composites ----
        liquid("methanol", 0xd6e4f0);
        gas("propylene", 0xe6d3a8);
        liquid("acrylonitrile", 0xc9b8e8);
        pellets("synthetic-rubber", 0x3a3a40);
        sulfurIcon("epoxy-resin", 0xe0a94a);
        pane("carbon-fiber", 0x2b2d33);
        ingot("carbon-composite", 0x4a5260);
        methanolSynthesizer("methanol-synthesizer");
        olefinReactor("olefin-reactor");
        nozzles("olefin-reactor", 3, "eclipse-propylene", 0xe6d3a8);
        nozzles("olefin-reactor", 3, "eclipse-wastewater", 0x6f7f5a);
        epoxyPlant("epoxy-plant");
        rubberPlant("rubber-plant");
        acrylonitrileReactor("acrylonitrile-reactor");
        carbonFiberFurnace("carbon-fiber-furnace");
        compositePress("composite-press");
        wallArt("composite-wall", 1, 1);
        wallArt("composite-wall-large", 2, 1);

        // ---- biology ----
        pellets("biomass", 0x6fae4a);
        pellets("fertilizer", 0xe8e2c8);
        sulfurIcon("enzyme-extract", 0xe86fd0);
        liquid("biofuel", 0xf0e6a0);
        fertilizerPlant("fertilizer-plant");
        cultivator("biomass-cultivator");
        photobioreactor("algae-photobioreactor");
        enzymeExtractor("enzyme-extractor");
        fermenter("biochemical-fermenter");
        nozzles("biochemical-fermenter", 3, "eclipse-biofuel", 0xf0e6a0);
        nozzles("biochemical-fermenter", 3, "eclipse-carbon-dioxide", 0xc8ccd0);
        biofuelEngine("biofuel-engine");
        digester("anaerobic-digester");

        // ---- mechanical components ----
        gearIcon("copper-gear", 0xd99d73);
        rod("titanium-rod", 0x8da1e3);
        gearIcon("titanium-gear", 0x8da1e3);
        cellIcon("pressure-casing", 0x9ab0b8);
        ringIcon("industrial-bearing", 0xb8bcc4);
        pane("superalloy-frame", 0xcdb98a);
        precisionComponentIcon("precision-component");
        experimentalCoreIcon("experimental-core");
        //art pass (art.md §5.1): mechanical components
        gearPress("gear-press");
        metalExtruder("metal-extruder");
        heavyGearPress("heavy-gear-press");
        casingForge("casing-forge");
        bearingPress("bearing-press");
        frameForge("frame-forge");
        precisionAssembler("precision-assembler");
        coreAssembler("experimental-core-assembler");
        componentDepotArt("component-depot");

        // ---- electronics & ore processing ----
        coilIcon("copper-wire", 0xe8a070);
        boardIcon("circuit-board", 0x3f9a5c);
        chip("control-unit", 0x5ab8ff);
        magnetIcon("alnico-magnet", 0xc04a4a);
        chunks("crushed-ore", 0x8a7f72);
        liquid("chilled-water", 0x7fd8ff);
        wireDrawer("wire-drawing-machine");
        boardPrinter("circuit-board-printer");
        controlAssembler("control-unit-assembler");
        oreCrusher("ore-crusher");
        oreWasher("ore-washer");
        inductionFurnace("induction-furnace");
        thermalStorage("thermal-storage");
        coolingTower("cooling-tower");

        // ---- upgraded vanilla production ----
        reductionFurnace("silicon-reduction-furnace");
        pyrolysisReactor("methane-pyrolysis-reactor");
        glassFurnace("soda-glass-furnace");
        chloridePlant("chloride-titanium-plant");
        electrowinningCell("electrowinning-cell");

        // ---- strategic defence and support ----
        scanner("wave-intelligence-scanner");
        disruptionArray("wave-disruption-array", 3);
        shieldBeacon("unit-shield-beacon");
        repairBay("emergency-repair-dispatch-bay");

        // ---- endgame & aetherite ----
        sulfurIcon("aetherite", 0x7fe0ff);
        ore("ore-aetherite", 0x5f8fae);
        aetheriteForge("aetherite-module-forge");
        aetheriteCapacitor("aetherite-capacitor");
        geothermalTap("geothermal-heat-tap");
        ingot("superconductor", 0x8fd0ff);
        synthesisComplex("chemical-synthesis-complex");
        disruptionArray("temporal-interference-array", 4);
        worldEngineArt("world-engine");

        // ---- robotics ----
        robotHub("robot-dispatch-hub");
        supportDrone("logistics-drone", 0);
        supportDrone("repair-drone", 1);
        supportDrone("shield-drone", 2);
        for(String n : new String[]{"production-accelerator", "power-optimizer", "output-amplifier", "deep-mining"}) moduleIcon(n + "-5", 0x7fe0ff, 5);

        // ---- military ----
        etchedIcon("etched", 0xe8d44d);
        jammedIcon("jammed", 0xb37aff);
        mine("mine", 0xe0683a);
        acidMortar("acid-mortar");
        minelayer("minelayer");
        heatLance("heat-lance");
        jammerArt("jammer");
        piledriver("piledriver");
        kestrel("kestrel");
        thunderhead("thunderhead");
        cloudbreaker("cloudbreaker");
        rivet("rivet");
        sickle("sickle");
        tremor("tremor");
        apiary("apiary");
        eventHorizon("event-horizon");
        units();
    }

    // ---- units: drawn facing up, lit from the right; the game adds outlines, wrecks and tread animation frames ----

    static final Mat cellMat = new Mat(new int[]{0x8c8c8c, 0xb4b4b4, 0xdcdcdc, 0xffffff, 0xffffff}, false);
    static final Mat rubber = new Mat(new int[]{0x1d1d22, 0x26262c, 0x33333b, 0x45454e, 0x55555f}, false);

    /** "-cell": the team-colour layer, same geometry as the masked parts of the body, in white tones. */
    static void cell(String name, int s, boolean[] mask, float z) throws IOException{
        R c = new R(s).turret();
        c.fill(mask, z, cellMat, 1);
        save(c, name + "-cell");
    }

    /**
     * Tank: hull between two tread strips. The tread sheet holds the left strip (the game mirrors and animates it);
     * the hull carries a turret ring for the main weapon, a rear engine deck and team-colour side panels.
     */
    static void tankUnit(String name, int s, int treadX, int treadH, int a) throws IOException{
        int w = Math.max(4, s / 7), ty = (s - treadH) / 2;
        R tr = new R(s).turret();
        tr.fill(tr.rect(treadX, ty, w, treadH), 4, rubber, 1);
        //track links: metal plates every 4 px (the game scrolls them), with the guide horn down the middle
        for(int y = ty + 1; y < ty + treadH - 2; y += 4) tr.fill(tr.rect(treadX, y, w, 2), 5, tGrey, 0);
        if(w >= 6) tr.fill(tr.rect(treadX + w / 2 - 1, ty, 2, treadH), 5.5f, rubber, 0);
        save(tr, name + "-treads");

        R r = new R(s).turret();
        int hx = treadX + w - 1, hw = s - 2 * hx, hh = treadH - 4, hy = ty + 2;
        float c = s / 2f;
        r.fill(r.oct(hx, hy, hw, hh, Math.max(2, hw * 0.25f)), 6, tGrey, 2);
        r.raise(r.rect(hx + 2, hy + 2, hw - 4, Math.max(2, s / 24)), 1f);
        int deckH = Math.max(4, hh / 4);
        r.fill(r.rect(hx + 3, hy + hh - deckH - 2, hw - 6, deckH), 7, tGrey, 1);
        for(int x = hx + 5; x < hx + hw - 5; x += 3) r.fill(r.rect(x, hy + hh - deckH - 1, 1, deckH - 2), 4, bore, 0);
        hullStyle(r, name, s, hx, hy, hw, hh);
        //side skirts over the inner tread edge, front headlights
        int sk = Math.max(2, s / 32);
        for(int x : new int[]{hx - 1, hx + hw + 1 - sk}) r.fill(r.rect(x, hy + hh / 6, sk, hh * 2 / 3), 7.5f, tGrey, 0);
        int lamp = Math.max(1, s / 32);
        for(int x : new int[]{hx + 3, hx + hw - 3 - lamp * 2}) r.fill(r.rect(x, hy + 1, lamp * 2, lamp), 8, lit(0xffe0a0), 0);
        //team-colour panels on both flanks and a stripe across the engine deck
        int pw = Math.max(2, hw / 7), ph = Math.max(4, hh * 2 / 5);
        boolean[] panels = r.or(r.or(r.rect(hx + 2, (int)c - ph / 2, pw, ph), r.rect(hx + hw - 2 - pw, (int)c - ph / 2, pw, ph)),
            r.rect(hx + 3, hy + hh - deckH - 4, hw - 6, Math.max(1, s / 40)));
        r.fill(panels, 7, tGrey, 1);
        save(r, name);
        cell(name, s, panels, 7);
    }

    /** Turret-style unit weapon: a base ring with barrels pointing up; the canvas centre is the mount point. */
    static void unitWeapon(String name, int baseR, int barrelLen, int barrelW, int barrels, Mat barrel, boolean fat) throws IOException{
        int s = Math.max(baseR * 2 + 4, (barrelLen + 2) * 2);
        s += s % 2;
        float c = s / 2f;
        R r = new R(s).turret();
        int gap = barrelW + 1, x0 = (int)(c - (barrels * gap - 1) / 2f);
        for(int i = 0; i < barrels; i++){
            int bx = x0 + i * gap;
            r.fill(r.rect(bx, (int)c - barrelLen, barrelW, barrelLen), 9, barrel, fat ? 2 : 1);
            //muzzle brake on cannons, dark bore at the tip
            if(barrelW >= 4) r.fill(r.rect(bx - 1, (int)c - barrelLen, barrelW + 2, Math.max(2, barrelW / 2)), 10, barrel, 1);
            r.fill(r.rect(bx + 1, (int)c - barrelLen, Math.max(1, barrelW - 2), Math.max(2, barrelW / 2)), 10.5f, bore, 0);
        }
        r.fill(r.oct(c - baseR, c - baseR, baseR * 2, baseR * 2, baseR * 0.4f), 8, tGrey, 2);
        //cheek armour either side of the gun, and a commander's hatch
        if(baseR >= 6){
            int ch = Math.max(2, baseR / 3);
            r.fill(r.rect((int)(c - baseR) + 1, (int)(c - baseR / 2f), ch, baseR), 9, tGrey, 1);
            r.fill(r.rect((int)(c + baseR) - 1 - ch, (int)(c - baseR / 2f), ch, baseR), 9, tGrey, 1);
            r.fill(r.circle(c + baseR * 0.35f, c + baseR * 0.4f, Math.max(1.5f, baseR * 0.2f)), 10, tGrey, 1);
        }
        r.fill(r.circle(c, c, Math.max(1.5f, baseR * 0.3f)), 10, tGrey, 1);
        save(r, name);
    }

    static void missilePod(String name, int w, int h, int a) throws IOException{
        int s = Math.max(w, h) + 4;
        s += s % 2;
        float c = s / 2f;
        R r = new R(s).turret();
        r.fill(r.rect((int)(c - w / 2f), (int)(c - h / 2f), w, h), 8, tGrey, 2);
        for(int i = 0; i < 2; i++) r.fill(r.circle(c - w / 4f + i * w / 2f, c - h / 4f, Math.max(1.5f, w / 6f)), 9, bore, 0);
        r.fill(r.rect((int)(c - w / 2f) + 1, (int)(c + h / 4f), w - 2, 2), 9, acc(a), 0);
        save(r, name);
    }

    static void units() throws IOException{
        // tank line (sprite size, tread x, tread length must match MilitaryUnits.tank)
        tankUnit("reaper", 32, 6, 26, 0xe0683a);
        tankUnit("bulwark", 48, 9, 38, 0xe0683a);
        tankUnit("rampart", 64, 12, 52, 0xe0683a);
        tankUnit("annihilator", 96, 18, 80, 0xe0683a);
        tankUnit("colossus", 128, 24, 108, 0xe0683a);
        unitWeapon("reaper-gun", 5, 12, 3, 1, tGrey, false);
        unitWeapon("bulwark-cannon", 8, 20, 4, 1, tGrey, false);
        unitWeapon("rampart-cannon", 6, 16, 4, 1, tGrey, false);
        unitWeapon("annihilator-mortar", 15, 25, 10, 1, tGrey, true);
        unitWeapon("annihilator-gun", 5, 14, 3, 2, tGrey, false);
        unitWeapon("colossus-cannon", 20, 48, 8, 1, tGrey, true);
        unitWeapon("colossus-mortar", 8, 15, 7, 1, decalHot, true);
        unitWeapon("valkyrie-gun", 3, 9, 2, 2, tGrey, false);
        unitWeapon("leviathan-point-defense", 4, 7, 2, 1, tGrey, false);
        unitWeapon("leviathan-lance", 9, 26, 5, 1, acc(0xb37aff), false);
        missilePod("interceptor-launcher", 8, 12, 0x7ab8ff);

        // strike line
        flyerWraith("wraith");
        flyerInterceptor("interceptor");
        flyerValkyrie("valkyrie");
        flyerTempest("tempest");
        flyerLeviathan("leviathan");

        // the Swarm (enemy faction): chitin plates, acid sacs, spore pods
        swarmMech("mite", 24, 0);
        swarmMech("spitter", 36, 1);
        swarmMech("brood", 48, 2);
        swarmMech("behemoth", 80, 3);
        swarmWasp("wasp");

        // ---- military endgame ----
        tankUnit("dreadnought", 160, 30, 136, 0xe0683a);
        unitWeapon("dreadnought-cannon", 11, 34, 6, 1, tGrey, true);
        unitWeapon("dreadnought-lance", 14, 32, 6, 1, decalHot, false);
        ewDrone("ew-drone");
        titanComplex("titan-manufacturing-complex");
        orbitalPlatform("orbital-weapons-platform");
    }

    /** EW drone: a small quad-rotor with a sensor mast. */
    static void ewDrone(String name) throws IOException{
        int s = 32;
        float c = s / 2f;
        R r = new R(s).turret();
        for(int i = 0; i < 4; i++){
            r.fill(r.bar(c, c, 3, 10, 3, 45 + i * 90), 5, tGrey, 1);
            float px = c + (float)Math.cos(Math.toRadians(45 + i * 90)) * 11, py = c - (float)Math.sin(Math.toRadians(45 + i * 90)) * 11;
            r.fill(r.ring(px, py, 2.5f, 4.5f), 6, tGrey, 1);
        }
        r.fill(r.oct(c - 6, c - 6, 12, 12, 3), 7, tGrey, 2);
        r.fill(r.rect((int)c - 1, (int)c - 10, 2, 8), 9, tGrey, 0);
        r.fill(r.circle(c, c - 10, 1.8f), 10, lit(0x7fd8ff), 0);
        boolean[] cellM = r.rect((int)c - 3, (int)c + 1, 6, 3);
        r.fill(cellM, 8, tGrey, 1);
        save(r, name);
        cell(name, s, cellM, 8);
    }

    /**
     * Titan Manufacturing Complex (7x7): an assembly bay floor (base) and a heavy gantry frame drawn over the
     * unit being built ("-top", open in the middle so the titan shows through).
     */
    /**
     * Titan Manufacturing Complex (7×7, art.md §12.13): a vehicle construction facility. A long assembly bed with cradle
     * rails and clamps in the middle (the Dreadnought is built there and shows through), material staging bays down
     * both sides, support frames in the corners; "-top" carries the two gantry beams with their crane trolleys,
     * open over the bed.
     */
    static void titanComplex(String name) throws IOException{
        int s = 7 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 10, 5, steel);
        //assembly bed with cradle rails and clamp blocks
        r.fill(r.rect(52, 30, s - 104, s - 60), 3, deep, 1);
        for(int x : new int[]{76, s - 82}) r.fill(r.rect(x, 34, 6, s - 68), 4, metal, 1);
        for(int y = 50; y < s - 50; y += 40){
            r.fill(r.rect(64, y, 12, 14), 6, metal, 2);
            r.fill(r.rect(s - 76, y, 12, 14), 6, metal, 2);
        }
        //staging bays down both sides, each with its material
        int[] mats = {0x50525e, 0xcdb98a, 0xd9dde6, 0x7fe0ff, 0xff795e};
        for(int i = 0; i < 5; i++){
            int y = 26 + i * 36;
            for(int x : new int[]{6, s - 44}){
                r.fill(r.rect(x, y, 38, 30), 9, metal, 3);
                r.fill(r.rect(x + 5, y + 5, 28, 20), 4, deep, 0);
                r.fill(r.rect(x + 9, y + 9, 20, 12), 5, dull(mats[i]), 1);
            }
        }
        //support frames in the corners
        for(int[] p : new int[][]{{6, 6}, {s - 26, 6}, {6, s - 26}, {s - 26, s - 26}}) r.fill(r.oct(p[0], p[1], 20, 20, 5), 11, metal, 3);
        r.fill(r.rect(s / 2 - 10, s - 18, 20, 6), 10, lit(0xe0683a), 0);
        save(r, name);

        R top = canvas(s);
        for(int y : new int[]{14, s - 30}){
            top.fill(top.rect(6, y, s - 12, 16), 13, metal, 3);
            top.fill(top.rect(s / 2 - 18, y - 4, 36, 24), 15, steel, 3);
            top.fill(top.rect(s / 2 - 4, y + 4, 8, 8), 16, lit(0xe0683a), 0);
        }
        save(top, name + "-top");
    }

    /**
     * Orbital Weapons Platform (5×5): a ground installation. Capacitor banks left and right (the two-minute charge),
     * a munitions loader on top (blast compound), a feed bay below (superconductors, aetherite), and a turntable in the
     * middle carrying the aiming emitter ("-dish", turned to the target by the code).
     */
    static void orbitalPlatform(String name) throws IOException{
        int s = 5 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 8, 6, steel);
        for(int x : new int[]{6, 122}){
            r.fill(r.rect(x, 22, 32, 116), 10, metal, 3);
            for(int y = 28; y < 132; y += 13) r.fill(r.rect(x + 4, y, 24, 8), 11, dull(0x4a4b53), 1);
            r.fill(r.rect(x + 13, 26, 6, 108), 12, lit(0xff8a3d), 0);
        }
        r.fill(r.rect(46, 4, 68, 22), 10, metal, 3);
        for(int x = 52; x < 108; x += 14) r.fill(r.rect(x, 9, 10, 12), 5, deep, 0);
        r.fill(r.rect(46, 134, 68, 22), 10, metal, 3);
        r.fill(r.rect(56, 139, 16, 12), 11, acc(0x9fd8ff), 1);
        r.fill(r.rect(88, 139, 16, 12), 11, acc(0x7fe0ff), 1);
        r.fill(r.circle(c, c, 44), 9, metal, 4);
        r.fill(r.ring(c, c, 36, 40), 10, steel, 1);
        r.fill(r.circle(c, c, 32), 4, deep, 1);
        save(r, name);

        R dish = canvas(s);
        dish.worn = false;
        dish.fill(dish.oct(c - 26, c - 26, 52, 52, 16), 8, metal, 3);
        for(int x : new int[]{(int)c - 10, (int)c + 4}) dish.fill(dish.rect(x, 10, 6, (int)c - 4), 11, steel, 2);
        for(int y = 16; y < c - 10; y += 8) dish.fill(dish.rect((int)c - 13, y, 26, 3), 12, copperM, 1);
        dish.fill(dish.circle(c, c + 4, 10), 12, lit(0xff8a3d), 2);
        save(dish, name + "-dish");
    }

    static final Mat chitin = new Mat(new int[]{0x232a1c, 0x34402a, 0x4d5c3b, 0x6b7d51, 0x8a9c6a}, false);

    /**
     * Swarm walker: overlapping chitin plates over a body, acid sacs (spitters), spore pods (brood), and the mech
     * parts the game animates: "-leg" (one leg, drawn for both sides) and "-base" (hips, turned to the walking direction).
     */
    static void swarmMech(String name, int s, int kind) throws IOException{
        float c = s / 2f;
        R r = new R(s).turret();
        //segmented carapace: head plate in front, overlapping plates behind
        int segs = 2 + kind;
        float seg = s * 0.7f / segs;
        for(int i = segs - 1; i >= 0; i--){
            float w = s * (0.55f - i * 0.04f), y = s * 0.18f + i * seg;
            r.dome(r.oct(c - w / 2f, y, w, seg + 3, w * 0.3f), 4 + i * 0.5f, 3, chitin);
        }
        r.dome(r.oct(c - s * 0.2f, s * 0.08f, s * 0.4f, s * 0.22f, s * 0.08f), 7, 2, chitin);
        //eyes / sensor pits
        for(float x : new float[]{c - s * 0.1f, c + s * 0.1f}) r.fill(r.circle(x, s * 0.16f, Math.max(1f, s / 24f)), 9, lit(0xc9d36a), 0);
        boolean[] cellM;
        if(kind == 2){
            //spore pods on the back
            for(float[] p : new float[][]{{c - s * 0.18f, s * 0.6f}, {c + s * 0.18f, s * 0.6f}, {c, s * 0.75f}}) r.dome(r.circle(p[0], p[1], s * 0.09f), 8, 3, acc(0xb37aff));
            cellM = r.rect((int)(c - s * 0.05f), (int)(s * 0.32f), Math.max(2, (int)(s * 0.1f)), (int)(s * 0.2f));
        }else{
            //acid sac (bigger on spitters and the behemoth)
            float sac = s * (kind == 0 ? 0.1f : 0.16f);
            r.dome(r.circle(c, s * 0.62f, sac), 8, 3, glass(0xc9d36a));
            cellM = r.or(r.rect((int)(c - s * 0.24f), (int)(s * 0.4f), Math.max(2, s / 12), Math.max(3, s / 6)), r.rect((int)(c + s * 0.24f) - Math.max(2, s / 12), (int)(s * 0.4f), Math.max(2, s / 12), Math.max(3, s / 6)));
        }
        r.fill(cellM, 7, chitin, 1);
        save(r, name);
        cell(name, s, cellM, 7);

        R base = new R(s).turret();
        base.dome(base.oct(c - s * 0.3f, c - s * 0.16f, s * 0.6f, s * 0.32f, s * 0.1f), 3, 2, chitin);
        save(base, name + "-base");

        //one leg, pointing up from the hip; the game swings it for both sides
        //cropped to the leg itself: the game draws it centred on the leg position
        int lw = Math.max(3, s / 7), ll = (int)(s * 0.42f);
        R leg = new R(lw + 2, ll).turret();
        leg.fill(leg.rect(1, 0, lw, ll), 4, chitin, 1);
        leg.fill(leg.rect(0, 0, lw + 2, Math.max(2, lw / 2)), 5, chitin, 1);
        save(leg, name + "-leg");
    }

    static void swarmWasp(String name) throws IOException{
        int s = 28;
        float c = s / 2f;
        R r = new R(s).turret();
        Mat wing = glass(0xc9d8a0);
        r.fill(r.or(r.poly(new int[]{14, 27, 25, 14}, new int[]{9, 5, 16, 14}), r.poly(new int[]{14, 1, 3, 14}, new int[]{9, 5, 16, 14})), 4, wing, 1);
        r.dome(r.oct(c - 4, 4, 8, 8, 2), 6, 2, chitin);
        r.dome(r.oct(c - 3, 11, 6, 14, 2), 5, 2, chitin);
        for(int y = 14; y < 24; y += 3) r.fill(r.rect((int)c - 3, y, 6, 1), 7, acc(0xc9d36a), 0);
        boolean[] cellM = r.rect((int)c - 1, 6, 2, 3);
        save(r, name);
        cell(name, s, cellM, 7);
    }

    /** Wraith (art.md §17.3): a light recon craft. A slim needle with short forward-swept wings, wingtip guns and a nose sensor. */
    static void flyerWraith(String name) throws IOException{
        int s = 28;
        R r = new R(s).turret();
        boolean[] wings = r.or(r.poly(new int[]{14, 26, 26, 14}, new int[]{11, 15, 19, 19}), r.poly(new int[]{14, 2, 2, 14}, new int[]{11, 15, 19, 19}));
        r.fill(wings, 5, tGrey, 1);
        //wingtip guns (the weapon sits 3 units out) and the fuselage
        for(int x : new int[]{1, 25}) r.fill(r.rect(x, 9, 2, 10), 7, steel, 0);
        r.fill(r.oct(11, 2, 6, 23, 2), 7, tGrey, 2);
        r.fill(r.rect(13, 0, 2, 3), 8, steel, 0);
        r.fill(r.circle(14, 3, 1.2f), 9, lit(0x7fd8ff), 0);
        r.fill(r.oct(12, 6, 4, 5, 1), 9, glass(0x9fd8ff), 0);
        r.fill(r.rect(12, 24, 4, 2), 8, lit(0xffa060), 0);
        boolean[] cellM = r.and(wings, r.or(r.rect(4, 15, 5, 4), r.rect(19, 15, 5, 4)));
        save(r, name);
        cell(name, s, cellM, 6);
    }

    /** Interceptor (art.md §17.3): a sleek anti-air fighter. Narrow delta, canards, twin fins and two missile hardpoints. */
    static void flyerInterceptor(String name) throws IOException{
        int s = 40;
        R r = new R(s).turret();
        boolean[] wings = r.or(r.poly(new int[]{21, 37, 37, 21}, new int[]{12, 25, 30, 30}), r.poly(new int[]{19, 3, 3, 19}, new int[]{12, 25, 30, 30}));
        r.fill(wings, 5, tGrey, 2);
        //hardpoint pylons under the launchers (4 units out)
        for(int x : new int[]{2, 35}) r.fill(r.rect(x, 16, 3, 12), 6, steel, 1);
        r.fill(r.or(r.poly(new int[]{22, 28, 22}, new int[]{8, 13, 13}), r.poly(new int[]{18, 12, 18}, new int[]{8, 13, 13})), 6, tGrey, 1);
        r.fill(r.oct(17, 1, 6, 36, 3), 7, tGrey, 2);
        for(int x : new int[]{14, 24}) r.fill(r.rect(x, 29, 2, 8), 8, tGrey, 1);
        r.fill(r.oct(18, 5, 4, 8, 2), 9, glass(0x9fd8ff), 0);
        r.fill(r.rect(18, 36, 4, 3), 8, lit(0xffa060), 0);
        boolean[] cellM = r.and(wings, r.or(r.rect(7, 25, 8, 4), r.rect(25, 25, 8, 4)));
        save(r, name);
        cell(name, s, cellM, 6);
    }

    /** Valkyrie (art.md §17.3): a wide gunship. Broad wings with gun mounts at the tips, engine nacelles and an armoured hull. */
    static void flyerValkyrie(String name) throws IOException{
        int s = 56;
        R r = new R(s).turret();
        boolean[] wing = r.oct(2, 18, 52, 14, 4);
        r.fill(wing, 5, tGrey, 2);
        //engine nacelles on the wings
        for(int x : new int[]{12, 36}){
            r.fill(r.oct(x, 24, 8, 18, 3), 7, tGrey, 2);
            r.fill(r.rect(x + 2, 41, 4, 2), 8, lit(0xffa060), 0);
        }
        //armoured hull with plate seams, cockpit and tail
        r.fill(r.oct(20, 44, 16, 8, 3), 6, tGrey, 1);
        r.fill(r.oct(19, 4, 18, 44, 7), 8, tGrey, 2);
        for(int y : new int[]{20, 30, 38}) r.fill(r.rect(22, y, 12, 1), 6, bore, 0);
        r.fill(r.oct(24, 8, 8, 9, 3), 10, glass(0x9fd8ff), 0);
        //gun mounts where the guns sit (5 units out, 1 forward)
        for(int x : new int[]{8, 48}){
            r.fill(r.circle(x, 24, 5.5f), 8, tGrey, 1);
            r.fill(r.circle(x, 24, 3), 5, bore, 0);
        }
        boolean[] cellM = r.or(r.and(wing, r.or(r.rect(2, 29, 10, 3), r.rect(44, 29, 10, 3))), r.rect(26, 31, 4, 6));
        save(r, name);
        cell(name, s, cellM, 9);
    }

    /** Tempest (art.md §17.3): a strategic bomber. A flying wing round a long bomb bay, four engines on the trailing edge. */
    static void flyerTempest(String name) throws IOException{
        int s = 96;
        R r = new R(s).turret();
        boolean[] wing = r.poly(new int[]{48, 94, 94, 2, 2}, new int[]{6, 54, 62, 62, 54});
        r.fill(wing, 6, tGrey, 3);
        //engine nacelles across the trailing edge, intakes forward, exhausts glowing at the back
        for(int x : new int[]{14, 26, 60, 72}){
            r.fill(r.oct(x, 42, 10, 26, 4), 8, tGrey, 2);
            r.fill(r.rect(x + 3, 44, 4, 3), 5, bore, 0);
            r.fill(r.rect(x + 2, 64, 6, 3), 9, lit(0xffa060), 0);
        }
        r.fill(r.oct(34, 6, 28, 70, 10), 9, tGrey, 2);
        //the bomb bay: a framed recess with two rows of doors
        r.fill(r.rect(37, 27, 22, 36), 10, steel, 1);
        r.fill(r.rect(39, 29, 18, 32), 4, bore, 0);
        for(int y = 31; y < 60; y += 7){
            r.fill(r.rect(40, y, 7, 5), 6, tGrey, 0);
            r.fill(r.rect(49, y, 7, 5), 6, tGrey, 0);
        }
        r.fill(r.oct(42, 11, 12, 10, 4), 11, glass(0x9fd8ff), 0);
        boolean[] cellM = r.and(wing, r.or(r.rect(3, 55, 10, 6), r.rect(83, 55, 10, 6)));
        save(r, name);
        cell(name, s, cellM, 7);
    }

    static void flyerLeviathan(String name) throws IOException{
        int s = 128;
        float c = s / 2f;
        R r = new R(s).turret();
        r.fill(r.oct(14, 14, 100, 100, 30), 6, tGrey, 4);
        r.fill(r.oct(28, 28, 72, 72, 20), 8, tGrey, 2);
        for(float[] p : new float[][]{{22, 22}, {106, 22}, {22, 106}, {106, 106}}){
            r.fill(r.circle(p[0], p[1], 13), 9, tGrey, 2);
            r.fill(r.circle(p[0], p[1], 8), 4, bore, 0);
            r.fill(r.circle(p[0], p[1], 4), 6, lit(0xffa060), 1);
        }
        r.fill(r.ring(c, c + 14, 11, 17), 11, tGrey, 2);
        r.fill(r.circle(c, c + 14, 11), 10, lit(0xb37aff), 2);
        boolean[] cellM = r.or(r.rect(20, 54, 8, 20), r.rect(100, 54, 8, 20));
        r.fill(cellM, 7, tGrey, 1);
        save(r, name);
        cell(name, s, cellM, 7);
    }

    // ---- turrets: drawn pointing up, lit from the right; the game adds the outline and the base ("block-<size>") ----

    static final Mat tGrey = new Mat(new int[]{0x2c2d38, 0x4d4e58, 0x6a6b75, 0x8a8b94, 0xa6a8b0}, false);
    static final Mat tCopper = new Mat(new int[]{0x5c3a2e, 0x8d6352, 0xb88566, 0xd9a888, 0xeec6a6}, false);
    static final Mat bore = new Mat(new int[]{0x16161c, 0x1f1f27, 0x2c2d38, 0x34353f, 0x3c3d47}, false);
    static final Mat decalHot = new Mat(new int[]{0x8f3b38, 0xbc5452, 0xea8878, 0xfeb380, 0xffd4b0}, true);

    /** Acid Mortar: squat armoured body, two acid canisters, a short fat mortar tube. */
    static void acidMortar(String name) throws IOException{
        int s = 2 * T;
        R r = new R(s).turret();
        Mat acid = glass(0xb6dc4c);
        r.fill(r.oct(12, 22, 40, 36, 10), 6, tGrey, 2);
        r.fill(r.oct(17, 28, 30, 24, 6), 8, tGrey, 2);
        for(float x : new float[]{9, 55}){
            r.fill(r.circle(x, 40, 7.5f), 7, tGrey, 1);
            r.fill(r.circle(x, 40, 5), 8, acid, 1);
            r.paint(r.rect((int)x + 1, 37, 1, 2), 0xccffffff);
        }
        r.fill(r.rect(24, 6, 16, 26), 9, tGrey, 2);
        r.fill(r.rect(22, 4, 20, 6), 10, tGrey, 1);
        r.fill(r.oct(26, 5, 12, 5, 2), 9.5f, bore, 0);
        r.fill(r.rect(24, 18, 16, 3), 10, acc(0xb6dc4c), 0);
        r.fill(r.rect(28, 44, 8, 6), 9, acc(0xb6dc4c), 1);
        save(r, name);
        heatOf(r, name, r.rect(24, 4, 16, 10));
    }

    /** Minelayer: copper launcher with two short tubes and a rack of mines behind. */
    static void minelayer(String name) throws IOException{
        int s = 2 * T;
        R r = new R(s).turret();
        r.fill(r.oct(10, 24, 44, 32, 9), 6, tCopper, 2);
        for(int x : new int[]{14, 26, 38}) r.dome(r.circle(x + 6, 48, 5), 8, 2, tGrey);
        for(int x : new int[]{16, 36}){
            r.fill(r.rect(x, 6, 12, 26), 9, tCopper, 2);
            r.fill(r.oct(x + 2, 6, 8, 6, 2), 9.5f, bore, 0);
            r.fill(r.rect(x, 22, 12, 3), 10, decalHot, 0);
        }
        r.fill(r.rect(28, 14, 8, 20), 8, tGrey, 1);
        save(r, name);
        heatOf(r, name, r.or(r.rect(16, 6, 12, 10), r.rect(36, 6, 12, 10)));
    }

    /**
     * Heat Lance (3×3 turret): a heavy thermal platform. Radiator fins either side, a heat-collector block at the back
     * with amber vents, and a long focusing emitter pointing forward; no glowing disc.
     */
    static void heatLance(String name) throws IOException{
        int s = 3 * T;
        R r = new R(s).turret();
        r.fill(r.oct(18, 44, 60, 46, 12), 6, tGrey, 3);
        for(int i = 0; i < 5; i++){
            int y = 48 + i * 8;
            r.fill(r.rect(8, y, 16, 4), 7, tGrey, 1);
            r.fill(r.rect(72, y, 16, 4), 7, tGrey, 1);
        }
        r.fill(r.oct(30, 58, 36, 26, 6), 9, tGrey, 2);
        for(int x = 36; x < 62; x += 6) r.fill(r.rect(x, 64, 3, 14), 10, decalHot, 0);
        r.fill(r.rect(40, 10, 16, 52), 10, tGrey, 2);
        for(int y : new int[]{16, 28, 40}) r.fill(r.rect(36, y, 24, 6), 12, tGrey, 2);
        r.fill(r.oct(42, 4, 12, 8, 2), 11, bore, 0);
        save(r, name);
        heatOf(r, name, r.or(r.rect(40, 4, 16, 40), r.rect(36, 64, 26, 14)));
    }

    /** Piledriver: a heavy armoured breech with recoil rails and one long, thick barrel ending in a carbide-grey muzzle. */
    static void piledriver(String name) throws IOException{
        int s = 3 * T;
        R r = new R(s).turret();
        r.fill(r.oct(16, 46, 64, 44, 12), 6, tGrey, 3);
        r.fill(r.oct(24, 52, 48, 32, 8), 9, tGrey, 2);
        for(int x : new int[]{20, 68}) r.fill(r.rect(x, 54, 8, 30), 8, tCopper, 1);
        r.fill(r.rect(38, 6, 20, 56), 10, tGrey, 2);
        for(int y : new int[]{18, 30, 42}) r.fill(r.rect(35, y, 26, 4), 11, tGrey, 1);
        r.fill(r.rect(36, 2, 24, 8), 12, metal, 1);
        r.fill(r.oct(42, 3, 12, 6, 2), 11, bore, 0);
        r.fill(r.rect(38, 56, 20, 3), 10, decalHot, 0);
        save(r, name);
        heatOf(r, name, r.rect(36, 2, 24, 18));
    }

    /** Kestrel: a compact launcher with one interceptor on its rail (lit seeker nose) and a sensor dome. */
    static void kestrel(String name) throws IOException{
        int s = 2 * T;
        R r = new R(s).turret();
        r.fill(r.oct(12, 26, 40, 30, 8), 6, tGrey, 2);
        r.fill(r.rect(27, 6, 10, 40), 8, tGrey, 2);
        r.fill(r.rect(29, 4, 6, 30), 10, metal, 1);
        r.fill(r.tri(32, 10, new float[]{-3, 0, 3}, new float[]{0, -7, 0}, 0), 11, lit(0x8fd8ff), 0);
        for(int x : new int[]{24, 40}) r.fill(r.rect(x, 20, 3, 10), 9, tGrey, 1);
        r.dome(r.circle(44, 44, 5), 8, 2, glass(0x8fd8ff));
        save(r, name);
        heatOf(r, name, r.rect(27, 4, 10, 14));
    }

    /** Thunderhead: a stack of copper coils feeding a three-pronged emitter, with a lit blue core. */
    static void thunderhead(String name) throws IOException{
        int s = 3 * T;
        R r = new R(s).turret();
        r.fill(r.oct(16, 40, 64, 48, 12), 6, tGrey, 3);
        for(int i = 0; i < 4; i++) r.fill(r.ring(48, 64 - i * 0.5f, 14 - i * 2, 18 - i * 2), 8 + i, tCopper, 1);
        r.fill(r.circle(48, 64, 7), 12, lit(0x8fd8ff), 1);
        r.fill(r.rect(44, 14, 8, 38), 10, tGrey, 2);
        for(int dx : new int[]{-10, 0, 10}) r.fill(r.rect(46 + dx - 2, 6, 4, 14), 11, metal, 1);
        r.fill(r.rect(34, 18, 28, 4), 11, tGrey, 1);
        save(r, name);
        heatOf(r, name, r.or(r.rect(34, 6, 28, 16), r.circle(48, 64, 7)));
    }

    /**
     * Cloudbreaker (3×3 turret): a broad radial deployment hub. Six small launch ports round a central control drum,
     * cyan port lights; no mines drawn on it (they are separate entities).
     */
    static void cloudbreaker(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = new R(s).turret();
        r.fill(r.circle(c, c + 2, 40), 6, tGrey, 3);
        for(int i = 0; i < 6; i++){
            double a = Math.toRadians(i * 60 - 90);
            float px = c + (float)Math.cos(a) * 29, py = c + 2 + (float)Math.sin(a) * 29;
            r.fill(r.circle(px, py, 9), 9, tGrey, 2);
            r.fill(r.circle(px, py, 5), 5, bore, 0);
            r.fill(r.rect((int)px - 1, (int)py + 5, 2, 2), 10, lit(0x8fd8ff), 0);
        }
        r.fill(r.circle(c, c + 2, 16), 11, tGrey, 3);
        r.fill(r.circle(c, c + 2, 8), 12, metal, 1);
        r.fill(r.rect((int)c - 3, 4, 6, 14), 10, tGrey, 1);
        save(r, name);
        heatOf(r, name, r.circle(c, c + 2, 16));
    }

    /** Rivet: a small copper drum with a cluster of three short barrels (a gatling) and an ammo box on the side. */
    static void rivet(String name) throws IOException{
        int s = T;
        R r = new R(s).turret();
        r.fill(r.oct(6, 14, 20, 16, 5), 6, tCopper, 1.5f);
        r.fill(r.rect(22, 18, 6, 8), 7, tGrey, 1);
        r.fill(r.circle(16, 21, 6), 8, tGrey, 1.5f);
        for(int dx : new int[]{-4, 0, 4}) r.fill(r.rect(15 + dx, 2, 3, 16), 9 + (dx == 0 ? 1 : 0), tGrey, 1);
        r.fill(r.rect(11, 6, 11, 2), 11, metal, 0.5f);
        r.fill(r.circle(16, 21, 2.5f), 9.5f, bore, 0);
        save(r, name);
        heatOf(r, name, r.rect(11, 2, 11, 8));
    }

    /** Sickle: a launcher arm holding a toothed sawblade, on a bearing ring with a counterweight behind. */
    static void sickle(String name) throws IOException{
        int s = 2 * T;
        R r = new R(s).turret();
        r.fill(r.oct(12, 30, 40, 28, 9), 6, tGrey, 2);
        r.fill(r.ring(32, 44, 9, 13), 8, tCopper, 1);
        r.fill(r.rect(18, 50, 28, 8), 7, tGrey, 1.5f);
        for(int x : new int[]{24, 38}) r.fill(r.rect(x, 8, 4, 32), 9, tGrey, 1);
        //the blade in its cradle: teeth around a steel disc with a hub
        for(int i = 0; i < 8; i++) r.fill(r.bar(32, 18, 9, 5, 4, i * 45 + 22.5f), 10, steel, 0.5f);
        r.fill(r.circle(32, 18, 10), 11, metal, 1.5f);
        r.fill(r.circle(32, 18, 3), 12, tGrey, 1);
        save(r, name);
        heatOf(r, name, r.circle(32, 18, 13));
    }

    /** Tremor: a squat round body ringed with eight piston feet around a heavy central hammer. */
    static void tremor(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = new R(s).turret();
        r.fill(r.circle(c, c, 38), 6, tGrey, 3);
        for(int i = 0; i < 8; i++){
            r.fill(r.bar(c, c, 26, 14, 9, i * 45), 8, tGrey, 1.5f);
            r.fill(r.bar(c, c, 36, 4, 9, i * 45), 9, decalHot, 0.5f);
        }
        r.fill(r.circle(c, c, 22), 9, tCopper, 2);
        r.dome(r.circle(c, c, 15), 10, 5, tGrey);
        r.fill(r.circle(c, c, 5), 15, lit(0xd9b88a), 1);
        save(r, name);
        heatOf(r, name, r.ring(c, c, 32, 40));
    }

    /** Apiary: a hexagonal hive of drone cells around a launch bay that opens to the front. */
    static void apiary(String name) throws IOException{
        int s = 3 * T;
        R r = new R(s).turret();
        r.fill(r.oct(12, 22, 72, 64, 18), 6, tGrey, 3);
        for(int[] p : new int[][]{{30, 46}, {48, 40}, {66, 46}, {30, 66}, {48, 72}, {66, 66}}){
            r.fill(r.oct(p[0] - 8, p[1] - 7, 16, 14, 4), 8, tGrey, 1.5f);
            r.fill(r.oct(p[0] - 5, p[1] - 4, 10, 8, 3), 7, bore, 0);
            r.paint(r.rect(p[0] - 1, p[1] - 1, 2, 2), 0xfff8c166);
        }
        r.fill(r.rect(36, 6, 24, 26), 9, tGrey, 2);
        r.fill(r.rect(40, 4, 16, 14), 8, bore, 0);
        r.fill(r.rect(36, 22, 24, 3), 10, acc(0xf8c166), 0.5f);
        save(r, name);
        heatOf(r, name, r.rect(36, 4, 24, 14));
    }

    /** Event Horizon: a heavy ring frame holding a black core between two curved emitter prongs, with aetherite cells. */
    static void eventHorizon(String name) throws IOException{
        int s = 4 * T;
        float c = s / 2f;
        R r = new R(s).turret();
        r.fill(r.oct(20, 50, 88, 70, 20), 6, tGrey, 4);
        for(int x : new int[]{22, 94}){
            r.fill(r.rect(x, 64, 12, 40), 8, tGrey, 2);
            for(int y = 70; y < 100; y += 10) r.fill(r.rect(x + 3, y, 6, 6), 9, glass(0x7fe0ff), 0.5f);
        }
        r.fill(r.ring(c, 48, 22, 32), 10, tGrey, 2.5f);
        r.fill(r.ring(c, 48, 18, 22), 9, metal, 1);
        r.fill(r.circle(c, 48, 18), 4, bore, 0);
        r.fill(r.ring(c, 48, 15, 18), 9.5f, lit(0xb37aff), 0);
        for(int dx : new int[]{-1, 1}) r.fill(r.tri(c + dx * 22, 22, new float[]{-5, 0, 5}, new float[]{10, -16, 10}, dx * -12), 11, tGrey, 1.5f);
        r.fill(r.rect(52, 96, 24, 16), 9, tGrey, 2);
        save(r, name);
        heatOf(r, name, r.ring(c, 48, 18, 32));
    }

    /** "-heat": white where the turret glows after firing; the game tints it with the turret's heat colour. */
    static void heatOf(R r, String name, boolean[] m) throws IOException{
        BufferedImage img = img(r.w);
        for(int i = 0; i < m.length; i++) if(m[i] && r.mat[i] != null) img.setRGB(i % r.w, i / r.w, 0xffffffff);
        save(img, name + "-heat");
    }

    /** Jammer (a building, block lighting): emitter housing with a turning dish ("-dish") and a pulse light ("-glow"). */
    static void jammer(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = new R(s);
        housing(r);
        deck(r, 4);
        r.fill(r.circle(c, c, 22), 8, metal, 2);
        r.fill(r.circle(c, c, 16), 3, deep, 0);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 18, 8, 4, i * 90 + 45), 9, steel, 1);
        connectors(r, 0xb37aff);
        save(r, name);

        R dish = new R(s);
        dish.fill(dish.oct(c - 14, c - 5, 28, 10, 4), 6, metal, 2);
        dish.fill(dish.rect((int)c - 2, (int)c - 14, 4, 28), 7, steel, 1);
        dish.fill(dish.circle(c, c, 4), 9, lit(0xb37aff), 1);
        save(dish, name + "-dish");
        save(dish.glowImage(), name + "-glow");
    }

    /** Mine (bullet sprite, front + back): a flat disc with a pressure cap. */
    static void mine(String name, int rgb) throws IOException{
        R r = new R(16);
        r.fill(r.circle(8, 8, 6.5f), 3, tGrey, 1);
        r.fill(r.circle(8, 8, 2.5f), 5, acc(rgb), 1);
        save(r, name);
        BufferedImage back = img(16);
        Graphics2D g = g(back);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Float(1, 2, 14, 14));
        g.dispose();
        save(back, name + "-back");
    }

    /** Status icon: a broken signal (jammed targeting). */
    static void jammedIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        r.fill(r.ring(16, 18, 9, 12), 5, m, 1);
        r.fill(r.ring(16, 18, 3, 6), 6, m, 1);
        r.clear(r.rect(0, 20, T, 12));
        r.clear(r.bar(16, 16, -16, 32, 3, 45));
        r.fill(r.circle(16, 22, 2.5f), 6, m, 0);
        save(r, name);
    }

    /** Status icon: an armour plate eaten through by caustic sulfur. */
    static void etchedIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        r.fill(r.poly(new int[]{5, 27, 27, 16, 5}, new int[]{5, 5, 18, 29, 18}), 6, metal, 2);
        r.fill(r.poly(new int[]{9, 23, 23, 16, 9}, new int[]{9, 9, 17, 25, 17}), 7, steel, 1);
        //pitting where the acid bit in, and the dripping sulfur
        for(int[] p : new int[][]{{11, 12}, {19, 11}, {14, 18}, {20, 17}}) r.fill(r.rect(p[0], p[1], 2, 2), 3, deep, 0);
        r.dome(r.circle(22, 8, 4), 8, 3, acc(rgb));
        r.dome(r.circle(24, 15, 2.6f), 8, 2, acc(rgb));
        save(r, name);
    }

    // =====================================================================================================
    // Materials. A material is a five-step ramp: 0 deepest, 1 dark (bottom/left faces), 2 mid (flat tops),
    // 3 lit (top/right faces), 4 highlight. Only "shiny" materials (accents, glass) reach 0 and 4 from slope alone.
    // Base metal follows the vanilla block palette: #6E7080 / #989AA4 / #B0BAC0, deep interiors #4A4B53.
    // =====================================================================================================

    /** glow: painted into the "-glow" layer, which the game pulses in the product's colour while the block works. */
    record Mat(int[] c, boolean shiny, boolean glow){
        Mat(int[] c, boolean shiny){
            this(c, shiny, false);
        }
    }

    /** An active part (heater, lamp, reaction zone): coloured in the sprite and lit in the "-glow" layer. */
    static Mat lit(int rgb){
        return new Mat(tones(rgb), true, true);
    }

    static final Mat metal = new Mat(new int[]{0x5d5e6a, 0x6e7080, 0x989aa4, 0xb0bac0, 0xc3cbd0}, false);
    static final Mat steel = new Mat(new int[]{0x4b4c57, 0x5d5f6c, 0x7a7c88, 0x8f929c, 0xa3a7b0}, false);
    static final Mat deep = new Mat(new int[]{0x2b2c33, 0x36373f, 0x4a4b53, 0x54555e, 0x5e606a}, false);
    static final Mat brass = acc(0xd8c27a), copperM = acc(0xd99d73), redM = acc(0xd94f4f);

    static Mat acc(int rgb){
        return new Mat(tones(rgb), true);
    }

    /** Dull material ramp (no 0/4 from slope): walls and large tinted plates. */
    static Mat dull(int rgb){
        return new Mat(tones(rgb), false);
    }

    /** Tinted glass: dark body with a bright glint on the lit rim. */
    static Mat glass(int rgb){
        return new Mat(new int[]{mixi(rgb, 0x16171d, 0.86f), mixi(rgb, 0x16171d, 0.74f), mixi(rgb, 0x16171d, 0.56f),
            mixi(rgb, 0x16171d, 0.34f), tint(rgb, 0.45f)}, true);
    }

    static int[] tones(int rgb){
        return new int[]{shade(rgb, 0.52f), shade(rgb, 0.74f), rgb, tint(rgb, 0.30f), tint(rgb, 0.56f)};
    }

    /** Darker tone: lower brightness, slightly richer saturation (like the vanilla decal ramp). */
    static int shade(int rgb, float k){
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
        return Color.HSBtoRGB(hsb[0], Math.min(1f, hsb[1] * (1f + (1f - k) * 0.35f)), hsb[2] * k) & 0xffffff;
    }

    /** Lighter tone: towards a warm white. */
    static int tint(int rgb, float t){
        return mixi(rgb, 0xfffaf0, t);
    }

    static int mixi(int a, int b, float t){
        int r = (int)(((a >> 16) & 0xff) + (((b >> 16) & 0xff) - ((a >> 16) & 0xff)) * t);
        int g = (int)(((a >> 8) & 0xff) + (((b >> 8) & 0xff) - ((a >> 8) & 0xff)) * t);
        int bl = (int)((a & 0xff) + ((b & 0xff) - (a & 0xff)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // =====================================================================================================
    // Relief canvas: parts are painted as masks with a material and a height; render() shades by slope.
    // =====================================================================================================

    static final class R{
        final int w, h;
        final float[] z;
        final Mat[] mat;
        final int[] flat; //unshaded ARGB paint (indicator lights, decals); 0 = none
        final boolean[] winGlow; //see-through windows, faintly lit in the "-glow" layer
        final int[] part; //which painted part each pixel belongs to (-1 = none), for diagonal split lighting
        final List<float[]> parts = new ArrayList<>(); //cx, cy, split (1/0)
        int seed; //per sprite, so every block wears differently but the same on every build
        /** Light direction: blocks (1, 1) = upper right; turrets (1, 0) = right (vanilla turret convention). */
        float lx = 1f, ly = 1f;
        /** Edge wear; off for turrets, whose outline the game draws. */
        boolean worn = false;

        R turret(){
            lx = 1.4f;
            ly = 0f;
            worn = false;
            return this;
        }

        R(int s){
            this(s, s);
        }

        R(int w, int h){
            this.w = w;
            this.h = h;
            z = new float[w * h];
            mat = new Mat[w * h];
            flat = new int[w * h];
            part = new int[w * h];
            Arrays.fill(part, -1);
            winGlow = new boolean[w * h];
        }

        boolean in(int x, int y){
            return x >= 0 && y >= 0 && x < w && y < h;
        }

        float zAt(int x, int y){
            return in(x, y) ? z[y * w + x] : 0f;
        }

        // ---- masks ----

        boolean[] rect(int x, int y, int rw, int rh){
            boolean[] m = new boolean[w * h];
            for(int yy = Math.max(0, y); yy < Math.min(h, y + rh); yy++){
                for(int xx = Math.max(0, x); xx < Math.min(w, x + rw); xx++) m[yy * w + xx] = true;
            }
            return m;
        }

        boolean[] shape(Shape s){
            boolean[] m = new boolean[w * h];
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++) m[y * w + x] = s.contains(x + 0.5, y + 0.5);
            return m;
        }

        boolean[] oct(float x, float y, float ow, float oh, float cut){
            Path2D.Float p = new Path2D.Float();
            p.moveTo(x + cut, y);
            p.lineTo(x + ow - cut, y);
            p.lineTo(x + ow, y + cut);
            p.lineTo(x + ow, y + oh - cut);
            p.lineTo(x + ow - cut, y + oh);
            p.lineTo(x + cut, y + oh);
            p.lineTo(x, y + oh - cut);
            p.lineTo(x, y + cut);
            p.closePath();
            return shape(p);
        }

        boolean[] circle(float cx, float cy, float r){
            //large round parts are built from 45-degree cuts, as in vanilla
            if(r >= 5f) return oct(cx - r, cy - r, r * 2, r * 2, Math.round(r * 0.6f));
            boolean[] m = new boolean[w * h];
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                float dx = x + 0.5f - cx, dy = y + 0.5f - cy;
                m[y * w + x] = dx * dx + dy * dy <= r * r;
            }
            return m;
        }

        boolean[] ring(float cx, float cy, float r0, float r1){
            return minus(circle(cx, cy, r1), circle(cx, cy, r0));
        }

        /** A bar from offset to offset+len along angle (degrees, 0 = right, counter-clockwise on screen), width wide. */
        boolean[] bar(float cx, float cy, float offset, float len, float width, float deg){
            Shape r = new Rectangle2D.Float(cx + offset, cy - width / 2f, len, width);
            return shape(AffineTransform.getRotateInstance(-Math.toRadians(deg), cx, cy).createTransformedShape(r));
        }

        /** Triangle in local coordinates (x outwards along deg), rotated around (cx, cy). */
        boolean[] tri(float cx, float cy, float[] xs, float[] ys, float deg){
            Path2D.Float p = new Path2D.Float();
            p.moveTo(cx + xs[0], cy + ys[0]);
            p.lineTo(cx + xs[1], cy + ys[1]);
            p.lineTo(cx + xs[2], cy + ys[2]);
            p.closePath();
            return shape(AffineTransform.getRotateInstance(-Math.toRadians(deg), cx, cy).createTransformedShape(p));
        }

        boolean[] poly(int[] xs, int[] ys){
            Path2D.Float p = new Path2D.Float();
            p.moveTo(xs[0], ys[0]);
            for(int i = 1; i < xs.length; i++) p.lineTo(xs[i], ys[i]);
            p.closePath();
            return shape(p);
        }

        boolean[] grow(boolean[] m, int n){
            boolean[] cur = m;
            for(int k = 0; k < n; k++){
                boolean[] next = cur.clone();
                for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                    if(cur[y * w + x]) continue;
                    for(int dy = -1; dy <= 1 && !next[y * w + x]; dy++) for(int dx = -1; dx <= 1; dx++){
                        if(in(x + dx, y + dy) && cur[(y + dy) * w + x + dx]){
                            next[y * w + x] = true;
                            break;
                        }
                    }
                }
                cur = next;
            }
            return cur;
        }

        boolean[] minus(boolean[] a, boolean[] b){
            boolean[] m = new boolean[w * h];
            for(int i = 0; i < m.length; i++) m[i] = a[i] && !b[i];
            return m;
        }

        boolean[] and(boolean[] a, boolean[] b){
            boolean[] m = new boolean[w * h];
            for(int i = 0; i < m.length; i++) m[i] = a[i] && b[i];
            return m;
        }

        boolean[] or(boolean[] a, boolean[] b){
            boolean[] m = new boolean[w * h];
            for(int i = 0; i < m.length; i++) m[i] = a[i] || b[i];
            return m;
        }

        /** Pixels of the mask that already hold paint. */
        boolean[] painted(boolean[] m){
            boolean[] r = new boolean[w * h];
            for(int i = 0; i < r.length; i++) r[i] = m[i] && (mat[i] != null || flat[i] != 0);
            return r;
        }

        // ---- paint ----

        /** Chamfer distance from each mask pixel to the nearest pixel outside the mask (edge pixels = 1). */
        float[] dist(boolean[] m){
            float[] d = new float[w * h];
            final float diag = 1.4142f;
            for(int i = 0; i < d.length; i++) d[i] = m[i] ? 1e6f : 0f;
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                int i = y * w + x;
                if(!m[i]) continue;
                float v = d[i];
                v = Math.min(v, at(d, x - 1, y) + 1);
                v = Math.min(v, at(d, x, y - 1) + 1);
                v = Math.min(v, at(d, x - 1, y - 1) + diag);
                v = Math.min(v, at(d, x + 1, y - 1) + diag);
                d[i] = v;
            }
            for(int y = h - 1; y >= 0; y--) for(int x = w - 1; x >= 0; x--){
                int i = y * w + x;
                if(!m[i]) continue;
                float v = d[i];
                v = Math.min(v, at(d, x + 1, y) + 1);
                v = Math.min(v, at(d, x, y + 1) + 1);
                v = Math.min(v, at(d, x + 1, y + 1) + diag);
                v = Math.min(v, at(d, x - 1, y + 1) + diag);
                d[i] = v;
            }
            return d;
        }

        float at(float[] d, int x, int y){
            return in(x, y) ? d[y * w + x] : 0f;
        }

        /** Flat-topped part at height top; the outer bevel pixels slope down so the edges catch light / shade. */
        R fill(boolean[] m, float top, Mat mt, float bevel){
            float[] d = bevel > 0 ? dist(m) : null;
            int id = addPart(m, mt);
            for(int i = 0; i < m.length; i++){
                if(!m[i]) continue;
                mat[i] = mt;
                flat[i] = 0;
                part[i] = id;
                z[i] = d != null && d[i] <= bevel ? top - (bevel + 1 - d[i]) : top;
            }
            return this;
        }

        /**
         * Registers a part. Raised parts smaller than a third of the sprite get vanilla's diagonal split:
         * the half towards the light reads one tone lighter than the half away from it.
         */
        int addPart(boolean[] m, Mat mt){
            float sx = 0, sy = 0;
            int n = 0;
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                if(!m[y * w + x]) continue;
                sx += x + 0.5f;
                sy += y + 0.5f;
                n++;
            }
            boolean split = mt != deep && n > 8;
            parts.add(new float[]{n == 0 ? 0 : sx / n, n == 0 ? 0 : sy / n, split ? 1 : 0});
            return parts.size() - 1;
        }

        /**
         * Raised cap / cushion / ridge, built as crisp terraces (a rim step and a raised top) instead of a smooth
         * gradient: vanilla shows height with a few hard-edged plates, not airbrushed domes.
         */
        R dome(boolean[] m, float base, float height, Mat mt){
            float[] d = dist(m);
            float max = 1f;
            for(int i = 0; i < m.length; i++) if(m[i]) max = Math.max(max, d[i]);
            float step = Math.max(1.5f, Math.min(3f, max * 0.35f));
            int id = addPart(m, mt);
            for(int i = 0; i < m.length; i++){
                if(!m[i]) continue;
                mat[i] = mt;
                flat[i] = 0;
                part[i] = id;
                z[i] = d[i] <= 1.01f ? base + height * 0.3f : d[i] <= step + 0.01f ? base + height * 0.6f : base + height;
            }
            return this;
        }

        /** Horizontal cylinder (pipe, drum) lying along x. */
        R cylH(int x, int y, int len, int th, float base, float height, Mat mt){
            boolean[] m = rect(x, y, len, th);
            for(int yy = y; yy < y + th; yy++) for(int xx = x; xx < x + len; xx++){
                if(!in(xx, yy)) continue;
                float t = (yy + 0.5f - (y + th / 2f)) / (th / 2f);
                int i = yy * w + xx;
                mat[i] = mt;
                flat[i] = 0;
                part[i] = -1;
                z[i] = base + height * (float)Math.sqrt(Math.max(0f, 1f - t * t));
            }
            return this;
        }

        /** Vertical cylinder lying along y. */
        R cylV(int x, int y, int th, int len, float base, float height, Mat mt){
            for(int yy = y; yy < y + len; yy++) for(int xx = x; xx < x + th; xx++){
                if(!in(xx, yy)) continue;
                float t = (xx + 0.5f - (x + th / 2f)) / (th / 2f);
                int i = yy * w + xx;
                mat[i] = mt;
                flat[i] = 0;
                part[i] = -1;
                z[i] = base + height * (float)Math.sqrt(Math.max(0f, 1f - t * t));
            }
            return this;
        }

        /** Sloped-in hopper / funnel / basin: walls fall from top at the rim to bottom in the middle. */
        R funnel(boolean[] m, float top, float bottom, Mat mt){
            float[] d = dist(m);
            float max = 1f;
            for(int i = 0; i < m.length; i++) if(m[i]) max = Math.max(max, d[i]);
            for(int i = 0; i < m.length; i++){
                if(!m[i]) continue;
                mat[i] = mt;
                flat[i] = 0;
                part[i] = -1;
                z[i] = top - (top - bottom) * Math.min(1f, (d[i] - 1f) / Math.max(1f, max - 1f));
            }
            return this;
        }

        /** The "-glow" layer: active parts fully lit, see-through windows faintly. Null when there is nothing to light. */
        BufferedImage glowImage(){
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            boolean any = false;
            for(int i = 0; i < w * h; i++){
                if(mat[i] != null && mat[i].glow() && flat[i] == 0){
                    img.setRGB(i % w, i / w, 0xe6ffffff);
                    any = true;
                }else if(winGlow[i]){
                    img.setRGB(i % w, i / w, 0x5cffffff);
                    any = true;
                }
            }
            return any ? img : null;
        }

        R raise(boolean[] m, float dz){
            for(int i = 0; i < m.length; i++) if(m[i] && mat[i] != null) z[i] += dz;
            return this;
        }

        R recolor(boolean[] m, Mat mt){
            for(int i = 0; i < m.length; i++) if(m[i] && mat[i] != null) mat[i] = mt;
            return this;
        }

        /** Transparent: the block's liquid / bottom layer shows through here. */
        R clear(boolean[] m){
            for(int i = 0; i < m.length; i++){
                if(!m[i]) continue;
                mat[i] = null;
                flat[i] = 0;
                part[i] = -1;
                z[i] = 0f;
            }
            return this;
        }

        /** Unshaded paint on top (lights, glints). */
        R paint(boolean[] m, int argb){
            for(int i = 0; i < m.length; i++) if(m[i]) flat[i] = argb;
            return this;
        }

        /**
         * Edge wear on plain metal (not accents, glass or deep interiors), so blocks don't look factory-new:
         * short chips in the lit edge bands, dents in the shadowed ones, the odd scratch catching light,
         * and a few scuffs on the flat plate just inside an edge. Deliberate 1-2 pixel marks, never noise fields.
         */
        void wear(int[] tone, byte[] band){
            int[] src = tone.clone();
            for(int y = 1; y < h - 1; y++) for(int x = 1; x < w - 1; x++){
                int i = y * w + x;
                Mat mt = mat[i];
                if(mt == null || mt.shiny || mt == deep || flat[i] != 0) continue;
                if(band[i] != 0){
                    //marks run two pixels along the band: this pixel, or the one before it, starts a mark
                    int prev = band[i] == 1 ? i - 1 : i - w;
                    int roll = Math.min(hash(x, y), band[prev] == band[i] ? hash(prev % w, prev / w) : 100);
                    if(src[i] == 3 && roll < 6) tone[i] = 2;            //chipped lit edge
                    else if(src[i] == 1 && roll < 5) tone[i] = 0;        //dent in the shadowed edge
                    else if(src[i] == 1 && roll < 7) tone[i] = 2;       //scratch catching the light
                }else if(src[i] == 2 || src[i] == 3){
                    //scuffs on the plate right next to an edge band
                    boolean nearEdge = band[i - 1] != 0 || band[i + 1] != 0 || band[i - w] != 0 || band[i + w] != 0;
                    if(nearEdge && hash(x, y) < 3) tone[i] = src[i] - 1;
                }
            }
        }

        int hash(int x, int y){
            int k = x * 73856093 ^ y * 19349663 ^ seed * 83492791;
            k ^= k >>> 13;
            k *= 0x5bd1e995;
            k ^= k >>> 15;
            return Math.floorMod(k, 100);
        }

        BufferedImage render(){
            int[] tone = new int[w * h];
            byte[] band = new byte[w * h]; //edge band direction: 1 runs horizontally, 2 vertically
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                int i = y * w + x;
                Mat mt = mat[i];
                if(mt == null) continue;
                float c = z[i];
                //light from the upper right. Sobel gradient: gx rises to the right, gy rises downwards
                float gx = zAt(x + 1, y - 1) + 2 * zAt(x + 1, y) + zAt(x + 1, y + 1) - zAt(x - 1, y - 1) - 2 * zAt(x - 1, y) - zAt(x - 1, y + 1);
                float gy = zAt(x - 1, y + 1) + 2 * zAt(x, y + 1) + zAt(x + 1, y + 1) - zAt(x - 1, y - 1) - 2 * zAt(x, y - 1) - zAt(x + 1, y - 1);
                float slope = gy * ly - gx * lx;
                int t = 2;
                if(slope > 1.5f) t = slope > 14f && mt.shiny ? 4 : 3;
                else if(slope < -1.5f) t = slope < -14f && mt.shiny ? 0 : 1;
                if(t != 2) band[i] = (byte)(Math.abs(gy) >= Math.abs(gx) ? 1 : 2);
                else if(part[i] >= 0 && parts.get(part[i])[2] > 0){
                    //vanilla's diagonal split: the half of a raised plate towards the light is one tone lighter
                    float[] p = parts.get(part[i]);
                    if((x + 0.5f - p[0]) * Math.signum(lx) - (y + 0.5f - p[1]) * Math.signum(ly) > 0.5f) t = 3;
                }
                //short cast shadow from taller parts towards the upper right
                for(int k = 1; k <= 3; k++){
                    if(zAt(x + k * (int)Math.signum(lx), y - k * (int)Math.signum(ly)) - c > 1.5f * k + 0.5f){
                        t--;
                        break;
                    }
                }
                tone[i] = Math.max(0, Math.min(4, t));
            }
            //despeckle: a lone pixel whose four neighbours agree on another tone of the same material takes that tone
            int[] clean = tone.clone();
            for(int y = 1; y < h - 1; y++) for(int x = 1; x < w - 1; x++){
                int i = y * w + x;
                Mat mt = mat[i];
                if(mt == null || flat[i] != 0) continue;
                int[] nb = {i - 1, i + 1, i - w, i + w};
                boolean same = true;
                for(int n : nb) if(mat[n] != mt || flat[n] != 0 || tone[n] != tone[nb[0]]) same = false;
                if(same && tone[nb[0]] != tone[i]) clean[i] = tone[nb[0]];
            }
            if(worn) wear(clean, band);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for(int i = 0; i < w * h; i++){
                if(flat[i] != 0) img.setRGB(i % w, i / w, flat[i]);
                else if(mat[i] != null) img.setRGB(i % w, i / w, 0xff000000 | mat[i].c[clean[i]]);
            }
            return img;
        }
    }

    // =====================================================================================================
    // Shared building parts
    // =====================================================================================================

    /** Full-footprint housing: light top/right bevel, dark bottom/left bevel, chamfered corners (vanilla body). */
    static void housing(R r){
        int s = r.w;
        r.fill(r.oct(0, 0, s, s, s >= 96 ? 5 : 3), 6, metal, s >= 96 ? 4 : 3);
    }

    /**
     * Accent connector caps at the middle of each edge, as on vanilla crafters (kiln, smelter, separator).
     * Drawn only over the frame and deck, never over machinery.
     */
    static void connectors(R r, int a){
        int s = r.w, cw = Math.max(12, (int)(s * 0.3f)), dep = Math.max(5, Math.min(8, s / 8));
        boolean[] caps = r.or(r.or(r.oct(s / 2f - cw / 2f, -dep, cw, dep * 2, dep * 0.8f), r.oct(s / 2f - cw / 2f, s - dep, cw, dep * 2, dep * 0.8f)),
            r.or(r.oct(-dep, s / 2f - cw / 2f, dep * 2, cw, dep * 0.8f), r.oct(s - dep, s / 2f - cw / 2f, dep * 2, cw, dep * 0.8f)));
        for(int i = 0; i < caps.length; i++) if(caps[i] && (r.mat[i] == null || r.z[i] > 6.5f)) caps[i] = false;
        r.fill(caps, 7, acc(a), 1);
        //a dark socket line where each cap meets the machine
        boolean[] inner = r.or(r.or(r.rect(s / 2 - cw / 2 + 2, dep - 1, cw - 4, 1), r.rect(s / 2 - cw / 2 + 2, s - dep, cw - 4, 1)),
            r.or(r.rect(dep - 1, s / 2 - cw / 2 + 2, 1, cw - 4), r.rect(s - dep, s / 2 - cw / 2 + 2, 1, cw - 4)));
        r.recolor(r.and(inner, caps), new Mat(new int[]{shade(a, 0.4f), shade(a, 0.45f), shade(a, 0.55f), shade(a, 0.65f), shade(a, 0.75f)}, false));
    }

    /** Sunken work surface inside the housing. */
    static void deck(R r, int inset){
        int s = r.w;
        r.fill(r.rect(inset, inset, s - inset * 2, s - inset * 2), 4, metal, 2);
        //large decks are assembled from plates: seams keep them from reading as one empty slab
        if(s >= 96){
            for(int k = 1; k < s / 32; k++){
                r.raise(r.rect(k * 32 - 1, inset + 3, 2, s - inset * 2 - 6), -1.5f);
                r.raise(r.rect(inset + 3, k * 32 - 1, s - inset * 2 - 6, 2), -1.5f);
            }
        }
    }

    static void bolt(R r, int x, int y){
        float base = r.zAt(x, y);
        r.fill(r.rect(x, y, 2, 2), base + 1.5f, metal, 0);
    }

    static void cornerBolts(R r, int inset){
        int s = r.w;
        bolt(r, inset, inset);
        bolt(r, s - inset - 2, inset);
        bolt(r, inset, s - inset - 2);
        bolt(r, s - inset - 2, s - inset - 2);
    }

    static void pipeH(R r, int x, int y, int len, int th, float z){
        if(len <= 0) return;
        r.cylH(x, y, len, th, z, th * 0.7f, metal);
        r.cylH(x, y - 1, 2, th + 2, z + 0.5f, th * 0.7f, steel);
        r.cylH(x + len - 2, y - 1, 2, th + 2, z + 0.5f, th * 0.7f, steel);
    }

    static void pipeV(R r, int x, int y, int len, int th, float z){
        if(len <= 0) return;
        r.cylV(x, y, th, len, z, th * 0.7f, metal);
        r.cylV(x - 1, y, th + 2, 2, z + 0.5f, th * 0.7f, steel);
        r.cylV(x - 1, y + len - 2, th + 2, 2, z + 0.5f, th * 0.7f, steel);
    }

    /**
     * A framed opening. see = transparent, so the block's liquid tile (DrawLiquidTile over the -bottom layer)
     * shows through; otherwise tinted glass for blocks that have no liquid layer.
     */
    static void window(R r, boolean[] m, boolean see, int rgb){
        boolean[] lip = r.painted(r.minus(r.grow(m, 1), m));
        r.raise(lip, 1f).recolor(lip, steel);
        if(see){
            r.clear(m);
            for(int i = 0; i < m.length; i++) if(m[i]) r.winGlow[i] = true;
        }else{
            float lowest = 1e6f;
            for(int i = 0; i < m.length; i++) if(m[i] && r.mat[i] != null) lowest = Math.min(lowest, r.z[i]);
            r.fill(m, (lowest == 1e6f ? 4 : lowest) - 1f, glass(rgb), 0);
        }
    }

    /** Liquid/heat output port on the right edge (DrawHeatOutput / DrawLiquidOutputs layers). */
    static R port(int s, int len, Mat inner){
        R r = new R(s);
        int y = s / 2 - len / 2;
        r.fill(r.rect(s - 8, y, 8, len), 8, metal, 1);
        for(int yy = y + 3; yy < y + len - 3; yy += 3) r.fill(r.rect(s - 7, yy, 6, 2), 7, inner, 0);
        return r;
    }

    /** The current sprite's rotating layer ("-rotator"). The game spins it around the block centre while the block works. */
    static R rotor;

    static R rotor(int s){
        rotor = new R(s);
        return rotor;
    }

    /** Writes "-glow" from the lit parts / windows (a small central light if there are none) and "-rotator" if one was drawn. */
    static void layers(R r, String name) throws IOException{
        BufferedImage glow = r.glowImage();
        if(glow == null){
            glow = img(r.w);
            Graphics2D g = g(glow);
            g.setColor(new Color(255, 255, 255, 120));
            g.fillRect(r.w / 2 - 3, r.w / 2 - 3, 6, 6);
            g.dispose();
        }
        save(glow, name + "-glow");
        if(rotor != null){
            save(rotor, name + "-rotator");
            rotor = null;
        }
    }

    static void lamp(R r, int x, int y, int a){
        r.fill(r.rect(x, y, 3, 3), r.zAt(x + 1, y + 1) + 1.5f, lit(a), 0);
    }

    /** Exhaust stack seen from above: thick rim around a dark flue. */
    static void stack(R r, float cx, float cy, float rad){
        r.fill(r.circle(cx, cy, rad), 11, metal, 2);
        r.fill(r.circle(cx, cy, rad - 2.5f), 2, deep, 0);
    }

    /** Storage / dosing tank seen from above: body, lid and a lit level gauge. */
    static void tank(R r, float x, float y, float w, float h, int a){
        float cut = Math.min(w, h) * 0.3f;
        r.fill(r.oct(x, y, w, h, cut), 8, metal, 2);
        r.fill(r.oct(x + 3, y + 3, w - 6, h - 6, cut * 0.6f), 9.5f, metal, 1);
        r.fill(r.rect((int)(x + w / 2) - 1, (int)(y + 4), 3, Math.max(3, (int)(h / 2) - 4)), 10.5f, lit(a), 0);
    }

    /** Horizontal rotary drum with hoop ribs. */
    static void drum(R r, int x, int y, int w, int h, int ribStep){
        r.cylH(x, y, w, h, 6, h * 0.45f, metal);
        for(int xx = x + 3; xx < x + w - 2; xx += ribStep) r.cylH(xx, y - 1, 2, h + 2, 6.5f, h * 0.45f, steel);
    }

    static boolean[] gear(R r, float cx, float cy, float rad, int teeth){
        boolean[] m = r.circle(cx, cy, rad - 2);
        for(int i = 0; i < teeth; i++) m = r.or(m, r.bar(cx, cy, rad - 4, 4, Math.max(3, rad * 0.35f), i * 360f / teeth));
        return m;
    }

    /** Feed hopper: frame and sloped walls down to a dark throat. */
    static void hopper(R r, float x, float y, float w, float h){
        r.fill(r.oct(x, y, w, h, 3), 9, metal, 2);
        r.funnel(r.oct(x + 3, y + 3, w - 6, h - 6, 2), 8, 2, steel);
        r.fill(r.rect((int)(x + w / 2) - 2, (int)(y + h / 2) - 2, 4, 4), 1.5f, deep, 0);
    }

    static BufferedImage flipY(BufferedImage src){
        int s = src.getHeight();
        BufferedImage f = new BufferedImage(src.getWidth(), s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = f.createGraphics();
        g.drawImage(src, 0, s, src.getWidth(), -s, null);
        g.dispose();
        return f;
    }

    static void save(R r, String name) throws IOException{
        r.seed = name.hashCode();
        save(r.render(), name);
    }

    static void save(BufferedImage img, String name) throws IOException{
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    static BufferedImage img(int s){
        return new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D g(BufferedImage img){
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return g;
    }

    static Color alpha(int rgb, int a){
        return new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, a);
    }

    // =====================================================================================================
    // Crafters with a liquid layer: "-bottom" + main sprite with see-through windows
    // =====================================================================================================

    static void machine(String name, int size, int a, boolean round) throws IOException{
        int s = size * T;
        R b = new R(s);
        b.fill(b.rect(0, 0, s, s), 2, deep, 0);
        for(int x = 6; x < s - 4; x += 8) b.fill(b.rect(x, 3, 2, s - 6), 3, deep, 0);
        save(b, name + "-bottom");

        R r = new R(s);
        housing(r);
        switch(name){
            case "water-treatment-plant" -> filters(r, a, true);
            case "industrial-scrubber" -> scrubber(r, a, true);
            case "wastewater-treatment-plant" -> clarifier(r, a, true);
            case "ore-washer" -> drumWasher(r, a, true);
            case "mineral-separator" -> centrifuge(r, a, true);
            case "electrolysis-plant" -> cells(r, a, true);
            case "chlor-alkali-cell" -> membraneCells(r, a, true);
            case "electrowinning-cell" -> tankHouse(r, a, true);
            case "air-separation-unit" -> coldBox(r, a, true);
            case "gas-separator" -> columns(r, a, true);
            case "distillation-column", "cooling-tower" -> tower(r, a, name, true);
            case "cryogenic-condenser" -> condenser(r, a, true);
            case "biomass-cultivator", "biochemical-fermenter" -> bio(r, a, name, true);
            case "algae-photobioreactor" -> tubeReactor(r, a, true);
            case "anaerobic-digester" -> digester(r, a, true);
            case "silicon-refinery" -> zoneRefiner(r, a, true);
            case "circuit-board-printer" -> precision(r, a, true);
            case "platinum-refinery" -> filterPress(r, a, true);
            case "aqua-regia-mixer" -> mixer(r, a, true);
            case "sulfuric-acid-plant" -> converter(r, a, true);
            case "industrial-neutralizer" -> neutralizer(r, a, true);
            case "nitric-acid-plant" -> gauzeBurner(r, a, true);
            case "ammonium-nitrate-plant" -> prillTower(r, a, true);
            case "catalyst-plant" -> catalystDrum(r, a, true);
            case "fertilizer-plant" -> granulator(r, a, true);
            case "industrial-boiler" -> boiler(r, a, true);
            case "alumina-refinery" -> autoclaves(r, a, true);
            case "lithium-extractor" -> ponds(r, a, true);
            case "acid-synthesizer" -> hclBurner(r, a, true);
            case "acid-leaching-vat" -> vat(r, a, true);
            case "high-pressure-reactor" -> forged(r, a, true);
            case "polymerization-reactor" -> loopReactor(r, a, true);
            case "steam-cracker" -> cracker(r, a, true);
            case "methanol-synthesizer" -> tubular(r, a, true);
            case "olefin-reactor" -> fluidBed(r, a, true);
            case "reprocessing-plant" -> hotCell(r, a, true);
            case "epoxy-plant" -> kettle(r, a, true);
            case "rubber-plant" -> rubberLine(r, a, true);
            case "acrylonitrile-reactor", "chemical-synthesis-complex" -> pressure(r, a, name, true, round);
            default -> chamber(r, a, true);
        }
        connectors(r, a);
        save(r, name);
        layers(r, name);
    }

    /** Filtration / washing: filter housings with grated sight windows under a feed manifold. */
    static void filters(R r, int a, boolean see){
        int s = r.w, n = s <= 64 ? 2 : 3, gap = s <= 64 ? 6 : 7, m = s <= 64 ? 9 : 11;
        int w = (s - 2 * m - gap * (n - 1)) / n, y0 = s <= 64 ? 17 : 21, h = s - y0 - (s <= 64 ? 13 : 16);
        deck(r, 4);
        pipeH(r, 6, 7, s - 12, 5, 8);
        for(int i = 0; i < n; i++){
            int x = m + i * (w + gap);
            pipeV(r, x + w / 2 - 2, 11, y0 - 10, 4, 7);
            r.fill(r.oct(x, y0, w, h, 2), 9, metal, 2);
            window(r, r.rect(x + 3, y0 + 4, w - 6, h - 11), see, a);
            for(int yy = y0 + 7; yy < y0 + h - 8; yy += 4) r.fill(r.rect(x + 3, yy, w - 6, 1), 8.5f, steel, 0);
            r.fill(r.rect(x + 2, y0 + h - 5, w - 4, 3), 10, acc(a), 0);
        }
        r.fill(r.rect(m, s - 10, s - 2 * m, 4), 2, deep, 0);
        r.fill(r.rect(s / 2 - 3, s - 9, 6, 2), 2.5f, acc(a), 0);
        bolt(r, 5, s - 7);
        bolt(r, s - 7, s - 7);
    }

    /** Electrochemistry: open cells with electrode plates bridging them and copper bus bars. */
    static void cells(R r, int a, boolean see){
        int s = r.w, n = s <= 64 ? 2 : 3, gap = 4, m = 7;
        int w = (s - 2 * m - gap * (n - 1)) / n, y0 = 15, h = s - y0 - 13;
        deck(r, 4);
        r.fill(r.rect(6, 6, s - 12, 4), 11, copperM, 1);
        for(int i = 0; i < n; i++){
            int x = m + i * (w + gap);
            r.fill(r.rect(x, y0, w, h), 9, metal, 1);
            window(r, r.rect(x + 2, y0 + 2, w - 4, h - 4), see, a);
            r.fill(r.rect(x + w / 2, y0 + 2, 1, h - 4), 7, acc(a), 0);
            for(int ex : new int[]{x + 3, x + w - 5}){
                r.fill(r.rect(ex, y0 - 1, 2, h + 1), 11, steel, 0);
                r.fill(r.rect(ex, 9, 2, y0 - 9), 11.5f, copperM, 0);
            }
        }
        pipeH(r, 6, s - 10, s - 12, 5, 8);
    }

    /** Gas separation: three column heads of different sizes on a shared header, with a sight glass. */
    static void columns(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        float cy = s * 0.42f;
        float[] rad = s <= 64 ? new float[]{7, 9, 7} : new float[]{10, 13, 10};
        float[] cx = {s * 0.21f, s * 0.5f, s * 0.79f};
        pipeH(r, 6, (int)cy - 2, s - 12, 5, 7);
        for(int i = 0; i < 3; i++){
            r.fill(r.circle(cx[i], cy, rad[i] + 2), 8, steel, 1);
            r.dome(r.circle(cx[i], cy, rad[i]), 8, rad[i] * 0.8f, metal);
            r.raise(r.ring(cx[i], cy, rad[i] * 0.55f, rad[i] * 0.55f + 1.2f), 0.8f);
            r.dome(r.circle(cx[i], cy, 2.5f), 8 + rad[i] * 0.8f, 1.5f, acc(a));
        }
        window(r, r.rect(10, s - 15, s - 20, 5), see, a);
        for(int x = 14; x < s - 12; x += 6) r.fill(r.rect(x, s - 15, 1, 2), 6, steel, 0);
        cornerBolts(r, 5);
    }

    /** Towers: a stacked distillation column, or a cooling tower basin with its fan. */
    static void tower(R r, int a, String name, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        if(name.equals("cooling-tower")){
            float r0 = s * 0.42f;
            r.dome(r.ring(c, c, r0 - 6, r0), 6, 5, metal);
            window(r, r.circle(c, c, r0 - 6), see, a);
            R rot = rotor(s);
            for(int i = 0; i < 3; i++) rot.fill(rot.bar(c, c, 0, r0 - 8, 4, 30 + i * 120), 9, steel, 1.5f);
            rot.dome(rot.circle(c, c, 5.5f), 9, 3, metal);
            rot.fill(rot.circle(c, c, 1.6f), 12.5f, lit(a), 0);
            return;
        }
        float r0 = s * 0.25f;
        pipeV(r, 7, 9, s - 18, 5, 7);
        pipeV(r, s - 12, 9, s - 18, 5, 7);
        pipeH(r, 9, (int)c - 2, s - 18, 4, 7);
        r.fill(r.circle(c, c, r0 + 3), 8, steel, 1);
        r.dome(r.circle(c, c, r0), 8, r0 * 0.8f, metal);
        for(float k : new float[]{0.4f, 0.72f}) r.raise(r.ring(c, c, r0 * k, r0 * k + 1.2f), 1f);
        r.dome(r.circle(c, c, 3f), 8 + r0 * 0.8f, 1.5f, acc(a));
        int ww = s <= 64 ? 10 : 14;
        window(r, r.rect((int)c - ww / 2, s - 12, ww, 5), see, a);
        window(r, r.rect((int)c - ww / 2, 7, ww, 5), see, a);
        if(name.equals("cryogenic-condenser")){
            //frost fins on the column flange
            for(int i = 0; i < 8; i++) r.fill(r.bar(c, c, r0 + 1, 4, 2, i * 45 + 22.5f), 10, acc(a), 0);
        }
    }

    /** Biology: covered growth troughs, or a sealed fermentation vessel with a sight band. */
    static void bio(R r, int a, String name, boolean see){
        int s = r.w;
        deck(r, 4);
        if(name.equals("biomass-cultivator") || name.equals("algae-photobioreactor")){
            int rows = s <= 64 ? 3 : 4, m = 7, gap = 3;
            int rh = (s - 2 * m - gap * (rows - 1)) / rows, tw = s - 2 * m - 6;
            pipeV(r, s - 11, 6, s - 12, 5, 9);
            for(int i = 0; i < rows; i++){
                int y = m + i * (rh + gap);
                r.fill(r.rect(m, y, tw, rh), 8, metal, 1);
                window(r, r.rect(m + 2, y + 2, tw - 4, rh - 4), see, a);
                for(int x = m + 6; x < m + tw - 4; x += 7) r.fill(r.rect(x, y + 1, 1, rh - 2), 9, glass(0xd8efff), 0);
                r.fill(r.rect(m + tw - 1, y + rh / 2 - 1, 4, 3), 9, steel, 0);
                r.paint(r.rect(m + 1, y + 1, 2, 1), 0xff000000 | tint(a, 0.4f));
            }
            return;
        }
        float c = s / 2f, r0 = s * 0.32f;
        pipeH(r, 5, (int)c - 3, s - 10, 6, 7);
        r.fill(r.circle(c, c, r0 + 2), 7, steel, 1);
        r.dome(r.circle(c, c, r0), 7, r0 * 0.7f, metal);
        window(r, r.and(r.rect(0, (int)c - 3, s, 6), r.circle(c, c, r0 - 4)), see, a);
        r.dome(r.circle(c + r0 * 0.5f, c - r0 * 0.5f, 3.5f), 7 + r0 * 0.5f, 2, acc(a));
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c, r0 - 0.5f, 2.5f, 2, i * 45), 9.5f, steel, 0);
    }

    /** Precision equipment: gantry over a work chuck, a feed reservoir with a sight glass. */
    static void precision(R r, int a, boolean see){
        int s = r.w;
        r.fill(r.rect(5, 5, s - 10, s - 10), 3, metal, 1);
        r.cylV(8, 6, 4, s - 12, 6, 2, steel);
        r.cylV(s - 12, 6, 4, s - 12, 6, 2, steel);
        int cw = s <= 64 ? 18 : 26, x0 = s / 2 - cw / 2, y0 = s / 2 - cw / 2 + 3;
        r.fill(r.rect(x0, y0, cw, cw), 6, metal, 1);
        r.fill(r.rect(x0 + 4, y0 + 4, cw - 8, cw - 8), 7, acc(a), 1);
        r.fill(r.rect(x0 + 6, y0 + cw / 2 - 1, cw - 12, 1), 8, brass, 0);
        r.fill(r.rect(x0 + cw / 2 - 1, y0 + 6, 1, cw - 12), 8, brass, 0);
        int by = (int)(s * 0.27f);
        r.fill(r.rect(6, by - 3, s - 12, 6), 11, steel, 1);
        r.fill(r.rect(s / 2 - 5, by - 5, 10, 10), 13, metal, 1);
        r.fill(r.rect(s / 2 - 2, by - 2, 4, 4), 14, acc(a), 0);
        int ww = s - 32;
        window(r, r.rect(16, s - 12, ww, 5), see, a);
    }

    /** Dosing / mixing: two corner feed tanks piped into a central basin with a turning agitator (rotator). */
    static void mixer(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f, tr = s <= 64 ? 7 : 10, br = s <= 64 ? 15 : 22;
        deck(r, 4);
        float[][] tanks = {{tr + 6, tr + 6}, {s - tr - 6, tr + 6}};
        for(float[] t : tanks){
            pipeV(r, (int)t[0] - 2, (int)(t[1] + tr), (int)(c - t[1] - tr), 4, 6);
            r.fill(r.circle(t[0], t[1], tr + 2), 7, steel, 1);
            r.dome(r.circle(t[0], t[1], tr), 7, tr * 0.8f, metal);
            r.fill(r.circle(t[0], t[1], 2), 7 + tr * 0.8f + 1, lit(a), 0);
        }
        r.fill(r.circle(c, c, br + 3), 9, metal, 2);
        window(r, r.circle(c, c, br), see, a);
        R rot = rotor(s);
        rot.fill(rot.bar(c, c, -br + 1, br * 2 - 2, 3, 0), 11, steel, 1);
        rot.fill(rot.bar(c, c, br - 7, 5, 6, 0), 11, acc(a), 0);
        rot.fill(rot.bar(c, c, -br + 2, 5, 6, 0), 11, acc(a), 0);
        rot.fill(rot.circle(c, c, 4), 12, metal, 1);
        rot.fill(rot.circle(c, c, 1.5f), 14, lit(a), 0);
    }

    /** Boilers / digesters: horizontal drum over a tube bank and fire grate. */
    static void boiler(R r, int a, boolean see){
        int s = r.w, dx = 7, dh = s <= 64 ? 18 : 26, dy = 8;
        deck(r, 4);
        r.cylH(dx, dy, s - 2 * dx, dh, 6, dh * 0.45f, metal);
        for(int x : new int[]{dx, s / 2 - 1, s - dx - 3}) r.cylH(x, dy - 1, 3, dh + 2, 7, dh * 0.45f, steel);
        window(r, r.rect(s / 2 - 12, dy + dh / 2 - 2, 9, 4), see, a);
        window(r, r.rect(s / 2 + 4, dy + dh / 2 - 2, 9, 4), see, a);
        int ty = dy + dh + 2, th = s - ty - 15;
        for(int x = 10; x < s - 12; x += 6) r.cylV(x, ty, 4, th, 5, 1.5f, steel);
        r.fill(r.rect(9, s - 12, s - 18, 6), 2, deep, 0);
        for(int x = 11; x < s - 11; x += 4) r.fill(r.rect(x, s - 11, 2, 4), 2.5f, acc(mixi(a, 0x572c22, 0.3f)), 0);
    }

    /** Pressure vessels: clamped domes with sight slots; variants keep these from all looking alike. */
    static void pressure(R r, int a, String name, boolean see, boolean round){
        int s = r.w;
        deck(r, 4);
        if(name.equals("acid-leaching-vat")){
            r.fill(r.rect(6, 6, s - 12, s - 12), 9, metal, 2);
            window(r, r.rect(10, 10, s - 20, s - 20), see, a);
            r.fill(r.rect(6, s / 2 - 3, s - 12, 6), 12, steel, 1);
            for(int x : new int[]{s / 4, s * 3 / 4}) r.fill(r.rect(x - 1, 11, 3, s - 22), 10, steel, 0);
            r.dome(r.circle(s / 2f, s / 2f, 6), 12, 3, metal);
            r.fill(r.circle(s / 2f, s / 2f, 2), 15.5f, acc(a), 0);
            return;
        }
        if(s >= 128){
            float q = s * 0.27f;
            pipeH(r, 6, s / 2 - 3, s - 12, 6, 7);
            pipeV(r, s / 2 - 3, 6, s - 12, 6, 7);
            for(int i = 0; i < 4; i++) vessel(r, i % 2 == 0 ? q : s - q, i < 2 ? q : s - q, 16, a, i % 2 == 0, see);
            r.fill(r.oct(s / 2f - 12, s / 2f - 12, 24, 24, 7), 10, metal, 2);
            r.fill(r.oct(s / 2f - 6, s / 2f - 6, 12, 12, 3), 11, acc(a), 1);
            return;
        }
        int variant = Math.floorMod(name.hashCode(), 3);
        float c = s / 2f;
        if(variant == 1){
            float vr = s <= 64 ? 9 : 13;
            pipeH(r, 5, (int)c - 2, s - 10, 5, 7);
            vessel(r, s * 0.29f, c, vr, a, round, see);
            vessel(r, s * 0.71f, c, vr, a, !round, see);
        }else{
            float vr = s <= 64 ? 13 : 20;
            pipeH(r, 5, (int)c - 3, s - 10, 6, 7);
            if(variant == 2) pipeV(r, (int)c - 3, 5, s - 10, 6, 7);
            vessel(r, c, c, vr, a, variant == 0 ? round : !round, see);
        }
        if(name.equals("steam-cracker") || name.equals("methanol-synthesizer")){
            for(int y : new int[]{7, s - 10}) r.cylH(9, y, s - 18, 3, 6, 1.5f, acc(mixi(a, 0x6a3a2a, 0.35f)));
        }
    }

    static void vessel(R r, float cx, float cy, float vr, int a, boolean round, boolean see){
        boolean[] ringM = round ? r.circle(cx, cy, vr + 3) : r.oct(cx - vr - 3, cy - vr - 3, (vr + 3) * 2, (vr + 3) * 2, vr * 0.6f);
        boolean[] body = round ? r.circle(cx, cy, vr) : r.oct(cx - vr, cy - vr, vr * 2, vr * 2, vr * 0.55f);
        r.fill(ringM, 8, steel, 1);
        r.dome(body, 8, vr * 0.7f, metal);
        for(int i = 0; i < 8; i++){
            double ang = Math.PI * 2 * i / 8 + Math.PI / 8;
            int bx = (int)Math.floor(cx + Math.cos(ang) * (vr + 1.5f) - 1), by = (int)Math.floor(cy - Math.sin(ang) * (vr + 1.5f) - 1);
            r.fill(r.rect(bx, by, 2, 2), 10.5f, metal, 0);
        }
        window(r, r.and(r.rect((int)(cx - vr * 0.55f), (int)cy - 2, (int)(vr * 1.1f), 4), body), see, a);
        r.dome(r.circle(cx + vr * 0.35f, cy - vr * 0.5f, 2.5f), 8 + vr * 0.6f, 1.5f, acc(a));
    }

    /** General processing chamber: a framed window crossed by piston rods. */
    static void chamber(R r, int a, boolean see){
        int s = r.w, w = s <= 64 ? 30 : 44, h = s - 18, x = (s - w) / 2, y = 9;
        deck(r, 4);
        pipeH(r, 4, s / 2 - 2, x - 3, 5, 7);
        pipeH(r, x + w - 1, s / 2 - 2, s - x - w - 3, 5, 7);
        r.fill(r.rect(x, y, w, h), 9, metal, 2);
        window(r, r.rect(x + 4, y + 5, w - 8, h - 10), see, a);
        for(int i = 0; i < 3; i++){
            int yy = y + 8 + i * (h - 19) / 2;
            r.fill(r.rect(x + 2, yy, w - 4, 3), 11, steel, 1);
            r.fill(r.rect(x + w / 2 - 2, yy - 1, 4, 5), 12, acc(a), 1);
        }
        cornerBolts(r, 5);
    }

    // =====================================================================================================
    // Per-building designs: each machine is drawn from what it physically does
    // =====================================================================================================

    /** Scrubber: packed spray tower, inlet duct and exhaust stack. */
    static void scrubber(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.oct(8, 8, 30, 46, 8), 9, metal, 2);
        window(r, r.rect(13, 15, 20, 30), see, a);
        for(int y = 18; y < 44; y += 5) r.fill(r.rect(13, y, 20, 1), 8.5f, steel, 0);
        r.cylH(10, 10, 26, 3, 11, 1.5f, steel);
        for(int x = 13; x < 34; x += 5) lamp(r, x, 9, a);
        stack(r, s - 15, 16, 8);
        r.fill(r.rect(40, 34, 18, 20), 8, steel, 2);
        for(int y = 37; y < 52; y += 3) r.fill(r.rect(43, y, 12, 1), 3, deep, 0);
    }

    /** Clarifier: round settling basin; the skimmer bridge sweeps round (rotator). */
    static void clarifier(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        r.fill(r.circle(c, c, 28), 8, metal, 2);
        window(r, r.circle(c, c, 24), see, a);
        r.fill(r.ring(c, c, 19, 20.5f), 7.5f, steel, 0);
        r.fill(r.circle(c, c, 5), 10, metal, 1);
        cornerBolts(r, 5);
        R rot = rotor(s);
        rot.fill(rot.bar(c, c, -23, 46, 4, 0), 11, steel, 1);
        rot.fill(rot.bar(c, c, 17, 5, 7, 0), 12, lit(a), 0);
        rot.fill(rot.circle(c, c, 4), 12, metal, 1);
    }

    /** Drum washer: ribbed rotary drum over a water trough with a spray bar. */
    static void drumWasher(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        drum(r, 7, 8, s - 14, 18, 7);
        r.fill(r.rect(6, 30, s - 12, 26), 8, metal, 2);
        window(r, r.rect(9, 33, s - 18, 20), see, a);
        r.cylH(9, 40, s - 18, 3, 10, 1.5f, steel);
        for(int x = 12; x < s - 12; x += 6) lamp(r, x, 39, a);
    }

    /** Centrifuge: round bowl with a spinning rotor (rotator) and three discharge chutes. */
    static void centrifuge(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        for(int x : new int[]{10, s / 2 - 3, s - 16}) r.fill(r.rect(x, s - 11, 6, 8), 7, steel, 1);
        r.fill(r.circle(c, c, 25), 9, metal, 2);
        r.fill(r.circle(c, c, 20), 3, deep, 0);
        window(r, r.ring(c, c, 15, 19), see, a);
        R rot = rotor(s);
        for(int i = 0; i < 3; i++) rot.fill(rot.bar(c, c, 3, 15, 5, i * 120), 8, steel, 1);
        rot.fill(rot.circle(c, c, 6), 10, metal, 2);
        rot.fill(rot.circle(c, c, 2), 12, lit(a), 0);
    }

    /** Membrane cells: rows of flat cells under a chlorine collector, copper bus bars down both sides. */
    static void membraneCells(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.oct(28, 6, s - 56, 14, 5), 10, metal, 2);
        r.fill(r.rect(34, 11, s - 68, 4), 11, lit(a), 0);
        for(int i = 0; i < 4; i++){
            int y = 24 + i * 16;
            r.fill(r.rect(12, y, s - 24, 12), 8, metal, 1);
            window(r, r.rect(14, y + 2, s - 28, 8), see, a);
            for(int x = 22; x < s - 16; x += 12) r.fill(r.rect(x, y + 1, 2, 10), 9, acc(a), 0);
        }
        r.fill(r.rect(6, 22, 4, s - 30), 11, copperM, 1);
        r.fill(r.rect(s - 10, 22, 4, s - 30), 11, copperM, 1);
    }

    /** Tank house: one long bath with a row of hanging cathode plates and a travelling crane. */
    static void tankHouse(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(8, 14, s - 16, s - 26), 8, metal, 2);
        window(r, r.rect(11, 17, s - 22, s - 32), see, a);
        for(int x = 15; x < s - 14; x += 6) r.fill(r.rect(x, 15, 2, s - 28), 10, steel, 0);
        r.fill(r.rect(8, 10, s - 16, 4), 11.5f, copperM, 1);
        r.fill(r.rect(8, s - 13, s - 16, 4), 11.5f, copperM, 1);
        r.cylH(4, s / 2 - 2, s - 8, 4, 13, 2, steel);
        r.fill(r.rect(40, s / 2 - 7, 16, 14), 15, metal, 2);
        lamp(r, 46, s / 2 - 2, a);
    }

    /** Air separation: insulated cold box with frosted panels, two columns and a sight glass. */
    static void coldBox(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(8, 8, 42, s - 16), 10, metal, 3);
        r.fill(r.rect(12, 12, 34, s - 24), 11, dull(tint(a, 0.55f)), 1);
        for(int y = 26; y < s - 14; y += 18) r.raise(r.rect(12, y, 34, 2), -1.5f);
        lamp(r, 40, 15, a);
        for(int cy : new int[]{28, s - 28}){
            r.fill(r.circle(72, cy, 13), 8, steel, 1);
            r.dome(r.circle(72, cy, 11), 8, 6, metal);
            r.fill(r.circle(72, cy, 3), 15, lit(a), 0);
            pipeH(r, 50, cy - 2, 10, 4, 7);
        }
        window(r, r.rect(60, s / 2 - 3, 24, 6), see, a);
    }

    /** Condenser: serpentine frosted coil over the coolant bath, with a compressor box. */
    static void condenser(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(6, 6, 36, s - 12), 8, metal, 2);
        window(r, r.rect(9, 9, 30, s - 18), see, a);
        Mat coil = acc(tint(a, 0.15f));
        for(int i = 0; i < 6; i++){
            int y = 11 + i * 7;
            r.cylH(11, y, 26, 4, 9, 2, coil);
            if(i < 5) r.cylV(i % 2 == 0 ? 33 : 11, y + 2, 4, 7, 9, 2, coil);
        }
        r.fill(r.rect(45, 8, 13, 30), 9, metal, 2);
        for(int y = 12; y < 34; y += 3) r.fill(r.rect(47, y, 9, 1), 3, deep, 0);
        lamp(r, 49, 44, a);
        pipeV(r, 49, 40, 16, 4, 7);
    }

    /** Photobioreactor: rows of glass growth tubes between two manifolds. */
    static void tubeReactor(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        window(r, r.rect(10, 15, s - 20, s - 30), see, a);
        Mat tube = new Mat(tones(mixi(a, 0xffffff, 0.15f)), true, true);
        for(int x = 12; x < s - 14; x += 7) r.cylV(x, 13, 5, s - 26, 8, 3, tube);
        pipeH(r, 6, 8, s - 12, 6, 10);
        pipeH(r, 6, s - 14, s - 12, 6, 10);
        lamp(r, s - 14, 9, a);
    }

    /** Digester: big sealed egg with a gas-holder dome and a sludge pit. */
    static void digester(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(36, 50, 30), 8, steel, 1);
        r.dome(r.circle(36, 50, 27), 8, 10, metal);
        r.fill(r.rect(30, 20, 12, 4), 19, steel, 0);
        r.fill(r.circle(74, 22, 14), 9, steel, 1);
        r.dome(r.circle(74, 22, 12), 9, 6, lit(a));
        r.fill(r.rect(60, 62, 30, 24), 8, metal, 2);
        window(r, r.rect(63, 65, 24, 18), see, a);
        pipeH(r, 62, 44, 28, 4, 7);
    }

    /** Zone refiner: a long crucible track with a travelling induction heater ring. */
    static void zoneRefiner(R r, int a, boolean see){
        int s = r.w;
        r.fill(r.rect(5, 5, s - 10, s - 10), 3, metal, 1);
        r.cylH(6, 8, s - 12, 4, 6, 2, steel);
        r.cylH(6, s - 12, s - 12, 4, 6, 2, steel);
        r.fill(r.rect(6, 20, s - 12, 24), 8, metal, 2);
        window(r, r.rect(9, 24, s - 18, 16), see, a);
        r.fill(r.rect(24, 15, 12, 34), 11, copperM, 2);
        r.fill(r.rect(28, 18, 4, 28), 12, lit(0xff8a3d), 0);
        lamp(r, s - 12, 50, a);
    }

    /** Platinum refinery: dissolver vessel feeding a plate-and-frame filter press. */
    static void filterPress(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        vessel(r, 18, 22, 10, a, true, see);
        r.fill(r.rect(36, 7, 22, 46), 7, metal, 2);
        for(int x = 39; x < 56; x += 3) r.fill(r.rect(x, 10, 2, 40), 9.5f, steel, 0);
        r.fill(r.rect(36, 7, 22, 3), 11, steel, 0);
        r.fill(r.rect(36, 50, 22, 3), 11, steel, 0);
        r.fill(r.rect(6, 42, 28, 14), 8, metal, 2);
        window(r, r.rect(9, 45, 22, 8), see, a);
        lamp(r, 44, 55, a);
    }

    /** Contact-process converter: stacked catalyst beds beside an absorber, acid trough below. */
    static void converter(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(22, 24, 17), 8, steel, 1);
        r.fill(r.circle(22, 24, 14), 9, metal, 2);
        for(float k : new float[]{5, 9}) r.raise(r.ring(22, 24, k, k + 1.5f), 1.5f);
        r.fill(r.circle(22, 24, 3), 12, lit(a), 0);
        tank(r, 42, 8, 15, 30, a);
        pipeH(r, 36, 20, 8, 4, 7);
        r.fill(r.rect(6, 44, s - 14, 13), 8, metal, 2);
        window(r, r.rect(9, 47, s - 20, 7), see, a);
    }

    /** Neutralizer: one broad dosing basin with two reagent pumps and a paddle agitator (rotator). */
    static void neutralizer(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        r.fill(r.circle(c, c, 25), 9, metal, 2);
        window(r, r.circle(c, c, 21), see, a);
        for(int x : new int[]{9, s - 9}){
            r.fill(r.circle(x, s - 9, 6), 10, metal, 2);
            r.fill(r.circle(x, s - 9, 2), 12, lit(a), 0);
        }
        R rot = rotor(s);
        rot.fill(rot.bar(c, c, -18, 36, 3, 0), 11, steel, 1);
        rot.fill(rot.bar(c, c, -18, 36, 3, 90), 11, steel, 1);
        rot.fill(rot.circle(c, c, 4.5f), 12, metal, 1);
    }

    /** Nitric acid: platinum gauze burner (glowing mesh) and an absorption column. */
    static void gauzeBurner(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(22, 22, 17), 9, metal, 2);
        r.fill(r.circle(22, 22, 12), 4, deep, 0);
        Mat mesh = lit(0xffb46b);
        for(int y = 13; y < 32; y += 3) r.fill(r.and(r.rect(10, y, 24, 1), r.circle(22, 22, 12)), 5, mesh, 0);
        tank(r, 40, 10, 17, 40, a);
        pipeH(r, 34, 18, 8, 4, 7);
        r.fill(r.rect(6, 43, 30, 14), 8, metal, 2);
        window(r, r.rect(9, 46, 24, 8), see, a);
    }

    /** Prilling tower: wide round tower with a spray ring, and a product bin. */
    static void prillTower(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(26, 26, 21), 8, steel, 1);
        r.dome(r.circle(26, 26, 18), 8, 6, metal);
        r.fill(r.ring(26, 26, 7, 9.5f), 15, lit(a), 0);
        r.fill(r.circle(26, 26, 3), 16, steel, 0);
        r.fill(r.rect(40, 42, 18, 15), 8, metal, 2);
        window(r, r.rect(43, 45, 12, 9), see, a);
        r.cylV(48, 8, 4, 30, 7, 2, steel);
    }

    /** Catalyst plant: impregnation drum over a heated drying kiln. */
    static void catalystDrum(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        drum(r, 6, 9, s - 12, 20, 8);
        r.fill(r.oct(s / 2f - 5, 13, 10, 12, 3), 12, lit(a), 1);
        r.fill(r.rect(8, 36, s - 16, 20), 8, metal, 2);
        window(r, r.rect(11, 39, s - 22, 9), see, a);
        for(int x = 12; x < s - 12; x += 5) r.fill(r.rect(x, 50, 3, 3), 9, lit(0xff8a3d), 0);
    }

    /** Granulator: a rotating pan (rotator) fed by a hopper. */
    static void granulator(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        hopper(r, s - 20, 4, 16, 14);
        r.fill(r.circle(c, c + 2, 24), 9, metal, 2);
        window(r, r.circle(c, c + 2, 20), see, a);
        R rot = rotor(s);
        for(int i = 0; i < 6; i++) rot.fill(rot.bar(c, c, 13, 7, 3, i * 60), 10, steel, 1);
        rot.fill(rot.circle(c, c, 5), 11, metal, 2);
        rot.fill(rot.circle(c, c, 2), 13, lit(a), 0);
    }

    /** Alumina: a row of three digester autoclaves above a thickener. */
    static void autoclaves(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        pipeH(r, 6, 24, s - 12, 5, 7);
        for(int x : new int[]{19, 48, 77}) vessel(r, x, 26, 11, a, true, see);
        r.fill(r.rect(9, 52, s - 18, 34), 8, metal, 2);
        window(r, r.rect(12, 55, s - 24, 28), see, a);
        r.fill(r.rect(12, 68, s - 24, 3), 10, steel, 0);
        lamp(r, s / 2 - 1, 68, a);
    }

    /** Lithium: two evaporation ponds and a brine pump house. */
    static void ponds(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(6, 6, 26, s - 12), 7, metal, 2);
        window(r, r.rect(9, 9, 20, s - 18), see, a);
        r.fill(r.rect(35, 6, 23, 30), 7, metal, 2);
        window(r, r.rect(38, 9, 17, 24), see, a);
        r.fill(r.rect(35, 40, 23, 18), 10, metal, 2);
        r.fill(r.circle(46, 49, 5), 12, steel, 1);
        lamp(r, 52, 43, a);
    }

    /** HCl synthesis: a horizontal burner with a flame port feeding a packed absorber. */
    static void hclBurner(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.cylH(6, 8, s - 12, 18, 7, 7, metal);
        r.fill(r.oct(10, 12, 12, 10, 3), 14, lit(0xff8a3d), 1);
        r.fill(r.rect(6, 32, s - 12, 24), 8, metal, 2);
        window(r, r.rect(9, 35, s - 18, 18), see, a);
        for(int x = 12; x < s - 10; x += 5) r.fill(r.rect(x, 35, 1, 18), 8.5f, steel, 0);
        pipeV(r, s - 16, 24, 10, 4, 9);
    }

    /** Leaching vat: open bath, a fixed bridge and a turning paddle (rotator). */
    static void vat(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        r.fill(r.rect(6, 6, s - 12, s - 12), 9, metal, 2);
        window(r, r.rect(10, 10, s - 20, s - 20), see, a);
        r.fill(r.rect(6, s / 2 - 3, s - 12, 6), 12, steel, 1);
        R rot = rotor(s);
        rot.fill(rot.bar(c, c, -30, 60, 4, 90), 13, steel, 1);
        rot.fill(rot.bar(c, c, 22, 6, 9, 90), 13, acc(a), 1);
        rot.fill(rot.bar(c, c, -28, 6, 9, 90), 13, acc(a), 1);
        rot.fill(rot.circle(c, c, 6), 14, metal, 2);
        rot.fill(rot.circle(c, c, 2), 16, lit(a), 0);
    }

    /** Ammonia-style forged vessel: very thick flange with sixteen bolts and a small sight port. */
    static void forged(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        pipeH(r, 4, (int)c - 3, s - 8, 6, 7);
        r.fill(r.circle(c, c, 36), 9, steel, 3);
        r.dome(r.circle(c, c, 28), 10, 8, metal);
        for(int i = 0; i < 16; i++){
            double ang = Math.PI * 2 * i / 16;
            int bx = (int)Math.floor(c + Math.cos(ang) * 32 - 1), by = (int)Math.floor(c - Math.sin(ang) * 32 - 1);
            r.fill(r.rect(bx, by, 3, 3), 12, metal, 0);
        }
        window(r, r.rect((int)c - 8, (int)c + 8, 16, 5), see, a);
        r.fill(r.circle(c, c - 6, 4), 19, lit(a), 0);
    }

    /** Loop reactor: a closed racetrack pipe with circulation pump and settling legs. */
    static void loopReactor(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        boolean[] outer = r.oct(10, 10, s - 20, s - 20, 22), inner = r.oct(20, 20, s - 40, s - 40, 16);
        r.dome(r.minus(outer, inner), 6, 6, metal);
        window(r, r.oct(25, 25, s - 50, s - 50, 12), see, a);
        r.fill(r.circle(16, s - 16, 9), 12, metal, 2);
        r.fill(r.circle(16, s - 16, 3), 14, lit(a), 0);
        for(int x : new int[]{40, 52}) r.cylV(x, s - 14, 5, 10, 8, 2, steel);
    }

    /** Steam cracker: firebox of glowing U-tubes, a convection stack and a quench drum. */
    static void cracker(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(7, 7, 56, s - 14), 9, metal, 3);
        window(r, r.rect(12, 12, 46, s - 24), see, a);
        Mat tube = lit(0xff8a3d);
        for(int x = 15; x < 56; x += 8){
            r.cylV(x, 14, 3, s - 30, 10, 1.5f, tube);
            r.cylV(x + 4, 14, 3, s - 30, 10, 1.5f, tube);
            r.cylH(x, s - 18, 7, 3, 10, 1.5f, tube);
        }
        stack(r, 79, 20, 10);
        r.cylV(72, 44, 14, 40, 6, 6, metal);
        lamp(r, 78, 50, a);
    }

    /** Tubular reactor: shell full of catalyst tubes, beside a separator drum. */
    static void tubular(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(40, 48, 32), 9, steel, 2);
        r.fill(r.circle(40, 48, 28), 10, metal, 1);
        for(int y = 26; y <= 70; y += 7) for(int x = 18; x <= 62; x += 7){
            if(Math.hypot(x - 40, y - 48) < 23) r.fill(r.circle(x + 0.5f, y + 0.5f, 2.2f), 7, deep, 0);
        }
        r.fill(r.circle(40.5f, 48.5f, 2.2f), 11, lit(a), 0);
        r.cylV(78, 10, 12, s - 20, 6, 6, metal);
        window(r, r.rect(80, 40, 8, 16), see, a);
    }

    /** Fluidised bed: reactor vessel, riser to a cyclone, catalyst return. */
    static void fluidBed(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.circle(34, 52, 26), 8, steel, 1);
        r.dome(r.circle(34, 52, 23), 8, 8, metal);
        window(r, r.and(r.rect(18, 49, 32, 6), r.circle(34, 52, 20)), see, a);
        r.fill(r.circle(74, 22, 14), 9, metal, 2);
        r.funnel(r.circle(74, 22, 11), 9, 3, steel);
        r.fill(r.circle(74, 22, 3), 2, deep, 0);
        r.cylV(72, 36, 5, 40, 7, 2, steel);
        r.cylH(48, 20, 14, 5, 7, 2, steel);
        lamp(r, 34, 36, a);
    }

    /** Reprocessing hot cell: thick shielding, a viewing window and two master-slave manipulators. */
    static void hotCell(R r, int a, boolean see){
        int s = r.w;
        r.fill(r.rect(4, 4, s - 8, s - 8), 11, dull(0x8a8d96), 4);
        r.fill(r.rect(26, 38, 44, 26), 9, steel, 2);
        window(r, r.rect(29, 41, 38, 20), see, a);
        for(int x : new int[]{36, 56}){
            r.fill(r.rect(x, 12, 5, 32), 13, steel, 1);
            r.fill(r.rect(x - 2, 10, 9, 6), 14, metal, 1);
        }
        for(int x = 10; x < s - 10; x += 8) r.fill(r.rect(x, s - 14, 4, 4), 12, x % 16 == 10 ? lit(0xffd84a) : steel, 0);
    }

    /** Kettle: jacketed reaction kettle with an agitator (rotator) and a dosing tank. */
    static void kettle(R r, int a, boolean see){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        tank(r, s - 17, 4, 13, 18, a);
        r.fill(r.circle(c, c, 24), 8, steel, 1);
        r.fill(r.ring(c, c, 17, 22), 10, metal, 2);
        window(r, r.circle(c, c, 17), see, a);
        R rot = rotor(s);
        rot.fill(rot.bar(c, c, -15, 30, 3, 0), 11, steel, 1);
        rot.fill(rot.bar(c, c, 8, 6, 7, 0), 11, acc(a), 0);
        rot.fill(rot.bar(c, c, -14, 6, 7, 0), 11, acc(a), 0);
        rot.fill(rot.circle(c, c, 4), 12, metal, 1);
    }

    /** Rubber line: coagulation trough above a pair of sheeting rollers. */
    static void rubberLine(R r, int a, boolean see){
        int s = r.w;
        deck(r, 4);
        r.fill(r.rect(6, 8, s - 12, 34), 8, metal, 2);
        window(r, r.rect(9, 11, s - 18, 28), see, a);
        for(int x = 20; x < s - 10; x += 14) r.fill(r.rect(x, 9, 2, 32), 9.5f, steel, 0);
        for(int y : new int[]{52, 66}) r.cylH(8, y, s - 16, 12, 7, 5, steel);
        r.fill(r.rect(10, 80, s - 20, 6), 6, dull(0x3a3a40), 1);
        lamp(r, s - 14, 46, a);
    }

    // ---- furnaces ----

    static void furnaceDesign(R r, String name, int g){
        int s = r.w;
        Mat molten = lit(g), coal = dull(0x3a3c44);
        switch(name){
            case "sintering-furnace" -> {
                deck(r, 3);
                r.fill(r.rect(6, 16, s - 12, 32), 10, metal, 3);
                r.fill(r.rect(10, 26, s - 20, 12), 3, molten, 1);
                for(int x = 12; x < s - 10; x += 4) r.fill(r.rect(x, 26, 2, 12), 4, steel, 0);
                r.cylV(2, 18, 5, 28, 7, 2, steel);
                r.cylV(s - 7, 18, 5, 28, 7, 2, steel);
                stack(r, 20, 9, 5);
                stack(r, 44, 9, 5);
            }
            case "aluminium-smelter" -> {
                deck(r, 4);
                r.fill(r.rect(4, 10, 4, s - 20), 8, copperM, 1);
                r.fill(r.rect(s - 8, 10, 4, s - 20), 8, copperM, 1);
                for(int i = 0; i < 2; i++){
                    int y = 10 + i * 40;
                    r.fill(r.rect(10, y, s - 20, 34), 9, metal, 2);
                    r.fill(r.rect(14, y + 4, s - 28, 26), 2, deep, 0);
                    r.fill(r.rect(16, y + 16, s - 32, 12), 3, molten, 1);
                    for(int x = 18; x < s - 22; x += 12) r.fill(r.rect(x, y + 5, 8, 13), 6, coal, 1);
                    r.fill(r.rect(12, y + 9, s - 24, 3), 11, copperM, 1);
                }
            }
            case "glass-kiln", "soda-glass-furnace" -> {
                if(name.equals("glass-kiln")){
                    r.fill(r.rect(8, 10, s - 16, s - 16), 10, metal, 3);
                    r.fill(r.rect(13, 15, s - 26, s - 26), 2, deep, 0);
                    r.fill(r.rect(15, 26, s - 30, s - 39), 3, molten, 1);
                    for(int y : new int[]{20, 32, 44}){
                        r.fill(r.rect(2, y, 7, 4), 11, steel, 1);
                        r.fill(r.rect(s - 9, y, 7, 4), 11, steel, 1);
                    }
                    hopper(r, 22, 3, 20, 12);
                }else{
                    float c = s / 2f;
                    r.fill(r.circle(c, c, 27), 10, metal, 3);
                    r.fill(r.circle(c, c, 21), 2, deep, 0);
                    for(int i = 0; i < 4; i++){
                        double ang = Math.PI / 4 + i * Math.PI / 2;
                        r.fill(r.circle(c + (float)Math.cos(ang) * 12, c - (float)Math.sin(ang) * 12, 5), 4, molten, 1);
                    }
                    r.fill(r.circle(c, c, 3.5f), 1.5f, deep, 0);
                }
            }
            case "arc-furnace", "silicon-reduction-furnace" -> {
                float c = s / 2f;
                if(name.equals("arc-furnace")){
                    r.fill(r.circle(c, c, 42), 9, metal, 3);
                    r.fill(r.circle(c, c, 34), 2, deep, 0);
                    r.fill(r.circle(c, c, 24), 3, molten, 1);
                    r.fill(r.rect(s - 12, (int)c - 4, 10, 8), 8, steel, 1);
                    for(int i = 0; i < 3; i++){
                        float ang = 90 + i * 120;
                        float px = c + (float)Math.cos(Math.toRadians(ang)) * 11, py = c - (float)Math.sin(Math.toRadians(ang)) * 11;
                        r.fill(r.bar(c, c, 14, 30, 4, ang), 11, copperM, 1);
                        r.fill(r.circle(px, py, 6.5f), 12, coal, 2);
                        r.fill(r.circle(px, py, 2.5f), 13, steel, 0);
                    }
                }else{
                    r.fill(r.oct(8, 8, s - 16, s - 16, 14), 9, metal, 3);
                    r.fill(r.oct(14, 14, s - 28, s - 28, 10), 2, deep, 0);
                    r.fill(r.oct(22, 34, s - 44, 28, 8), 3, molten, 1);
                    for(int x : new int[]{28, 48, 68}){
                        r.fill(r.rect(x - 2, 8, 4, 36), 11, copperM, 1);
                        r.fill(r.circle(x, 48, 7), 12, coal, 2);
                        r.fill(r.circle(x, 48, 2.5f), 13, steel, 0);
                    }
                }
            }
            case "thermal-waste-processor" -> {
                deck(r, 3);
                hopper(r, 5, 4, 18, 14);
                drum(r, 4, 24, 40, 18, 6);
                r.fill(r.rect(44, 6, 16, s - 12), 10, metal, 2);
                r.fill(r.rect(47, 10, 10, s - 20), 3, molten, 1);
                for(int y = 12; y < s - 12; y += 4) r.fill(r.rect(47, y, 10, 1), 4, steel, 0);
                stack(r, 18, s - 12, 7);
            }
            case "carbon-fiber-furnace" -> {
                deck(r, 4);
                r.fill(r.rect(6, 20, s - 12, 58), 10, metal, 3);
                r.fill(r.rect(10, 28, s - 20, 42), 3, molten, 1);
                for(int y = 30; y < 68; y += 4) r.fill(r.rect(4, y, s - 8, 1), 12, coal, 0);
                r.cylV(3, 24, 5, 50, 13, 2, steel);
                r.cylV(s - 8, 24, 5, 50, 13, 2, steel);
                for(int x : new int[]{24, 48, 72}) stack(r, x, 11, 6);
            }
            case "frame-forge" -> {
                deck(r, 4);
                r.fill(r.oct(8, 34, 34, 40, 6), 10, metal, 3);
                r.fill(r.oct(13, 39, 24, 30, 4), 2, deep, 0);
                for(int x = 17; x < 34; x += 5) r.dome(r.circle(x, 55, 2.6f), 3, 1.8f, molten);
                r.fill(r.rect(50, 46, 36, 18), 10, steel, 2);
                r.fill(r.rect(56, 40, 24, 8), 12, steel, 1);
                r.fill(r.rect(60, 50, 16, 4), 13, molten, 0);
                r.fill(r.rect(48, 10, 40, 8), 13, metal, 2);
                r.fill(r.rect(60, 16, 16, 16), 15, dull(0x5a5c66), 2);
                stack(r, 25, 18, 8);
            }
            case "methane-pyrolysis-reactor" -> {
                deck(r, 4);
                r.fill(r.circle(40, 52, 32), 8, steel, 2);
                r.fill(r.circle(40, 52, 28), 10, metal, 2);
                r.fill(r.circle(40, 52, 19), 4, molten, 1);
                for(int i = 0; i < 6; i++){
                    double ang = i * Math.PI / 3 + 0.3;
                    r.fill(r.circle(40 + (float)Math.cos(ang) * 10, 52 + (float)Math.sin(ang) * 10, 1.6f), 3, coal, 0);
                }
                stack(r, 80, 18, 9);
                r.cylH(60, 16, 12, 5, 8, 2, steel);
                hopper(r, 66, 64, 24, 24);
            }
            case "chloride-titanium-plant" -> {
                deck(r, 4);
                r.fill(r.circle(32, 54, 26), 9, metal, 3);
                r.fill(r.circle(32, 54, 19), 3, deep, 0);
                r.fill(r.circle(32, 54, 13), 4, molten, 1);
                tank(r, 62, 8, 26, 34, g);
                tank(r, 62, 54, 26, 34, g);
                pipeH(r, 50, 26, 14, 4, 7);
                r.cylV(10, 8, 6, 22, 7, 2, steel);
            }
            default -> {
                //crucible furnace (alloy furnace): heavy casing, brick-lined throat, grate and glowing charge
                int f = s <= 64 ? 12 : 17, cw = s - 2 * f;
                r.fill(r.oct(f - 5, f - 5, cw + 10, cw + 10, 6), 10, metal, 3);
                r.fill(r.oct(f, f, cw, cw, 4), 2, deep, 0);
                int bedY = f + cw / 2, bedH = Math.max(4, cw / 4);
                for(int yy = f + 5; yy < bedY - 2; yy += 4) r.raise(r.rect(f + 3, yy, cw - 6, 1), 0.8f);
                r.fill(r.rect(f + 3, bedY, cw - 6, bedH), 3, acc(mixi(g, 0x572c22, 0.55f)), 1);
                for(int x = f + 6; x < f + cw - 6; x += 5) r.dome(r.circle(x, bedY + bedH / 2f, 2.6f), 3, 1.8f, molten);
                for(int x = f + 4; x < f + cw - 4; x += 4) r.fill(r.rect(x, bedY + bedH, 2, cw / 2 - bedH - 2), 3, steel, 0);
                r.fill(r.rect(f + 2, f - 3, cw - 4, 3), 11.5f, steel, 0);
                for(int i = 0; i < 3; i++){
                    int yy = f + 2 + i * (cw - 4) / 3;
                    r.fill(r.rect(3, yy, f - 9, 2), 3, deep, 0);
                    r.fill(r.rect(s - f + 6, yy, f - 9, 2), 3, deep, 0);
                }
            }
        }
        cornerBolts(r, 4);
    }

    // ---- presses and assemblers ----

    static void pressDesign(R r, String name, int a){
        int s = r.w;
        float c = s / 2f;
        switch(name){
            case "gear-press", "heavy-gear-press" -> {
                boolean heavy = name.startsWith("heavy");
                deck(r, 4);
                r.fill(r.circle(c, c, heavy ? 22 : 19), 3, deep, 0);
                if(heavy){
                    for(int x : new int[]{4, s - 12}) r.fill(r.rect(x, 12, 8, s - 24), 11, metal, 2);
                }else{
                    r.fill(r.rect(6, 4, s - 12, 8), 11, metal, 2);
                    r.fill(r.rect((int)c - 4, 9, 8, 6), 12, steel, 1);
                }
                R rot = rotor(s);
                rot.fill(gear(rot, c, c, heavy ? 19 : 16, heavy ? 12 : 10), 6, acc(a), 2);
                rot.fill(rot.ring(c, c, 4, 7), 7.5f, steel, 1);
                rot.clear(rot.circle(c, c, 4));
            }
            case "metal-extruder" -> {
                deck(r, 4);
                r.fill(r.rect(3, 26, 6, 12), 9, steel, 1);
                r.fill(r.circle(18, 32, 12), 10, metal, 2);
                r.fill(r.circle(18, 32, 6), 5, lit(0xff8a3d), 1);
                r.fill(r.rect(30, 22, 8, 20), 11, steel, 2);
                for(int y : new int[]{26, 31, 36}) r.cylH(38, y, s - 42, 3, 6, 1.5f, acc(a));
                for(int x : new int[]{46, 56}) r.cylV(x, 22, 3, 20, 8, 1.5f, steel);
            }
            case "casing-forge" -> {
                deck(r, 4);
                for(int x : new int[]{6, s - 14}) r.fill(r.rect(x, 6, 8, 40), 11, metal, 2);
                r.fill(r.rect(6, 6, s - 12, 8), 12, metal, 2);
                r.fill(r.rect(22, 13, 20, 14), 14, dull(0x5a5c66), 2);
                r.fill(r.rect(18, 32, 28, 14), 9, steel, 2);
                r.fill(r.rect(24, 34, 16, 6), 11, lit(0xff8a3d), 1);
                r.fill(r.rect(8, 50, s - 16, 9), 7, glass(0x7fc8ff), 1);
            }
            case "bearing-press" -> {
                deck(r, 4);
                hopper(r, s - 20, 4, 16, 16);
                r.fill(r.circle(c, c + 2, 21), 9, metal, 2);
                r.fill(r.ring(c, c + 2, 8, 16), 4, deep, 0);
                r.fill(r.ring(c, c + 2, 10.5f, 13.5f), 5.5f, acc(a), 1);
                r.fill(r.circle(c, c + 2, 6), 12, steel, 2);
                lamp(r, 8, 8, a);
            }
            case "precision-assembler" -> {
                deck(r, 4);
                r.fill(r.circle(18, 44, 10), 9, metal, 2);
                r.fill(r.bar(18, 44, 0, 20, 5, 50), 11, steel, 1);
                float ex = 18 + (float)Math.cos(Math.toRadians(50)) * 20, ey = 44 - (float)Math.sin(Math.toRadians(50)) * 20;
                r.fill(r.circle(ex, ey, 3.5f), 12, metal, 1);
                r.fill(r.bar(ex, ey, 0, 14, 4, 5), 12, steel, 1);
                r.fill(r.circle(ex + 14, ey - 1, 2.5f), 13, lit(a), 0);
                for(int y = 0; y < 3; y++) for(int x = 0; x < 3; x++){
                    r.fill(r.rect(38 + x * 7, 34 + y * 7, 5, 5), 3, deep, 0);
                    r.fill(r.rect(39 + x * 7, 35 + y * 7, 3, 3), 4, acc(a), 0);
                }
            }
            case "wire-drawing-machine" -> {
                deck(r, 4);
                r.fill(r.circle(c, c, 18), 3, deep, 0);
                r.fill(r.rect(4, (int)c - 5, 9, 10), 10, steel, 2);
                r.cylH(12, (int)c - 1, 8, 2, 8, 1, copperM);
                r.fill(r.circle(s - 12, s - 12, 9), 9, copperM, 2);
                r.fill(r.circle(s - 12, s - 12, 3), 3, deep, 0);
                R rot = rotor(s);
                rot.fill(rot.ring(c, c, 11, 16), 7, metal, 2);
                rot.fill(rot.ring(c, c, 13, 14.5f), 8, copperM, 0);
                for(int i = 0; i < 4; i++) rot.fill(rot.bar(c, c, 4, 8, 3, i * 90), 6, steel, 1);
                rot.fill(rot.circle(c, c, 4), 8, metal, 1);
            }
            case "ore-crusher" -> {
                deck(r, 4);
                hopper(r, 14, 3, s - 28, 12);
                r.fill(r.circle(c, c, 20), 3, deep, 0);
                r.fill(r.rect((int)c - 8, s - 10, 16, 7), 7, steel, 1);
                r.fill(r.rect((int)c - 5, s - 8, 10, 3), 2, deep, 0);
                R rot = rotor(s);
                for(int i = 0; i < 4; i++) rot.fill(rot.bar(c, c, 4, 14, 6, i * 90 + 45), 7, steel, 1.5f);
                for(int i = 0; i < 4; i++) rot.fill(rot.bar(c, c, 13, 5, 7, i * 90 + 45), 8, acc(a), 1);
                rot.fill(rot.circle(c, c, 5), 9, metal, 2);
            }
            case "basic-module-assembler", "advanced-module-assembler", "module-recycling-facility" -> {
                deck(r, 4);
                if(name.equals("module-recycling-facility")){
                    hopper(r, 8, 4, s - 16, 12);
                    r.fill(r.circle(c, c + 4, 16), 3, deep, 0);
                    for(int x : new int[]{8, s - 20}) r.fill(r.rect(x, s - 14, 12, 9), 8, metal, 2);
                    R rot = rotor(s);
                    for(int i = 0; i < 6; i++) rot.fill(rot.bar(c, c, 3, 12, 3, i * 60), 7, steel, 1);
                    rot.fill(rot.circle(c, c, 4), 8, metal, 1);
                    rot.fill(rot.circle(c, c, 1.5f), 9.5f, lit(a), 0);
                }else{
                    int slots = name.startsWith("advanced") ? 3 : 1, sw = slots == 1 ? 30 : 22, gap = 6;
                    int x0 = (s - (slots * sw + (slots - 1) * gap)) / 2;
                    for(int i = 0; i < slots; i++){
                        int x = x0 + i * (sw + gap), y = s / 2 - 8;
                        r.fill(r.rect(x, y, sw, sw - 4), 3, deep, 0);
                        r.fill(r.rect(x + 3, y + 3, sw - 6, sw - 12), 6, acc(a), 1);
                        for(int px = x + 4; px < x + sw - 4; px += 3) r.fill(r.rect(px, y + sw - 8, 2, 3), 5, brass, 0);
                    }
                    r.fill(r.rect(6, 7, s - 12, 6), 11, steel, 1);
                    r.fill(r.rect((int)c - 6, 4, 12, 13), 13, metal, 2);
                    lamp(r, (int)c - 1, 13, a);
                }
            }
            default -> {
                //fabrication bed: guide rails, a crosshead on a gantry, the workpiece, feed trays on both sides
                r.fill(r.rect(5, 5, s - 10, s - 10), 3, metal, 1);
                int bx = s <= 64 ? 14 : 20, bw = s - 2 * bx;
                r.fill(r.rect(bx, 8, bw, s - 16), 4, deep, 0);
                r.cylV(bx - 4, 7, 4, s - 14, 5, 2, steel);
                r.cylV(bx + bw, 7, 4, s - 14, 5, 2, steel);
                int part = s <= 64 ? 12 : 18, py = (int)c + 2;
                r.fill(r.rect((int)c - part / 2, py - part / 2, part, part), 6, acc(a), 2);
                int gy = (int)(s * 0.28f);
                r.fill(r.rect(bx - 6, gy - 3, bw + 12, 6), 11, steel, 1);
                r.fill(r.rect((int)c - 5, gy - 5, 10, 10), 13, metal, 1);
                lamp(r, (int)c - 1, gy - 1, a);
            }
        }
    }

    /** Opaque bays of the fuel-cell family that have no dedicated design yet get these. */
    static void bayDesign(R r, String name, int g){
        int s = r.w;
        float c = s / 2f;
        deck(r, 4);
        switch(name){
            case "battery-assembly-plant" -> {
                r.fill(r.rect(4, 26, s - 8, 12), 5, dull(0x3a3c44), 1);
                for(int x = 6; x < s - 6; x += 4) r.raise(r.rect(x, 27, 1, 10), -0.8f);
                for(int x = 8; x < s - 8; x += 10) r.fill(r.rect(x, 28, 6, 8), 7, acc(g), 1);
                for(int x : new int[]{12, 36}){
                    r.fill(r.rect(x, 7, 16, 16), 10, metal, 2);
                    lamp(r, x + 6, 12, g);
                }
                tank(r, 10, 42, 18, 16, g);
                r.fill(r.rect(36, 44, 20, 12), 7, metal, 2);
            }
            case "fuel-rod-fabricator" -> {
                r.fill(r.rect(6, 6, 34, s - 12), 9, dull(0x8a8d96), 3);
                r.fill(r.rect(10, 10, 26, s - 20), 3, deep, 0);
                for(int x = 12; x < 34; x += 6) r.cylV(x, 12, 4, s - 24, 5, 2, lit(g));
                r.fill(r.rect(44, 8, 12, s - 16), 10, metal, 2);
                r.fill(r.rect(42, 28, 16, 8), 12, steel, 1);
                lamp(r, 48, 12, g);
            }
            case "composite-press" -> {
                r.fill(r.rect(10, 10, s - 20, s - 20), 10, metal, 3);
                for(int y = 16; y < s - 14; y += 6) r.fill(r.rect(14, y, s - 28, 2), 11, lit(g), 0);
                for(float[] p : new float[][]{{9, 9}, {s - 9, 9}, {9, s - 9}, {s - 9, s - 9}}){
                    r.fill(r.circle(p[0], p[1], 6), 12, steel, 2);
                    r.fill(r.circle(p[0], p[1], 2), 13.5f, metal, 0);
                }
            }
            case "experimental-core-assembler" -> {
                for(int i = 0; i < 4; i++){
                    float ang = 45 + i * 90;
                    float px = c + (float)Math.cos(Math.toRadians(ang)) * 36, py = c - (float)Math.sin(Math.toRadians(ang)) * 36;
                    r.fill(r.bar(c, c, 18, 18, 6, ang), 11, steel, 1);
                    r.fill(r.circle(px, py, 8), 10, metal, 2);
                    r.fill(r.circle(px, py, 2.5f), 12.5f, lit(g), 0);
                }
                r.fill(r.oct(c - 22, c - 22, 44, 44, 13), 10, metal, 3);
                r.fill(r.oct(c - 16, c - 16, 32, 32, 9), 3, deep, 0);
                r.fill(r.oct(c - 9, c - 9, 18, 18, 5), 8, lit(g), 2);
            }
            default -> {
                int count = s <= 64 ? 3 : 4, gap = 3, pad = 9;
                int cellW = (s - pad * 2 - gap * (count - 1)) / count;
                for(int i = 0; i < count; i++){
                    int x = pad + i * (cellW + gap);
                    r.fill(r.rect(x, 10, cellW, s - 26), 9, metal, 2);
                    window(r, r.rect(x + 3, 16, cellW - 6, s - 38), false, g);
                    r.fill(r.rect(x + 2, 11, cellW - 4, 2), 10, lit(g), 0);
                }
            }
        }
    }

    // =====================================================================================================
    // Furnaces, heat, power
    // =====================================================================================================

    // =====================================================================================================
    // Art pass (art.md): one design per machine, built around what it does. No shared housing template,
    // no wear noise, light from the top right, accents only on working parts.
    // =====================================================================================================

    /** A fresh canvas for the art pass: no edge wear (art.md §1.3: no random surface noise). */
    static R canvas(int s){
        R r = new R(s);
        r.worn = false;
        return r;
    }

    /** A plain machine body: chamfered slab with a broad bevel, the part every design sits on. */
    static void slab(R r, float x, float y, float w, float h, float cut, float top, Mat m){
        r.fill(r.oct(x, y, w, h, cut), top, m, 3);
    }

    /** Saves the glow layer: the lit (glow) materials of the design, or nothing visible if it has none. */
    static void glowOf(R r, String name) throws IOException{
        BufferedImage glow = r.glowImage();
        save(glow != null ? glow : img(r.w), name + "-glow");
    }

    static final Mat copperGearM = acc(0xd99d73), titaniumM = acc(0x8da1e3);

    /**
     * Gear Press (2×2): a square press. Two side columns carry a thick press plate ("-press", animated) over a recessed
     * forming bed; a copper gear sits in the bed; a feed throat on the top edge, an output channel on the bottom edge.
     */
    static void gearPress(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        //forming bed, recessed, with the gear just pressed lying in it
        r.fill(r.rect(12, 10, 40, 44), 2, deep, 1);
        r.fill(r.ring(22, 47, 3, 7), 4, copperGearM, 1);
        for(int i = 0; i < 8; i++) r.fill(r.bar(22, 47, 6, 3, 3, i * 45), 4, copperGearM, 0);
        //side columns
        for(int x : new int[]{2, 52}) r.fill(r.oct(x, 8, 10, 48, 2), 10, steel, 3);
        //feed throat (top) and output channel (bottom)
        r.fill(r.rect(24, 1, 16, 7), 8, steel, 1);
        r.fill(r.rect(27, 2, 10, 4), 3, deep, 0);
        r.fill(r.rect(26, 56, 12, 7), 5, steel, 1);
        r.fill(r.rect(29, 58, 6, 4), 2, deep, 0);
        r.fill(r.rect(30, 59, 4, 1), 6, lit(0xd99d73), 0);
        save(r, name);
        glowOf(r, name);

        //the press plate: spans the columns, a raised boss in the middle
        R p = canvas(s);
        p.fill(p.oct(6, 21, 52, 20, 3), 14, metal, 3);
        p.fill(p.oct(23, 17, 18, 28, 3), 17, metal, 3);
        p.fill(p.rect(28, 26, 8, 10), 15, steel, 2);
        save(p, name + "-press");
    }

    /**
     * Heavy Gear Press (2×2): heavier and darker than the Gear Press. A thick steel frame on all four sides with
     * hydraulic rams in the corners, a broad pressure plate ("-press") with a round central die, a feed slot on the left.
     */
    static void heavyGearPress(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 8, 6, steel);
        r.fill(r.oct(9, 9, 46, 46, 6), 3, deep, 1);
        //titanium gear in the die bed
        r.fill(r.ring(32, 32, 5, 9), 4, titaniumM, 1);
        for(int i = 0; i < 10; i++) r.fill(r.bar(32, 32, 8, 3, 3, i * 36), 4, titaniumM, 0);
        //hydraulic rams in the corners, outside the plate
        for(int[] c : new int[][]{{9, 9}, {55, 9}, {9, 55}, {55, 55}}){
            r.fill(r.circle(c[0], c[1], 6), 11, steel, 2);
            r.fill(r.circle(c[0], c[1], 3), 13, metal, 1);
        }
        //feed slot, left edge
        r.fill(r.rect(0, 24, 7, 16), 8, steel, 1);
        r.fill(r.rect(1, 27, 4, 10), 3, deep, 0);
        r.fill(r.rect(2, 30, 1, 4), 6, lit(0x8da1e3), 0);
        save(r, name);
        glowOf(r, name);

        R p = canvas(s);
        p.fill(p.oct(14, 14, 36, 36, 5), 14, steel, 3);
        p.fill(p.oct(17, 17, 30, 30, 4), 15, metal, 2);
        p.fill(p.circle(32, 32, 9), 18, metal, 2);
        p.fill(p.circle(32, 32, 4), 16, steel, 1);
        p.fill(p.rect(22, 47, 20, 2), 16, titaniumM, 0);
        save(p, name + "-press");
    }

    /**
     * Metal Extruder (2×2): a long horizontal extrusion line instead of a square press. A feed hopper on the left,
     * a pair of guide rollers over the enclosed channel, the die head on the right with a hot die and the rod coming out.
     */
    static void metalExtruder(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        //a low footing over the whole footprint, the long line raised on it
        slab(r, 0, 0, s, s, 3, 3, steel);
        slab(r, 0, 10, s, 44, 4, 5, metal);
        //enclosed channel running the length of the machine
        r.fill(r.rect(10, 22, 44, 20), 9, steel, 3);
        r.fill(r.rect(12, 28, 40, 8), 6, deep, 0);
        //feed hopper (left)
        r.fill(r.oct(2, 18, 18, 28, 3), 11, metal, 2);
        r.fill(r.rect(6, 24, 10, 16), 3, deep, 1);
        //guide rollers across the channel
        for(int x : new int[]{22, 32}){
            r.fill(r.oct(x, 17, 8, 30, 2), 13, metal, 2);
        }
        //die head (right): a heavier block with the hot die and the rod leaving it
        r.fill(r.oct(42, 16, 18, 32, 4), 14, metal, 3);
        r.fill(r.circle(51, 32, 5), 9, deep, 1);
        r.fill(r.circle(51, 32, 2.5f), 10, lit(0xffb46b), 0);
        r.fill(r.rect(58, 30, 6, 4), 8, titaniumM, 1);
        save(r, name);
        glowOf(r, name);
    }

    /**
     * Casing Forge (2×2): a closed forming chamber. A capsule-shaped mould (the pressure casing being formed, with its
     * seam), two clamp halves ("-press") closing on it, a metaglass window and two small feed slots on top.
     */
    static void casingForge(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, metal);
        r.fill(r.oct(8, 12, 48, 44, 4), 3, steel, 2);
        //the mould cavity and the casing inside it
        r.fill(r.oct(18, 18, 28, 34, 10), 2, deep, 1);
        r.fill(r.oct(22, 22, 20, 26, 8), 5, dull(0x9ab0b8), 2);
        r.fill(r.rect(31, 22, 2, 26), 4, dull(0x6e7f86), 0);
        //metaglass window and feed slots
        r.fill(r.rect(4, 3, 14, 7), 8, metal, 1);
        r.fill(r.rect(6, 4, 10, 4), 7, glass(0xc8e6f0), 0);
        for(int x : new int[]{26, 42}){
            r.fill(r.rect(x, 2, 10, 7), 8, steel, 1);
            r.fill(r.rect(x + 2, 3, 6, 4), 3, deep, 0);
        }
        r.fill(r.rect(48, 58, 10, 2), 7, lit(0x9ab0b8), 0);
        save(r, name);
        glowOf(r, name);

        //clamp halves, left and right of the mould
        R p = canvas(s);
        for(int x : new int[]{8, 44}){
            p.fill(p.oct(x, 16, 12, 38, 3), 12, metal, 3);
            p.fill(p.rect(x + 3, 22, 6, 26), 13, steel, 1);
        }
        save(p, name + "-press");
    }

    /**
     * Bearing Press (2×2): a precision forming unit with a round silhouette. A heavy outer race, the copper cage with its
     * balls ("-rotator", turning while it presses), three alignment guides and a short feed slot.
     */
    static void bearingPress(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 3, steel);
        slab(r, 2, 2, s - 4, s - 4, 14, 6, metal);
        r.fill(r.circle(c, c, 25), 9, steel, 3);
        r.fill(r.circle(c, c, 19), 4, deep, 1);
        r.fill(r.circle(c, c, 7), 7, metal, 2);
        r.fill(r.circle(c, c, 3), 5, deep, 0);
        //alignment guides
        for(int i = 0; i < 3; i++) r.fill(r.bar(c, c, 23, 6, 6, i * 120 + 90), 11, metal, 1);
        //feed slot, bottom left
        r.fill(r.bar(c, c, 24, 8, 8, 225), 8, steel, 1);
        r.fill(r.rect(1, s - 6, 3, 3), 10, lit(0xb8bcc4), 0);
        save(r, name);
        glowOf(r, name);

        //the cage: a copper ring holding eight balls
        R rot = canvas(s);
        rot.fill(rot.ring(c, c, 11, 16), 6, copperGearM, 1);
        for(int i = 0; i < 8; i++){
            double a = Math.toRadians(i * 45 + 22.5);
            rot.dome(rot.circle(c + (float)Math.cos(a) * 13.5f, c + (float)Math.sin(a) * 13.5f, 2.6f), 7, 2, metal);
        }
        save(rot, name + "-rotator");
    }

    /**
     * Precision Assembler (2×2): a clean, light assembly bed. Three component bays along the bottom (titanium gear,
     * silicon, high-purity silicon), an articulated arm with a tool head ("-rotator") over the bed, the finished part
     * leaving on the right, and small cyan indicators.
     */
    static void precisionAssembler(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, metal);
        r.fill(r.rect(6, 6, 44, 38), 4, steel, 2);
        //component bays
        int[] bay = {0x8da1e3, 0x53565f, 0x3d4a5c};
        for(int i = 0; i < 3; i++){
            int x = 6 + i * 15;
            r.fill(r.rect(x, 47, 14, 14), 8, metal, 2);
            r.fill(r.rect(x + 3, 50, 8, 8), 3, deep, 0);
            r.fill(r.rect(x + 4, 51, 6, 6), 4, dull(bay[i]), 0);
        }
        //output edge (right)
        r.fill(r.rect(52, 14, 10, 22), 7, steel, 1);
        r.fill(r.rect(54, 18, 6, 14), 3, deep, 0);
        for(int y : new int[]{8, 40}) r.fill(r.rect(56, y, 3, 2), 9, lit(0x6dd0f0), 0);
        //pivot of the arm
        r.fill(r.circle(32, 32, 6), 9, steel, 2);
        save(r, name);
        glowOf(r, name);

        //the arm: two segments from the pivot to a small tool head
        R rot = canvas(s);
        rot.fill(rot.bar(32, 32, 0, 15, 7, 300), 11, metal, 2);
        rot.fill(rot.circle(39.5f, 19, 5), 12, steel, 1);
        rot.fill(rot.bar(39.5f, 19, 0, 11, 6, 20), 12, metal, 2);
        rot.fill(rot.circle(50, 23, 5), 13, steel, 2);
        rot.fill(rot.circle(50, 23, 2), 14, lit(0x6dd0f0), 0);
        rot.fill(rot.circle(32, 32, 4), 12, metal, 1);
        save(rot, name + "-rotator");
    }

    /**
     * Frame Forge (3×3, heat): a big open hot-forming frame. Two massive columns joined by a crossbeam that carries the
     * forming head, a heated die below it, feed rails entering from the left and an exit run at the bottom.
     */
    static void frameForge(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //work floor between the columns
        r.fill(r.rect(18, 8, 60, 80), 3, deep, 1);
        r.fill(r.rect(22, 30, 52, 44), 3.5f, steel, 1);
        //feed rails (left) and exit run (bottom)
        for(int y : new int[]{38, 52}) r.fill(r.rect(2, y, 30, 4), 5, metal, 1);
        r.fill(r.rect(38, 74, 20, 20), 2, deep, 1);
        for(int x : new int[]{40, 52}) r.fill(r.rect(x, 74, 4, 20), 4, metal, 1);
        //heated die: a dark bed with a glowing strip
        r.fill(r.oct(30, 42, 36, 24, 4), 6, steel, 2);
        r.fill(r.rect(34, 48, 28, 12), 2, deep, 0);
        r.fill(r.rect(36, 52, 24, 4), 3, lit(0xffb46b), 0);
        //the columns and crossbeam with the forming head
        for(int x : new int[]{8, 74}) r.fill(r.oct(x, 4, 14, 72, 3), 14, steel, 3);
        r.fill(r.rect(16, 12, 64, 14), 16, steel, 3);
        r.fill(r.oct(36, 18, 24, 16, 3), 18, metal, 2);
        r.fill(r.rect(42, 33, 12, 6), 15, metal, 1);
        save(r, name);
        glowOf(r, name);

        //heat input glow on the receiving edge (rotated to each heated side by the game)
        edgeGlow(name + "-heat", 3);
    }

    /**
     * Experimental Core Assembler (3×3): an enclosed high-tier chamber. A round cradle with three precision arms
     * ("-rotator") converging on a small core, four separate feed bays for its four materials, a heavy dark frame and
     * only a little purple.
     */
    static void coreAssembler(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 3, deep);
        slab(r, 2, 2, s - 4, s - 4, 10, 6, steel);
        r.fill(r.circle(c, c, 34), 9, metal, 3);
        r.fill(r.circle(c, c, 27), 3, deep, 1);
        //cradle seat and the core itself (small: no floating orb)
        r.fill(r.ring(c, c, 8, 12), 6, steel, 1);
        r.fill(r.circle(c, c, 5), 8, lit(0xc88fff), 1);
        //feed bays for precision components, surge superalloy, platinum and phase fabric
        int[][] bays = {{4, 18}, {74, 6}, {80, 70}, {12, 78}};
        int[] contents = {0x6dd0f0, 0xf3e979, 0xd9dde6, 0xffd59e};
        for(int i = 0; i < 4; i++){
            int x = bays[i][0], y = bays[i][1];
            r.fill(r.rect(x, y, 13, 13), 11, metal, 2);
            r.fill(r.rect(x + 3, y + 3, 7, 7), 4, deep, 0);
            r.fill(r.rect(x + 5, y + 5, 3, 3), 5, dull(contents[i]), 0);
        }
        save(r, name);
        glowOf(r, name);

        R rot = canvas(s);
        for(int i = 0; i < 3; i++){
            rot.fill(rot.bar(c, c, 13, 14, 5, i * 120 + 30), 11, metal, 1);
            double a = Math.toRadians(i * 120 + 30);
            rot.fill(rot.circle(c + (float)Math.cos(a) * 27, c - (float)Math.sin(a) * 27, 4), 12, steel, 1);
            rot.fill(rot.bar(c, c, 9, 5, 3, i * 120 + 30), 12, steel, 0);
        }
        save(rot, name + "-rotator");
    }

    // ---- art pass batch 4 (art.md §7): production and extraction ----

    /**
     * Carbide Drill (3×3): a thick frame with four heavy supports round a large cutting head of dark carbide teeth
     * ("-rotator"), a hub cap with a tungsten accent ("-top"); the ore contact shows round the head.
     */
    static void carbideDrill(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 6, steel);
        r.fill(r.circle(c, c, 38), 4, deep, 1);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 34, 14, 16, i * 90 + 45), 11, metal, 3);
        save(r, name);

        R rot = canvas(s);
        rot.fill(rot.circle(c, c, 30), 6, steel, 2);
        for(int i = 0; i < 6; i++){
            rot.fill(rot.bar(c, c, 8, 22, 9, i * 60), 8, dull(0x50525e), 2);
            rot.fill(rot.bar(c, c, 24, 6, 7, i * 60 + 12), 9, dull(0x3a3b44), 1);
        }
        save(rot, name + "-rotator");

        R top = canvas(s);
        top.fill(top.circle(c, c, 11), 12, metal, 3);
        top.fill(top.circle(c, c, 5), 13, dull(0x50525e), 1);
        save(top, name + "-top");

        BufferedImage rim = img(s);
        Graphics2D rg = g(rim);
        rg.setColor(new Color(255, 255, 255, 90));
        rg.draw(new Ellipse2D.Float(9, 9, s - 19, s - 19));
        rg.dispose();
        save(rim, name + "-rim");
    }

    /** Biomass Cultivator (2×2): a metal frame round four cultivation trays (windows) under a lamp bar, two feed points. */
    static void cultivator(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        for(int[] t : new int[][]{{6, 14}, {34, 14}, {6, 38}, {34, 38}}){
            r.fill(r.rect(t[0], t[1], 24, 20), 8, steel, 2);
            liquidWindow(r, r.rect(t[0] + 3, t[1] + 3, 18, 14), 9);
        }
        r.fill(r.rect(4, 3, 56, 7), 11, metal, 2);
        r.fill(r.rect(8, 5, 48, 3), 12, lit(0xe8f0c8), 0);
        r.fill(r.rect(28, 58, 8, 5), 10, acc(0x8fd3ff), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    /** Algae Photobioreactor (3×3): rows of flat culture panels between water/CO2 and fertilizer manifolds. */
    static void photobioreactor(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.rect(4, 4, 88, 10), 10, steel, 2);
        r.fill(r.rect(10, 7, 8, 4), 11, acc(0x8fd3ff), 0);
        r.fill(r.rect(26, 7, 8, 4), 11, acc(0xd8dde3), 0);
        r.fill(r.rect(4, 82, 88, 10), 10, steel, 2);
        r.fill(r.rect(42, 85, 12, 4), 11, acc(0xb8a878), 0);
        for(int i = 0; i < 4; i++){
            int x = 8 + i * 21;
            r.fill(r.rect(x, 18, 17, 60), 9, steel, 2);
            liquidWindow(r, r.rect(x + 3, 21, 11, 54), 10);
        }
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    // ---- art pass batch 5 (art.md §8-11): logistics, liquid valves, power, walls ----
    // Rotating logistics blocks are drawn facing right: input from the left, front = right, sides = up/down.

    /** A lane from the centre to one side (0 right, 1 up, 2 left, 3 down), dark with steel lips. */
    static void lane(R r, int side, int width, float top){
        float c = r.w / 2f;
        r.fill(r.bar(c, c, 0, c, width + 4, -side * 90), top, steel, 1);
        r.fill(r.bar(c, c, 0, c, width, -side * 90), top - 4, deep, 0);
    }

    /** A small outward chevron on a lane, in the block's accent colour. */
    static void chevron(R r, int side, float dist, Mat m){
        float c = r.w / 2f;
        r.fill(r.tri(c, c, new float[]{dist - 3, dist + 2, dist - 3}, new float[]{-3, 0, 3}, -side * 90), 6, m, 0);
    }

    static void threeWaySorter(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        for(int side : new int[]{0, 1, 2, 3}) lane(r, side, 6, 7);
        //a turntable diverter in the middle
        r.fill(r.circle(16, 16, 7), 9, steel, 2);
        r.fill(r.bar(16, 16, -5, 10, 3, 45), 10, metal, 1);
        for(int side : new int[]{0, 1, 3}) chevron(r, side, 12, acc(0xffd84a));
        save(r, name);
    }

    static void controlledSplitter(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        for(int side : new int[]{0, 1, 2, 3}) lane(r, side, 6, 7);
        //a splitter wedge pointing back at the supply
        r.fill(r.poly(new int[]{10, 22, 22}, new int[]{16, 9, 23}), 9, steel, 1);
        r.fill(r.poly(new int[]{14, 20, 20}, new int[]{16, 12, 20}), 10, acc(0x9fe07a), 0);
        save(r, name);
    }

    static void priorityMerger(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        for(int side : new int[]{1, 2, 3}) lane(r, side, 5, 7);
        lane(r, 0, 8, 7);
        //a funnel into the exit, and priority marks (1, 2, 3 bars) at the three entries
        r.fill(r.poly(new int[]{12, 22, 12}, new int[]{9, 16, 23}), 9, steel, 1);
        Mat m = acc(0x7ab8ff);
        r.fill(r.rect(3, 9, 1, 3), 8, m, 0);
        for(int k = 0; k < 2; k++) r.fill(r.rect(9 + k * 2, 2, 1, 3), 8, m, 0);
        for(int k = 0; k < 3; k++) r.fill(r.rect(8 + k * 2, 27, 1, 3), 8, m, 0);
        save(r, name);
    }

    static void itemFilter(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        lane(r, 2, 8, 7);
        lane(r, 0, 8, 7);
        lane(r, 1, 4, 7);
        lane(r, 3, 4, 7);
        //a filter grille across the straight path
        r.fill(r.rect(14, 8, 4, 16), 10, steel, 1);
        for(int y = 10; y < 23; y += 3) r.fill(r.rect(15, y, 2, 1), 11, acc(0xff9a3c), 0);
        save(r, name);
    }

    static void batchGate(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        lane(r, 2, 8, 7);
        lane(r, 0, 8, 7);
        //a holding chamber, then a gate across the exit
        r.fill(r.rect(7, 7, 14, 18), 7, steel, 1);
        r.fill(r.rect(9, 9, 10, 14), 3, deep, 0);
        r.fill(r.rect(21, 6, 4, 20), 10, acc(0xb37aff), 1);
        save(r, name);
    }

    static void smartUnloader(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 3, 6, steel);
        //a stepped hopper opening with an extraction claw over it
        r.fill(r.rect(5, 5, 22, 22), 8, metal, 2);
        r.fill(r.rect(8, 8, 16, 16), 5, steel, 1);
        r.fill(r.rect(11, 11, 10, 10), 3, deep, 0);
        r.fill(r.bar(16, 16, 0, 9, 3, 45), 10, metal, 0);
        r.fill(r.bar(16, 16, 0, 9, 3, 135), 10, metal, 0);
        r.fill(r.rect(15, 15, 2, 2), 11, acc(0xe0683a), 0);
        save(r, name);
    }

    static void distributionHub(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        for(int side : new int[]{0, 1, 2, 3}) lane(r, side, 10, 7);
        //a sorting drum with three pockets
        r.fill(r.circle(32, 32, 15), 10, steel, 3);
        for(int i = 0; i < 3; i++) r.fill(r.circle(32 + (float)Math.cos(Math.toRadians(i * 120)) * 7, 32 + (float)Math.sin(Math.toRadians(i * 120)) * 7, 3.5f), 6, deep, 0);
        r.fill(r.circle(32, 32, 3), 12, metal, 1);
        for(int side : new int[]{0, 1, 3}) chevron(r, side, 26, acc(0xffd84a));
        save(r, name);
    }

    static void lineBalancer(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        lane(r, 2, 10, 7);
        //a manifold with three equal branches, a balancing dial on each
        r.fill(r.rect(18, 18, 28, 28), 9, steel, 3);
        r.fill(r.rect(22, 22, 20, 20), 5, deep, 0);
        for(int side : new int[]{0, 1, 3}){
            lane(r, side, 6, 8);
            float c = 32;
            float dx = side == 0 ? 54 : c, dy = side == 1 ? 10 : side == 3 ? 54 : c;
            r.fill(r.circle(dx, dy, 4), 11, metal, 1);
            r.fill(r.rect((int)dx - 1, (int)dy - 1, 2, 2), 12, acc(0x9fe07a), 0);
        }
        save(r, name);
    }

    static void logisticsBuffer(String name, boolean heavy) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, heavy ? 6 : 3, 6, heavy ? steel : metal);
        int frame = heavy ? 6 : 4;
        //inputs from the back and sides (small slots), a storage bed of bays, a pusher on the front
        r.fill(r.rect(0, 26, 6, 12), 8, steel, 1);
        r.fill(r.rect(26, 0, 12, 6), 8, steel, 1);
        r.fill(r.rect(26, 58, 12, 6), 8, steel, 1);
        r.fill(r.rect(frame + 2, frame + 2, 44 - frame, s - frame * 2 - 4), 10, heavy ? metal : steel, 2);
        int cols = heavy ? 3 : 2, rows = 3, bw = (36 - frame) / cols, bh = (s - frame * 2 - 12) / rows;
        for(int i = 0; i < cols; i++) for(int j = 0; j < rows; j++) r.fill(r.rect(frame + 5 + i * bw, frame + 5 + j * bh, bw - 3, bh - 3), 5, deep, 0);
        r.fill(r.rect(50, 20, 12, 24), heavy ? 13 : 11, heavy ? metal : steel, 2);
        r.fill(r.rect(56, 26, 6, 12), 6, deep, 0);
        r.fill(r.rect(52, 22, 2, 20), 12, acc(heavy ? 0xd8c27a : 0x9aa0aa), 0);
        save(r, name);
    }

    static void overflowValve(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        lane(r, 2, 8, 7);
        lane(r, 0, 8, 7);
        lane(r, 1, 4, 7);
        lane(r, 3, 4, 7);
        //a gate disc on the main line; past it, the overflow spills to the sides
        r.fill(r.circle(13, 16, 5), 10, steel, 1);
        r.fill(r.rect(12, 11, 2, 10), 11, acc(0x5ab8ff), 0);
        save(r, name);
    }

    static void liquidBalancerArt(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        for(int side : new int[]{0, 1, 2, 3}) lane(r, side, 5, 7);
        //a round dividing chamber, three equal branches
        r.fill(r.circle(16, 16, 8), 10, steel, 2);
        r.fill(r.circle(16, 16, 5), 7, acc(0x5ab8ff), 1);
        r.fill(r.circle(16, 16, 2), 11, metal, 0);
        save(r, name);
    }

    static void reserveValve(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, metal);
        lane(r, 2, 8, 7);
        lane(r, 0, 8, 7);
        //a handwheel valve sitting on the line
        r.fill(r.ring(16, 16, 6, 9), 11, steel, 1);
        for(int i = 0; i < 4; i++) r.fill(r.bar(16, 16, 0, 7, 2, i * 90 + 45), 10, metal, 0);
        r.fill(r.circle(16, 16, 2.5f), 12, acc(0x3f86d6), 0);
        save(r, name);
    }

    /** Battery: base, charge layers ("-power*") filling the given windows from the bottom, and "-top". */
    static void batteryLayers(String name, R base, int[][] windows, int rgb, R top) throws IOException{
        save(base, name);
        int s = base.w;
        for(String suffix : new String[]{"-power", "-power-empty", "-power-full"}){
            BufferedImage overlay = img(s);
            Graphics2D pg = g(overlay);
            for(int[] w : windows){
                if(suffix.endsWith("empty")){
                    pg.setColor(new Color(0x3a3530));
                    pg.fillRect(w[0], w[1], w[2], w[3]);
                }else{
                    int lh = suffix.endsWith("full") ? w[3] : w[3] / 2;
                    pg.setColor(new Color(shade(rgb, 0.8f)));
                    pg.fillRect(w[0], w[1] + w[3] - lh, w[2], lh);
                    pg.setColor(new Color(tint(rgb, 0.35f)));
                    pg.fillRect(w[0], w[1] + w[3] - lh, w[2], 1);
                }
            }
            pg.dispose();
            save(overlay, name + suffix);
        }
        save(top, name + "-top");
    }

    static void industrialBattery(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //four lead-acid cells and the copper bus between them
        int[][] cells = {{5, 7}, {35, 7}, {5, 35}, {35, 35}};
        int[][] win = new int[4][];
        for(int i = 0; i < 4; i++){
            int x = cells[i][0], y = cells[i][1];
            r.fill(r.rect(x, y, 24, 22), 9, dull(0x4a4d55), 2);
            r.fill(r.rect(x + 4, y + 5, 16, 13), 4, deep, 0);
            win[i] = new int[]{x + 4, y + 5, 16, 13};
        }
        r.fill(r.rect(30, 6, 4, 52), 10, copperGearM, 1);
        R top = canvas(s);
        for(int[] c : cells){
            top.dome(top.rect(c[0] + 5, c[1] + 1, 3, 3), 11, 1.5f, redM);
            top.dome(top.rect(c[0] + 16, c[1] + 1, 3, 3), 11, 1.5f, metal);
        }
        batteryLayers(name, r, win, 0x7f9a63, top);
    }

    static void lithiumBattery(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        //a dense pack of slim cells above and below a central management plate
        int[][] win = new int[8][];
        int k = 0;
        for(int row = 0; row < 2; row++){
            int y = row == 0 ? 5 : 39;
            for(int i = 0; i < 4; i++){
                int x = 5 + i * 14;
                r.fill(r.rect(x, y, 12, 20), 9, metal, 1);
                r.fill(r.rect(x + 3, y + 3, 6, 14), 4, deep, 0);
                win[k++] = new int[]{x + 3, y + 3, 6, 14};
            }
        }
        r.fill(r.rect(3, 27, 58, 10), 11, metal, 2);
        for(int x = 10; x < 56; x += 8) r.fill(r.rect(x, 30, 4, 4), 12, lit(0xd9cfe9), 0);
        R top = canvas(s);
        top.fill(top.rect(4, 26, 2, 12), 12, steel, 0);
        batteryLayers(name, r, win, 0xd9cfe9, top);
    }

    static void flowBattery(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //two electrolyte tanks with loops down into the cell stack
        r.fill(r.circle(24, 28, 20), 10, dull(0x5f4a86), 3);
        r.fill(r.circle(24, 28, 8), 12, steel, 1);
        r.fill(r.circle(72, 28, 20), 10, dull(0x8c8a4a), 3);
        r.fill(r.circle(72, 28, 8), 12, steel, 1);
        for(int x : new int[]{16, 28, 64, 76}) r.fill(r.rect(x, 44, 4, 16), 9, metal, 1);
        r.fill(r.rect(10, 58, 76, 32), 11, steel, 3);
        int[][] win = new int[5][];
        for(int i = 0; i < 5; i++){
            int x = 16 + i * 14;
            r.fill(r.rect(x, 63, 10, 22), 5, deep, 0);
            win[i] = new int[]{x, 63, 10, 22};
        }
        R top = canvas(s);
        top.fill(top.rect(10, 56, 76, 3), 12, metal, 1);
        batteryLayers(name, r, win, 0x7a5fa8, top);
    }

    static void aetheriteCapacitor(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 6, steel);
        //a heavy casing; charge columns either side of a small secured crystal
        r.fill(r.oct(6, 6, 52, 52, 8), 9, metal, 3);
        int[][] win = {{12, 12, 8, 40}, {44, 12, 8, 40}};
        for(int[] w : win) r.fill(r.rect(w[0], w[1], w[2], w[3]), 4, deep, 0);
        r.fill(r.oct(24, 20, 16, 24, 4), 5, deep, 0);
        r.fill(r.poly(new int[]{32, 37, 32, 27}, new int[]{23, 32, 41, 32}), 7, lit(0x7fe0ff), 1);
        R top = canvas(s);
        for(int y : new int[]{19, 42}) top.fill(top.rect(24, y, 16, 3), 12, steel, 1);
        batteryLayers(name, r, win, 0x7fe0ff, top);
    }

    static void fuelCellArt(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //hydrogen in on the left, oxygen on the right, water out at the bottom, the cell stack between
        stub(r, 2, 18, 12, acc(0xe8edf0));
        stub(r, 0, 18, 12, acc(0xd8efff));
        stub(r, 3, 26, 12, acc(0x8fd3ff));
        r.fill(r.rect(14, 6, 36, 48), 10, steel, 3);
        for(int y = 10; y < 52; y += 4) r.fill(r.rect(16, y, 32, 2), 11, metal, 0);
        r.fill(r.rect(28, 4, 8, 4), 12, lit(0x7fc8ff), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void methaneTurbine(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //combustor (left) feeding a round turbine housing (right); flue gas leaves at the bottom left
        r.fill(r.rect(2, 14, 22, 30), 10, steel, 3);
        r.fill(r.rect(7, 22, 12, 14), 4, deep, 0);
        r.fill(r.rect(9, 26, 8, 6), 5, lit(0xff9a4a), 0);
        r.fill(r.circle(42, 30, 19), 11, steel, 3);
        r.fill(r.circle(42, 30, 13), 5, deep, 1);
        for(int i = 0; i < 8; i++) r.fill(r.bar(42, 30, 3, 9, 3, i * 45 + 20), 7, metal, 0);
        r.fill(r.circle(10, 54, 6), 9, metal, 2);
        r.fill(r.circle(10, 54, 3), 4, deep, 0);
        save(r, name);
        glowOf(r, name);
    }

    static void biofuelEngine(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, steel);
        //an engine block with four cylinder heads, a flywheel, an exhaust manifold and a fuel gauge
        r.fill(r.rect(4, 12, 34, 40), 10, metal, 3);
        for(int[] c : new int[][]{{13, 22}, {29, 22}, {13, 42}, {29, 42}}){
            r.fill(r.circle(c[0], c[1], 6), 12, steel, 2);
            r.fill(r.circle(c[0], c[1], 2), 13, metal, 0);
        }
        r.fill(r.circle(50, 32, 11), 11, steel, 3);
        for(int i = 0; i < 4; i++) r.fill(r.bar(50, 32, 2, 8, 3, i * 90 + 45), 12, metal, 0);
        r.fill(r.rect(4, 3, 34, 7), 9, steel, 1);
        r.fill(r.circle(54, 8, 4), 9, deep, 0);
        r.fill(r.rect(8, 55, 10, 4), 11, lit(0xf0c64a), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void steamTurbineArt(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //a stepped turbine casing, narrow at the steam inlet, wide at the exhaust; the return-water section below
        int[][] stages = {{6, 30, 20, 26}, {24, 24, 22, 38}, {44, 18, 26, 50}};
        for(int[] st : stages){
            r.fill(r.rect(st[0], st[1], st[2], st[3]), 11, steel, 3);
            for(int x = st[0] + 3; x < st[0] + st[2] - 2; x += 4) r.fill(r.rect(x, st[1] + 3, 2, st[3] - 6), 12, metal, 0);
        }
        r.fill(r.oct(70, 22, 22, 42, 5), 12, metal, 3);
        r.fill(r.rect(76, 36, 10, 14), 13, copperGearM, 1);
        r.fill(r.rect(6, 74, 84, 16), 9, steel, 2);
        r.fill(r.rect(10, 78, 76, 8), 10, glass(0x8fd3ff), 0);
        r.fill(r.rect(2, 40, 6, 8), 12, lit(0x9fd8ff), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void boilerArt(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //a horizontal pressure drum with a steam dome, water in on the left, a heat coupling plate underneath
        r.fill(r.oct(6, 20, 84, 48, 20), 11, steel, 4);
        for(int x = 22; x < 78; x += 14) r.fill(r.rect(x, 22, 2, 44), 12, metal, 0);
        r.fill(r.circle(c, 44, 12), 13, metal, 3);
        r.fill(r.circle(c, 44, 5), 14, steel, 1);
        stub(r, 2, 38, 12, acc(0x8fd3ff));
        liquidWindow(r, r.rect(14, 38, 10, 12), 12);
        r.fill(r.rect(14, 74, 68, 16), 8, metal, 2);
        r.fill(r.rect(18, 78, 60, 8), 9, acc(0xd97a52), 0);
        r.fill(r.rect(46, 66, 4, 4), 12, lit(0x9fd8ff), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        heatLayer(r, name, r.rect(18, 78, 60, 8));
    }

    static void fissionReactor(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //a thick containment shell, coolant ports, and a small grid of fuel channels in the core
        r.fill(r.oct(6, 6, 84, 84, 22), 11, metal, 5);
        r.fill(r.oct(16, 16, 64, 64, 16), 12, steel, 3);
        r.fill(r.oct(26, 26, 44, 44, 10), 5, deep, 1);
        for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) r.fill(r.circle(c + i * 11, c + j * 11, 3.5f), 6, (i == 0 && j == 0) ? lit(0xff7ad1) : dull(0x8a6a80), 1);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 38, 8, 10, i * 90), 13, metal, 2);
        save(r, name);
        //the glow follows the octagonal core exactly
        BufferedImage glow = img(s);
        boolean[] core = r.oct(26, 26, 44, 44, 10);
        for(int i = 0; i < core.length; i++) if(core[i]) glow.setRGB(i % s, i / s, 0x46ff7ad1);
        save(glow, name + "-glow");
        edgeGlow(name + "-heat", 3);
        BufferedImage top = port(s, 22, acc(shade(0xff7ad1, 0.75f))).render();
        save(top, name + "-top1");
        save(flipY(top), name + "-top2");
    }

    /** Walls: edge-to-edge plates that tile; layered armour, kind 0 superalloy, 1 composite, 2 surge superalloy. */
    static void wallArt(String name, int size, int kind) throws IOException{
        int s = size * T;
        int[] rgb = {0xcdb98a, 0x4a5260, 0xf2c45a};
        Mat body = dull(mixi(0x989aa4, rgb[kind], 0.3f)), armor = dull(rgb[kind]);
        R r = canvas(s);
        r.fill(r.rect(0, 0, s, s), 6, body, 3);
        int in = size == 1 ? 5 : 6;
        r.fill(r.rect(in, in, s - in * 2, s - in * 2), 9, armor, 2);
        if(kind == 0){
            //layered plating: an inner plate and seams
            r.fill(r.rect(in + 3, in + 3, s - in * 2 - 6, s - in * 2 - 6), 10, armor, 1);
            for(int y = in + 6; y < s - in - 4; y += size == 1 ? 7 : 9) r.fill(r.rect(in + 3, y, s - in * 2 - 6, 1), 9.5f, body, 0);
        }else if(kind == 1){
            //composite: broad diagonal laminate bands
            for(int k = -s; k < s; k += 8) r.fill(r.and(r.rect(in, in, s - in * 2, s - in * 2), r.bar(k + s / 2f, s / 2f, -s, s * 2, 3, 45)), 10, dull(0x5c6576), 0);
        }else{
            r.fill(r.rect(in + 3, in + 3, s - in * 2 - 6, s - in * 2 - 6), 10, armor, 1);
            int[] at = size == 1 ? new int[]{9, 21} : new int[]{12, 28, 44};
            for(int x : at) for(int y : at) r.fill(r.rect(x, y, 3, 3), 11, copperGearM, 0);
        }
        if(size == 2) for(int x : new int[]{20, 42}) r.fill(r.rect(x, in, 2, s - in * 2), 8, body, 0);
        save(r, name);
    }

    // ---- art pass batch 6 (art.md §12, §7.2, §10.2): effect, augmentation and support blocks ----

    /**
     * Coupler family (1×1 and the 2×2 hub): a bracket with module slots. "-light" glows over each slot that holds a
     * module; "-connector" is the arm the code turns towards the machine it is attached to.
     */
    static void couplerLayers(String name, R r, float[][] slots, float slotR) throws IOException{
        for(float[] sl : slots){
            r.fill(r.circle(sl[0], sl[1], slotR + 2), r.zAt((int)sl[0], (int)sl[1]) + 2, brass, 1);
            r.fill(r.circle(sl[0], sl[1], slotR), 4, deep, 0);
        }
        save(r, name);
        BufferedImage light = img(r.w);
        Graphics2D l = g(light);
        for(float[] sl : slots){
            l.setColor(new Color(255, 255, 255, 120));
            l.fill(new Ellipse2D.Float(sl[0] - slotR, sl[1] - slotR, slotR * 2, slotR * 2));
            l.setColor(Color.white);
            l.fill(new Ellipse2D.Float(sl[0] - slotR / 2.5f, sl[1] - slotR / 2.5f, slotR * 0.8f, slotR * 0.8f));
        }
        l.dispose();
        save(light, name + "-light");
        R arm = canvas(r.w);
        float c = r.w / 2f;
        arm.cylH((int)c + 5, (int)c - 4, (int)c - 5, 8, 8, 3, brass);
        arm.fill(arm.rect((int)c + 5, (int)c - 1, (int)c - 5, 2), 11, acc(0x9a8650), 0);
        save(arm, name + "-connector");
    }

    static void augmentationCoupler(String name) throws IOException{
        R r = canvas(T);
        slab(r, 3, 3, 26, 26, 3, 6, metal);
        //a mounting bracket with four feet and one module slot
        for(int[] f : new int[][]{{0, 0}, {25, 0}, {0, 25}, {25, 25}}) r.fill(r.rect(f[0], f[1], 7, 7), 7, steel, 1);
        couplerLayers(name, r, new float[][]{{16, 16}}, 5);
    }

    static void advancedCoupler(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 3, 6, steel);
        r.fill(r.rect(6, 3, 20, 26), 8, metal, 2);
        couplerLayers(name, r, new float[][]{{16, 10}, {16, 22}}, 4);
    }

    static void chemicalInterface(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 3, 6, metal);
        //three slots and a small coolant reservoir
        r.fill(r.circle(23, 23, 6), 9, steel, 1);
        r.fill(r.circle(23, 23, 3.5f), 10, glass(0x8fd3ff), 0);
        couplerLayers(name, r, new float[][]{{9, 9}, {23, 9}, {9, 23}}, 3.5f);
    }

    static void militaryInterface(String name) throws IOException{
        R r = canvas(T);
        slab(r, 0, 0, T, T, 2, 6, dull(0x5d5f6c));
        r.fill(r.rect(3, 3, 14, 26), 8, steel, 2);
        //three slots down the side, a small targeting lens and cooling fins
        r.fill(r.circle(23, 11, 4), 9, steel, 1);
        r.fill(r.circle(23, 11, 2), 10, lit(0xff795e), 0);
        for(int y = 19; y < 28; y += 3) r.fill(r.rect(19, y, 9, 1), 8, metal, 0);
        couplerLayers(name, r, new float[][]{{10, 8}, {10, 16}, {10, 24}}, 3);
    }

    static void augmentationHub(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 6, steel);
        //four bays round a central control plate
        r.fill(r.oct(22, 22, 20, 20, 4), 10, metal, 2);
        for(int x = 26; x < 40; x += 4) r.fill(r.rect(x, 30, 2, 4), 11, lit(0xff9a3c), 0);
        for(float[] b : new float[][]{{32, 11}, {53, 32}, {32, 53}, {11, 32}}) r.fill(r.circle(b[0], b[1], 10), 8, metal, 2);
        couplerLayers(name, r, new float[][]{{32, 11}, {53, 32}, {32, 53}, {11, 32}}, 5);
    }

    static void repairBay(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //a service pad with landing marks, a stock rack of repair material, a crane arm over the pad
        r.fill(r.rect(26, 30, 56, 56), 4, deep, 1);
        for(int[] m : new int[][]{{30, 34}, {74, 34}, {30, 78}, {74, 78}}) r.fill(r.rect(m[0], m[1], 4, 4), 5, acc(0x98ffa9), 0);
        r.fill(r.rect(4, 30, 18, 56), 9, steel, 2);
        for(int y = 34; y < 82; y += 8) r.fill(r.rect(7, y, 12, 4), 10, titaniumM, 1);
        r.fill(r.rect(6, 6, 84, 14), 11, steel, 3);
        r.fill(r.oct(56, 4, 22, 22, 4), 13, metal, 2);
        r.fill(r.rect(64, 24, 6, 16), 12, metal, 1);
        r.fill(r.rect(10, 10, 6, 6), 12, lit(0x98ffa9), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void robotHub(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //four docking pads round a central charging pylon
        for(float[] d : new float[][]{{c, 16}, {80, c}, {c, 80}, {16, c}}){
            r.fill(r.circle(d[0], d[1], 13), 8, metal, 2);
            r.fill(r.ring(d[0], d[1], 8, 10), 9, steel, 0);
            r.fill(r.circle(d[0], d[1], 2), 9, lit(0x98ffa9), 0);
        }
        r.fill(r.oct(c - 14, c - 14, 28, 28, 6), 12, metal, 3);
        r.fill(r.circle(c, c, 6), 14, steel, 2);
        r.fill(r.circle(c, c, 3), 15, lit(0x98ffa9), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void shieldBeacon(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, metal);
        //an emitter stack with three focusing prongs and a power interface below
        for(int i = 0; i < 3; i++){
            r.fill(r.bar(c, c - 4, 14, 22, 10, i * 120 - 90), 10, steel, 2);
            double a = Math.toRadians(i * 120 - 90);
            r.fill(r.circle(c + (float)Math.cos(a) * 36, c - 4 - (float)Math.sin(a) * 36, 5), 12, metal, 2);
        }
        r.fill(r.circle(c, c - 4, 16), 11, metal, 3);
        r.fill(r.ring(c, c - 4, 6, 10), 12, steel, 1);
        r.fill(r.circle(c, c - 4, 5), 13, lit(0x7fd8ff), 1);
        r.fill(r.rect(30, 78, 36, 14), 9, steel, 2);
        r.fill(r.rect(36, 82, 24, 4), 10, copperGearM, 0);
        save(r, name);
        glowOf(r, name);
    }

    static void disruptionArray(String name, int size) throws IOException{
        int s = size * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, size == 3 ? 6 : 10, 5, steel);
        if(size == 3){
            //a pulse coil with four directional antenna fins reaching for the corners
            for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 18, 26, 8, i * 90 + 45), 10, metal, 2);
            for(int k = 0; k < 3; k++) r.fill(r.ring(c, c, 8 + k * 6, 11 + k * 6), 12 - k, k == 1 ? copperGearM : metal, 1);
            r.fill(r.circle(c, c, 6), 13, steel, 1);
            r.fill(r.circle(c, c, 3), 14, lit(0xff9a3c), 0);
            r.fill(r.rect(6, 86, 10, 4), 11, lit(0x7fd8ff), 0);
        }else{
            //six focusing pylons and struts round two counter-set rings and a small purple core
            for(int i = 0; i < 6; i++){
                r.fill(r.bar(c, c, 30, 22, 6, i * 60 + 30), 9, metal, 1);
                double a = Math.toRadians(i * 60 + 30);
                r.fill(r.oct(c + (float)Math.cos(a) * 48 - 8, c - (float)Math.sin(a) * 48 - 8, 16, 16, 4), 12, metal, 3);
            }
            r.fill(r.ring(c, c, 22, 28), 11, steel, 2);
            r.fill(r.ring(c, c, 12, 17), 12, metal, 2);
            for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 12, 16, 3, i * 90), 13, steel, 0);
            r.fill(r.circle(c, c, 7), 13, deep, 0);
            r.fill(r.circle(c, c, 4), 14, lit(0xc88fff), 0);
        }
        save(r, name);
        glowOf(r, name);
    }

    static void componentDepotArt(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 6, steel);
        //six component bays (gear, rod, casing, bearing, frame, precision part) and a transfer rail through the middle
        int[] tint = {0xd99d73, 0x8da1e3, 0x9ab0b8, 0xb8bcc4, 0xcdb98a, 0x6dd0f0};
        for(int i = 0; i < 6; i++){
            int x = 6 + (i % 3) * 29, y = i < 3 ? 6 : 58;
            r.fill(r.rect(x, y, 26, 32), 9, metal, 2);
            r.fill(r.rect(x + 4, y + 4, 18, 24), 4, deep, 0);
            r.fill(r.rect(x + 8, y + 10, 10, 12), 5, dull(tint[i]), 1);
        }
        r.fill(r.rect(2, 41, 92, 14), 10, steel, 2);
        r.fill(r.rect(36, 39, 24, 18), 12, metal, 2);
        r.fill(r.rect(44, 46, 8, 4), 13, lit(0xcdb98a), 0);
        save(r, name);
    }

    static void scanner(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        //a sensor plate with a short antenna mast, a processor housing beside it
        r.fill(r.circle(24, 26, 19), 9, steel, 3);
        for(int k = 0; k < 3; k++) r.fill(r.ring(24, 26, 5 + k * 5, 6 + k * 5), 10, metal, 0);
        r.fill(r.circle(24, 26, 4), 12, metal, 2);
        r.fill(r.circle(24, 26, 2), 13, lit(0x7fd8ff), 0);
        r.fill(r.rect(46, 8, 14, 48), 10, metal, 2);
        for(int y = 14; y < 52; y += 6) r.fill(r.rect(49, y, 8, 3), 11, deep, 0);
        r.fill(r.rect(50, 50, 6, 3), 12, lit(0x7fd8ff), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void jammerArt(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 6, steel);
        //an emitter socket ringed by six short radiator fins; the antenna ("-dish") turns on top
        for(int i = 0; i < 6; i++) r.fill(r.bar(c, c, 16, 12, 6, i * 60), 9, metal, 2);
        r.fill(r.circle(c, c, 15), 10, metal, 3);
        r.fill(r.circle(c, c, 9), 5, deep, 0);
        save(r, name);
        R dish = canvas(s);
        dish.fill(dish.oct(c - 14, c - 5, 28, 10, 4), 12, metal, 2);
        dish.fill(dish.rect((int)c - 2, (int)c - 14, 4, 28), 13, steel, 1);
        dish.fill(dish.circle(c, c, 4), 14, lit(0xb37aff), 1);
        save(dish, name + "-dish");
        save(dish.glowImage(), name + "-glow");
    }

    static void worldEngineArt(String name) throws IOException{
        int s = 6 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 10, 6, steel);
        //structural frame
        for(int k : new int[]{8, s - 20}){
            r.fill(r.rect(k, 8, 12, s - 16), 9, metal, 3);
            r.fill(r.rect(8, k, s - 16, 12), 9, metal, 3);
        }
        //chemical subsystem (left): two tanks; power interface (right): bus bars; control section (bottom)
        for(int y : new int[]{50, 110}){
            r.fill(r.circle(42, y + 16, 18), 11, metal, 3);
            r.fill(r.circle(42, y + 16, 8), 12, acc(0xb6dc4c), 1);
        }
        for(int y = 40; y < 152; y += 14) r.fill(r.rect(140, y, 34, 8), 11, copperGearM, 2);
        r.fill(r.rect(64, 150, 64, 26), 11, metal, 3);
        for(int x = 70; x < 124; x += 10) r.fill(r.rect(x, 157, 6, 4), 12, lit(0x7fe0ff), 0);
        //the energy core: a contained chamber, not a giant glowing disc
        r.fill(r.oct(c - 44, c - 50, 88, 88, 22), 12, steel, 4);
        r.fill(r.oct(c - 34, c - 40, 68, 68, 16), 13, metal, 3);
        r.fill(r.oct(c - 22, c - 28, 44, 44, 11), 6, deep, 1);
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c - 6, 24, 8, 4, i * 45 + 22.5f), 14, steel, 0);
        r.fill(r.circle(c, c - 6, 9), 8, lit(0xff9a3c), 1);
        save(r, name);
        glowOf(r, name);
    }

    static void latticeDrillArt(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //segmented lattice deployers from the edges into a round drill body that collects everything
        for(int i = 0; i < 4; i++){
            for(int k = 0; k < 3; k++) r.fill(r.bar(c, c, 22 + k * 9, 7, 10, i * 90), 9, k % 2 == 0 ? metal : steel, 1);
            r.fill(r.bar(c, c, 44, 4, 6, i * 90), 10, acc(0x7fe0ff), 0);
        }
        r.fill(r.circle(c, c, 21), 11, metal, 3);
        r.fill(r.circle(c, c, 14), 5, deep, 1);
        r.fill(r.circle(c, c, 7), 7, glass(0x7fe0ff), 1);
        save(r, name);
        //the node and drill head layers stay as they were (drawn in latticeDrill)
    }

    /** Precision Component: one assembly - a small gear meshed onto a chip on a shared bracket. */
    static void precisionComponentIcon(String name) throws IOException{
        R r = canvas(T);
        r.fill(r.rect(5, 18, 22, 6), 4, steel, 1);
        r.fill(r.ring(11, 13, 2, 5), 7, titaniumM, 1);
        for(int k = 0; k < 6; k++) r.fill(r.bar(11, 13, 4, 3, 2, k * 60), 7, titaniumM, 0);
        r.fill(r.rect(16, 8, 11, 11), 6, dull(0x3d4048), 1);
        r.fill(r.rect(19, 11, 5, 5), 7, acc(0x6dd0f0), 0);
        for(int y = 9; y < 18; y += 3) r.fill(r.rect(27, y, 2, 1), 5, brass, 0);
        save(r, name);
    }

    /** Experimental Core: a small advanced component held in a reinforced frame; purple and orange kept small. */
    static void experimentalCoreIcon(String name) throws IOException{
        R r = canvas(T);
        r.fill(r.oct(5, 5, 22, 22, 4), 6, steel, 2);
        r.fill(r.oct(9, 9, 14, 14, 3), 4, deep, 0);
        r.fill(r.oct(12, 12, 8, 8, 2), 6, lit(0xc88fff), 1);
        for(int[] c : new int[][]{{6, 6}, {23, 6}, {6, 23}, {23, 23}}) r.fill(r.rect(c[0], c[1], 3, 3), 7, acc(0xff9a3c), 0);
        save(r, name);
    }

    /** A weapon mount pad at a weapon's offset (world units: x right, y forward; 4 sprite pixels per unit). */
    static void mountPad(R r, float wx, float wy, float rad){
        float c = r.w / 2f, px = c + wx * 4, py = c - wy * 4;
        //side mounts sit on an outrigger arm back to the hull
        if(Math.abs(wx) > 0.5f) r.fill(r.rect((int)Math.min(px, c), (int)(py - rad * 0.45f), (int)Math.abs(px - c), (int)Math.max(3, rad * 0.9f)), 7.2f, tGrey, 1);
        r.fill(r.circle(px, py, rad), 8, tGrey, 1);
        r.fill(r.circle(px, py, rad - 2), 5, bore, 0);
    }

    /**
     * Unit silhouettes (art.md §17.2, §17.5): Reaper narrow with a wedge nose, Bulwark broad behind a frontal armour
     * slab, Rampart with a frontal shield arc and twin mounts, Annihilator long with an artillery cradle and side guns,
     * Colossus broad with mortar sponsons, Dreadnought a titan with separate pads for every weapon system.
     */
    static void hullStyle(R r, String name, int s, int hx, int hy, int hw, int hh){
        float c = s / 2f;
        switch(name){
            case "reaper" -> {
                r.fill(r.poly(new int[]{hx + 2, (int)c, hx + hw - 2}, new int[]{hy + 6, hy - 2, hy + 6}), 7, tGrey, 1);
                mountPad(r, 0, 0, Math.max(4, hw * 0.3f));
            }
            case "bulwark" -> {
                r.fill(r.rect(2, hy - 1, s - 4, hh / 5), 9, tGrey, 2);
                mountPad(r, 0, 0, hw * 0.34f);
            }
            case "rampart" -> {
                r.fill(r.and(r.ring(c, c + hh * 0.35f, hh * 0.58f, hh * 0.7f), r.rect(0, 0, s, (int)c - 4)), 9, tGrey, 2);
                mountPad(r, 7, -2, 6);
                mountPad(r, -7, -2, 6);
            }
            case "annihilator" -> {
                r.fill(r.rect((int)c - 8, hy + 4, 16, (int)c - hy), 7.5f, tGrey, 1);
                mountPad(r, 0, 0, 12);
                mountPad(r, 10, -6, 6);
                mountPad(r, -10, -6, 6);
            }
            case "colossus" -> {
                for(int x : new int[]{4, s - 26}) r.fill(r.oct(x, (int)c + 14, 22, 36, 5), 7.5f, tGrey, 2);
                mountPad(r, 0, 0, 16);
                mountPad(r, 12, -8, 8);
                mountPad(r, -12, -8, 8);
            }
            case "dreadnought" -> {
                //frontal shield plate, sponsons for the mortars, a jamming mast at the rear
                r.fill(r.oct(6, hy - 2, s - 12, 18, 6), 9.5f, tGrey, 3);
                for(int x : new int[]{6, s - 38}) r.fill(r.oct(x, (int)c + 50, 32, 36, 6), 7.5f, tGrey, 2);
                mountPad(r, 13, 4, 12);
                mountPad(r, -13, 4, 12);
                mountPad(r, 0, -10, 10);
                mountPad(r, 15, -18, 9);
                mountPad(r, -15, -18, 9);
                r.fill(r.rect((int)c - 2, s - 30, 4, 14), 9, tGrey, 1);
                r.fill(r.circle(c, s - 31, 3), 10, lit(0xb37aff), 0);
            }
            default -> mountPad(r, 0, 0, Math.max(4, hw * 0.32f));
        }
    }

    /**
     * Support drones (art.md §17.1), each built round its job: the logistics drone round a cargo box, the repair
     * drone round a tool arm with an emitter head, the shield drone round an emitter dish with focusing nubs.
     */
    static void supportDrone(String name, int kind) throws IOException{
        int s = 48;
        float c = s / 2f;
        R r = canvas(s);
        float[][] rotors = kind == 0 ? new float[][]{{9, 9}, {39, 9}, {9, 39}, {39, 39}}
            : kind == 1 ? new float[][]{{8, 26}, {40, 26}}
            : new float[][]{{c, 7}, {9, 34}, {39, 34}};
        for(float[] ro : rotors){
            r.fill(r.bar(c, c, 0, (float)Math.hypot(ro[0] - c, ro[1] - c), 3, (float)Math.toDegrees(Math.atan2(c - ro[1], ro[0] - c))), 4, steel, 0);
            r.dome(r.ring(ro[0], ro[1], 3.5f, 6.5f), 4, 2.5f, metal);
            r.fill(r.circle(ro[0], ro[1], 3.5f), 2, deep, 0);
        }
        boolean[] cell;
        if(kind == 0){
            r.fill(r.rect(13, 12, 22, 24), 7, metal, 2);
            r.fill(r.rect(16, 15, 16, 18), 8, dull(0xb08a5a), 1);
            for(int y = 19; y < 32; y += 5) r.fill(r.rect(16, y, 16, 1), 8.5f, dull(0x7a5f3e), 0);
            cell = r.rect(13, 12, 4, 24);
        }else if(kind == 1){
            r.fill(r.circle(c, c + 4, 10), 7, metal, 2);
            r.fill(r.rect((int)c - 2, 6, 4, 14), 8, steel, 1);
            r.fill(r.circle(c, 7, 4), 9, steel, 1);
            r.fill(r.circle(c, 7, 2), 10, lit(0x98ffa9), 0);
            cell = r.circle(c, c + 4, 4);
        }else{
            r.fill(r.circle(c, c + 2, 11), 7, metal, 2);
            r.fill(r.ring(c, c + 2, 5, 8), 8, steel, 1);
            r.fill(r.circle(c, c + 2, 4), 9, lit(0x7fd8ff), 1);
            for(int i = 0; i < 3; i++) r.fill(r.bar(c, c + 2, 10, 4, 3, i * 120 + 30), 8, steel, 0);
            cell = r.minus(r.circle(c, c + 2, 11), r.circle(c, c + 2, 9));
        }
        save(r, name);
        cell(name, s, cell, 8);
    }

    // ---- art pass batch 2 (art.md §5.2): metallurgy, ore processing and glass ----

    static final Mat molten = lit(0xffb46b), sandM = dull(0xd6c08a), coalM = dull(0x3a3a40);

    /**
     * "-heat" for heat consumers: vanilla's DrawHeatInput draws this region once per side that receives heat, rotated to
     * that side, so it must be the glow of the input edge (the right edge, rotation 0), never a shape inside the block.
     * The heated chamber itself lights up through the "-glow" layer. (The mask is kept for the call sites' readability.)
     */
    static void heatLayer(R r, String name, boolean[] mask) throws IOException{
        edgeGlow(name + "-heat", r.w / T);
    }

    /** "-bottom": the dark floor under a liquid window (the liquid is drawn between it and the sprite). */
    static void liquidBottom(String name, int s) throws IOException{
        R b = canvas(s);
        b.fill(b.rect(0, 0, s, s), 2, deep, 0);
        for(int x = 6; x < s - 4; x += 8) b.fill(b.rect(x, 3, 2, s - 6), 3, deep, 0);
        save(b, name + "-bottom");
    }

    /** Liquid window: a steel lip, transparent inside so the liquid layer shows. */
    static void liquidWindow(R r, boolean[] m, float top){
        r.fill(r.minus(r.grow(m, 2), m), top, steel, 1);
        r.clear(m);
    }

    /** A square feed hopper with its material showing inside. */
    static void hopper(R r, int x, int y, int size, float top, Mat content){
        r.fill(r.oct(x, y, size, size, 2), top, metal, 2);
        r.fill(r.rect(x + 3, y + 3, size - 6, size - 6), top - 6, deep, 0);
        r.fill(r.rect(x + 4, y + 4, size - 8, size - 8), top - 5, content, 1);
    }

    /** Methane Pyrolysis Reactor (3×3, heat): a heavy round reactor, methane feed in, hydrogen stack up, carbon hopper out. */
    static void pyrolysisReactor(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 4, steel);
        //methane feed pipe from the top right into the vessel
        r.fill(r.rect(56, 6, 34, 8), 8, metal, 2);
        r.fill(r.rect(56, 6, 8, 22), 8, metal, 2);
        //the vessel and its dark reaction chamber
        r.fill(r.circle(42, 52, 34), 10, metal, 3);
        r.fill(r.circle(42, 52, 24), 4, deep, 1);
        r.fill(r.ring(42, 52, 14, 18), 5, molten, 0);
        //hydrogen stack and carbon hopper
        r.fill(r.circle(80, 30, 9), 14, steel, 2);
        r.fill(r.circle(80, 30, 5), 6, deep, 0);
        hopper(r, 70, 70, 20, 11, coalM);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.circle(42, 52, 24));
    }

    /** Silicon Reduction Furnace (3×3, heat): a long horizontal hot chamber, sand hoppers above, silicon collected below. */
    static void reductionFurnace(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        for(int x : new int[]{10, 38, 66}) hopper(r, x, 4, 20, 11, sandM);
        r.fill(r.oct(6, 32, 84, 32, 10), 12, steel, 3);
        r.fill(r.oct(12, 38, 72, 20, 7), 4, deep, 1);
        r.fill(r.rect(18, 45, 60, 6), 5, molten, 0);
        //collection trough with cast silicon
        r.fill(r.rect(10, 70, 76, 18), 6, steel, 2);
        r.fill(r.rect(14, 74, 68, 10), 3, deep, 0);
        for(int x = 18; x < 80; x += 12) r.fill(r.rect(x, 76, 8, 6), 4, dull(0x53565f), 1);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.oct(12, 38, 72, 20, 7));
    }

    /** Soda Glass Furnace (2×2, heat): a round crucible of molten glass sunk in a grey housing, a sand feed in the corner. */
    static void glassFurnace(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        r.fill(r.circle(34, 36, 22), 10, steel, 3);
        r.fill(r.circle(34, 36, 15), 3, deep, 1);
        r.fill(r.circle(34, 36, 10), 4, lit(0xbfe9ff), 1);
        hopper(r, 2, 2, 16, 12, sandM);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.circle(34, 36, 15));
    }

    /** Glass Kiln (2×2, heat): a rectangular fusing bed with heating bars and a reinforced pane in a titanium frame. */
    static void glassKiln(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        r.fill(r.rect(6, 10, 52, 44), 10, metal, 3);
        r.fill(r.rect(11, 15, 42, 34), 3, deep, 1);
        for(int y = 18; y < 48; y += 7) r.fill(r.rect(13, y, 38, 2), 4, molten, 0);
        r.fill(r.rect(20, 23, 24, 18), 7, titaniumM, 1);
        r.fill(r.rect(23, 26, 18, 12), 8, glass(0xbfe9ff), 0);
        for(int y : new int[]{2, 57}) r.fill(r.rect(14, y, 36, 5), 8, metal, 1);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.rect(11, 15, 42, 34));
    }

    /**
     * Distillation Column (3×3, oil window): a round separation body with radial trays over the oil, naphtha leaving
     * on the left, methane on the right (their fixed sides), and a sulfur recovery tray at the bottom.
     */
    static void distillationColumn(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //side ports: naphtha (left) and methane (right)
        r.fill(r.rect(0, 38, 16, 20), 9, steel, 2);
        r.fill(r.rect(80, 38, 16, 20), 9, steel, 2);
        r.fill(r.rect(2, 44, 6, 8), 10, acc(0xd9a45a), 0);
        r.fill(r.rect(88, 44, 6, 8), 10, acc(0xc4b3e6), 0);
        //the column: a ring of trays around an oil window
        r.fill(r.circle(c, c - 4, 34), 12, steel, 3);
        liquidWindow(r, r.circle(c, c - 4, 24), 13);
        for(int i = 0; i < 6; i++) r.fill(r.bar(c, c - 4, 6, 20, 3, i * 60 + 15), 13, metal, 0);
        r.fill(r.circle(c, c - 4, 6), 14, metal, 2);
        //sulfur tray
        r.fill(r.rect(30, 80, 36, 12), 8, steel, 2);
        r.fill(r.rect(34, 83, 28, 6), 5, acc(0xe8d44d), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    /** Alloy Furnace (3×3, heat): a thick heat-resistant casing round a big crucible, fed by three separate chutes. */
    static void alloyFurnace(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 8, 6, steel);
        r.fill(r.oct(12, 16, 72, 72, 16), 12, metal, 4);
        r.fill(r.oct(24, 28, 48, 48, 12), 4, deep, 1);
        r.fill(r.oct(38, 42, 20, 20, 6), 5, molten, 0);
        r.fill(r.ring(48, 52, 15, 18), 5, steel, 0);
        //three feed chutes for nickel, chromium and cobalt
        int[] metals = {0xc5c9a1, 0xb7c0cc, 0x6d8fd6};
        for(int i = 0; i < 3; i++){
            int x = 14 + i * 26;
            r.fill(r.rect(x, 0, 16, 14), 14, metal, 2);
            r.fill(r.rect(x + 4, 3, 8, 7), 6, dull(metals[i]), 0);
        }
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.oct(24, 28, 48, 48, 12));
    }

    /** Induction Furnace (2×2): a small melting pot wound with copper induction coils, two power terminals; no flame. */
    static void inductionFurnace(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, metal);
        r.fill(r.circle(c, c, 26), 8, steel, 3);
        for(int k = 0; k < 3; k++) r.fill(r.ring(c, c, 11 + k * 5, 14 + k * 5), 10 - k, copperGearM, 1);
        r.fill(r.circle(c, c, 9), 4, deep, 1);
        r.fill(r.circle(c, c, 5), 5, lit(0xff6a3d), 0);
        for(int x : new int[]{3, 51}) r.fill(r.rect(x, 3, 10, 8), 11, steel, 2);
        save(r, name);
        glowOf(r, name);
    }

    /** Arc Furnace (3×3): a strong square frame round an arc chamber with three electrodes and small bright tips. */
    static void arcFurnace(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.minus(r.rect(6, 6, 84, 84), r.rect(18, 18, 60, 60)), 12, steel, 3);
        r.fill(r.circle(c, c, 28), 9, metal, 3);
        r.fill(r.circle(c, c, 20), 3, deep, 1);
        for(int i = 0; i < 3; i++){
            double a = Math.toRadians(i * 120 - 90);
            float ex = c + (float)Math.cos(a) * 10, ey = c - (float)Math.sin(a) * 10;
            r.fill(r.circle(ex, ey, 5.5f), 14, dull(0x4a4b53), 2);
            r.fill(r.circle(ex, ey, 2), 15, lit(0xf2e878), 0);
            r.fill(r.bar(c, c, 22, 22, 6, i * 120 - 90), 13, steel, 1);
        }
        save(r, name);
        glowOf(r, name);
    }

    /** Sintering Furnace (2×2): a dark sintering chamber under a heavy compression lid ("-press"), a tungsten strip. */
    static void sinteringFurnace(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, metal);
        r.fill(r.rect(8, 8, 48, 48), 3, deep, 1);
        r.fill(r.rect(12, 28, 40, 8), 4, lit(0xff6a3d), 0);
        for(int x : new int[]{2, 56}) r.fill(r.rect(x, 6, 6, 52), 10, steel, 2);
        save(r, name);
        glowOf(r, name);

        R p = canvas(s);
        p.fill(p.oct(10, 10, 44, 44, 4), 14, steel, 3);
        p.fill(p.oct(16, 16, 32, 32, 3), 16, metal, 2);
        p.fill(p.rect(16, 30, 32, 4), 17, dull(0x50525e), 0);
        save(p, name + "-press");
    }

    /** Alumina Refinery (3×3, heat, liquid window): vessels, not plates. A digester, a small autoclave, and a settling tank. */
    static void aluminaRefinery(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 4, steel);
        //digester (top left) and autoclave (top right)
        r.fill(r.circle(30, 30, 24), 12, metal, 3);
        r.fill(r.circle(30, 30, 12), 5, deep, 1);
        r.fill(r.circle(30, 30, 7), 6, molten, 0);
        r.fill(r.circle(72, 24, 16), 11, metal, 3);
        r.fill(r.circle(72, 24, 6), 13, steel, 1);
        //connecting pipe and the settling tank with its caustic liquor
        r.fill(r.rect(50, 26, 12, 6), 9, steel, 1);
        r.fill(r.oct(8, 58, 80, 32, 6), 10, metal, 3);
        liquidWindow(r, r.rect(14, 64, 68, 20), 11);
        for(int x = 26; x < 80; x += 14) r.fill(r.rect(x, 63, 3, 22), 12, steel, 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        heatLayer(r, name, r.circle(30, 30, 12));
    }

    /** Aluminium Smelter (3×3): a line of three reduction pots with carbon anodes, a copper bus bar, light aluminium out. */
    static void aluminiumSmelter(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.rect(6, 4, 66, 8), 13, copperGearM, 2);
        for(int i = 0; i < 3; i++){
            int y = 16 + i * 26;
            r.fill(r.rect(6, y, 66, 22), 9, steel, 2);
            r.fill(r.rect(10, y + 4, 58, 14), 3, deep, 0);
            for(int x = 13; x < 64; x += 13) r.fill(r.rect(x, y + 5, 9, 12), 7, dull(0x3a3a40), 2);
            r.fill(r.rect(14 + i * 6, y - 4, 4, 6), 12, copperGearM, 0);
        }
        //output section: cast aluminium
        r.fill(r.rect(76, 14, 16, 70), 8, steel, 2);
        for(int y = 20; y < 80; y += 14) r.fill(r.rect(79, y, 10, 9), 10, dull(0xd8dde3), 1);
        r.fill(r.rect(36, 90, 6, 3), 12, lit(0x9fd8ff), 0);
        save(r, name);
        glowOf(r, name);
    }

    /** Mineral Separator (2×2, slag window): a round separator bowl over the slag, paddles ("-rotator"), an acid tank, outlets. */
    static void mineralSeparator(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.circle(c - 2, c + 2, 24), 10, steel, 3);
        liquidWindow(r, r.circle(c - 2, c + 2, 17), 11);
        //acid tank and outlets
        r.fill(r.oct(48, 4, 14, 22, 4), 12, metal, 2);
        r.fill(r.rect(51, 8, 8, 12), 13, acc(0xe8d44d), 0);
        for(int x : new int[]{8, 22, 36}) r.fill(r.rect(x, 57, 8, 6), 8, steel, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);

        R rot = canvas(s);
        for(int i = 0; i < 3; i++) rot.fill(rot.bar(c, c, 3, 15, 4, i * 120), 12, metal, 1);
        rot.fill(rot.circle(c, c, 4), 13, steel, 1);
        save(rot, name + "-rotator");
    }

    /** Chloride Titanium Plant (3×3, heat): sand and coal hoppers, a heavy chlorinator, a closed stack and the titanium tray. */
    static void chloridePlant(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, steel);
        hopper(r, 4, 4, 22, 12, sandM);
        hopper(r, 4, 30, 22, 12, coalM);
        r.fill(r.circle(54, 48, 28), 12, metal, 4);
        r.fill(r.circle(54, 48, 17), 4, deep, 1);
        r.fill(r.circle(54, 48, 9), 5, molten, 0);
        //closed exhaust stack and the titanium output
        r.fill(r.circle(80, 14, 10), 15, steel, 2);
        r.fill(r.circle(80, 14, 5), 7, deep, 0);
        r.fill(r.rect(30, 80, 56, 12), 8, steel, 2);
        r.fill(r.rect(34, 83, 48, 6), 9, titaniumM, 1);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.circle(54, 48, 17));
    }

    /** Electrowinning Cell (3×3, acid window): a long bath crossed by many parallel electrode plates between two bus bars. */
    static void electrowinningCell(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.rect(6, 14, 84, 68), 9, steel, 3);
        liquidWindow(r, r.rect(12, 20, 72, 56), 10);
        for(int i = 0; i < 9; i++){
            int x = 15 + i * 8;
            r.fill(r.rect(x, 18, 3, 60), 12, i % 2 == 0 ? copperGearM : dull(0x8c92ac), 1);
        }
        for(int y : new int[]{4, 84}) r.fill(r.rect(10, y, 76, 7), 13, copperGearM, 2);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    /** Ore Crusher (2×2): opposed crusher jaws ("-press") over a dark crushing gap, a feed hopper above, fragments below. */
    static void oreCrusher(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        hopper(r, 20, 0, 24, 12, dull(0x8a7f72));
        r.fill(r.rect(10, 22, 44, 26), 2, deep, 1);
        //fragments falling out at the bottom
        r.fill(r.rect(18, 50, 28, 12), 5, steel, 1);
        for(int[] f : new int[][]{{22, 53}, {30, 55}, {38, 52}, {26, 58}, {35, 58}}) r.fill(r.rect(f[0], f[1], 3, 3), 7, dull(0x8a7f72), 1);
        save(r, name);
        glowOf(r, name);

        R p = canvas(s);
        for(int x : new int[]{4, 38}){
            p.fill(p.oct(x, 18, 22, 32, 4), 12, metal, 3);
            for(int y = 24; y < 46; y += 6) p.fill(p.rect(x == 4 ? 22 : 38, y, 4, 3), 13, steel, 0);
        }
        save(p, name + "-press");
    }

    /** Ore Washer (2×2, water window): a shallow wash chamber with grates, a water channel in, mineral bins out. */
    static void oreWasher(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        r.fill(r.rect(0, 10, 14, 10), 9, steel, 2);
        r.fill(r.rect(2, 13, 10, 4), 10, acc(0x5fb4ff), 0);
        r.fill(r.rect(10, 6, 50, 36), 9, steel, 3);
        liquidWindow(r, r.rect(15, 11, 40, 26), 10);
        for(int x = 19; x < 54; x += 6) r.fill(r.rect(x, 10, 2, 28), 11, metal, 0);
        int[] ores = {0xc5c9a1, 0x6d8fd6, 0xb7c0cc};
        for(int i = 0; i < 3; i++){
            int x = 8 + i * 17;
            r.fill(r.rect(x, 46, 14, 14), 8, metal, 2);
            r.fill(r.rect(x + 3, 49, 8, 8), 3, deep, 0);
            r.fill(r.rect(x + 4, 50, 6, 6), 4, dull(ores[i]), 1);
        }
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    // ---- art pass batch 3 (art.md §5.3-5.4): electronics, heat and chemical processes ----

    /** A round vessel seen from above: a body with a lid ring and a centre fitting. */
    static void vessel(R r, float cx, float cy, float rad, float top, Mat body){
        r.fill(r.circle(cx, cy, rad), top, body, 3);
        r.fill(r.ring(cx, cy, rad * 0.55f, rad * 0.7f), top + 1, steel, 1);
        r.fill(r.circle(cx, cy, Math.max(2f, rad * 0.22f)), top + 2, metal, 1);
    }

    /** Short liquid port stub on one edge: 0 right, 1 top, 2 left, 3 bottom. */
    static void stub(R r, int side, int at, int width, Mat m){
        int s = r.w, d = 8;
        boolean[] b = switch(side){
            case 0 -> r.rect(s - d, at, d, width);
            case 1 -> r.rect(at, 0, width, d);
            case 2 -> r.rect(0, at, d, width);
            default -> r.rect(at, s - d, width, d);
        };
        r.fill(b, 9, steel, 1);
        boolean[] in = switch(side){
            case 0 -> r.rect(s - 5, at + 2, 4, width - 4);
            case 1 -> r.rect(at + 2, 1, width - 4, 4);
            case 2 -> r.rect(1, at + 2, 4, width - 4);
            default -> r.rect(at + 2, s - 5, width - 4, 4);
        };
        r.fill(in, 10, m, 0);
    }

    // §5.3 silicon and electronics

    static void siliconRefinery(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        r.fill(r.oct(10, 10, 44, 40, 6), 10, steel, 3);
        liquidWindow(r, r.oct(16, 16, 32, 28, 4), 11);
        r.fill(r.rect(22, 26, 20, 8), 12, dull(0x3d4a5c), 2);
        stub(r, 1, 6, 10, acc(0xdfe882));
        stub(r, 1, 46, 10, acc(0xe8edf0));
        r.fill(r.rect(26, 56, 12, 6), 8, steel, 1);
        r.fill(r.rect(28, 58, 8, 2), 9, lit(0x6d7a99), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void chipFabricator(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        r.fill(r.rect(8, 16, 48, 40), 8, metal, 2);
        r.fill(r.circle(32, 38, 15), 5, deep, 1);
        r.fill(r.circle(32, 38, 12), 6, dull(0x5b6273), 1);
        for(int[] t : new int[][]{{22, 36, 20, 2}, {31, 28, 2, 20}, {24, 44, 8, 2}}) r.fill(r.rect(t[0], t[1], t[2], t[3]), 7, acc(0x6dff9a), 0);
        //the tool head on a gantry over the wafer
        r.fill(r.rect(4, 6, 56, 8), 12, steel, 2);
        r.fill(r.oct(26, 4, 12, 14, 3), 14, metal, 2);
        r.fill(r.rect(31, 16, 2, 6), 13, lit(0x6dff9a), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void wireDrawer(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 4, steel);
        slab(r, 0, 16, s, 32, 3, 6, metal);
        //a narrow drawing channel with three fine roller pairs
        r.fill(r.rect(4, 30, 42, 4), 4, deep, 0);
        r.fill(r.rect(4, 31, 42, 2), 5, copperGearM, 0);
        for(int x : new int[]{10, 20, 30}){
            r.fill(r.circle(x, 25, 4), 9, metal, 2);
            r.fill(r.circle(x, 39, 4), 9, metal, 2);
        }
        //the output coil
        r.fill(r.circle(52, 32, 10), 9, steel, 2);
        r.fill(r.ring(52, 32, 4, 8), 10, copperGearM, 1);
        r.fill(r.circle(52, 32, 2), 8, deep, 0);
        save(r, name);
        glowOf(r, name);
    }

    static void boardPrinter(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        r.fill(r.rect(8, 14, 48, 36), 4, deep, 1);
        r.fill(r.rect(12, 18, 40, 28), 5, dull(0x2f6b45), 1);
        for(int[] t : new int[][]{{16, 22, 18, 3}, {32, 22, 3, 14}, {16, 38, 30, 3}, {44, 26, 3, 15}}) r.fill(r.rect(t[0], t[1], t[2], t[3]), 6, copperGearM, 0);
        //print head on its rail
        for(int x : new int[]{2, 56}) r.fill(r.rect(x, 8, 6, 48), 10, steel, 2);
        r.fill(r.rect(2, 26, 60, 6), 12, metal, 2);
        r.fill(r.oct(36, 22, 12, 14, 3), 14, steel, 2);
        r.fill(r.rect(41, 33, 2, 2), 15, lit(0x3f9a5c), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void controlAssembler(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        //chip, board and magnet bays down the left
        int[] bay = {0x6dd0f0, 0x2f6b45, 0xb05454};
        for(int i = 0; i < 3; i++){
            r.fill(r.rect(3, 6 + i * 18, 14, 14), 9, steel, 2);
            r.fill(r.rect(6, 9 + i * 18, 8, 8), 5, dull(bay[i]), 1);
        }
        //the controller housing with its drive motor
        r.fill(r.oct(22, 8, 38, 48, 5), 11, steel, 3);
        r.fill(r.rect(27, 14, 28, 20), 13, metal, 2);
        r.fill(r.circle(41, 45, 7), 13, metal, 2);
        r.fill(r.circle(41, 45, 3), 14, copperGearM, 1);
        for(int x = 30; x < 52; x += 6) r.fill(r.rect(x, 22, 3, 4), 14, lit(0x5ab8ff), 0);
        save(r, name);
        glowOf(r, name);
    }

    // §5.4 heat

    static void combustionHeater(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, steel);
        r.fill(r.circle(28, 32, 20), 10, metal, 3);
        r.fill(r.circle(28, 32, 12), 4, deep, 1);
        r.fill(r.circle(28, 32, 7), 5, lit(0xff8a3d), 0);
        //methane feed and the exhaust recess
        r.fill(r.rect(4, 4, 14, 8), 9, metal, 1);
        r.fill(r.rect(6, 6, 10, 4), 10, acc(0xc4b3e6), 0);
        r.fill(r.oct(6, 48, 14, 12, 3), 8, metal, 2);
        r.fill(r.rect(9, 51, 8, 6), 3, deep, 0);
        save(r, name);
        heatParts(name, 2, 0xff8a3d);
    }

    static void electricHeater(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, metal);
        r.fill(r.rect(8, 8, 40, 48), 3, deep, 1);
        for(int y = 12; y < 52; y += 8) r.cylH(10, y, 36, 5, 4, 2, acc(0xd97a52));
        r.fill(r.rect(10, 4, 4, 6), 10, lit(0x8fd8ff), 0);
        r.fill(r.rect(18, 4, 4, 6), 10, steel, 0);
        save(r, name);
        heatParts(name, 2, 0xff8a3d);
    }

    static void geothermalTap(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        //a broad collector plate pinned to the ground at four feet
        slab(r, 0, 0, s, s, 2, 3, steel);
        for(int[] f : new int[][]{{2, 2}, {50, 2}, {2, 50}, {50, 50}}) r.fill(r.rect(f[0], f[1], 12, 12), 5, dull(0x3a3a40), 1);
        r.fill(r.oct(10, 10, 44, 44, 10), 7, metal, 3);
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c, 10, 12, 4, i * 45), 9, steel, 1);
        r.fill(r.circle(c, c, 8), 11, metal, 2);
        r.fill(r.circle(c, c, 4), 9, lit(0xff6a3d), 0);
        save(r, name);
        heatParts(name, 2, 0xff6a3d);
    }

    static void heatExchanger(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //collection channels from the back and sides into a finned core, out of the front (right)
        r.fill(r.rect(4, 40, 40, 16), 8, steel, 2);
        r.fill(r.rect(40, 4, 16, 40), 8, steel, 2);
        r.fill(r.rect(40, 52, 16, 40), 8, steel, 2);
        r.fill(r.oct(30, 30, 40, 36, 6), 11, steel, 3);
        for(int x = 34; x < 68; x += 5) r.fill(r.rect(x, 33, 3, 30), 13, metal, 1);
        r.fill(r.rect(70, 40, 26, 16), 12, metal, 3);
        r.fill(r.rect(74, 45, 18, 6), 13, acc(0xff8a3d), 0);
        save(r, name);
        heatParts(name, 3, 0xff8a3d);
    }

    static void thermalStorage(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, steel);
        //layered insulation round the heat core
        r.fill(r.circle(c, c, 26), 9, metal, 3);
        r.fill(r.circle(c, c, 21), 10, dull(0x6d6560), 2);
        r.fill(r.circle(c, c, 16), 11, metal, 2);
        r.fill(r.circle(c, c, 9), 6, deep, 1);
        r.fill(r.rect(30, 26, 4, 12), 7, lit(0xff8a3d), 0);
        save(r, name);
        heatParts(name, 2, 0xff8a3d);
    }

    // §5.4 chemistry

    static void steamCracker(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //thermal supports either side of the cracking furnace, a radiant tube coil inside
        for(int x : new int[]{4, 80}) r.fill(r.rect(x, 10, 12, 76), 10, steel, 3);
        r.fill(r.oct(18, 16, 60, 64, 8), 12, steel, 3);
        r.fill(r.rect(24, 22, 48, 52), 4, deep, 1);
        for(int y = 28; y < 70; y += 9) r.cylH(28, y, 40, 5, 5, 2, molten);
        stub(r, 1, 40, 16, acc(0xd9a45a));
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        heatLayer(r, name, r.rect(24, 22, 48, 52));
    }

    static void catalystPlant(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        vessel(r, 26, 34, 18, 10, metal);
        liquidWindow(r, r.circle(26, 34, 7), 12);
        //replaceable catalyst cartridges
        for(int y : new int[]{8, 26, 44}){
            r.fill(r.rect(48, y, 12, 14), 11, steel, 2);
            r.fill(r.rect(51, y + 3, 6, 8), 12, acc(0xb07ad6), 1);
        }
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void polymerizationReactor(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 10, 5, steel);
        //sealed pressure housing with bolted ring, catalyst service hatch
        r.fill(r.circle(c, c, 40), 10, metal, 4);
        for(int i = 0; i < 12; i++) r.fill(r.bar(c, c, 34, 4, 4, i * 30), 12, steel, 0);
        r.fill(r.circle(c, c, 26), 12, steel, 3);
        liquidWindow(r, r.circle(c, c, 15), 13);
        r.fill(r.rect(72, 72, 20, 20), 11, metal, 2);
        r.fill(r.rect(77, 77, 10, 10), 12, acc(0xb07ad6), 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void methanolSynthesizer(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //a thick horizontal pressure vessel with domed ends and two gas inlets
        r.fill(r.oct(8, 28, 80, 40, 18), 12, steel, 4);
        r.fill(r.oct(14, 34, 68, 28, 13), 13, metal, 3);
        liquidWindow(r, r.rect(32, 42, 32, 12), 14);
        stub(r, 1, 18, 14, acc(0xb6dc4c));
        stub(r, 1, 64, 14, acc(0xe8edf0));
        //pressure lock and gauge
        r.fill(r.rect(36, 76, 24, 14), 11, steel, 2);
        r.fill(r.circle(48, 83, 4), 12, lit(0xff5a5a), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void olefinReactor(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //conversion chamber, propylene out left, wastewater out right
        r.fill(r.rect(0, 38, 20, 20), 9, steel, 2);
        r.fill(r.rect(76, 38, 20, 20), 9, steel, 2);
        r.fill(r.rect(2, 44, 8, 8), 10, acc(0xe6d3a8), 0);
        r.fill(r.rect(86, 44, 8, 8), 10, acc(0x6f7f5a), 0);
        r.fill(r.oct(18, 14, 60, 68, 10), 12, steel, 3);
        liquidWindow(r, r.rect(26, 24, 44, 26), 13);
        for(int x = 30; x < 68; x += 8) r.fill(r.rect(x, 56, 4, 18), 13, metal, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void epoxyPlant(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //mixing kettle (left) and curing compartment (right)
        r.fill(r.circle(20, 24, 16), 10, steel, 3);
        liquidWindow(r, r.circle(20, 24, 9), 11);
        r.fill(r.rect(38, 8, 22, 48), 10, steel, 3);
        r.fill(r.rect(42, 14, 14, 36), 4, deep, 0);
        for(int y = 17; y < 48; y += 8) r.fill(r.rect(43, y, 12, 5), 5, acc(0xe0a94a), 1);
        r.fill(r.rect(10, 46, 22, 10), 8, metal, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void rubberPlant(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //mixing chamber (top left), long processing channel, heated curing area (right), rubber out
        vessel(r, 24, 24, 18, 11, steel);
        r.fill(r.rect(8, 48, 80, 14), 9, steel, 2);
        liquidWindow(r, r.rect(12, 52, 52, 6), 10);
        r.fill(r.oct(56, 8, 34, 36, 5), 12, steel, 3);
        r.fill(r.rect(62, 14, 22, 24), 4, deep, 0);
        for(int y = 17; y < 36; y += 6) r.fill(r.rect(64, y, 18, 2), 5, molten, 0);
        r.fill(r.rect(64, 70, 26, 20), 9, metal, 2);
        r.fill(r.rect(68, 74, 18, 12), 10, dull(0x2a2a30), 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void acrylonitrileReactor(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //three feed connections into the main reactor, catalyst service bay below it
        for(int x : new int[]{14, 40, 66}) r.fill(r.rect(x, 0, 16, 18), 10, metal, 2);
        int[] feeds = {0xb5a8d8, 0xd8eef7, 0xe8edf0};
        for(int i = 0; i < 3; i++) r.fill(r.rect(18 + i * 26, 4, 8, 8), 11, acc(feeds[i]), 0);
        r.fill(r.circle(c, 50, 30), 12, metal, 4);
        liquidWindow(r, r.circle(c, 50, 16), 13);
        r.fill(r.rect(30, 80, 36, 12), 10, metal, 2);
        r.fill(r.rect(36, 83, 24, 6), 11, acc(0xc9b8e8), 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void carbonFiberFurnace(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, steel);
        //a long enclosed oxidation/carbonising path, heated in the middle, fibre spools out on the right
        r.fill(r.rect(4, 30, 70, 36), 12, metal, 3);
        r.fill(r.rect(10, 38, 58, 20), 4, deep, 1);
        r.fill(r.rect(26, 42, 26, 12), 5, molten, 0);
        for(int x = 12; x < 66; x += 6) r.fill(r.rect(x, 47, 4, 2), 6, dull(0x2a2a30), 0);
        for(int y : new int[]{18, 66}){
            r.fill(r.circle(84, y + 6, 9), 11, steel, 2);
            r.fill(r.circle(84, y + 6, 5), 12, dull(0x2a2a30), 1);
        }
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.rect(10, 38, 58, 20));
    }

    static void compositePress(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        //the layered laminate bed
        r.fill(r.rect(8, 10, 48, 44), 3, deep, 1);
        for(int i = 0; i < 5; i++) r.fill(r.rect(12, 14 + i * 8, 40, 5), 4 + i * 0.5f, dull(i % 2 == 0 ? 0x2a2a30 : 0x8fb8ff), 0);
        save(r, name);
        glowOf(r, name);
        R p = canvas(s);
        p.fill(p.oct(4, 18, 56, 28, 4), 14, metal, 3);
        p.fill(p.rect(10, 28, 44, 8), 16, steel, 1);
        save(p, name + "-press");
    }

    static void sulfuricPlant(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //sulfur burner (left), absorption tower (right), liquid line between
        r.fill(r.oct(4, 14, 24, 36, 4), 10, steel, 3);
        r.fill(r.rect(8, 20, 16, 24), 4, deep, 0);
        r.fill(r.rect(10, 28, 12, 8), 5, molten, 0);
        r.fill(r.circle(46, 32, 15), 11, metal, 3);
        liquidWindow(r, r.circle(46, 32, 8), 12);
        r.fill(r.rect(26, 28, 8, 6), 9, steel, 1);
        save(r, name);
        heatParts(name, 2, 0xe8b84a);
        liquidBottom(name, s);
    }

    static void batteryPlant(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //a row of cells being filled: lead plates in sealed cases, the electrolyte liner along the top
        r.fill(r.rect(6, 4, 52, 10), 10, steel, 2);
        r.fill(r.rect(9, 7, 46, 4), 11, acc(0xe8d44d), 0);
        for(int i = 0; i < 4; i++){
            int x = 6 + i * 13;
            r.fill(r.rect(x, 20, 11, 36), 9, dull(0x3d4048), 2);
            for(int y = 24; y < 52; y += 5) r.fill(r.rect(x + 2, y, 7, 2), 10, dull(0x8c92ac), 0);
        }
        r.fill(r.rect(28, 58, 8, 3), 11, lit(0xb7e07a), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void fuelRodFabricator(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        //a straight rod channel through a cladding head
        r.fill(r.rect(2, 26, 60, 12), 6, deep, 1);
        r.fill(r.rect(4, 30, 22, 4), 7, dull(0x9a9e5a), 0);
        r.fill(r.rect(38, 30, 22, 4), 7, dull(0x9fa8b0), 1);
        r.fill(r.oct(22, 12, 20, 40, 4), 12, metal, 3);
        r.fill(r.rect(27, 28, 10, 8), 13, lit(0xff7ad1), 0);
        for(int y : new int[]{4, 52}) r.fill(r.rect(8, y, 48, 8), 9, metal, 2);
        save(r, name);
        glowOf(r, name);
    }

    static void reprocessingPlant(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, steel);
        //sealed stages: dissolver, extraction column, filtration
        vessel(r, 28, 30, 22, 11, metal);
        liquidWindow(r, r.circle(28, 30, 9), 13);
        r.fill(r.oct(56, 8, 32, 52, 6), 11, metal, 3);
        for(int y = 14; y < 56; y += 8) r.fill(r.rect(60, y, 24, 4), 12, steel, 1);
        r.fill(r.rect(8, 64, 80, 26), 10, metal, 3);
        for(int x = 14; x < 84; x += 10) r.fill(r.rect(x, 69, 6, 16), 11, dull(0x4a3a48), 1);
        r.fill(r.rect(46, 52, 6, 14), 9, steel, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void coolingTower(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 4, metal);
        //basin around a broad fan shroud
        r.fill(r.rect(6, 6, 84, 84), 5, steel, 2);
        liquidWindow(r, r.minus(r.rect(10, 10, 76, 76), r.circle(c, c, 38)), 7);
        r.fill(r.circle(c, c, 36), 10, metal, 4);
        r.fill(r.circle(c, c, 30), 5, deep, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        R rot = canvas(s);
        for(int i = 0; i < 5; i++) rot.fill(rot.bar(c, c, 4, 25, 8, i * 72), 8, steel, 2);
        rot.fill(rot.circle(c, c, 6), 10, metal, 2);
        save(rot, name + "-rotator");
    }

    static void waterTreatment(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //inlet channel, filter bed with grates, purified (left) and brine (right) compartments
        r.fill(r.rect(16, 2, 32, 10), 9, steel, 2);
        liquidWindow(r, r.rect(20, 4, 24, 5), 10);
        r.fill(r.rect(12, 16, 40, 26), 9, steel, 3);
        liquidWindow(r, r.rect(16, 20, 32, 18), 10);
        for(int y = 22; y < 38; y += 4) r.fill(r.rect(16, y, 32, 1), 11, metal, 0);
        r.fill(r.rect(2, 44, 26, 16), 9, metal, 2);
        r.fill(r.rect(6, 48, 18, 8), 10, acc(0x8fd3ff), 0);
        r.fill(r.rect(36, 44, 26, 16), 9, metal, 2);
        r.fill(r.rect(40, 48, 18, 8), 10, acc(0xa7c9b4), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void electrolysisPlant(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //three cells with electrode plates; hydrogen housing (left) and oxygen housing (right) kept apart
        for(int i = 0; i < 3; i++){
            int x = 22 + i * 18;
            r.fill(r.rect(x, 12, 14, 72), 9, steel, 2);
            liquidWindow(r, r.rect(x + 3, 16, 8, 64), 10);
            r.fill(r.rect(x + 6, 14, 2, 68), 11, dull(0x8c92ac), 0);
        }
        r.fill(r.oct(2, 30, 18, 36, 4), 11, metal, 3);
        r.fill(r.rect(6, 42, 10, 12), 12, acc(0xe8edf0), 0);
        r.fill(r.oct(76, 30, 18, 36, 4), 11, metal, 3);
        r.fill(r.rect(80, 42, 10, 12), 12, acc(0xa9d8ff), 0);
        r.fill(r.rect(22, 4, 52, 6), 12, copperGearM, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void airSeparation(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, steel);
        //insulated cold box with two tall columns and a main heat exchanger
        r.fill(r.oct(8, 8, 80, 80, 8), 9, dull(0x7d8794), 3);
        vessel(r, 32, 34, 17, 11, metal);
        vessel(r, 64, 34, 13, 11, metal);
        r.fill(r.rect(18, 62, 60, 18), 10, metal, 2);
        liquidWindow(r, r.rect(22, 66, 52, 10), 11);
        r.fill(r.rect(44, 30, 8, 8), 12, steel, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void cryoCondenser(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, metal);
        //thick insulation (dark gaps) round a pale condenser core
        r.fill(r.circle(c, c, 26), 9, steel, 3);
        r.fill(r.ring(c, c, 18, 20), 6, deep, 0);
        r.fill(r.circle(c, c, 17), 10, dull(0x9fb7c8), 2);
        liquidWindow(r, r.circle(c, c, 9), 11);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 10, 6, 3, i * 90 + 45), 12, metal, 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void pressureReactor(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 8, 5, steel);
        //a forged vessel held by four locking clamps, a pressure gauge
        r.fill(r.circle(c, c, 38), 11, metal, 5);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 30, 14, 14, i * 90 + 45), 14, steel, 3);
        r.fill(r.circle(c, c, 24), 13, steel, 3);
        liquidWindow(r, r.circle(c, c, 12), 14);
        r.fill(r.circle(84, 12, 7), 12, metal, 2);
        r.fill(r.circle(84, 12, 3), 13, lit(0xff5a5a), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void nitricPlant(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //oxidation vessel with a gauze ring, gas in at the top, acid out at the bottom
        r.fill(r.circle(c, c, 22), 10, steel, 3);
        r.fill(r.ring(c, c, 12, 16), 11, dull(0xe4e6ea), 1);
        liquidWindow(r, r.circle(c, c, 10), 11);
        stub(r, 1, 26, 12, acc(0xd8eef7));
        stub(r, 3, 26, 12, acc(0xf0d98c));
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void chlorAlkali(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, steel);
        //a bank of membrane cells inside a separate frame; chlorine left, hydrogen right, caustic front
        r.fill(r.rect(14, 10, 68, 70), 9, metal, 3);
        for(int i = 0; i < 4; i++){
            int y = 14 + i * 16;
            liquidWindow(r, r.rect(20, y + 2, 56, 10), 10);
            r.fill(r.rect(46, y, 4, 14), 11, dull(0xd8dde3), 0);
        }
        r.fill(r.rect(0, 38, 12, 20), 9, metal, 2);
        r.fill(r.rect(2, 44, 6, 8), 10, acc(0xb6dc4c), 0);
        r.fill(r.rect(84, 38, 12, 20), 9, metal, 2);
        r.fill(r.rect(88, 44, 6, 8), 10, acc(0xe8edf0), 0);
        r.fill(r.rect(38, 82, 20, 12), 9, metal, 2);
        r.fill(r.rect(44, 86, 8, 6), 10, acc(0xd8dff0), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void acidSynth(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, metal);
        //hydrogen and chlorine meet in a dark burner; acid leaves at the bottom
        stub(r, 2, 14, 12, acc(0xe8edf0));
        stub(r, 0, 14, 12, acc(0xb6dc4c));
        r.fill(r.oct(12, 10, 40, 36, 6), 11, steel, 3);
        r.fill(r.oct(18, 16, 28, 24, 4), 4, deep, 1);
        r.fill(r.circle(32, 28, 5), 5, lit(0xdfe882), 0);
        r.fill(r.rect(22, 48, 20, 12), 9, metal, 2);
        liquidWindow(r, r.rect(26, 51, 12, 6), 10);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void leachingVat(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //a thick lined vat with the scrap basket, a liquid handling block on the side
        r.fill(r.rect(84, 20, 12, 56), 10, metal, 3);
        r.fill(r.rect(87, 26, 6, 44), 11, acc(0xe0a050), 0);
        r.fill(r.circle(c, c, 38), 10, dull(0x6d7a88), 4);
        r.fill(r.circle(c, c, 31), 11, metal, 3);
        liquidWindow(r, r.circle(c, c, 24), 12);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        R rot = canvas(s);
        for(int i = 0; i < 4; i++) rot.fill(rot.bar(c, c, 4, 19, 5, i * 90), 13, metal, 1);
        rot.fill(rot.circle(c, c, 5), 14, steel, 1);
        save(rot, name + "-rotator");
    }

    static void neutralizer(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, metal);
        //two different feeds (acid waste from the top, caustic from the left) into a stirred tank
        stub(r, 1, 26, 12, acc(0xc8b25a));
        stub(r, 2, 26, 12, acc(0xd8dff0));
        r.fill(r.circle(c, c, 22), 10, steel, 3);
        liquidWindow(r, r.circle(c, c, 15), 11);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        R rot = canvas(s);
        for(int i = 0; i < 3; i++) rot.fill(rot.bar(c, c, 2, 12, 4, i * 120), 12, metal, 1);
        rot.fill(rot.circle(c, c, 3), 13, steel, 0);
        save(rot, name + "-rotator");
    }

    static void scrubber(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, steel);
        //packed scrubber column (gas path up the middle), wash liquid sump, sulfur tray
        r.fill(r.oct(14, 4, 36, 40, 8), 11, metal, 3);
        for(int y = 10; y < 40; y += 6) r.fill(r.rect(20, y, 24, 3), 12, steel, 0);
        r.fill(r.rect(6, 46, 34, 14), 9, metal, 2);
        liquidWindow(r, r.rect(9, 49, 28, 8), 10);
        r.fill(r.rect(44, 46, 16, 14), 9, metal, 2);
        r.fill(r.rect(47, 49, 10, 8), 10, acc(0xe8d44d), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void gasSeparator(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //membrane chamber with manifolds on opposite sides: nitrogen left, carbon dioxide right
        r.fill(r.rect(0, 20, 18, 56), 10, steel, 3);
        r.fill(r.rect(78, 20, 18, 56), 10, steel, 3);
        r.fill(r.rect(3, 40, 8, 16), 11, acc(0xa9b8ff), 0);
        r.fill(r.rect(85, 40, 8, 16), 11, acc(0x8a8580), 0);
        r.fill(r.oct(22, 10, 52, 76, 8), 11, steel, 3);
        for(int x = 28; x < 70; x += 7) liquidWindow(r, r.rect(x, 18, 3, 60), 12);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void wastewaterPlant(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //dirty inlet, a round settling clarifier (scraper turns), sludge pit, clean water out
        stub(r, 2, 8, 12, acc(0x6f7f5a));
        r.fill(r.rect(44, 46, 18, 16), 8, steel, 2);
        r.fill(r.rect(47, 49, 12, 10), 4, dull(0x3e3a2e), 1);
        r.fill(r.circle(c, c, 22), 9, steel, 3);
        liquidWindow(r, r.circle(c, c, 16), 10);
        stub(r, 3, 8, 12, acc(0x8fd3ff));
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        R rot = canvas(s);
        rot.fill(rot.bar(c, c, 0, 15, 3, 0), 12, metal, 0);
        rot.fill(rot.bar(c, c, 0, 15, 3, 180), 12, metal, 0);
        rot.fill(rot.circle(c, c, 3), 13, steel, 0);
        save(rot, name + "-rotator");
    }

    static void thermalWasteProcessor(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, steel);
        //a dark incinerator chamber between a feed chute and an ash/solids drawer
        hopper(r, 22, 0, 20, 12, dull(0x5a5446));
        r.fill(r.oct(8, 18, 48, 30, 5), 11, metal, 3);
        r.fill(r.rect(14, 23, 36, 20), 4, deep, 0);
        r.fill(r.rect(18, 31, 28, 4), 5, molten, 0);
        r.fill(r.rect(14, 50, 36, 12), 8, metal, 2);
        r.fill(r.rect(18, 53, 28, 6), 4, dull(0x6a6a70), 1);
        save(r, name);
        glowOf(r, name);
        heatLayer(r, name, r.rect(14, 23, 36, 20));
    }

    static void lithiumExtractor(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 5, metal);
        //brine in, precipitation vessel, filter press plates, lithium collection
        stub(r, 1, 8, 12, acc(0xa7c9b4));
        vessel(r, 18, 30, 13, 10, steel);
        liquidWindow(r, r.circle(18, 30, 5), 12);
        r.fill(r.rect(36, 12, 24, 36), 9, steel, 2);
        for(int x = 38; x < 58; x += 4) r.fill(r.rect(x, 15, 3, 30), 10, dull(0xd9cfe9), 0);
        r.fill(r.rect(10, 50, 44, 10), 8, metal, 2);
        r.fill(r.rect(14, 53, 36, 4), 9, dull(0xe8e4f2), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void aquaRegiaMixer(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, steel);
        //a small, heavily sealed mixing vessel; two acid feeds; a tiny window
        stub(r, 1, 12, 12, acc(0xdfe882));
        stub(r, 1, 40, 12, acc(0xf0d98c));
        r.fill(r.circle(c, c, 22), 11, metal, 4);
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c, 18, 4, 4, i * 45 + 22.5f), 13, steel, 0);
        r.fill(r.circle(c, c, 13), 12, steel, 2);
        liquidWindow(r, r.circle(c, c, 6), 13);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
        R rot = canvas(s);
        rot.fill(rot.bar(c, c, 0, 11, 3, 45), 14, metal, 0);
        rot.fill(rot.bar(c, c, 0, 11, 3, 225), 14, metal, 0);
        save(rot, name + "-rotator");
    }

    static void platinumRefinery(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, steel);
        //acid-resistant digestion chamber, then a small filter and the recovered platinum
        r.fill(r.oct(4, 6, 34, 38, 6), 11, dull(0x6d7a88), 3);
        liquidWindow(r, r.oct(10, 12, 22, 26, 4), 12);
        r.fill(r.rect(42, 6, 18, 38), 10, metal, 2);
        for(int y = 10; y < 40; y += 5) r.fill(r.rect(45, y, 12, 2), 11, steel, 0);
        r.fill(r.rect(16, 50, 32, 10), 9, metal, 2);
        r.fill(r.rect(26, 52, 12, 6), 10, acc(0xe4e6ea), 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void synthesisComplex(String name) throws IOException{
        int s = 4 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 5, steel);
        //feed manifold (top), cryogenic section (left), catalyst section (right), control housing (bottom),
        //and the pressure synthesis chamber in the middle as the one focal point
        r.fill(r.rect(10, 4, 108, 14), 10, metal, 3);
        for(int x = 18; x < 112; x += 18) r.fill(r.rect(x, 7, 8, 8), 11, acc(0x8fd0ff), 0);
        r.fill(r.oct(4, 26, 26, 76, 6), 10, dull(0x7d8794), 3);
        for(int y = 34; y < 96; y += 12) r.fill(r.rect(8, y, 18, 6), 11, acc(0xbfe3ff), 0);
        r.fill(r.oct(98, 26, 26, 76, 6), 10, metal, 3);
        for(int y = 34; y < 96; y += 16) r.fill(r.rect(104, y, 14, 10), 11, acc(0xe4e6ea), 1);
        r.fill(r.rect(36, 108, 56, 16), 10, metal, 3);
        r.fill(r.rect(56, 113, 16, 6), 11, lit(0xff9a3c), 0);
        r.fill(r.circle(c, c + 2, 38), 12, metal, 5);
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c + 2, 30, 10, 10, i * 45), 15, steel, 2);
        r.fill(r.circle(c, c + 2, 24), 14, steel, 3);
        liquidWindow(r, r.circle(c, c + 2, 14), 15);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void nitratePlant(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //neutraliser vessel and a crystalliser bin with pale granules
        r.fill(r.circle(20, 22, 15), 10, steel, 3);
        liquidWindow(r, r.circle(20, 22, 8), 11);
        r.fill(r.rect(38, 8, 22, 48), 9, metal, 2);
        r.fill(r.rect(42, 12, 14, 40), 4, deep, 0);
        for(int[] g : new int[][]{{44, 40}, {49, 44}, {45, 47}, {51, 38}, {47, 34}}) r.fill(r.rect(g[0], g[1], 3, 3), 5, dull(0xe8edf0), 1);
        r.fill(r.rect(8, 44, 24, 14), 8, metal, 2);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void explosivesPlant(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 6, steel);
        //a reinforced sealed mixer: thick bands, a feed region, small orange status markers
        r.fill(r.rect(4, 4, 56, 12), 10, metal, 2);
        for(int x = 8; x < 56; x += 12) r.fill(r.rect(x, 7, 8, 6), 6, deep, 0);
        r.fill(r.circle(c, c + 6, 22), 11, metal, 4);
        r.fill(r.ring(c, c + 6, 15, 19), 13, steel, 2);
        r.fill(r.circle(c, c + 6, 9), 12, metal, 2);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c + 6, 20, 3, 4, i * 90 + 45), 14, lit(0xff795e), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void fertilizerPlant(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //granulator drum (turns) with a small granule outlet
        r.fill(r.oct(6, 8, 52, 44, 8), 9, steel, 3);
        liquidWindow(r, r.oct(12, 14, 40, 32, 6), 10);
        r.fill(r.rect(24, 52, 16, 10), 8, metal, 2);
        r.fill(r.rect(28, 55, 8, 4), 9, dull(0xb8a878), 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void enzymeExtractor(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 5, metal);
        //a contained biomass hopper and a centrifuge bowl
        hopper(r, 2, 2, 20, 12, dull(0x6fae4a));
        r.fill(r.circle(c + 6, c + 6, 22), 10, steel, 3);
        r.fill(r.circle(c + 6, c + 6, 16), 11, metal, 2);
        r.fill(r.ring(c + 6, c + 6, 6, 10), 12, steel, 1);
        r.fill(r.circle(c + 6, c + 6, 5), 10, lit(0x8fcf6a), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void fermenter(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 5, metal);
        //biofuel out left, carbon dioxide out right, a sealed fermentation tank with a gas dome
        r.fill(r.rect(0, 38, 18, 20), 9, steel, 2);
        r.fill(r.rect(2, 44, 8, 8), 10, acc(0xf0e6a0), 0);
        r.fill(r.rect(78, 38, 18, 20), 9, steel, 2);
        r.fill(r.rect(86, 44, 8, 8), 10, acc(0xd8dde3), 0);
        r.fill(r.circle(c, c, 32), 11, metal, 4);
        liquidWindow(r, r.ring(c, c, 18, 26), 12);
        r.dome(r.circle(c, c, 15), 12, 5, steel);
        r.fill(r.circle(c, c, 4), 18, metal, 1);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void digester(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 4, 4, steel);
        //a big squat waste tank under a gas-holder dome, sludge feed box, gas take-off
        r.fill(r.circle(c - 4, c + 4, 38), 9, dull(0x6d7262), 4);
        r.dome(r.circle(c - 4, c + 4, 28), 10, 8, metal);
        r.fill(r.circle(c - 4, c + 4, 5), 19, steel, 1);
        r.fill(r.rect(70, 4, 22, 22), 10, metal, 2);
        liquidWindow(r, r.rect(74, 8, 14, 14), 11);
        r.fill(r.circle(84, 84, 8), 12, steel, 2);
        r.fill(r.circle(84, 84, 4), 13, acc(0xc4b3e6), 0);
        save(r, name);
        glowOf(r, name);
        liquidBottom(name, s);
    }

    static void moduleAssembler(String name) throws IOException{
        int s = 2 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, metal);
        //a small work platform, one tool head on an arm, a module output tray
        r.fill(r.rect(6, 6, 40, 40), 4, steel, 2);
        r.fill(r.rect(16, 16, 20, 20), 5, deep, 0);
        r.fill(r.rect(19, 19, 14, 14), 6, dull(0x6e7080), 1);
        r.fill(r.rect(48, 6, 12, 50), 9, metal, 2);
        r.fill(r.rect(51, 40, 6, 12), 10, acc(0xff9a3c), 1);
        r.fill(r.rect(6, 50, 40, 10), 8, metal, 2);
        r.fill(r.circle(26, 26, 4), 8, lit(0xff9a3c), 0);
        save(r, name);
        glowOf(r, name);
    }

    static void advancedModuleAssembler(String name) throws IOException{
        int s = 3 * T;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 5, 6, steel);
        //two precision work areas, component bays, heavy supports
        for(int x : new int[]{4, 82}) r.fill(r.rect(x, 6, 10, 84), 11, metal, 3);
        for(int i = 0; i < 2; i++){
            int x = 18 + i * 32;
            r.fill(r.rect(x, 14, 28, 40), 5, metal, 2);
            r.fill(r.rect(x + 5, 20, 18, 22), 6, deep, 0);
            r.fill(r.rect(x + 8, 24, 12, 14), 7, dull(0x6e7080), 1);
            r.fill(r.rect(x + 12, 46, 4, 3), 8, lit(0x6dd0f0), 0);
        }
        for(int i = 0; i < 4; i++){
            r.fill(r.rect(18 + i * 16, 62, 13, 13), 9, metal, 2);
            r.fill(r.rect(22 + i * 16, 66, 5, 5), 5, deep, 0);
        }
        r.fill(r.rect(18, 80, 60, 8), 9, metal, 2);
        save(r, name);
        glowOf(r, name);
    }

    static void augmentationFacility(String name) throws IOException{
        int s = 4 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 8, 6, steel);
        //stages round a central precision assembly ring: intake (top), etching (left), testing (right), packing (bottom)
        r.fill(r.rect(30, 4, 68, 20), 10, metal, 3);
        for(int x = 36; x < 94; x += 12) r.fill(r.rect(x, 9, 8, 10), 6, deep, 0);
        r.fill(r.rect(4, 30, 22, 68), 10, metal, 3);
        for(int y = 36; y < 92; y += 14) r.fill(r.rect(9, y, 12, 8), 11, acc(0x6dd0f0), 0);
        r.fill(r.rect(102, 30, 22, 68), 10, metal, 3);
        for(int y = 38; y < 92; y += 18) r.fill(r.circle(113, y + 4, 5), 11, steel, 1);
        r.fill(r.rect(30, 104, 68, 20), 10, metal, 3);
        r.fill(r.rect(56, 110, 16, 8), 11, acc(0xe8d8ff), 1);
        r.fill(r.circle(c, c, 34), 12, metal, 4);
        r.fill(r.circle(c, c, 26), 6, deep, 1);
        r.fill(r.ring(c, c, 10, 14), 8, steel, 1);
        r.fill(r.circle(c, c, 6), 9, lit(0xb37aff), 1);
        save(r, name);
        glowOf(r, name);
        R rot = canvas(s);
        for(int i = 0; i < 4; i++) rot.fill(rot.bar(c, c, 15, 10, 5, i * 90 + 45), 13, metal, 1);
        save(rot, name + "-rotator");
    }

    static void aetheriteForge(String name) throws IOException{
        int s = 3 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 6, 6, steel);
        //a heavy forge body with a small contained crystal chamber at its heart, clamps on the chamber
        r.fill(r.oct(8, 8, 80, 80, 14), 10, metal, 4);
        r.fill(r.oct(28, 28, 40, 40, 10), 6, deep, 1);
        r.fill(r.poly(new int[]{48, 56, 48, 40}, new int[]{36, 48, 60, 48}), 9, lit(0x7fe0ff), 1);
        for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, 22, 14, 8, i * 90 + 45), 13, steel, 2);
        for(int y : new int[]{12, 76}) r.fill(r.rect(36, y, 24, 8), 12, steel, 1);
        save(r, name);
        glowOf(r, name);
    }

    static void moduleRecycler(String name) throws IOException{
        int s = 2 * T;
        float c = s / 2f;
        R r = canvas(s);
        slab(r, 0, 0, s, s, 3, 6, steel);
        //a disassembly opening with cutter wheels, then sorting bins
        r.fill(r.rect(8, 6, 48, 30), 9, metal, 2);
        r.fill(r.rect(14, 12, 36, 18), 3, deep, 1);
        r.fill(r.rect(8, 40, 48, 18), 8, metal, 2);
        int[] bins = {0x8a7f72, 0xc8ccd8, 0xffd84a};
        for(int i = 0; i < 3; i++) r.fill(r.rect(12 + i * 15, 44, 11, 10), 5, dull(bins[i]), 1);
        save(r, name);
        glowOf(r, name);
    }

    static void furnace(String name, int size, int glowRgb, boolean heatInput) throws IOException{
        int s = size * T;
        R r = new R(s);
        housing(r);
        furnaceDesign(r, name, glowRgb);
        connectors(r, glowRgb);
        save(r, name);
        layers(r, name);

        if(heatInput) edgeGlow(name + "-heat", size);
    }

    static void edgeGlow(String name, int size) throws IOException{
        int s = size * T;
        BufferedImage image = img(s);
        Graphics2D g = g(image);
        int x = s - 8, y = Math.max(8, s / 2 - 9);
        g.setColor(new Color(255, 140, 61, 70));
        g.fillRect(x - 2, y - 2, 6, 22);
        g.setColor(new Color(255, 180, 92, 135));
        g.fillRect(x, y, 2, 18);
        g.setColor(new Color(255, 226, 160, 180));
        g.fillRect(x, y + 3, 1, 11);
        g.dispose();
        save(image, name);
    }

    static void heatParts(String name, int size, int rgb) throws IOException{
        int s = size * T;
        edgeGlow(name + "-heat", size);
        BufferedImage glow = img(s);
        Graphics2D gl = g(glow);
        gl.setColor(new Color(255, 255, 255, 220));
        gl.fillRect(s - 6, s / 2 - 6, 4, 12);
        gl.dispose();
        save(glow, name + "-glow");
        BufferedImage top = port(s, 16, acc(shade(rgb, 0.8f))).render();
        save(top, name + "-top1");
        save(flipY(top), name + "-top2");
    }

    static void exchanger(String name, int size, int rgb) throws IOException{
        int s = size * T;
        R r = new R(s);
        housing(r);
        deck(r, 4);
        int x = 10, y = 9, w = s - 22, h = s - 18;
        r.fill(r.rect(x, y, w, h), 8, metal, 2);
        r.fill(r.rect(x + 3, y + 3, w - 6, h - 6), 3, deep, 0);
        int lanes = size == 2 ? 4 : 6, lh = (h - 8) / lanes;
        for(int i = 0; i < lanes; i++){
            int yy = y + 4 + i * lh;
            r.cylH(x + 4, yy, w - 8, lh - 1, 4, 2, acc(mixi(rgb, 0x5a3a2a, 0.35f)));
            int endX = i % 2 == 0 ? x + w - 8 : x + 4;
            if(i < lanes - 1) r.cylV(endX, yy + 1, 4, lh, 4.5f, 2, acc(mixi(rgb, 0x5a3a2a, 0.35f)));
        }
        for(int xx = x + 7; xx < x + w - 5; xx += 6) r.fill(r.rect(xx, y + 1, 1, h - 2), 8.5f, steel, 0);
        pipeV(r, 4, 10, s - 20, 4, 7);
        cornerBolts(r, 3);
        save(r, name);
        heatParts(name, size, rgb);
    }

    static void heater(String name, int size, int rgb) throws IOException{
        int s = size * T;
        R r = new R(s);
        housing(r);
        int x = 10, y = 10, w = s - 22, h = s - 20;
        r.fill(r.rect(x - 2, y - 2, w + 4, h + 4), 9, metal, 2);
        r.fill(r.rect(x + 2, y + 2, w - 4, h - 4), 2, deep, 0);
        int n = size == 2 ? 4 : 6, step = (h - 6) / n;
        Mat coil = acc(mixi(rgb, 0x572c22, 0.4f));
        for(int i = 0; i < n; i++) r.cylH(x + 4, y + 4 + i * step, w - 8, step - 2, 3, 2, coil);
        r.fill(r.rect(x + 3, y + 3, 2, h - 6), 5, brass, 0);
        r.fill(r.rect(x + w - 5, y + 3, 2, h - 6), 5, brass, 0);
        pipeV(r, s - 10, 8, s - 16, 4, 8);
        cornerBolts(r, 4);
        save(r, name);

        BufferedImage glow = img(s);
        Graphics2D gl = g(glow);
        gl.setColor(alpha(rgb, 70));
        gl.fillRect(x + 4, y + 4, w - 8, h - 8);
        for(int i = 0; i < n; i++){
            gl.setColor(alpha(0xffdc9b, 110));
            gl.fillRect(x + 6, y + 4 + i * step + (step - 2) / 2 - 1, w - 12, 1);
        }
        gl.dispose();
        save(glow, name + "-glow");
        edgeGlow(name + "-heat", size);
        BufferedImage top = port(s, s - 20, coil).render();
        save(top, name + "-top1");
        save(flipY(top), name + "-top2");
    }

    static void turbine(String name, int size, int glowRgb) throws IOException{
        int s = size * T;
        float c = s / 2f, d = s / 2f - (size == 2 ? 7 : 10);
        R r = new R(s);
        housing(r);
        r.dome(r.ring(c, c, d - 1, d + 4), 6, 4, metal);
        r.fill(r.circle(c, c, d - 1), 2, deep, 0);
        for(int i = 0; i < 6; i++) r.fill(r.bar(c, c, 4, d - 5, size == 2 ? 5 : 7, i * 60 + 15), 6, steel, 1.5f);
        r.dome(r.circle(c, c, size == 2 ? 6 : 8), 7, 3, metal);
        r.fill(r.circle(c, c, 2), 10.5f, acc(glowRgb), 0);
        cornerBolts(r, 3);
        save(r, name);

        BufferedImage glow = img(s);
        Graphics2D gl = g(glow);
        gl.setColor(alpha(glowRgb, 75));
        gl.fill(new Ellipse2D.Float(c - 4, c - 4, 8, 8));
        gl.setColor(new Color(255, 255, 255, 95));
        gl.fillRect((int)c - 1, (int)c - 1, 2, 2);
        gl.dispose();
        save(glow, name + "-glow");
    }

    static void battery(String name, int size, int rgb) throws IOException{
        int s = size * T, count = size == 2 ? 3 : 4, gap = 3, pad = 8;
        int cellW = (s - pad * 2 - gap * (count - 1)) / count, cy0 = 11, ch = s - 20;
        R r = new R(s);
        housing(r);
        deck(r, 4);
        int wy = cy0 + 6, wh = ch - 12;
        for(int i = 0; i < count; i++){
            int x = pad + i * (cellW + gap);
            r.fill(r.rect(x, cy0, cellW, ch), 9, metal, 2);
            r.fill(r.rect(x + 3, wy, cellW - 6, wh), 4, deep, 0);
            r.fill(r.rect(x + 2, cy0 + 2, cellW - 4, 2), 10, acc(rgb), 0);
        }
        save(r, name);

        for(String suffix : new String[]{"-power", "-power-empty", "-power-full"}){
            BufferedImage overlay = img(s);
            Graphics2D pg = g(overlay);
            for(int i = 0; i < count; i++){
                int x = pad + i * (cellW + gap) + 3, iw = cellW - 6;
                if(suffix.endsWith("empty")){
                    pg.setColor(new Color(0x3a3530));
                    pg.fillRect(x, wy, iw, wh);
                }else{
                    int lh = suffix.endsWith("full") ? wh : wh / 2;
                    pg.setColor(new Color(shade(rgb, 0.8f)));
                    pg.fillRect(x, wy + wh - lh, iw, lh);
                    pg.setColor(new Color(tint(rgb, 0.35f)));
                    pg.fillRect(x, wy + wh - lh, iw, 1);
                    pg.fillRect(x + iw - 1, wy + wh - lh, 1, lh);
                }
            }
            pg.dispose();
            save(overlay, name + suffix);
        }

        R top = new R(s);
        for(int i = 0; i < count; i++){
            int x = pad + i * (cellW + gap) + cellW / 2;
            top.dome(top.rect(x - 4, cy0 - 3, 3, 3), 10, 1.5f, redM);
            top.dome(top.rect(x + 1, cy0 - 3, 3, 3), 10, 1.5f, metal);
            //glass cover over the charge window
            top.fill(top.rect(x - cellW / 2 + 3, wy, 1, wh), 6, glass(0xffffff), 0);
        }
        save(top, name + "-top");
    }

    static void reactor(String name, int size, int glowRgb) throws IOException{
        int s = size * T;
        float c = s / 2f, d = s / 2f - (size == 3 ? 8 : 11);
        R r = new R(s);
        housing(r);
        deck(r, 4);
        r.dome(r.ring(c, c, d - (size == 3 ? 7 : 9), d), 6, 5, metal);
        r.fill(r.circle(c, c, d - (size == 3 ? 7 : 9)), 3, deep, 0);
        for(int i = 0; i < 8; i++) r.fill(r.bar(c, c, d - 3, 6, 3, i * 45 + 22.5f), 9.5f, steel, 1);
        float core = size == 3 ? 9 : 12;
        r.fill(r.oct(c - core, c - core, core * 2, core * 2, core * 0.4f), 6, steel, 1);
        r.fill(r.oct(c - core + 3, c - core + 3, core * 2 - 6, core * 2 - 6, core * 0.3f), 7, acc(glowRgb), 2);
        r.fill(r.rect((int)(c - core + 2), (int)c - 1, (int)(core * 2 - 4), 2), 8, steel, 0);
        cornerBolts(r, 6);
        save(r, name);

        BufferedImage glow = img(s);
        Graphics2D gl = g(glow);
        gl.setColor(alpha(glowRgb, 80));
        gl.fill(new Ellipse2D.Float(c - core + 2, c - core + 2, core * 2 - 4, core * 2 - 4));
        gl.setColor(new Color(255, 255, 255, 115));
        gl.fillRect((int)c - 2, (int)c - 4, 4, 2);
        gl.dispose();
        save(glow, name + "-glow");
        edgeGlow(name + "-heat", size);
        BufferedImage top = port(s, 22, acc(shade(glowRgb, 0.75f))).render();
        save(top, name + "-top1");
        save(flipY(top), name + "-top2");
    }

    /** Opaque crafters with a glow layer (no liquid tile). */
    static void fuelCell(String name, int size, int glowRgb) throws IOException{
        int s = size * T;
        float c = s / 2f;
        R r = new R(s);
        housing(r);
        switch(name){
            case "hydrogen-fuel-cell" -> {
                deck(r, 4);
                int w = 16, h = s - 22, x0 = (s - (2 * w + 6)) / 2;
                pipeH(r, 5, (int)c - 2, s - 10, 5, 7);
                for(int i = 0; i < 2; i++){
                    int x = x0 + i * (w + 6);
                    r.fill(r.rect(x, 11, w, h), 9, metal, 2);
                    for(int yy = 15; yy < 11 + h - 4; yy += 3) r.fill(r.rect(x + 2, yy, w - 4, 1), 8, steel, 0);
                    r.fill(r.rect(x + 3, 9, w - 6, 3), 10, lit(glowRgb), 0);
                }
            }
            case "enzyme-extractor" -> bio(r, glowRgb, "biochemical-fermenter", false);
            case "chip-fabricator", "control-unit-assembler" -> precision(r, glowRgb, false);
            case "induction-furnace" -> {
                deck(r, 4);
                r.fill(r.oct(c - 13, c - 14, 26, 28, 5), 9, metal, 2);
                r.fill(r.oct(c - 8, c - 9, 16, 18, 3), 2, deep, 0);
                r.fill(r.rect((int)c - 5, (int)c, 10, 5), 3, lit(glowRgb), 1);
                for(int i = 0; i < 4; i++){
                    int yy = (int)c - 11 + i * 6;
                    r.cylH((int)c - 17, yy, 34, 3, 9, 1.5f, copperM);
                }
            }
            case "explosives-plant" -> {
                deck(r, 4);
                int d = 16;
                int[][] p = {{(int)c - d / 2, 8}, {(int)c - d / 2, s - 8 - d}, {8, (int)c - d / 2}};
                pipeV(r, (int)c - 2, 12, s - 24, 4, 6);
                pipeH(r, 12, (int)c - 2, (int)c - 12, 4, 6);
                for(int[] pos : p){
                    r.fill(r.oct(pos[0], pos[1], d, d, 4), 9, metal, 2);
                    r.fill(r.oct(pos[0] + 4, pos[1] + 4, d - 8, d - 8, 2), 4, deep, 0);
                    r.fill(r.rect(pos[0] + d / 2 - 1, pos[1] + d / 2 - 1, 2, 2), 5, acc(glowRgb), 0);
                }
                r.fill(r.rect(s - 20, (int)c - 8, 12, 16), 8, metal, 1);
                r.fill(r.rect(s - 18, (int)c - 6, 8, 2), 9, redM, 0);
                r.fill(r.rect(s - 18, (int)c + 4, 8, 2), 9, redM, 0);
            }
            case "unit-shield-beacon" -> {
                deck(r, 4);
                for(int i = 0; i < 4; i++){
                    r.fill(r.bar(c, c, 10, s / 2f - 14, 6, i * 90 + 45), 8, steel, 1);
                    float px = c + (float)Math.cos(Math.toRadians(i * 90 + 45)) * (s / 2f - 13), py = c - (float)Math.sin(Math.toRadians(i * 90 + 45)) * (s / 2f - 13);
                    r.dome(r.circle(px, py, 5), 8, 3, metal);
                    r.fill(r.circle(px, py, 1.6f), 11.5f, acc(glowRgb), 0);
                }
                r.dome(r.ring(c, c, 9, 15), 7, 4, metal);
                r.fill(r.circle(c, c, 9), 4, deep, 0);
                r.dome(r.circle(c, c, 6), 5, 5, glass(glowRgb));
            }
            default -> bayDesign(r, name, glowRgb);
        }
        connectors(r, glowRgb);
        save(r, name);
        layers(r, name);
    }

    static void nozzles(String block, int size, String liquid, int rgb) throws IOException{
        int s = size * T, w = 7, h = 12, x = s - w, y = (s - h) / 2;
        R r = new R(s);
        r.fill(r.rect(x, y, w, h), 6, metal, 1);
        r.fill(r.rect(x + 1, y + 2, w - 2, h - 4), 6.5f, acc(rgb), 1);
        BufferedImage img = r.render();
        save(img, block + "-" + liquid + "-output1");
        save(flipY(img), block + "-" + liquid + "-output2");
    }

    // =====================================================================================================
    // Manufacturing, logistics, storage, defence
    // =====================================================================================================

    static void assembler(String name, int size, int a) throws IOException{
        int s = size * T;
        float c = s / 2f;
        R r = new R(s);
        housing(r);
        if(name.equals("robot-dispatch-hub") || name.equals("emergency-repair-dispatch-bay")){
            deck(r, 4);
            int bay = size == 3 ? 30 : 22, gap = 6, start = (s - (bay * 2 + gap)) / 2;
            for(int row = 0; row < 2; row++) for(int col = 0; col < 2; col++){
                float x = start + col * (bay + gap), y = start + row * (bay + gap);
                r.fill(r.oct(x, y, bay, bay, 5), 8, metal, 2);
                r.fill(r.circle(x + bay / 2f, y + bay / 2f, bay / 2f - 4), 3, deep, 0);
                r.dome(r.ring(x + bay / 2f, y + bay / 2f, bay / 2f - 7, bay / 2f - 5), 3, 1, acc(a));
                r.fill(r.rect((int)(x + bay / 2f) - 1, (int)(y + bay / 2f) - 1, 2, 2), 4, acc(a), 0);
            }
            if(name.startsWith("emergency")){
                r.fill(r.rect((int)c - 6, (int)c - 2, 12, 4), 10, acc(a), 1);
                r.fill(r.rect((int)c - 2, (int)c - 6, 4, 12), 10, acc(a), 1);
            }
        }else if(name.equals("aetherite-module-forge") || name.equals("experimental-augmentation-facility")){
            deck(r, 4);
            float d = s * 0.3f;
            r.fill(r.oct(c - d - 6, c - d - 6, (d + 6) * 2, (d + 6) * 2, d * 0.5f), 9, metal, 3);
            r.fill(r.oct(c - d, c - d, d * 2, d * 2, d * 0.4f), 3, deep, 0);
            r.fill(r.oct(c - d * 0.45f, c - d * 0.45f, d * 0.9f, d * 0.9f, d * 0.2f), 6, acc(a), 2);
            for(int i = 0; i < 4; i++) r.fill(r.bar(c, c, d * 0.5f, d * 0.55f, 4, i * 90 + 45), 8, steel, 1);
            for(int i = 0; i < 4; i++){
                float px = c + (float)Math.cos(Math.toRadians(i * 90)) * (d + 10), py = c - (float)Math.sin(Math.toRadians(i * 90)) * (d + 10);
                r.dome(r.circle(px, py, 4), 9, 2.5f, metal);
                r.fill(r.circle(px, py, 1.5f), 12, acc(a), 0);
            }
        }else{
            pressDesign(r, name, a);
            connectors(r, a);
        }
        save(r, name);
        layers(r, name);
    }

    static void storage(String name, int size, int a) throws IOException{
        int s = size * T, pad = 8, gap = 3, bays = 3;
        int bw = (s - pad * 2 - gap * (bays - 1)) / bays;
        R r = new R(s);
        housing(r);
        deck(r, 4);
        for(int i = 0; i < bays; i++){
            int x = pad + i * (bw + gap);
            r.fill(r.rect(x, 10, bw, s - 20), 9, metal, 2);
            r.fill(r.rect(x + 3, 15, bw - 6, s - 32), 6, steel, 1);
            r.fill(r.rect(x + 4, s - 15, bw - 8, 2), 10, acc(a), 0);
            r.fill(r.rect(x + bw / 2 - 2, 12, 4, 2), 10, steel, 0);
        }
        save(r, name);
    }

    static void wall(String name, int size, int rgb) throws IOException{
        int s = size * T;
        R r = new R(s);
        Mat body = dull(mixi(0x989aa4, rgb, 0.3f)), armor = dull(rgb);
        r.fill(r.rect(0, 0, s, s), 6, body, 2);
        if(size == 1){
            r.fill(r.oct(6, 6, 20, 20, 4), 9, armor, 2);
            r.fill(r.rect(14, 14, 4, 4), 7, body, 0);
        }else{
            for(int row = 0; row < 2; row++) for(int col = 0; col < 2; col++){
                r.fill(r.oct(5 + col * (s / 2 - 2), 5 + row * (s / 2 - 2), s / 2 - 8, s / 2 - 8, 4), 9, armor, 2);
            }
            r.fill(r.rect(s / 2 - 3, s / 2 - 3, 6, 6), 9, body, 1);
        }
        cornerBolts(r, 2);
        save(r, name);
    }

    static void router(String name, int size, int a, int[] outputs, int[] inputs) throws IOException{
        int s = size * T;
        float c = s / 2f;
        R r = new R(s);
        housing(r);
        int ch = size == 1 ? 8 : 10;
        for(int side = 0; side < 4; side++){
            boolean out = contains(outputs, side), in = contains(inputs, side);
            if(!out && !in) continue;
            boolean[] lane = r.bar(c, c, 0, c - 2, ch, side * 90);
            r.fill(lane, 3, deep, 0);
            r.fill(r.minus(r.bar(c, c, 0, c - 2, ch + 2, side * 90), lane), 7, steel, 0);
            if(out){
                //arrow decal pointing outwards
                r.fill(r.tri(c, c, new float[]{c - 10, c - 4, c - 10}, new float[]{-ch / 2f + 1, 0, ch / 2f - 1}, side * 90), 3.5f, acc(a), 0);
            }
        }
        int core = size == 1 ? 12 : 20;
        r.fill(r.oct(c - core / 2f - 1, c - core / 2f - 1, core + 2, core + 2, 3), 9, metal, 2);
        r.fill(r.rect((int)c - core / 2 + 3, (int)c - core / 2 + 3, core - 6, core - 6), 6, deep, 0);
        r.fill(r.rect((int)c - 2, (int)c - 2, 4, 4), 8, acc(a), 1);
        save(r, name);
    }

    static boolean contains(int[] values, int value){
        for(int v : values) if(v == value) return true;
        return false;
    }

    /**
     * Lattice Drill: a 3x3 core with a crystal drill head and four lattice roots leaving through the edge middles, plus
     * "-node": one white lattice tile (a node with arms to all four edges, so neighbouring tiles join up), tinted in game.
     */
    static void latticeDrill(String name) throws IOException{
        int s = 3 * T;
        latticeDrillArt(name);

        BufferedImage node = img(T);
        Graphics2D g = g(node);
        g.setColor(new Color(255, 255, 255, 150));
        g.fillRect(15, 0, 2, 32);
        g.fillRect(0, 15, 32, 2);
        g.setColor(new Color(255, 255, 255, 90));
        g.drawPolygon(new int[]{16, 26, 16, 6}, new int[]{6, 16, 26, 16}, 4);
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRect(14, 14, 4, 4);
        g.dispose();
        save(node, name + "-node");

        //"-head": a three-bladed drill bit with a crystal hub, swung over the patch on an arm and spun in game
        R head = new R(T);
        float hc = T / 2f;
        for(int i = 0; i < 3; i++) head.fill(head.bar(hc, hc, 3, hc - 4, 6, i * 120), 6, steel, 1.5f);
        for(int i = 0; i < 3; i++) head.fill(head.bar(hc, hc, hc - 8, 4, 4, i * 120 + 18), 7, acc(0x7fe0ff), 1);
        head.fill(head.circle(hc, hc, 6.5f), 8, metal, 2);
        head.dome(head.circle(hc, hc, 4), 6, 3, glass(0x7fe0ff));
        save(head, name + "-head");
    }

    static void drill(String name, int size, int a) throws IOException{
        int s = size * T;
        float c = s / 2f;
        R base = new R(s);
        housing(base);
        base.fill(base.circle(c, c, c - 5), 9, metal, 2);
        base.fill(base.circle(c, c, c - 9), 2, deep, 0);
        for(int[] p : new int[][]{{3, 3}, {s - 10, 3}, {3, s - 10}, {s - 10, s - 10}}){
            base.fill(base.rect(p[0], p[1], 7, 7), 10, steel, 1);
            bolt(base, p[0] + 2, p[1] + 2);
        }
        save(base, name);

        R rot = new R(s);
        for(int i = 0; i < 4; i++){
            rot.fill(rot.bar(c, c, 3, c - 12, size == 3 ? 9 : 7, i * 90), 5, steel, 1.5f);
            rot.fill(rot.bar(c, c, c - 13, 4, size == 3 ? 7 : 5, i * 90), 6, acc(a), 1);
        }
        save(rot, name + "-rotator");

        R top = new R(s);
        top.fill(top.oct(c - 11, c - 11, 22, 22, 7), 6, metal, 2);
        top.dome(top.circle(c, c, 7), 7, 3, metal);
        top.fill(top.circle(c, c, 3), 10.5f, acc(a), 1);
        save(top, name + "-top");

        BufferedImage rimLayer = img(s);
        Graphics2D rg = g(rimLayer);
        rg.setColor(new Color(255, 255, 255, 90));
        rg.draw(new Ellipse2D.Float(6, 6, s - 13, s - 13));
        rg.dispose();
        save(rimLayer, name + "-rim");
    }

    /**
     * Flow Meter: a framed LCD screen (the game draws the digits, item icon and "IPS" on it) over a vent grill.
     * The screen stays upright; the in/out ports are a separate "-ports" layer that turns with the block.
     * Screen area for the game's text: x 4-27, y 4-24 (image coordinates).
     */
    /**
     * The ECLIPSE logo ("logo"): blocky 5x7 pixel letters in the spirit of the Mindustry logo, bevelled metal (light on
     * top, darker below, a highlight on each top edge), a dark outline and a thin orange rim outside it.
     */
    static void logo(String name) throws IOException{
        String[][] letters = {
            {"11111", "10000", "10000", "11110", "10000", "10000", "11111"},
            {"01111", "10000", "10000", "10000", "10000", "10000", "01111"},
            {"10000", "10000", "10000", "10000", "10000", "10000", "11111"},
            {"11111", "00100", "00100", "00100", "00100", "00100", "11111"},
            {"11110", "10001", "10001", "11110", "10000", "10000", "10000"},
            {"01111", "10000", "10000", "01110", "00001", "00001", "11110"},
            {"11111", "10000", "10000", "11110", "10000", "10000", "11111"},
        };
        int cell = 12, gap = 12, pad = 10, lw = 5 * cell, lh = 7 * cell;
        int w = letters.length * lw + (letters.length - 1) * gap + pad * 2, h = lh + pad * 2;
        boolean[][] face = new boolean[w][h];
        for(int l = 0; l < letters.length; l++){
            int ox = pad + l * (lw + gap);
            for(int row = 0; row < 7; row++) for(int col = 0; col < 5; col++){
                if(letters[l][row].charAt(col) != '1') continue;
                for(int px = 0; px < cell; px++) for(int py = 0; py < cell; py++) face[ox + col * cell + px][pad + row * cell + py] = true;
            }
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int outline = 5, rim = 2;
        for(int x = 0; x < w; x++) for(int y = 0; y < h; y++){
            if(face[x][y]){
                //metal: light on top, darker toward the bottom, a highlight where the face starts below an edge
                float f = (y - pad) / (float)lh;
                int base = mixi(0xf2f3f8, 0x8d8fa0, Math.min(1f, Math.max(0f, f)));
                if(y > 0 && !face[x][y - 1] || y > 1 && !face[x][y - 2]) base = 0xffffff;
                if(y < h - 1 && !face[x][y + 1] || y < h - 2 && !face[x][y + 2]) base = mixi(base, 0x50525e, 0.6f);
                img.setRGB(x, y, 0xff000000 | base);
                continue;
            }
            int d = Integer.MAX_VALUE;
            for(int dx = -outline - rim; dx <= outline + rim; dx++) for(int dy = -outline - rim; dy <= outline + rim; dy++){
                int nx = x + dx, ny = y + dy;
                if(nx >= 0 && ny >= 0 && nx < w && ny < h && face[nx][ny]) d = Math.min(d, Math.max(Math.abs(dx), Math.abs(dy)));
            }
            if(d <= outline) img.setRGB(x, y, 0xff16161d);
            else if(d <= outline + rim) img.setRGB(x, y, 0xffff9a3c);
        }
        save(img, name);
    }

    static void flowMeter(String name) throws IOException{
        R r = new R(T);
        housing(r);
        r.fill(r.oct(1, 1, 30, 30, 3), 8, steel, 1);
        r.fill(r.rect(3, 3, 26, 23), 3, deep, 0);
        //screen: very dark green with faint scanlines
        r.paint(r.rect(4, 4, 24, 21), 0xff0e2016);
        for(int y = 5; y < 25; y += 2) r.paint(r.rect(4, y, 24, 1), 0xff11281b);
        //vent grill under the screen
        r.fill(r.rect(4, 26, 24, 3), 4, deep, 0);
        for(int x = 5; x < 28; x += 3) r.fill(r.rect(x, 27, 1, 1), 6, steel, 0);
        save(r, name);

        //ports: input stub at the back (left), output stub at the front (right), each with a green arrow pointing along the flow
        R p = new R(T);
        p.fill(p.rect(0, 12, 3, 8), 9, metal, 1);
        p.fill(p.rect(29, 12, 3, 8), 9, metal, 1);
        for(int x0 : new int[]{0, 29}){
            p.paint(p.rect(x0, 14, 1, 4), 0xff4cd964);
            p.paint(p.rect(x0 + 1, 15, 1, 2), 0xff4cd964);
        }
        save(p, name + "-ports");
    }

    static void coupler(String name) throws IOException{
        R r = new R(T);
        housing(r);
        r.fill(r.circle(16, 16, 9), 9, metal, 2);
        r.fill(r.circle(16, 16, 5.5f), 3, deep, 0);
        r.fill(r.rect(26, 12, 4, 8), 8, brass, 1);
        save(r, name);

        BufferedImage light = img(T);
        Graphics2D l = g(light);
        l.setColor(new Color(255, 255, 255, 120));
        l.fill(new Ellipse2D.Float(11, 11, 10, 10));
        l.setColor(Color.white);
        l.fill(new Ellipse2D.Float(14, 14, 4, 4));
        l.dispose();
        save(light, name + "-light");

        R arm = new R(T);
        arm.cylH(21, 12, 11, 8, 8, 3, brass);
        arm.fill(arm.rect(21, 15, 11, 2), 11, acc(0x9a8650), 0);
        save(arm, name + "-connector");
    }

    static void hub(String name) throws IOException{
        int s = 2 * T;
        R r = new R(s);
        housing(r);
        deck(r, 4);
        for(int[] p : new int[][]{{10, 10}, {34, 10}, {10, 34}, {34, 34}}){
            r.fill(r.rect(p[0], p[1], 20, 20), 9, metal, 2);
            r.fill(r.circle(p[0] + 10, p[1] + 10, 6), 3, deep, 0);
        }
        save(r, name);

        BufferedImage light = img(s);
        Graphics2D l = g(light);
        for(int[] p : new int[][]{{15, 15}, {39, 15}, {15, 39}, {39, 39}}){
            l.setColor(new Color(255, 255, 255, 140));
            l.fill(new Ellipse2D.Float(p[0], p[1], 10, 10));
            l.setColor(Color.white);
            l.fillRect(p[0] + 3, p[1] + 3, 4, 4);
        }
        l.dispose();
        save(light, name + "-light");

        R arm = new R(s);
        arm.cylH(s - 9, s / 2 - 6, 9, 12, 8, 4, brass);
        save(arm, name + "-connector");
    }

    static void worldEngine(String name) throws IOException{
        int s = 6 * T;
        float c = s / 2f;
        R r = new R(s);
        housing(r);
        deck(r, 6);
        r.dome(r.ring(c, c, 70, 84), 6, 6, metal);
        r.fill(r.circle(c, c, 70), 3, deep, 0);
        for(int i = 0; i < 12; i++) r.fill(r.bar(c, c, 50, 22, 6, i * 30), 7, steel, 1.5f);
        r.dome(r.ring(c, c, 34, 46), 6, 5, brass);
        r.fill(r.oct(c - 34, c - 34, 68, 68, 20), 4, deep, 0);
        r.fill(r.oct(c - 26, c - 26, 52, 52, 15), 8, acc(0xf08a2c), 4);
        r.fill(r.oct(c - 12, c - 12, 24, 24, 7), 10, acc(0xffb648), 3);
        for(float[] p : new float[][]{{22, 22}, {s - 22, 22}, {22, s - 22}, {s - 22, s - 22}}){
            r.fill(r.oct(p[0] - 12, p[1] - 12, 24, 24, 7), 9, metal, 3);
            r.dome(r.circle(p[0], p[1], 6), 9, 4, brass);
            r.fill(r.circle(p[0], p[1], 2), 14, acc(0xffb648), 0);
        }
        save(r, name);

        BufferedImage glow = img(s);
        Graphics2D gl = g(glow);
        gl.setColor(new Color(255, 255, 255, 200));
        gl.setStroke(new BasicStroke(3f));
        gl.draw(new Ellipse2D.Float(c - 40, c - 40, 80, 80));
        gl.setColor(new Color(255, 255, 255, 120));
        gl.fill(new Ellipse2D.Float(c - 10, c - 10, 20, 20));
        for(float[] p : new float[][]{{22, 22}, {s - 22, 22}, {22, s - 22}, {s - 22, s - 22}}) gl.fill(new Ellipse2D.Float(p[0] - 3, p[1] - 3, 6, 6));
        gl.dispose();
        save(glow, name + "-glow");
    }

    static void drone(String name, int rgb) throws IOException{
        int s = 48;
        float c = s / 2f;
        R r = new R(s);
        for(int i = 0; i < 4; i++){
            double ang = Math.toRadians(45 + i * 90);
            float px = c + (float)Math.cos(ang) * 15, py = c - (float)Math.sin(ang) * 15;
            r.fill(r.bar(c, c, 4, 13, 4, 45 + i * 90), 4, steel, 1);
            r.dome(r.ring(px, py, 4, 7.5f), 4, 2.5f, metal);
            r.fill(r.circle(px, py, 4), 2, deep, 0);
            r.fill(r.bar(px, py, -4, 8, 1.5f, i * 50), 3, steel, 0);
        }
        r.fill(r.oct(c - 10, c - 10, 20, 20, 6), 6, metal, 2);
        r.dome(r.oct(c - 7, c - 7, 14, 14, 4), 6, 3, metal);
        r.fill(r.rect((int)c + 3, (int)c - 3, 4, 6), 9.5f, acc(rgb), 1);
        save(r, name);

        BufferedImage cell = img(s);
        Graphics2D cg = g(cell);
        cg.setColor(Color.white);
        cg.fill(new Ellipse2D.Float(c - 5, c - 5, 10, 10));
        cg.dispose();
        save(cell, name + "-cell");
    }

    // =====================================================================================================
    // Ores (blocks/environment, 3 variants + base) and item / liquid / module icons (32x32)
    // =====================================================================================================

    static void ore(String name, int rgb) throws IOException{
        Random rand = new Random(name.hashCode());
        Mat m = acc(rgb);
        File dir = new File(out, "blocks/environment");
        dir.mkdirs();
        for(int v = 0; v <= 3; v++){
            R r = new R(T);
            List<float[]> stones = new ArrayList<>();
            int want = 3 + rand.nextInt(3);
            for(int t = 0; t < 400 && stones.size() < want; t++){
                float rad = 3.2f + rand.nextFloat() * 2.8f;
                float x = rad + 2 + rand.nextFloat() * (T - rad * 2 - 4), y = rad + 1 + rand.nextFloat() * (T - rad * 2 - 4);
                boolean ok = true;
                for(float[] o : stones) if(Math.hypot(o[0] - x, o[1] - y) < o[2] + rad + 1.5f) ok = false;
                if(ok) stones.add(new float[]{x, y, rad});
            }
            for(float[] st : stones){
                //irregular, slightly elongated pebble
                int n = 7;
                double rot = rand.nextDouble() * Math.PI, stretch = 1 + rand.nextDouble() * 0.35;
                Path2D.Float p = new Path2D.Float();
                for(int k = 0; k < n; k++){
                    double ang = Math.PI * 2 * k / n + rand.nextDouble() * 0.4;
                    double rr = st[2] * (0.82 + rand.nextDouble() * 0.3);
                    double px = Math.cos(ang) * rr * stretch, py = Math.sin(ang) * rr;
                    double fx = st[0] + px * Math.cos(rot) - py * Math.sin(rot), fy = st[1] + px * Math.sin(rot) + py * Math.cos(rot);
                    if(k == 0) p.moveTo(fx, fy); else p.lineTo(fx, fy);
                }
                p.closePath();
                r.dome(r.shape(p), 1, st[2] * 0.9f, m);
            }
            BufferedImage img = r.render();
            //drop shadow towards the lower left
            BufferedImage withShadow = new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
            for(int y = 0; y < T; y++) for(int x = 0; x < T; x++){
                int px = x + 1, py = y - 2;
                if(px < T && py >= 0 && (img.getRGB(px, py) >>> 24) != 0) withShadow.setRGB(x, y, 0x60000000);
            }
            Graphics2D g = withShadow.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            ImageIO.write(withShadow, "png", new File(dir, (v == 0 ? name : name + v) + ".png"));
        }
    }

    static void chunks(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        int[][][] rocks = {
            {{3, 9, 15, 16, 10, 3}, {14, 8, 10, 18, 24, 21}},
            {{11, 19, 27, 28, 21, 12}, {6, 3, 6, 15, 18, 13}},
            {{13, 22, 29, 28, 19, 12}, {20, 16, 19, 27, 29, 26}},
        };
        for(int[][] k : rocks) r.dome(r.poly(k[0], k[1]), 2, 4.5f, m);
        save(r, name);
    }

    static void ingot(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        r.fill(r.oct(3, 8, 26, 17, 4), 7, m, 4);
        r.raise(r.rect(9, 15, 14, 2), -1.5f);
        save(r, name);
    }

    static void sulfurIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        r.fill(r.poly(new int[]{3, 9, 16, 11}, new int[]{20, 8, 22, 30}), 8, m, 30);
        r.fill(r.poly(new int[]{12, 20, 29, 22}, new int[]{13, 2, 15, 27}), 9, m, 30);
        save(r, name);
    }

    static void pellets(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        float[][] ps = {{8, 18}, {17, 9}, {20, 21}, {9, 7}, {26, 14}};
        for(float[] p : ps) r.dome(r.circle(p[0], p[1], 4.6f), 2, 3.5f, m);
        save(r, name);
    }

    static void gearIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        boolean[] gear = r.circle(16, 16, 10.5f);
        for(int i = 0; i < 8; i++) gear = r.or(gear, r.bar(16, 16, 8, 6.5f, 5, i * 45));
        r.fill(gear, 5, m, 2);
        r.raise(r.ring(16, 16, 3.5f, 6.5f), 1.5f);
        r.clear(r.circle(16, 16, 3.5f));
        save(r, name);
    }

    static void ringIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        r.fill(r.ring(16, 16, 9.5f, 13.5f), 5, m, 1);
        r.fill(r.ring(16, 16, 6, 9.5f), 2, deep, 0);
        for(int i = 0; i < 8; i++){
            double ang = Math.PI * 2 * i / 8;
            r.dome(r.circle(16 + (float)Math.cos(ang) * 8, 16 + (float)Math.sin(ang) * 8, 2.4f), 3, 2.5f, metal);
        }
        r.fill(r.ring(16, 16, 3, 6), 5, m, 1);
        save(r, name);
    }

    static void rod(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        for(int i = 0; i < 3; i++){
            int x = 6 + i * 7;
            r.cylV(x, 6, 6, 21, 3, 3, m);
            r.cylV(x, 3, 6, 4, 4, 3, brass);
            r.cylV(x, 26, 6, 4, 4, 3, brass);
        }
        save(r, name);
    }

    static void wafer(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        r.fill(r.circle(16, 16, 13), 4, m, 1);
        for(int i = 8; i < 26; i += 4){
            r.raise(r.rect(5, i, 22, 1), -0.8f);
            r.raise(r.rect(i, 5, 1, 22), -0.8f);
        }
        r.clear(r.rect(0, 27, T, 5));
        r.fill(r.rect(9, 27, 14, 1), 3, m, 0);
        save(r, name);
    }

    static void chip(String name, int rgb) throws IOException{
        R r = new R(T);
        for(int i = 9; i <= 21; i += 4){
            r.fill(r.rect(i, 3, 2, 5), 2, brass, 0);
            r.fill(r.rect(i, 24, 2, 5), 2, brass, 0);
            r.fill(r.rect(3, i, 5, 2), 2, brass, 0);
            r.fill(r.rect(24, i, 5, 2), 2, brass, 0);
        }
        r.fill(r.rect(7, 7, 18, 18), 5, dull(0x3a3d45), 2);
        r.fill(r.rect(11, 11, 10, 10), 6, acc(rgb), 1);
        save(r, name);
    }

    static void pane(String name, int rgb) throws IOException{
        R r = new R(T);
        r.fill(r.poly(new int[]{5, 22, 28, 11}, new int[]{8, 3, 24, 29}), 4, glass(tint(rgb, 0.2f)), 2);
        r.paint(r.bar(12, 10, 0, 6, 1, -15), 0xccffffff);
        save(r, name);
    }

    static void coilIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(rgb);
        for(int i = 0; i < 8; i++) r.cylH(8, 8 + i * 2, 16, 2, 4, 1.2f, m);
        r.fill(r.rect(5, 5, 22, 3), 6, steel, 1);
        r.fill(r.rect(5, 24, 22, 3), 6, steel, 1);
        save(r, name);
    }

    static void boardIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        r.fill(r.rect(3, 6, 26, 20), 3, acc(rgb), 1);
        r.fill(r.rect(7, 10, 11, 1), 3.6f, brass, 0);
        r.fill(r.rect(17, 10, 1, 11), 3.6f, brass, 0);
        r.fill(r.rect(7, 20, 18, 1), 3.6f, brass, 0);
        r.fill(r.rect(6, 9, 3, 3), 3.8f, brass, 0);
        r.fill(r.rect(22, 19, 3, 3), 3.8f, brass, 0);
        r.fill(r.rect(19, 9, 7, 6), 5, dull(0x2e3036), 1);
        save(r, name);
    }

    static void magnetIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        boolean[] arc = r.and(r.ring(16, 14, 5, 12), r.rect(0, 0, T, 14));
        boolean[] legs = r.or(r.rect(4, 13, 7, 14), r.rect(21, 13, 7, 14));
        r.fill(r.or(arc, legs), 5, acc(rgb), 2);
        r.recolor(r.rect(0, 23, T, 5), metal);
        save(r, name);
    }

    static void cellIcon(String name, int rgb) throws IOException{
        R r = new R(T);
        r.dome(r.rect(10, 2, 4, 4), 3, 1, redM);
        r.dome(r.rect(18, 2, 4, 4), 3, 1, metal);
        r.fill(r.oct(6, 5, 20, 25, 3), 5, acc(rgb), 3);
        r.recolor(r.rect(9, 13, 14, 3), dull(0xe3ecf0));
        save(r, name);
    }

    static void liquid(String name, int rgb) throws IOException{
        R r = new R(T);
        Path2D.Float drop = new Path2D.Float();
        drop.moveTo(16, 2);
        drop.curveTo(16, 2, 27, 15, 27, 20);
        drop.curveTo(27, 26.5, 22, 30, 16, 30);
        drop.curveTo(10, 30, 5, 26.5, 5, 20);
        drop.curveTo(5, 15, 16, 2, 16, 2);
        r.dome(r.shape(drop), 2, 8, acc(rgb));
        r.paint(r.rect(19, 15, 2, 5), 0xd0ffffff);
        save(r, name);
    }

    static void gas(String name, int rgb) throws IOException{
        R r = new R(T);
        Mat m = acc(tint(rgb, 0.1f));
        r.dome(r.circle(10, 20, 7.5f), 2, 5, m);
        r.dome(r.circle(22, 21, 7.5f), 2, 5, m);
        r.dome(r.circle(15, 10, 7.5f), 2, 5, m);
        r.paint(r.rect(17, 7, 2, 2), 0xd0ffffff);
        save(r, name);
    }

    static final java.util.Set<String> legendary = java.util.Set.of("quantum-accelerator", "matter-replicator", "ancient-efficiency-core",
        "temporal-processing-core", "mass-production", "closed-loop-processing", "emergency-overdrive");

    /**
     * Upgrade module icons (art.md §16): one shared cartridge for all 70 - casing, four contact pins - with the family
     * shown by the mechanism in the middle and the tier by the casing: II corner braces, III an inner frame,
     * IV a dark casing with an experimental stripe, V an aetherite insert, legendary a refined brass-edged casing.
     */
    static void moduleIcon(String name, int rgb, int tier) throws IOException{
        String family = name.replaceAll("-\\d$", "");
        boolean legend = legendary.contains(family);
        R r = canvas(T);
        for(int x = 8; x <= 22; x += 4) r.fill(r.rect(x, 25, 2, 5), 3, brass, 0);
        Mat body = tier >= 4 || legend ? steel : metal;
        r.fill(r.oct(3, 3, 26, 23, 4), 5, body, 2);
        if(legend) r.fill(r.minus(r.oct(3, 3, 26, 23, 4), r.oct(5, 5, 22, 19, 3)), 6, brass, 1);
        if(tier >= 2 && !legend) for(int[] b : new int[][]{{3, 3}, {25, 3}, {3, 22}, {25, 22}}) r.fill(r.rect(b[0], b[1], 4, 4), 6.5f, steel, 1);
        if(tier >= 3 && !legend) r.fill(r.minus(r.rect(6, 6, 20, 17), r.rect(7, 7, 18, 15)), 6, steel, 0);
        //tier pips on the top edge for I-III (IV and V have their own marks)
        if(tier <= 3 && !legend) for(int k = 0; k < tier; k++) r.fill(r.rect(8 + k * 4, 3, 3, 3), 7, brass, 0);
        if(tier == 4) r.fill(r.rect(9, 4, 14, 2), 6.5f, acc(0xb37aff), 0);
        if(tier >= 5 && !legend) r.fill(r.poly(new int[]{25, 28, 25, 22}, new int[]{3, 6, 9, 6}), 7, lit(0x7fe0ff), 0);
        r.fill(r.rect(8, 7, 16, 15), 4, deep, 0);
        Mat a = acc(rgb);
        float cx = 16, cy = 14.5f;
        switch(family){
            case "production-accelerator" -> {
                r.fill(r.ring(cx, cy, 2, 5), 7, a, 1);
                for(int k = 0; k < 6; k++) r.fill(r.bar(cx, cy, 4, 3, 2, k * 60), 7, a, 0);
            }
            case "power-optimizer" -> {
                r.fill(r.rect(14, 8, 4, 13), 6, steel, 0);
                for(int y = 9; y < 21; y += 3) r.fill(r.rect(11, y, 10, 2), 7, a, 0);
            }
            case "output-amplifier" -> {
                for(int x : new int[]{10, 18}) r.fill(r.rect(x, 8, 4, 13), 7, a, 1);
                r.fill(r.rect(10, 13, 12, 2), 6, steel, 0);
            }
            case "precision-fabricator" -> {
                r.fill(r.rect(10, 9, 12, 11), 6, steel, 1);
                r.fill(r.rect(15, 9, 2, 11), 7, a, 0);
                r.fill(r.rect(10, 14, 12, 2), 7, a, 0);
            }
            case "material-recycler" -> {
                r.fill(r.poly(new int[]{9, 23, 19, 13}, new int[]{8, 8, 14, 14}), 6, steel, 1);
                r.fill(r.rect(14, 14, 4, 7), 7, a, 0);
            }
            case "reaction-accelerator" -> {
                r.fill(r.circle(cx, cy, 6), 6, steel, 1);
                r.fill(r.circle(cx, cy, 4), 7, a, 1);
                r.fill(r.bar(cx, cy, -4, 8, 2, 30), 8, metal, 0);
            }
            case "thermal-controller" -> {
                r.fill(r.rect(9, 9, 14, 11), 6, steel, 1);
                for(int y = 10; y < 20; y += 3) r.fill(r.rect(10, y, 12, 1), 7, a, 0);
            }
            case "pressure-stabilizer" -> {
                r.fill(r.ring(cx, cy, 3, 6), 7, steel, 1);
                r.fill(r.circle(cx, cy, 3), 6, a, 1);
                r.fill(r.rect(15, 8, 2, 3), 8, metal, 0);
            }
            case "catalyst-optimizer" -> {
                r.fill(r.oct(12, 8, 8, 13, 3), 6, steel, 1);
                r.fill(r.rect(14, 11, 4, 7), 7, a, 0);
            }
            case "purification-module" -> {
                for(int k = 0; k < 2 + tier; k++) r.fill(r.rect(10, 9 + k * 3, 12, 2), 6 + k * 0.3f, k % 2 == 0 ? a : steel, 0);
            }
            case "waste-recovery" -> {
                r.fill(r.minus(r.ring(cx, cy, 3, 6), r.rect(16, 8, 7, 4)), 7, a, 0);
                r.fill(r.circle(cx, cy, 2), 6, steel, 0);
            }
            case "thermal-recovery" -> {
                r.fill(r.circle(cx, cy, 6), 6, steel, 1);
                for(int k = 0; k < 4; k++) r.fill(r.bar(cx, cy, 1, 5, 2, k * 90 + 20), 7, a, 0);
            }
            case "energy-overcharger", "emergency-overdrive" -> {
                for(int x : new int[]{9, 13}) r.fill(r.rect(x, 9, 3, 11), 7, a, 0);
                r.fill(r.circle(20, cy, 4), 6, steel, 1);
                r.fill(r.circle(20, cy, 1.5f), 7, metal, 0);
            }
            case "capacitor-module" -> {
                for(int x = 9; x < 23; x += 5) r.fill(r.rect(x, 9, 4, 11), 7, a, 1);
            }
            case "deep-mining" -> {
                r.fill(r.rect(12, 8, 8, 4), 6, steel, 1);
                r.fill(r.poly(new int[]{11, 21, 16}, new int[]{12, 12, 21}), 7, a, 1);
            }
            case "advanced-drill-head" -> {
                r.fill(r.circle(cx, cy, 6), 6, steel, 1);
                for(int k = 0; k < 6; k++) r.fill(r.bar(cx, cy, 3, 4, 3, k * 60), 7, a, 0);
                r.fill(r.circle(cx, cy, 1.5f), 8, metal, 0);
            }
            case "geological-analyzer" -> {
                r.fill(r.poly(new int[]{16, 20, 16, 12}, new int[]{9, 14, 19, 14}), 7, a, 1);
                for(int k = 0; k < 4; k++) r.fill(r.bar(cx, cy, 6, 2, 1, k * 90 + 45), 7, steel, 0);
            }
            case "rapid-fire-controller" -> {
                r.fill(r.circle(13, cy, 4), 6, steel, 1);
                r.fill(r.circle(13, cy, 2), 7, a, 0);
                for(int y = 9; y < 21; y += 3) r.fill(r.rect(19, y, 4, 2), 7, metal, 0);
            }
            case "cooling-system" -> {
                for(int x = 9; x < 23; x += 3) r.fill(r.rect(x, 9, 2, 11), 7, a, 0);
            }
            case "advanced-ammunition" -> {
                r.fill(r.rect(13, 12, 6, 9), 6, brass, 1);
                r.fill(r.poly(new int[]{13, 19, 16}, new int[]{12, 12, 8}), 7, a, 0);
            }
            case "targeting-computer" -> {
                r.fill(r.ring(cx, cy, 3, 5), 7, steel, 1);
                r.fill(r.circle(cx, cy, 2), 7, a, 0);
                if(tier >= 2) for(int x : new int[]{9, 21}) r.fill(r.rect(x, 9, 2, 2), 7, a, 0);
            }
            case "ballistic-computer" -> {
                r.fill(r.ring(13, 15, 2, 4), 7, steel, 1);
                r.fill(r.bar(13, 15, 3, 9, 2, 35), 7, a, 0);
            }
            case "mass-production" -> {
                for(float gx : new float[]{12, 20}){
                    r.fill(r.ring(gx, cy, 1.5f, 3.5f), 7, a, 1);
                    for(int k = 0; k < 6; k++) r.fill(r.bar(gx, cy, 3, 2, 2, k * 60), 7, a, 0);
                }
            }
            case "closed-loop-processing" -> {
                r.fill(r.ring(cx, cy, 4, 6), 7, a, 0);
                r.fill(r.circle(cx, cy, 2.5f), 6, steel, 1);
            }
            case "quantum-accelerator" -> {
                r.fill(r.ring(cx, cy, 4, 6), 7, steel, 1);
                r.fill(r.circle(cx, cy, 2.5f), 8, lit(0xc88fff), 0);
            }
            case "matter-replicator" -> {
                for(int x : new int[]{9, 18}) r.fill(r.rect(x, 9, 5, 11), 6, steel, 1);
                r.fill(r.rect(15, 12, 2, 5), 8, lit(0xc88fff), 0);
            }
            case "ancient-efficiency-core" -> {
                r.fill(r.poly(new int[]{16, 21, 16, 11}, new int[]{8, 14, 20, 14}), 7, steel, 1);
                r.fill(r.poly(new int[]{16, 18, 16, 14}, new int[]{11, 14, 17, 14}), 8, lit(0x9fe8ff), 0);
            }
            case "temporal-processing-core" -> {
                r.fill(r.ring(cx, cy, 5, 6.5f), 7, steel, 0);
                r.fill(r.ring(cx, cy, 2.5f, 4), 7.5f, a, 0);
                r.fill(r.circle(cx, cy, 1.5f), 8, lit(0xc88fff), 0);
            }
            default -> r.fill(r.rect(10, 9, 12, 11), 6, a, 1);
        }
        save(r, name);
    }

    // ---- vanilla-based transport recolours ----

    static Map<String, BufferedImage> loadVanilla(File gameJar, String regex) throws IOException{
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
            if(!r.name.matches(regex)) continue;
            BufferedImage page = pages.computeIfAbsent(r.page.textureFile.path(), p -> {
                try{ return ImageIO.read(new File(p)); }catch(IOException ex){ throw new UncheckedIOException(ex); }
            });
            result.put(r.name, page.getSubimage(r.left, r.top, r.width, r.height));
        }
        return result;
    }

    static void polymer(String vanillaName, String name) throws IOException{
        recolor(vanillaName, name, 0x5f7468, 0xdfe9e1);
    }

    static void recolor(String vanillaName, String name, int darkRgb, int lightRgb) throws IOException{
        BufferedImage src = vanilla.get(vanillaName);
        if(src == null) throw new IOException("vanilla sprite not found: " + vanillaName);
        Color dark = new Color(darkRgb), light = new Color(lightRgb);
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < src.getHeight(); y++){
            for(int x = 0; x < src.getWidth(); x++){
                int argb = src.getRGB(x, y), a = argb >>> 24, r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                if(a == 0 || (r == 255 && g == 255 && b == 255)){
                    img.setRGB(x, y, argb);
                    continue;
                }
                float lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
                float t = Math.max(0f, Math.min(1f, (lum - 0.3f) / 0.5f));
                int nr = (int)(dark.getRed() + (light.getRed() - dark.getRed()) * t);
                int ng = (int)(dark.getGreen() + (light.getGreen() - dark.getGreen()) * t);
                int nb = (int)(dark.getBlue() + (light.getBlue() - dark.getBlue()) * t);
                img.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        save(img, name);
    }
}
