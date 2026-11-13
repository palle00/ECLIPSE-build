package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/**
 * Biology and agricultural chemistry: fertilizer, biomass, enzymes, fermentation and anaerobic digestion.
 * Closes loops with the rest of the mod: algae eat captured CO2, digesters turn industrial sludge back into methane.
 */
public class BiologyBlocks{
    public static Block fertilizerPlant, biomassCultivator, algaePhotobioreactor, enzymeExtractor, biochemicalFermenter,
        biofuelEngine, anaerobicDigester;

    public static void load(){
        // NH3 + H2SO4 -> ammonium sulfate fertilizer.
        fertilizerPlant = new GenericCrafter("fertilizer-plant"){{
            requirements(Category.crafting, with(Items.lead, 50, Items.titanium, 30, Items.metaglass, 30, Items.silicon, 20, pressureCasing, 5, titaniumRod, 10));
            size = 2;
            health = 300;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 60f;
            consumeLiquids(LiquidStack.with(ammonia, 3f / 60f, sulfuricAcid, 3f / 60f));
            consumePower(0.4f);
            outputItem = new ItemStack(fertilizer, 2);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(sulfuricAcid, 3f), new DrawDefault());
        }};

        // Purified water + fertilizer under grow lights -> biomass. Works on any floor, unlike the vanilla cultivator.
        biomassCultivator = new AttributeCrafter("biomass-cultivator"){{
            attribute = Attribute.spores; //faster on spore moss and moss
            baseEfficiency = 1f;
            boostScale = 0.25f;
            maxBoost = 0.5f;
            requirements(Category.production, with(Items.copper, 40, Items.lead, 40, Items.silicon, 20, Items.metaglass, 30, copperGear, 10));
            size = 2;
            health = 260;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 120f;
            consumeItem(fertilizer, 1);
            consumeLiquid(purifiedWater, 6f / 60f);
            consumePower(0.6f);
            outputItem = new ItemStack(biomass, 3);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(purifiedWater, 3f), new DrawDefault());
        }};

        // Algae grown in lit tubes: water + CO2 + fertilizer -> biomass + oxygen. A use for captured carbon dioxide.
        algaePhotobioreactor = new GenericCrafter("algae-photobioreactor"){{
            requirements(Category.production, with(Items.lead, 60, Items.metaglass, 60, Items.silicon, 40, Items.titanium, 30, titaniumRod, 10, copperGear, 10));
            size = 3;
            health = 500;
            liquidCapacity = 40f;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItem(fertilizer, 1); //nutrients
            consumeLiquids(LiquidStack.with(Liquids.water, 8f / 60f, carbonDioxide, 3f / 60f));
            consumePower(1f);
            outputItem = new ItemStack(biomass, 3);
            outputLiquid = new LiquidStack(oxygen, 3f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.water, 4f),
                new DrawBubbles(Color.valueOf("7bd96a")){{ amount = 14; radius = 4f; spread = 10f; }},
                new DrawDefault());
        }};

        // Biomass + spore pods -> enzyme extract.
        enzymeExtractor = new GenericCrafter("enzyme-extractor"){{
            requirements(Category.crafting, with(Items.lead, 50, Items.silicon, 40, Items.metaglass, 30, Items.titanium, 20, copperGear, 10, titaniumGear, 5));
            size = 2;
            health = 280;
            itemCapacity = 10;
            craftTime = 120f;
            consumeItems(with(biomass, 2, Items.sporePod, 1));
            consumePower(0.8f);
            outputItem = new ItemStack(enzymeExtract, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("e86fd0"); }});
        }};

        // Biomass + purified water -> biofuel (ethanol) + CO2. Enzyme extract doubles the speed and is slowly used up.
        biochemicalFermenter = new ChemicalReactor("biochemical-fermenter"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.metaglass, 50, Items.silicon, 30, pressureCasing, 10, titaniumGear, 5));
            size = 3;
            health = 550;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            itemCapacity = 10;
            craftTime = 90f;
            catalyst = enzymeExtract;
            catalystOptional = true;
            catalystBoost = 2f;
            craftsPerCatalyst = 20;
            consumeItem(biomass, 2);
            consumeLiquid(purifiedWater, 4f / 60f);
            consumePower(0.5f);
            outputLiquids = LiquidStack.with(biofuel, 6f / 60f, carbonDioxide, 2f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(biofuel, 4f),
                new DrawBubbles(Color.valueOf("f0e6a0")){{ amount = 10; radius = 4f; spread = 9f; }},
                new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Biofuel -> power. Exhaust leaves as flue gas.
        biofuelEngine = new ConsumeGenerator("biofuel-engine"){{
            requirements(Category.power, with(Items.copper, 50, Items.lead, 40, Items.graphite, 30, Items.silicon, 20, copperGear, 15, titaniumRod, 5));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            powerProduction = 2.5f;
            consumeLiquid(biofuel, 4f / 60f);
            outputLiquid = new LiquidStack(flueGas, 6f / 60f); //vented when not piped away
            generateEffect = Fx.generatespark;
            effectChance = 0.05f;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("f0c64a"); }});
        }};

        // Industrial sludge + biomass digested by bacteria -> biogas (methane) + fertilizer (digestate). Needs no power.
        anaerobicDigester = new GenericCrafter("anaerobic-digester"){{
            requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60, Items.metaglass, 40, Items.graphite, 30, pressureCasing, 10, copperGear, 10));
            size = 3;
            health = 600;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 120f;
            consumeItems(with(industrialSludge, 2, biomass, 1));
            outputItem = new ItemStack(fertilizer, 1);
            outputLiquid = new LiquidStack(methane, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(methane, 4f), new DrawDefault());
        }};
    }
}
