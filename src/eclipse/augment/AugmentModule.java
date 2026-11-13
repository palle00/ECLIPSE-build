package eclipse.augment;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * A physical upgrade module: an item that is manufactured, transported and inserted into an
 * {@link AugmentationCoupler}. Effects are combined per machine by {@link Augmentation}.
 */
public class AugmentModule extends Item{
    public static final Stat
        tierStat = new Stat("eclipse-module-tier"),
        speedStat = new Stat("eclipse-module-speed"),
        powerStat = new Stat("eclipse-module-power"),
        costStat = new Stat("eclipse-module-cost"),
        compatibleStat = new Stat("eclipse-module-compatible"),
        yieldStat = new Stat("eclipse-module-yield"),
        savingStat = new Stat("eclipse-module-saving"),
        recycleStat = new Stat("eclipse-module-recycle"),
        exclusiveStat = new Stat("eclipse-module-exclusive"),
        thermalStat = new Stat("eclipse-module-thermal"),
        pressureStat = new Stat("eclipse-module-pressure"),
        catalystStat = new Stat("eclipse-module-catalyst"),
        liquidYieldStat = new Stat("eclipse-module-liquid-yield"),
        wasteStat = new Stat("eclipse-module-waste"),
        coolantStat = new Stat("eclipse-module-coolant"),
        thermalRecoveryStat = new Stat("eclipse-module-thermal-recovery"),
        overchargeStat = new Stat("eclipse-module-overcharge"),
        capacitorStat = new Stat("eclipse-module-capacitor"),
        damageStat = new Stat("eclipse-module-damage"),
        accuracyStat = new Stat("eclipse-module-accuracy"),
        projectileStat = new Stat("eclipse-module-projectile"),
        coolingStat = new Stat("eclipse-module-cooling"),
        legendaryStat = new Stat("eclipse-module-legendary"),
        uniqueStat = new Stat("eclipse-module-unique");

    public enum ModuleCategory{
        production(Color.valueOf("ff9a3c")),
        energy(Color.valueOf("5ab8ff")),
        industrial(Color.valueOf("ffd84a")),
        mining(Color.valueOf("c47a3c")),
        chemical(Color.valueOf("b37aff")),
        military(Color.valueOf("ff5a5a")),
        legendary(Color.valueOf("e8d8ff"));

        public final Color color;

        ModuleCategory(Color color){
            this.color = color;
        }
    }

    public ModuleCategory category = ModuleCategory.production;
    /** 1-5 (I-V). */
    public int tier = 1;

    /** Extra machine speed, as a fraction (0.2 = +20%). */
    public float speedBonus = 0f;
    /** Extra power the coupler draws to drive a speed bonus, per tick: flat + this fraction of the machine's own usage. */
    public float flatPowerCost = 0f, relativePowerCost = 0f;
    /** Fraction of the machine's power draw refunded to its grid (0.2 = -20%). */
    public float powerReduction = 0f;
    /** Machine slowdown as a fraction (0.1 = -10% speed). Not subject to diminishing returns. */
    public float speedPenalty = 0f;
    /** Extra output per production cycle (0.2 = +20%), delivered through a fractional accumulator. */
    public float yieldBonus = 0f;
    /** Fraction of consumed item inputs refunded to the machine each cycle. */
    public float inputSaving = 0f;
    /** Fraction of consumed item inputs returned as scrap, which the coupler outputs. */
    public float recycleFraction = 0f;

    // ---- chemical (phase 3) ----
    /** Coolant this module needs per tick (water-equivalent; colder coolants are used up proportionally slower). */
    public float coolantUse = 0f;
    /** Heat supplied to a heat-driven machine, as a fraction of its heat requirement. */
    public float thermalSupply = 0f;
    /** Faster pressurizing / slower pressure loss for pressurized reactors (0.5 = +50%). */
    public float pressureBoost = 0f;
    /** Longer catalyst life (0.5 = 50% more crafts per catalyst). */
    public float catalystLife = 0f;
    /** Extra product liquid (waste liquids excluded). */
    public float liquidYield = 0f;
    /** Fraction of the machine's acidic waste turned back into its input acid. */
    public float wasteRecovery = 0f;

    // ---- energy & mining (phase 4) ----
    /** Waste heat turned into power: for heat producers per unit of heat, otherwise per tick of hot operation. */
    public float thermalRecovery = 0f;
    /** Speed bonus during an overcharge burst (the coupler charges, then bursts). */
    public float overchargeBonus = 0f;
    /** Internal energy store that keeps the machine powered through shortages. */
    public float capacitorCapacity = 0f;
    /** Shows the ore deposits around a drill. */
    public boolean analyzer = false;

