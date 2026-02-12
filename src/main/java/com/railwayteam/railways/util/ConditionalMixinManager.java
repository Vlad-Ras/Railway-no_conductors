/*
 * MIT License
 *
 * Copyright (c) 2023-Present Bawnorton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.railwayteam.railways.util;

import com.railwayteam.railways.annotation.mixin.ConditionalMixin;
import com.railwayteam.railways.annotation.mixin.DevEnvMixin;
import com.railwayteam.railways.compat.Mods;
import com.railwayteam.railways.mixin.CRMixinPlugin;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.List;

public class ConditionalMixinManager {
    public static boolean shouldApply(String className) {
        try {
            org.objectweb.asm.tree.ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(className);
            List<AnnotationNode> annotationNodes = classNode.visibleAnnotations;
            if (annotationNodes == null && (classNode.methods == null || classNode.methods.isEmpty())) return true;

            boolean shouldApply = true;

            // Check class-level annotations first
            if (annotationNodes != null) {
                for (AnnotationNode node : annotationNodes) {
                    if (node.desc.equals(Type.getDescriptor(ConditionalMixin.class))) {
                        List<Mods> mods = Annotations.getValue(node, "mods", true, Mods.class);
                        boolean applyIfPresent = Annotations.getValue(node, "applyIfPresent", Boolean.TRUE);
                        boolean anyModsLoaded = anyModsLoaded(mods);
                        shouldApply = anyModsLoaded == applyIfPresent;
                        CRMixinPlugin.LOGGER.debug("{} is{}being applied because the mod(s) {} are{}loaded", className, shouldApply ? " " : " not ", mods, anyModsLoaded ? " " : " not ");
                    }
                    if (node.desc.equals(Type.getDescriptor(DevEnvMixin.class))) {
                        shouldApply &= Utils.isDevEnv();
                        CRMixinPlugin.LOGGER.debug("{} is{}being applied because it's marked with @DevEnvMixin and isDevEnv={}", className, shouldApply ? " " : " not ", Utils.isDevEnv());
                    }
                }
            }

            // Also check method-level annotations (e.g., method-targeted @DevEnvMixin)
            if (classNode.methods != null) {
                for (org.objectweb.asm.tree.MethodNode method : classNode.methods) {
                    List<AnnotationNode> methodAnns = method.visibleAnnotations;
                    if (methodAnns == null) continue;
                    for (AnnotationNode node : methodAnns) {
                        if (node.desc.equals(Type.getDescriptor(DevEnvMixin.class))) {
                            shouldApply &= Utils.isDevEnv();
                            CRMixinPlugin.LOGGER.debug("{}#{} is{}being applied because the method is marked with @DevEnvMixin and isDevEnv={}", className, method.name, shouldApply ? " " : " not ", Utils.isDevEnv());
                        }
                    }
                }
            }

            return shouldApply;
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean anyModsLoaded(List<Mods> mods) {
        for (Mods mod : mods) {
            if (mod.isLoaded) return true;
        }
        return false;
    }
}
