package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Metal extraction and alloys. */
public class MetallurgyBlocks{
    public static Block oreCrusher, oreWasher, inductionFurnace,
        mineralSeparator, platinumRefinery, lithiumExtractor, alloyFurnace, sinteringFurnace, carbideDrill, superalloyWall, superalloyWallLarge,
        aluminaRefinery, aluminiumSmelter, glassKiln, arcFurnace, surgeSuperalloyWall, surgeSuperalloyWallLarge;

    public static void load(){
        // Slag + sulfuric acid -> trace metals. The metal source on maps without the new ores.
        mineralSeparator = new Separator("mineral-separator"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.titanium, 50, Items.silicon, 40, Items.metaglass, 30, titaniumGear, 10, pressureCasing, 5));
            size = 2;
            health = 320;
            craftTime = 40f;
            itemCapacity = 20;
            liquidCapacity = 30f;
            results = with(nickel, 5, chromium, 3, cobalt, 2, molybdenum, 2, vanadium, 2, tungsten, 1);
            consumeLiquids(LiquidStack.with(Liquids.slag, 4f / 60f, sulfuricAcid, 2f / 60f));
            consumePower(1.1f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.slag, 3f), new DrawDefault());
        }};

        // Scrap + aqua regia -> platinum (recovered from old electronics), leaving acidic waste.
        platinumRefinery = new ChemicalReactor("platinum-refinery"){{
            requirements(Category.crafting, with(polyethylene, 40, Items.titanium, 50, Items.silicon, 40, Items.metaglass, 30, pressureCasing, 15));
            size = 2;
            health = 320;
            itemCapacity = 20;
            liquidCapacity = 30f;
            craftTime = 120f;
            consumeItems(with(Items.scrap, 4));
            consumeLiquid(aquaRegia, 4f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(platinum, 1);
            outputLiquid = new LiquidStack(acidicWaste, 3f / 60f);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(aquaRegia, 3f), new DrawDefault());
        }};

        // Brine -> lithium.
        lithiumExtractor = new GenericCrafter("lithium-extractor"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.silicon, 40, Items.metaglass, 40, pressureCasing, 5, titaniumRod, 5));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 90f;
            consumeLiquid(brine, 6f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(lithium, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(brine, 3f), new DrawDefault());
        }};

        // Nickel + chromium + cobalt + heat -> superalloy.
        alloyFurnace = new HeatCrafter("alloy-furnace"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 80, Items.silicon, 60, Items.graphite, 60, titaniumRod, 20, industrialBearing, 5));
            size = 3;
            health = 700;
            itemCapacity = 20;
            craftTime = 90f;
            heatRequirement = 8f;
            maxEfficiency = 2f;
            consumeItems(with(nickel, 2, chromium, 1, cobalt, 1));
            consumePower(1f);
            outputItem = new ItemStack(superalloy, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ffb46b"); }}, new DrawHeatInput());
        }};

        // Tungsten + graphite -> tungsten carbide (powder metallurgy).
        sinteringFurnace = new GenericCrafter("sintering-furnace"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 50, Items.silicon, 40, Items.graphite, 40, titaniumRod, 10, industrialBearing, 5));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(tungsten, 1, Items.graphite, 1));
            consumePower(2f);
            outputItem = new ItemStack(tungstenCarbide, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff6a3d"); }});
        }};

        // Faster than the laser drill at the same size.
        carbideDrill = new Drill("carbide-drill"){{
            requirements(Category.production, with(tungstenCarbide, 30, Items.titanium, 40, Items.silicon, 40, Items.graphite, 30, titaniumGear, 20, industrialBearing, 10, controlUnit, 5));
            size = 3;
            health = 600;
            tier = 4;
            //compact but fast: carbide bits cut hard ore easily (half the usual hardness penalty), water boost as strong as the airblast drill's
            drillTime = 120f;
            hardnessDrillMultiplier = 25f;
            liquidBoostIntensity = 1.8f;
            hasPower = true;
            consumePower(2.2f);
            consumeLiquid(Liquids.water, 0.08f).boost();
        }};

        superalloyWall = new Wall("superalloy-wall"){{
            requirements(Category.defense, with(superalloy, 6));
            health = 210 * 4;
            absorbLasers = true;
        }};

        superalloyWallLarge = new Wall("superalloy-wall-large"){{
            requirements(Category.defense, mult(superalloyWall.requirements, 4));
            size = 2;
            health = 210 * 4 * 4;
            absorbLasers = true;
        }};

        // ---- aluminium (Bayer + Hall-Heroult) ----

        // Sand + sodium hydroxide + heat -> alumina.
        aluminaRefinery = new HeatCrafter("alumina-refinery"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 60, Items.silicon, 40, Items.metaglass, 40, pressureCasing, 10, titaniumGear, 5));
            size = 3;
            health = 650;
            itemCapacity = 20;
            liquidCapacity = 30f;
            craftTime = 60f;
            heatRequirement = 6f;
            maxEfficiency = 2f;
            consumeItem(Items.sand, 3);
            consumeLiquid(sodiumHydroxide, 4f / 60f);
            consumePower(0.5f);
            outputItem = new ItemStack(alumina, 2);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(sodiumHydroxide, 4f), new DrawDefault(), new DrawHeatInput());
        }};

        // Alumina + graphite anodes + lots of power -> aluminium.
        aluminiumSmelter = new GenericCrafter("aluminium-smelter"){{
            requirements(Category.crafting, with(microchip, 15, Items.titanium, 60, Items.silicon, 50, Items.lead, 60, industrialBearing, 10));
            size = 3;
            health = 650;
            itemCapacity = 20;
            craftTime = 60f;
            consumeItems(with(alumina, 2, Items.graphite, 1));
            consumePower(5f);
            outputItem = new ItemStack(aluminium, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("9fd8ff"); }});
        }};

        // ---- advanced vanilla combinations ----

        // Metaglass + titanium + heat -> pressure-rated glass.
        glassKiln = new HeatCrafter("glass-kiln"){{
            requirements(Category.crafting, with(Items.lead, 40, Items.graphite, 30, Items.titanium, 30, Items.metaglass, 20, titaniumRod, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 60f;
            heatRequirement = 5f;
            maxEfficiency = 2f;
            consumeItems(with(Items.metaglass, 2, Items.titanium, 1));
            outputItem = new ItemStack(reinforcedGlass, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("9fd6ff"); }}, new DrawHeatInput());
        }};

        // Surge alloy + superalloy -> surge superalloy.
        arcFurnace = new GenericCrafter("arc-furnace"){{
            requirements(Category.crafting, with(superalloy, 40, tungstenCarbide, 30, microchip, 20, Items.titanium, 80, Items.silicon, 60, superalloyFrame, 10, precisionComponent, 5, tungsten, 20));
            size = 3;
            health = 800;
            itemCapacity = 20;
            craftTime = 120f;
            consumeItems(with(Items.surgeAlloy, 1, superalloy, 2));
            consumePower(8f);
            outputItem = new ItemStack(surgeSuperalloy, 1);
            craftEffect = Fx.lightningShoot;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("f2e878"); }});
        }};

        surgeSuperalloyWall = new Wall("surge-superalloy-wall"){{
            requirements(Category.defense, with(surgeSuperalloy, 6));
            health = 300 * 4;
            absorbLasers = true;
            lightningChance = 0.05f;
            lightningDamage = 30f;
        }};

        surgeSuperalloyWallLarge = new Wall("surge-superalloy-wall-large"){{
            requirements(Category.defense, mult(surgeSuperalloyWall.requirements, 4));
            size = 2;
            health = 300 * 4 * 4;
            absorbLasers = true;
            lightningChance = 0.05f;
            lightningDamage = 30f;
        }};
    
        // ---- ore processing: a steady scrap-to-metal route that needs no slag or acid ----

        // Scrap -> crushed ore, with sand left over.
        oreCrusher = new GenericCrafter("ore-crusher"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.graphite, 30, copperGear, 15, titaniumGear, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItem(Items.scrap, 3);
            consumePower(0.8f);
            outputItems = with(crushedOre, 2, Items.sand, 1);
            craftEffect = Fx.pulverize;
        }};

        // Crushed ore + water -> metals by density (jigs and flotation). Each craft yields one metal; rarely aetherite crystals.
        oreWasher = new Separator("ore-washer"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.metaglass, 30, pressureCasing, 5, titaniumGear, 5));
            size = 2;
            health = 320;
            craftTime = 30f;
            itemCapacity = 20;
            liquidCapacity = 30f;
            results = with(nickel, 8, chromium, 6, cobalt, 6, molybdenum, 4, vanadium, 4, tungsten, 4, platinum, 2, aetherite, 1);
            consumeItem(crushedOre, 1);
            consumeLiquid(Liquids.water, 6f / 60f);
            consumePower(0.6f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.water, 3f), new DrawDefault());
        }};

        // Aluminium + nickel + cobalt melted electrically -> AlNiCo permanent magnets. Needs no heat network.
        inductionFurnace = new GenericCrafter("induction-furnace"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 50, copperWire, 40, industrialBearing, 5));
            size = 2;
            health = 340;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(aluminium, 1, nickel, 1, cobalt, 1));
            consumePower(3f);
            outputItem = new ItemStack(alnicoMagnet, 2);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff6a3d"); }});
        }};
}
}
