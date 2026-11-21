package eclipse.ui;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.content.blocks.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * The wiki's comparison page: generators, ECLIPSE turrets and ECLIPSE units side by side, read from the game data so the
 * numbers always match the current balance.
 */
public class WikiTables{
    static final String num = "[#8fd8ff]";

    public static void build(EclipseWiki wiki, Table page){
        generators(wiki, page);
        turrets(wiki, page);
        units(wiki, page);
    }

    static void generators(EclipseWiki wiki, Table page){
        EclipseWiki.sub(page, "eclipse-wiki.compare.generators");
        Table t = table(page, "eclipse-wiki.compare.col.block", "eclipse-wiki.compare.col.size", "eclipse-wiki.compare.col.power", "eclipse-wiki.compare.col.fuel");
        Seq<PowerGenerator> gens = content.blocks().select(b -> b instanceof PowerGenerator g && g.powerProduction > 0f && b.isVisible() && b.environmentBuildable()
            && (b.name.startsWith("eclipse-") || b.minfo.mod == null)).map(b -> (PowerGenerator)b);
        gens.sort(Structs.comparingFloat(g -> g.powerProduction));
        for(PowerGenerator g : gens){
            wiki.link(t, g, null);
            t.add(num + g.size + "x" + g.size);
            t.add(num + Strings.autoFixed(g.powerProduction * 60f, 0) + "/s");
            t.add("[lightgray]" + fuel(g)).left().row();
        }
        page.add(t).left().row();
        //heat into power, from the boiler and turbine data
        if(PowerBlocks.industrialBoiler instanceof mindustry.world.blocks.production.HeatCrafter boiler && boiler.outputLiquid != null
            && PowerBlocks.steamTurbine instanceof PowerGenerator turbine){
            ConsumeLiquid steamUse = turbine.findConsumer(c -> c instanceof ConsumeLiquid);
            if(steamUse != null){
                float steamPerHeat = boiler.outputLiquid.amount / boiler.heatRequirement, powerPerSteam = turbine.powerProduction / steamUse.amount;
                page.add("[lightgray]" + Core.bundle.format("eclipse-wiki.compare.heat-power", Strings.autoFixed(steamPerHeat * powerPerSteam * 60f, 0)))
                    .wrap().width(600f).left().padBottom(8f).row();
            }
        }
    }

    /** What a generator burns, per second. */
    static String fuel(PowerGenerator g){
        Seq<String> parts = new Seq<>();
        float itemsPerSecond = g instanceof ConsumeGenerator cg ? 60f / cg.itemDuration : g instanceof NuclearReactor nr ? 60f / nr.itemDuration
            : g instanceof ImpactReactor ir ? 60f / ir.itemDuration : 0f;
        for(Consume c : g.consumers){
            if(c instanceof ConsumeItems ci) for(ItemStack s : ci.items) parts.add(Strings.autoFixed(s.amount * itemsPerSecond, 2) + " " + s.item.localizedName);
            else if(c instanceof ConsumeItemFilter) parts.add(Strings.autoFixed(itemsPerSecond, 2) + " " + Core.bundle.get("eclipse-wiki.compare.any-fuel"));
            else if(c instanceof ConsumeLiquid cl && !cl.booster) parts.add(Strings.autoFixed(cl.amount * 60f, 1) + " " + cl.liquid.localizedName);
            else if(c instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) parts.add(Strings.autoFixed(s.amount * 60f, 1) + " " + s.liquid.localizedName);
            else if(c instanceof ConsumeLiquidFilter cf) parts.add(Strings.autoFixed(cf.amount * 60f, 1) + " " + Core.bundle.get("eclipse-wiki.compare.any-fuel"));
            else if(c instanceof ConsumePower cp && cp.usage > 0f) parts.add(Core.bundle.format("eclipse-wiki.compare.power-in", Strings.autoFixed(cp.usage * 60f, 0)));
        }
        return parts.isEmpty() ? Core.bundle.get("eclipse-wiki.compare.no-fuel") : parts.toString(", ");
    }

