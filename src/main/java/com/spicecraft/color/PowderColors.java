package com.spicecraft.color;

/** Groups perceptually related hues in Oklab, without merging them into muddy RGB averages. */
public final class PowderColors {
    private static final int HUES = 24, BINS = HUES + 4;
    private final double[] mass = new double[BINS], weights = new double[BINS], red = new double[BINS], green = new double[BINS], blue = new double[BINS];
    private double total;

    public void add(int argb) {
        int alpha = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        if (alpha < 128 || Math.max(r, Math.max(g, b)) < 24 || Math.min(r, Math.min(g, b)) > 240) return;
        double lr = linear(r), lg = linear(g), lb = linear(b);
        double l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb);
        double m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb);
        double s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb);
        double lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
        double a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
        double bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
        double chroma = Math.hypot(a, bb);
        int bin = chroma < 0.04 ? HUES + Math.min(3, (int) (lightness * 4))
                : (int) Math.floor((Math.atan2(bb, a) + Math.PI) * HUES / (2 * Math.PI)) % HUES;
        double coverage = alpha / 255.0;
        // Prefer visible midtones/highlights, but do not invent brightness for naturally dark foods.
        double weight = coverage * (0.08 + Math.pow(lightness, 3)) * (0.7 + Math.min(1, chroma / 0.18));
        total += coverage;
        mass[bin] += coverage; weights[bin] += weight;
        red[bin] += r * weight; green[bin] += g * weight; blue[bin] += b * weight;
    }

    public int color() {
        int best = -1;
        double score = -1;
        for (int bin = 0; bin < BINS; bin++) {
            double coverage = sum(mass, bin), candidate = sum(weights, bin);
            // A tiny bright accent must not replace the main food color.
            if (coverage >= total * 0.08 && candidate > score && candidate > 0) { best = bin; score = candidate; }
        }
        if (best < 0) return -1;
        double weight = sum(weights, best);
        return (int) Math.round(sum(red, best) / weight) << 16
                | (int) Math.round(sum(green, best) / weight) << 8 | (int) Math.round(sum(blue, best) / weight);
    }

    private double sum(double[] values, int bin) {
        // Overlapping hue neighborhoods avoid arbitrary boundaries splitting related gold shades.
        return bin >= HUES ? values[bin] : values[bin] + values[(bin + HUES - 1) % HUES] + values[(bin + 1) % HUES];
    }
    private static double linear(int value) {
        double c = value / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
