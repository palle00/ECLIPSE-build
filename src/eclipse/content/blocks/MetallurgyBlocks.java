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
    public static Block oreCrusher, oreWasher,
        mineralSeparator, platinumRefinery, lithiumExtractor, carbideDrill, superalloyWall, superalloyWallLarge,
        aluminaRefinery, surgeSuperalloyWall, surgeSuperalloyWallLarge, vacuumFreezer,
        blastFurnace, electricFurnace;

    public static void load(){
        // Slag + sulfuric acid -> trace metals. The metal source on maps without the new ores.
        mineralSeparator = new Separator("mineral-separator"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.titanium, 50, Items.silicon, 40, Items.metaglass, 30, titaniumGear, 10, pressureCasing, 5));
            size = 2;
            health = 320;
            craftTime = 40f;
            itemCapacity = 20;
            liquidCapacity = 30f;
            results = with(vanadium, 3, chromium, 2); //steel slag really carries vanadium and chromium
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
            consumeItem(sodaAsh, 1); //lithium carbonate precipitated with soda ash
            consumePower(1f);
            outputItem = new ItemStack(lithium, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(brine, 3f), new DrawDefault());
        }};

        // Faster than the laser drill at the same size.
        carbideDrill = new Drill("carbide-drill"){{
            requirements(Category.production, with(tungstenCarbide, 30, Items.titanium, 40, Items.silicon, 40, superalloyGear, 10, permanentMagnetMotor, 3, controlUnit, 5));
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

        // ---- advanced vanilla combinations ----

        // Heat-driven furnace: everything that melts or hot-forms with heat from adjacent heaters. Hot superalloy (nickel,
        // chromium, cobalt), superalloy frames (hot-formed plates, rods and bolts), soda glass and reinforced glass.
        // Every recipe stops below its heat. Its cost must not need superalloy frames or later components.
        blastFurnace = new RecipeCrafter("blast-furnace"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 80, Items.silicon, 60, Items.graphite, 60, titaniumRod, 20, industrialBearing, 5));
            size = 3;
            health = 700;
            itemCapacity = 20;
            recipe(3, 90f, with(nickel, 2, chromium, 1, cobalt, 1), with(hotSuperalloy, 1)).heat(8f);
            recipe(3, 120f, with(superalloyPlate, 2, titaniumRod, 2, titaniumBolt, 2), with(superalloyFrame, 1)).heat(8f);
            recipe(2, 60f, with(Items.sand, 3, sodaAsh, 1), with(hotMetaglass, 3)).heat(6f); //soda-lime glass: sand with a soda ash flux
            recipe(2, 60f, with(Items.metaglass, 2, Items.titanium, 1), with(hotReinforcedGlass, 1)).heat(5f);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ffb46b"); }}, new DrawHeatInput());
        }};

        // Electric furnace: melting and sintering by electricity alone. AlNiCo magnets (induction), hot tungsten carbide
        // (sintering, cobalt binder), silicon (sand reduced with graphite in the arc; CO2 vented) and hot surge superalloy (electric arc). Its cost must not need anything it makes.
        electricFurnace = new RecipeCrafter("electric-furnace"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 80, Items.silicon, 60, tungstenRod, 10, heatingCoil, 4, industrialBearing, 5));
            size = 3;
            health = 700;
            itemCapacity = 20;
            recipe(3, 60f, with(aluminium, 1, nickel, 1, cobalt, 1), with(alnicoMagnet, 2));
            recipe(3, 120f, with(tungsten, 2, Items.graphite, 2, cobalt, 1), with(hotTungstenCarbide, 2)); //cemented carbide: WC in a cobalt binder
            recipe(2, 60f, with(Items.sand, 2, Items.graphite, 1), with(hotSilicon, 2)).liquidsOut(carbonDioxide, 1f / 60f).vented(); //SiO2 + C in the arc
            recipe(5, 120f, with(Items.surgeAlloy, 1, superalloy, 2), with(hotSurgeSuperalloy, 1)).liquidsIn(argon, 1f / 60f); //arc under argon
            recipe(3, 120f, with(petroleumCoke, 2), with(Items.graphite, 2)); //coke graphitized in the electric furnace
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff6a3d"); }});
        }};

        // Hot metal + cryofluid -> usable metal. Every midgame and later furnace metal cools here before use.
        // Its cost must not need any of the metals it cools.
        vacuumFreezer = new RecipeCrafter("vacuum-freezer"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.lead, 60, Items.metaglass, 40, Items.silicon, 40, pressureCasing, 10, industrialBearing, 5));
            size = 3;
            health = 650;
            itemCapacity = 20;
            liquidCapacity = 30f;
            autoSelect = true; //nothing selected: cools whatever hot metal arrives, so a mixed belt never jams it
            recipe(3, 30f, with(hotSuperalloy, 1), with(superalloy, 1)).liquidsIn(Liquids.cryofluid, 3f / 60f);
            recipe(3, 30f, with(hotTungstenCarbide, 1), with(tungstenCarbide, 1)).liquidsIn(Liquids.cryofluid, 3f / 60f);
            recipe(3, 30f, with(hotAluminium, 1), with(aluminium, 1)).liquidsIn(Liquids.cryofluid, 2f / 60f);
            recipe(5, 60f, with(hotSurgeSuperalloy, 1), with(surgeSuperalloy, 1)).liquidsIn(Liquids.cryofluid, 6f / 60f);
            //tier II: the ECLIPSE routes to vanilla materials
            recipe(2, 30f, with(hotTitanium, 2), with(Items.titanium, 2)).liquidsIn(Liquids.cryofluid, 1f / 60f);
            recipe(2, 30f, with(hotSilicon, 4), with(Items.silicon, 4)).liquidsIn(Liquids.cryofluid, 1f / 60f);
            recipe(2, 30f, with(hotMetaglass, 3), with(Items.metaglass, 3)).liquidsIn(Liquids.cryofluid, 1f / 60f);
            recipe(2, 30f, with(hotReinforcedGlass, 1), with(reinforcedGlass, 1)).liquidsIn(Liquids.cryofluid, 1f / 60f);
            craftEffect = Fx.freezing;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.cryofluid, 4f), new DrawDefault());
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

}
}
