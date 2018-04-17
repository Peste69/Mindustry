package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Fire extends TimedEntity {
    private static final GridMap<Fire> map = new GridMap<>();
    private static final float baseLifetime = 1000f;

    private Tile tile;
    private float baseFlammability = -1, puddleFlammability;

    /**Start a fire on the tile. If there already is a file there, refreshes its lifetime..*/
    public static void create(Tile tile){
        Fire fire = map.get(tile.x, tile.y);

        if(fire == null){
            map.put(tile.x, tile.y, new Fire(tile).add());
        }else{
            fire.lifetime = baseLifetime;
            fire.time = 0f;
        }
    }

    /**Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.*/
    public static void extinguish(Tile tile, float intensity){
        if(map.containsKey(tile.x, tile.y)){
            map.get(tile.x, tile.y).time += intensity * Timers.delta();
        }
    }

    private Fire(Tile tile){
        this.tile = tile;
        lifetime = baseLifetime;
    }

    @Override
    public void update() {
        super.update();

        TileEntity entity = tile.target().entity;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Timers.delta()*8;
        }

        if (baseFlammability < 0){
            baseFlammability = tile.block().getFlammability(tile);
        }

        if(damage) {
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Timers.delta();
        }

        if (flammability > 1f && Mathf.chance(0.03 * Timers.delta() * Mathf.clamp(flammability/5f, 0.3f, 2f))) {
            GridPoint2 p = Mathf.select(Geometry.d4);
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            create(other);
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.fire, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));

            Puddle p = Puddle.getPuddle(tile);
            if(p != null){
                puddleFlammability = p.getFlammability()/3f;
            }else{
                puddleFlammability = 0;
            }

            if(damage){
                entity.damage(0.4f);
            }
            DamageArea.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f, unit -> unit.applyEffect(StatusEffects.burning, 0.8f));
        }

        if(Mathf.chance(0.05 * Timers.delta())){
            Effects.effect(EnvironmentFx.smoke, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));
        }
    }

    @Override
    public Fire add(){
        return add(effectGroup);
    }

    @Override
    public void removed() {
        map.remove(tile.x, tile.y);
    }
}