    static void turrets(EclipseWiki wiki, Table page){
        EclipseWiki.sub(page, "eclipse-wiki.compare.turrets");
        page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.compare.turrets.hint")).wrap().width(600f).left().padBottom(4f).row();
        Table t = table(page, "eclipse-wiki.compare.col.block", "eclipse-wiki.compare.col.size", "eclipse-wiki.compare.col.range", "eclipse-wiki.compare.col.ammo");
        for(Block b : content.blocks()){
            if(!(b instanceof Turret tur) || !b.name.startsWith("eclipse-") || !b.isVisible()) continue;
            wiki.link(t, b, null);
            t.add(num + b.size + "x" + b.size);
            t.add(num + Strings.autoFixed(tur.range / tilesize, 0));
            Table ammo = new Table().left();
            float shotsPerSecond = tur.shoot.shots * 60f / tur.reload;
            ObjectMap<? extends UnlockableContent, BulletType> types = b instanceof ItemTurret it ? it.ammoTypes : b instanceof LiquidTurret lt ? lt.ammoTypes : null;
            if(types != null){
                int n = 0;
                for(var e : types){
                    if(n++ >= 4){ ammo.add("[lightgray]+" + (types.size - 4)); break; }
                    ammo.image(e.key.uiIcon).size(20f).padRight(4f);
                    ammo.add(shot(e.value, shotsPerSecond * e.value.reloadMultiplier)).padRight(10f);
                }
            }else if(b instanceof PowerTurret pt && pt.shootType != null){
                ammo.add(shot(pt.shootType, shotsPerSecond));
            }else if(b instanceof ContinuousTurret ct && ct.shootType != null){
                ammo.add(shot(ct.shootType, shotsPerSecond));
            }
            t.add(ammo).left().row();
        }
        page.add(t).left().padBottom(8f).row();
    }

    /** Damage per hit (direct + blast) and hits per second; a beam gives its damage per second. */
    static String shot(BulletType b, float perSecond){
        if(b instanceof ContinuousBulletType c) return num + Strings.autoFixed(c.damage * 60f / c.damageInterval, 0) + "[lightgray]/s";
        float hit = b.damage + b.splashDamage;
        return num + Strings.autoFixed(hit, 0) + "[lightgray] x" + Strings.autoFixed(perSecond, 2) + "/s";
    }

    static void units(EclipseWiki wiki, Table page){
        EclipseWiki.sub(page, "eclipse-wiki.compare.units");
        page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.compare.units.hint")).wrap().width(600f).left().padBottom(4f).row();
        Table t = table(page, "eclipse-wiki.compare.col.unit", "eclipse-wiki.compare.col.health", "eclipse-wiki.compare.col.armor", "eclipse-wiki.compare.col.dps");
        for(UnitType u : content.units()){
            if(!u.name.startsWith("eclipse-")) continue;
            wiki.link(t, u, null);
            t.add(num + (int)u.health);
            t.add(num + Strings.autoFixed(u.armor, 0));
            float dps = dps(u);
            t.add(dps <= 0f ? "[lightgray]-" : num + Strings.autoFixed(dps, 0)).left().row();
        }
        page.add(t).left().padBottom(8f).row();
    }

    /** Direct + blast damage per second of a unit's weapons (point defence and repair beams don't count). */
    public static float dps(UnitType u){
        float total = 0f;
        for(Weapon w : u.weapons){
            if(w instanceof PointDefenseWeapon || w instanceof RepairBeamWeapon || w.bullet == null || w.reload <= 0f) continue;
            if(w.bullet instanceof ContinuousBulletType c){
                total += c.damage * 60f / c.damageInterval;
                continue;
            }
            total += w.shoot.shots * 60f / w.reload * (w.bullet.damage + w.bullet.splashDamage);
        }
        return total;
    }

    static Table table(Table page, String... headers){
        Table t = new Table().left();
        t.defaults().left().padRight(16f).padBottom(2f);
        for(String h : headers) t.add("[accent]" + Core.bundle.get(h));
        t.row();
        return t;
    }
}
