package com.railwayteam.railways.util.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Compatibility shim around Create's AllTags API.
 * Uses reflection to avoid hard-linking to methods that are deprecated-for-removal in Create 6+.
 */
public class CreateTagsCompat {
    private CreateTagsCompat() {}

    @SuppressWarnings("unchecked")
    public static TagKey<Item> commonItemTag(String path) {
        try {
            Class<?> allTags = Class.forName("com.simibubi.create.AllTags");
            // Most common historic API: AllTags.commonItemTag(String)
            try {
                return (TagKey<Item>) allTags.getMethod("commonItemTag", String.class).invoke(null, path);
            } catch (NoSuchMethodException ignored) {
                // Some variants: AllTags.commonTag(String) or itemTag(String)
                try {
                    return (TagKey<Item>) allTags.getMethod("commonTag", String.class).invoke(null, path);
                } catch (NoSuchMethodException ignored2) {
                    try {
                        return (TagKey<Item>) allTags.getMethod("itemTag", String.class).invoke(null, path);
                    } catch (NoSuchMethodException ignored3) {
                        // fall through
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback: assume forge/common style tag location "c:<path>"
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
