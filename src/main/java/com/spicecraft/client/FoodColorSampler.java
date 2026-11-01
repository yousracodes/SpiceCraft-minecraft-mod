package com.spicecraft.client;

import com.spicecraft.SpiceCraft;
import com.spicecraft.color.PowderColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FoodColorSampler {
    private static final Map<String, Integer> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> entry) { return size() > 512; }
    };
    private static int generation;
    public static int generation() { return generation; }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) FoodColorSampler::clear);
    }
    private static void clear(ResourceManager manager) { CACHE.clear(); generation++; }
    public static int color(ItemStack food) {
        String key = BuiltInRegistries.ITEM.getKey(food.getItem()) + ":" + food.getTag();
        return CACHE.computeIfAbsent(key, ignored -> sample(food));
    }
    private static int sample(ItemStack food) {
        try {
            var minecraft = Minecraft.getInstance();
            var model = minecraft.getItemRenderer().getModel(food, minecraft.level, minecraft.player, 0);
            if (model.isCustomRenderer()) return -1;
            List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, RandomSource.create(42)));
            for (Direction face : Direction.values()) quads.addAll(model.getQuads(null, face, RandomSource.create(42)));
            if (quads.isEmpty() || quads.size() > 2048) return -1;
            Map<String, BitSet> sampled = new HashMap<>();
            PowderColors colors = new PowderColors();
            for (BakedQuad quad : quads) {
                var sprite = quad.getSprite();
                var contents = sprite.contents();
                if (contents.name().toString().equals("minecraft:missingno") || contents.getUniqueFrames().limit(2).count() > 1) return -1;
                int tint = quad.isTinted() ? minecraft.getItemColors().getColor(food, quad.getTintIndex()) : 0xFFFFFF;
                BitSet pixels = sampled.computeIfAbsent(contents.name() + ":" + tint, ignored -> new BitSet());
                if (sampled.size() > 32) return -1;
                int stride = Math.max(1, (int) Math.ceil(Math.sqrt((double) contents.width() * contents.height() / 4096)));
                int[] vertices = quad.getVertices();
                int vertexSize = vertices.length / 4;
                double[] u = new double[4], v = new double[4];
                for (int i = 0; i < 4; i++) {
                    u[i] = (Float.intBitsToFloat(vertices[i * vertexSize + 4]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
                    v[i] = (Float.intBitsToFloat(vertices[i * vertexSize + 5]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
                }
                for (int y = 0; y < contents.height(); y += stride) for (int x = 0; x < contents.width(); x += stride) {
                    int pixel = y * contents.width() + x;
                    if (pixels.get(pixel) || !covered((x + 0.5) / contents.width(), (y + 0.5) / contents.height(), u, v)) continue;
                    pixels.set(pixel);
                    int abgr = sprite.getPixelRGBA(0, x, y);
                    int r = (abgr & 255) * (tint >> 16 & 255) / 255;
                    int g = (abgr >> 8 & 255) * (tint >> 8 & 255) / 255;
                    int b = (abgr >> 16 & 255) * (tint & 255) / 255;
                    colors.add(abgr & 0xFF000000 | r << 16 | g << 8 | b);
                }
            }
            return colors.color();
        } catch (RuntimeException exception) {
            // Custom models can lack readable sprites. A failed sample never blocks conversion.
            return -1;
        }
    }
    private static boolean covered(double x, double y, double[] u, double[] v) {
        return triangle(x, y, u, v, 0, 1, 2) || triangle(x, y, u, v, 0, 2, 3);
    }
    private static boolean triangle(double x, double y, double[] u, double[] v, int a, int b, int c) {
        double area = (u[b] - u[a]) * (v[c] - v[a]) - (v[b] - v[a]) * (u[c] - u[a]);
        if (Math.abs(area) < 1e-10) return false;
        double p = ((x - u[a]) * (v[c] - v[a]) - (y - v[a]) * (u[c] - u[a])) / area;
        double q = ((u[b] - u[a]) * (y - v[a]) - (v[b] - v[a]) * (x - u[a])) / area;
        return p >= -1e-6 && q >= -1e-6 && p + q <= 1.000001;
    }
    private FoodColorSampler() {}
}
