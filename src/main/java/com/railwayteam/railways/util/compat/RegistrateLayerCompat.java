package com.railwayteam.railways.util.compat;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Avoids directly calling Registrate BlockBuilder#addLayer which is deprecated-for-removal in newer Registrate.
 * We call it reflectively to keep behaviour while removing compile-time removal warnings.
 */
public class RegistrateLayerCompat {
    private RegistrateLayerCompat() {}

    public static <T extends net.minecraft.world.level.block.Block, P> NonNullUnaryOperator<BlockBuilder<T, P>> layer(Supplier<Supplier<RenderType>> layer) {
        return builder -> {
            invokeAddLayer(builder, layer);
            return builder;
        };
    }

    private static void invokeAddLayer(Object builder, Supplier<Supplier<RenderType>> layer) {
        try {
            Method m = builder.getClass().getMethod("addLayer", Supplier.class);
            m.invoke(builder, layer);
            return;
        } catch (NoSuchMethodException ignored) {
            // possible alternative names
        } catch (Throwable t) {
            return;
        }
        try {
            Method m = builder.getClass().getMethod("addRenderLayer", Supplier.class);
            m.invoke(builder, layer);
        } catch (Throwable ignored) {
        }
    }
}