    // ---- military (phase 5) ----
    /** Extra bullet damage (0.2 = +20%). */
    public float damageBonus = 0f;
    /** Fraction of a turret's spread removed (1 = perfectly accurate). */
    public float accuracy = 0f;
    /** Faster projectiles (0.5 = +50% speed, same range). */
    public float projectileSpeed = 0f;
    /** Faster turret heat dissipation (1 = twice as fast). */
    public float coolingBonus = 0f;

    // ---- advanced (phase 6) ----
    /** Only one unique module of a family works per machine; further copies are reported as conflicting. */
    public boolean unique = false;
    /** Legendary module (shown in its stats); legendary modules are always unique. */
    public boolean legendary = false;

    /**
     * Modules of different families in the same exclusive group cannot both work on one machine;
     * the one attached first wins. Null = no restriction.
     */
    public @Nullable String exclusiveGroup;
    /** Module family (all tiers of one module share it); defaults to the name without the tier suffix. */
    public String family;

    /** Which machines this module can be attached to. */
    public Boolf<Block> compatible = b -> true;
    /** Bundle key suffix describing compatible machines, e.g. "production" -> stat.eclipse-compat-production. */
    public String compatibleKey = "any";

    /** Manufacturing. */
    public ItemStack[] recipe = ItemStack.empty;
    public float craftTime = 360f;

    public AugmentModule(String name, Color color){
        super(name, color);
        family = name.replaceAll("-\\d+$", "");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(tierStat, StatValues.string(romanTier()));
        if(speedBonus > 0f) stats.add(speedStat, StatValues.string("+@%", Math.round(speedBonus * 100)));
        if(speedPenalty > 0f) stats.add(speedStat, StatValues.string("[scarlet]-@%[]", Math.round(speedPenalty * 100)));
        if(yieldBonus > 0f) stats.add(yieldStat, StatValues.string("+@%", Math.round(yieldBonus * 100)));
        if(inputSaving > 0f) stats.add(savingStat, StatValues.string("-@%", Math.round(inputSaving * 100)));
        if(recycleFraction > 0f) stats.add(recycleStat, StatValues.string("@%", Math.round(recycleFraction * 100)));
        if(exclusiveGroup != null) stats.add(exclusiveStat, StatValues.string(Core.bundle.get("stat.eclipse-exclusive-" + exclusiveGroup)));
        if(thermalSupply > 0f) stats.add(thermalStat, StatValues.string("@%", Math.round(thermalSupply * 100)));
        if(pressureBoost > 0f) stats.add(pressureStat, StatValues.string("+@%", Math.round(pressureBoost * 100)));
        if(catalystLife > 0f) stats.add(catalystStat, StatValues.string("+@%", Math.round(catalystLife * 100)));
        if(liquidYield > 0f) stats.add(liquidYieldStat, StatValues.string("+@%", Math.round(liquidYield * 100)));
        if(wasteRecovery > 0f) stats.add(wasteStat, StatValues.string("@%", Math.round(wasteRecovery * 100)));
        if(coolantUse > 0f) stats.add(coolantStat, StatValues.string(Core.bundle.format("stat.eclipse-module-coolant-value", Strings.autoFixed(coolantUse * 60f, 1))));
        if(thermalRecovery > 0f) stats.add(thermalRecoveryStat, StatValues.string("@%", Math.round(thermalRecovery * 100)));
        if(overchargeBonus > 0f) stats.add(overchargeStat, StatValues.string(Core.bundle.format("stat.eclipse-module-overcharge-value", Math.round(overchargeBonus * 100))));
        if(capacitorCapacity > 0f) stats.add(capacitorStat, StatValues.string("@", Math.round(capacitorCapacity)));
        if(damageBonus > 0f) stats.add(damageStat, StatValues.string("+@%", Math.round(damageBonus * 100)));
        if(accuracy > 0f) stats.add(accuracyStat, StatValues.string("-@%", Math.round(accuracy * 100)));
        if(projectileSpeed > 0f) stats.add(projectileStat, StatValues.string("+@%", Math.round(projectileSpeed * 100)));
        if(coolingBonus > 0f) stats.add(coolingStat, StatValues.string("+@%", Math.round(coolingBonus * 100)));
        if(legendary) stats.add(legendaryStat, true);
        else if(unique) stats.add(uniqueStat, true);
        if(powerReduction > 0f) stats.add(powerStat, StatValues.string("-@%", Math.round(powerReduction * 100)));
        if(flatPowerCost > 0f || relativePowerCost > 0f){
            stats.add(costStat, StatValues.string(Core.bundle.format("stat.eclipse-module-cost-value",
                Math.round(flatPowerCost * 60f), Math.round(relativePowerCost * 100))));
        }
        stats.add(compatibleStat, StatValues.string(Core.bundle.get("stat.eclipse-compat-" + compatibleKey)));
    }

    public String romanTier(){
        return switch(tier){
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }
}
