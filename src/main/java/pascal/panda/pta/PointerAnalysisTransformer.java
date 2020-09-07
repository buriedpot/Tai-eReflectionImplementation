/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta;

import pascal.panda.pta.core.solver.PointerAnalysis;
import pascal.panda.pta.core.solver.PointerAnalysisBuilder;
import pascal.panda.pta.jimple.JimplePointerAnalysis;
import pascal.panda.pta.options.Options;
import soot.SceneTransformer;

import java.util.Map;

public class PointerAnalysisTransformer extends SceneTransformer {

    private static final PointerAnalysisTransformer INSTANCE =
            new PointerAnalysisTransformer();

    private PointerAnalysisTransformer() {
    }

    public static PointerAnalysisTransformer v() {
        return INSTANCE;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        PointerAnalysis pta = new PointerAnalysisBuilder()
                .build(Options.get());
        pta.analyze();
        JimplePointerAnalysis.v().setPointerAnalysis(pta);
    }
}