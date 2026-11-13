package eclipse.content;

import arc.graphics.*;
import eclipse.defense.*;
import eclipse.type.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/**
 * Military phase M5 (endgame):
 * - Titan Manufacturing Complex: builds the Dreadnought, one per team (the only new unit-production building, user decision).
 * - Orbital Weapons Platform: a long-charge strike anywhere within 150 tiles (see {@link OrbitalPlatform}).
 * - Electronic Warfare Drone: built in the Air Factory; its pulse slows enemy turrets for a while (see {@link EwPulseAbility}).
 */
public class MilitaryEndgame{
    public static UnitType dreadnought, ewDrone;
    public static Block titanComplex, orbitalPlatform;

    public static void load(){
        dreadnought = MilitaryUnits.tank("dreadnought", 160, 30, 136, t -> {
            t.health = 60000f;
            t.armor = 24f;
            t.speed = 0.3f;
            t.hitSize = 42f;
            t.crushDamage = 6f;
            t.abilities.add(new ShieldArcAbility(){{
                radius = 60f;
                angle = 100f;
                regen = 2f;
                cooldown = 60f * 8f;
                max = 3000f;
                width = 8f;
            }}, new JamPulseAbility());
            t.weapons.add(new Weapon("eclipse-dreadnought-cannon"){{
                x = 13f;
                y = 4f;
                rotate = true;
                rotateSpeed = 1f;
                alternate = true;
                reload = 120f;
                shootY = 22f;
                recoil = 5f;
                shake = 6f;
                shootSound = Sounds.shootSpectre;
                bullet = new BasicBulletType(9f, 300f){{
                    pierceArmor = true;
                    pierce = true;
                    pierceCap = 4;
                    pierceBuilding = true;
                    width = 18f;
                    height = 28f;
                    lifetime = 34f;
                    frontColor = Color.valueOf("c8ccd8");
                    backColor = Color.valueOf("50525e");
                    trailLength = 10;
                    trailWidth = 3.5f;
                    trailColor = Color.valueOf("50525e");
                    hitEffect = Fx.hitBulletBig;
                }};
            }}, new Weapon("eclipse-dreadnought-lance"){{
                mirror = false;
                y = -10f;
                rotate = true;
                rotateSpeed = 1.5f;
                reload = 100f;
                shootY = 14f;
                shootSound = Sounds.shootLancer;
                bullet = new LaserBulletType(220f){{
                    length = 260f;
                    width = 20f;
                    status = StatusEffects.melting;
                    statusDuration = 120f;
                    colors = new Color[]{Color.valueOf("ff8a3d66"), Color.valueOf("ff8a3d"), Color.white};
                }};
            }}, new Weapon("eclipse-colossus-mortar"){{
                x = 15f;
                y = -18f;
                rotate = true;
                rotateSpeed = 2f;
                reload = 70f;
                shootSound = Sounds.shootArtillerySap;
                bullet = MilitaryUnits.colossus.weapons.find(w -> w.name.equals("eclipse-colossus-mortar")).bullet;
            }});
        });

        ewDrone = MilitaryUnits.flyer("ew-drone", t -> {
            t.health = 300f;
            t.speed = 2.6f;
            t.armor = 2f;
            t.hitSize = 9f;
            t.accel = 0.1f;
            t.drag = 0.04f;
            t.abilities.add(new EwPulseAbility());
        });
        ((UnitFactory)Blocks.airFactory).plans.add(new UnitPlan(ewDrone, 60f * 45f, with(microchip, 15, controlUnit, 3, alnicoMagnet, 8, carbonComposite, 10)));

        titanComplex = new TitanComplex("titan-manufacturing-complex"){{
            requirements(Category.units, with(Items.thorium, 1000, Items.plastanium, 500, Items.phaseFabric, 200, surgeSuperalloy, 300,
                superalloyFrame, 200, industrialBearing, 100, precisionComponent, 100, controlUnit, 50, experimentalCore, 20));
            size = 7;
            health = 12000;
            plans.add(new UnitPlan(dreadnought, 60f * 240f, with(Items.phaseFabric, 150, surgeSuperalloy, 200, tungstenCarbide, 400,
                superalloyFrame, 100, superconductor, 50, experimentalCore, 10)));
            consumePower(40f);
        }};

        orbitalPlatform = new OrbitalPlatform("orbital-weapons-platform"){{
            requirements(Category.turret, with(Items.surgeAlloy, 300, Items.phaseFabric, 200, surgeSuperalloy, 300, superconductor, 150,
                carbonComposite, 150, aetherite, 100, precisionComponent, 80, experimentalCore, 15));
            size = 5;
            health = 6000;
            consumePower(25f);
            consumeItems(with(superconductor, 10, aetherite, 5, Items.blastCompound, 40));
        }};
    }
}
