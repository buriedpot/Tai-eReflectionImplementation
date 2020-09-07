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

package pascal.panda.pta.core.cs;

import pascal.panda.pta.core.solver.PointerFlowEdge;
import pascal.panda.pta.element.Type;
import pascal.panda.pta.set.PointsToSet;

import java.util.Set;

/**
 * Represent pointers/nodes in pointer analysis/pointer flow graph.
 */
public interface Pointer {

    PointsToSet getPointsToSet();

    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * @param edge an out edge of this pointer
     * @return if the given edge is an new edge for this pointer.
     */
    boolean addOutEdge(PointerFlowEdge edge);

    /**
     * @return out edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getOutEdges();

    /**
     * @return the type of this pointer
     */
    Type getType();
}