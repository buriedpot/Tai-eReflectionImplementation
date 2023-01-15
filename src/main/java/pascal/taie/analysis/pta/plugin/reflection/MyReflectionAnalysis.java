/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.PARAMETER_PASSING;
import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.RETURN;

public class MyReflectionAnalysis implements Plugin {

//    private Model classModel;

    private MetaObjModel metaObjModel;

    private Model reflectiveActionModel;

    private Solver solver;

    private CSManager csManager;

    private final MultiMap<Var, ReflectiveCallEdge> reflectiveArgs = Maps.newMultiMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
//        classModel = new ClassModel(solver);
        if (solver.getOptions().getString("reflection-log") != null) {
            metaObjModel = new LogBasedModel(solver);
        } else {
            metaObjModel = new MyStringBasedModel(solver);
        }
        reflectiveActionModel = new MyReflectiveActionModel(solver);
        csManager = solver.getCSManager();
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR()
                .invokes(false)
                .forEach(invoke -> {
//                    classModel.handleNewInvoke(invoke);
                    metaObjModel.handleNewInvoke(invoke);
                    reflectiveActionModel.handleNewInvoke(invoke);
                });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
//        if (classModel.isRelevantVar(csVar.getVar())) {
//            classModel.handleNewPointsToSet(csVar, pts);
//        }
        if (metaObjModel.isRelevantVar(csVar.getVar())) {
            metaObjModel.handleNewPointsToSet(csVar, pts);
        }
        if (reflectiveActionModel.isRelevantVar(csVar.getVar())) {
            reflectiveActionModel.handleNewPointsToSet(csVar, pts);
        }
        // 如果出现了新的数组，那就应该把新数组全部建立PFG Edge!(No need,
        // 在对arrays中对invoke的处理已经实现
//        reflectiveArgs.get(csVar.getVar())
//                .forEach(edge -> passReflectiveArgs(edge, pts));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        metaObjModel.handleNewCSMethod(csMethod);
    }

    /**
     * 这里的反射调用边指的是源方法是m.invoke调用点，目标方法是具体的方法的调用边
     * 这一步主要是在给反射调用边添加PFG edges
     * @param edge 边缘
     */
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        // no need

//        if (edge instanceof ReflectiveCallEdge refEdge) {
//            Context callerCtx = refEdge.getCallSite().getContext();
//            // pass argument
//            Var args = refEdge.getArgs();
//            if (args != null) {
//                CSVar csArgs = csManager.getCSVar(callerCtx, args);
//                passReflectiveArgs(refEdge, solver.getPointsToSetOf(csArgs));
//                // record args for later-arrive array objects
//                reflectiveArgs.put(args, refEdge);
//            }
//            // pass return value
//            Invoke invoke = refEdge.getCallSite().getCallSite();
//            Context calleeCtx = refEdge.getCallee().getContext();
//            JMethod callee = refEdge.getCallee().getMethod();
//            Var result = invoke.getResult();
//            if (result != null && isConcerned(callee.getReturnType())) {
//                CSVar csResult = csManager.getCSVar(callerCtx, result);
//                callee.getIR().getReturnVars().forEach(ret -> {
//                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
//                    solver.addPFGEdge(csRet, csResult, RETURN);
//                });
//            }
//        }
    }

    /**
     * 给反射调用参数添加PFG edges
     * @param edge   边缘
     * @param arrays 是反射调用第二个参数（也就是那个Object数组指向的对象集合）
     */
    private void passReflectiveArgs(ReflectiveCallEdge edge, PointsToSet arrays) {
        Context calleeCtx = edge.getCallee().getContext();
        JMethod callee = edge.getCallee().getMethod();
        arrays.forEach(array -> {
            ArrayIndex elems = csManager.getArrayIndex(array);
            callee.getIR().getParams().forEach(param -> {
                Type paramType = param.getType();
                if (isConcerned(paramType)) {
                    CSVar csParam = csManager.getCSVar(calleeCtx, param);
                    solver.addPFGEdge(elems, csParam, PARAMETER_PASSING, paramType);
                }
            });
        });
    }

    /**
     * TODO: merge with DefaultSolver.isConcerned(Exp)
     */
    private static boolean isConcerned(Type type) {
        return type instanceof ClassType || type instanceof ArrayType;
    }
}
