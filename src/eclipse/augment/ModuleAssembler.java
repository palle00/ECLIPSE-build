package eclipse.augment;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Manufactures the selected {@link AugmentModule} from its recipe. Each assembler handles a range of tiers. */
public class ModuleAssembler extends Block{
    public int minTier = 1, maxTier = 1;
    public Effect craftEffect = Fx.pulverizeMedium;

    public ModuleAssembler(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 40;
        saveConfig = true;
        clearOnDoubleTap = true;
        group = BlockGroup.units;

        consume(new ConsumeItemDynamic((AssemblerBuild e) -> e.selected == null ? ItemStack.empty : e.selected.recipe));

        config(Item.class, (AssemblerBuild e, Item item) -> {
            if(item instanceof AugmentModule m && canMake(m) && e.selected != m){
                e.selected = m;
                e.progress = 0f;
            }
        });
        configClear((AssemblerBuild e) -> {
            e.selected = null;
            e.progress = 0f;
        });
    }

    public boolean canMake(AugmentModule m){
        return m.tier >= minTier && m.tier <= maxTier;
    }

    public Seq<AugmentModule> modules(){
        Seq<AugmentModule> out = new Seq<>();
        for(Item item : content.items()) if(item instanceof AugmentModule m && canMake(m)) out.add(m);
        return out;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.output, table -> {
            table.row();
            for(AugmentModule m : modules()){
                table.table(Styles.grayPanel, t -> {
                    t.image(m.uiIcon).size(32f).pad(6f);
                    t.add(m.localizedName).left().growX();
                    t.table(r -> {
                        for(ItemStack stack : m.recipe) r.add(StatValues.displayItem(stack.item, stack.amount, m.craftTime, true)).pad(2f);
                    }).right().pad(4f);
                }).growX().pad(4f).row();
            }
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (AssemblerBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.progress));
    }

    public class AssemblerBuild extends Building{
        public @Nullable AugmentModule selected;
        public float progress;

        @Override
        public boolean acceptItem(Building source, Item item){
            if(selected == null || items.get(item) >= itemCapacity) return false;
            for(ItemStack stack : selected.recipe) if(stack.item == item) return true;
            return false;
        }

        @Override
        public boolean shouldConsume(){
            return selected != null && items.get(selected) < itemCapacity && enabled;
        }

        @Override
        public void updateTile(){
            if(selected != null && efficiency > 0f){
                progress += getProgressIncrease(selected.craftTime);
                if(progress >= 1f){
                    consume();
                    items.add(selected, 1);
                    craftEffect.at(x, y);
                    progress %= 1f;
                }
            }
            if(selected != null) dump(selected);
        }

        @Override
        public boolean canDump(Building to, Item item){
            return item instanceof AugmentModule;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(ModuleAssembler.this, table, modules(), () -> selected, this::configure);
        }

        @Override
        public Object config(){
            return selected;
        }

        @Override
        public void draw(){
            super.draw();
            if(selected != null){
                Draw.rect(selected.fullIcon, x, y, 6f + size * 2f, 6f + size * 2f);
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(selected == null ? -1 : selected.id);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                short id = read.s();
                selected = id == -1 ? null : content.item(id) instanceof AugmentModule m ? m : null;
                progress = read.f();
            }
        }
    }
}
