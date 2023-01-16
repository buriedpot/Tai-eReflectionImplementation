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

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.mock.UJMethod;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.Reflections;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.List;

class MyStringBasedModel extends MetaObjModel {

    private final static String META_DESC = "ReflectionMetaObj";
    public final static String ARRAY_LENGTH_DESC = "ArrayLengthObj";

    private final static String UNKNOWN_SIG_MTD = "UnknownSigMtd";
    private final ClassType method;

    private final ClassType integer;


    MyStringBasedModel(Solver solver) {
        super(solver);
        method = typeSystem.getClassType(ClassNames.METHOD);
        integer = typeSystem.getClassType(ClassNames.INTEGER);
    }

    @Override
    protected void registerVarAndHandler() {
//        registerRelevantVarIndexes(get("getConstructor"), BASE);
//        registerAPIHandler(get("getConstructor"), this::getConstructor);
//
//        registerRelevantVarIndexes(get("getDeclaredConstructor"), BASE);
//        registerAPIHandler(get("getDeclaredConstructor"), this::getDeclaredConstructor);

        registerRelevantVarIndexes(get("getMethod"), BASE, 0);
        registerAPIHandler(get("getMethod"), this::getMethod);


        registerRelevantVarIndexes(get("getMethods"), BASE);
        registerAPIHandler(get("getMethods"), this::getMethods);
//
//        registerRelevantVarIndexes(get("getDeclaredMethod"), BASE, 0);
//        registerAPIHandler(get("getDeclaredMethod"), this::getDeclaredMethod);


    }

    private void getConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = solver.makePointsToSet();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getConstructors(jclass)
                            .map(ctor -> {
                                Obj ctorObj = getReflectionObj(ctor);
                                return csManager.getCSObj(defaultHctx, ctorObj);
                            })
                            .forEach(ctorObjs::addObject);
                }
            });
            if (!ctorObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void getDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = solver.makePointsToSet();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getDeclaredConstructors(jclass)
                            .map(ctor -> {
                                Obj ctorObj = getReflectionObj(ctor);
                                return csManager.getCSObj(defaultHctx, ctorObj);
                            })
                            .forEach(ctorObjs::addObject);
                }
            });
            if (!ctorObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    /**
     * get方法
     * Method m = c′.getMethod(mName, ...)
     * O_i^String \belong pt(mName) c^- \belong pt(c')
     * -------------------------------------------------
     * pt(m) contains {
     * {m_s^t} if c^- = c^t and o_i^String \belong SC
     * {m_u^t} if c^- = c^t and o_i^String not \belong SC
     * {m_s^u} if c^- = c^u and o_i^String \belong SC
     * {m_u^u} if c^- = c^u and o_i^String not \belong SC
     * <p>
     * s.t_r=u
     * s.n_m = val(o_i^String)
     * s.p = u
     *
     * @param csVar  cs var c' mName or other
     * @param pts    分
     * @param invoke 调用
     */
    private void getMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            // c' 的 对象集合
            PointsToSet clsObjs = args.get(0);
            // mName的对象集合
            PointsToSet nameObjs = args.get(1);
            // ...(数组)的对象集合
            PointsToSet mtdObjs = solver.makePointsToSet();


            // 对每个c'指向的对象
            clsObjs.forEach(clsObj -> {
                //  [P-GetMtd]
                JClass cls = CSObjs.toClass(clsObj);
                // if c-=c^t and o_iString belongsto SC
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        // if c− = ct ∧ o_i^String \belongs SC
                        if (name != null) {
                            UJMethod ujMethod = new UJMethod(
                                    cls.getName(), UJMethod.UNKNOWN_STRING,
                                    name, UJMethod.UNKNOWN_LIST);
                            Obj unknownMthdObj = heapModel.getMockObj(META_DESC, ujMethod, method);
                            CSObj csObj = csManager.getCSObj(defaultHctx, unknownMthdObj);
                            mtdObjs.addObject(csObj);
                        }
                        // if c- == ct and o_iString not \belongs SC
                        // pt(m_ contains m_u^t
                        else {
                            UJMethod ujMethod = new UJMethod(cls.getName(), UJMethod.UNKNOWN_STRING,
                                    UJMethod.UNKNOWN_STRING, UJMethod.UNKNOWN_LIST);
                            Obj unknownSigMtdObj = heapModel.getMockObj(META_DESC, ujMethod, method);
                            CSObj csObj = csManager.getCSObj(defaultHctx, unknownSigMtdObj);
                            mtdObjs.addObject(csObj);
                        }
                    });
                } else {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        // if c− = cu ∧ o_i^String \belongs SC
                        // TODO reconstruct after verifying correctness
                        if (name != null) {
                            UJMethod ujMethod = new UJMethod(UJMethod.UNKNOWN_STRING, UJMethod.UNKNOWN_STRING,
                                    name, UJMethod.UNKNOWN_LIST);
                            Obj unknownMthdObj = heapModel.getMockObj(META_DESC, ujMethod, method);
                            CSObj csObj = csManager.getCSObj(defaultHctx, unknownMthdObj);
                            mtdObjs.addObject(csObj);
                        } else {
                            UJMethod ujMethod = new UJMethod(UJMethod.UNKNOWN_STRING, UJMethod.UNKNOWN_STRING,
                                    UJMethod.UNKNOWN_STRING, UJMethod.UNKNOWN_LIST);
                            Obj unknownMthdObj = heapModel.getMockObj(META_DESC, ujMethod, method);
                            CSObj csObj = csManager.getCSObj(defaultHctx, unknownMthdObj);
                            mtdObjs.addObject(csObj);
                        }
                    });
                }

            });

            if (!mtdObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtdObjs);
            }
        }
    }

    private void getMethods(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE);
            // c' 的 对象集合
            PointsToSet clsObjs = args.get(0);
            // mName的对象集合
            // ...(数组)的对象集合
            PointsToSet mtdObjs = solver.makePointsToSet();


            // 对每个c'指向的对象
            clsObjs.forEach(clsObj -> {
                //  [P-GetMtd]
                JClass cls = CSObjs.toClass(clsObj);
                // if c-=c^t and o_iString belongsto SC
                if (cls != null) {
                    UJMethod ujMethod = new UJMethod(
                            cls.getName(), UJMethod.UNKNOWN_STRING,
                            UJMethod.UNKNOWN_STRING, UJMethod.UNKNOWN_LIST);
                    ujMethod.setMethodInMethods(true);
                    Obj unknownMthdObj = heapModel.getMockObj(META_DESC, ujMethod, method);
                    CSObj csObj = csManager.getCSObj(defaultHctx, unknownMthdObj);
                    mtdObjs.addObject(csObj);
                } else {

                }

            });

            if (!mtdObjs.isEmpty()) {
                if (invoke.getLValue() != null) {
                    CSVar csLValue = csManager.getCSVar(csVar.getContext(), invoke.getLValue());
                    CSObj methodsCSObj = csManager.getCSObj(csVar.getContext(), heapModel.getMockObj(META_DESC, "methodsobj",
                            typeSystem.getArrayType(method, 1)));
                    solver.addPointsTo(csLValue, methodsCSObj);
                    solver.addPointsTo(csManager.getArrayIndex(methodsCSObj), mtdObjs);

                }
            }
        }
    }

    private void getDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mtdObjs = solver.makePointsToSet();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            Reflections.getDeclaredMethods(cls, name)
                                    .map(mtd -> {
                                        Obj mtdObj = getReflectionObj(mtd);
                                        return csManager.getCSObj(defaultHctx, mtdObj);
                                    })
                                    .forEach(mtdObjs::addObject);
                        }
                    });
                }
            });
            if (!mtdObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtdObjs);
            }
        }
    }

    @Override
    void handleNewCSMethod(CSMethod csMethod) {
        JMethod jMethod = csMethod.getMethod();
        jMethod.getIR()
                .getStmts().stream().forEach(stmt -> {
                    if (stmt instanceof New newStmt &&
                            newStmt.getRValue() instanceof NewArray) {
                        NewArray newArrayExp = (NewArray) newStmt.getRValue();
                        if (newArrayExp.getLength().isConst()) {
                            Literal arrayLenLiteral = newArrayExp.getLength().getConstValue();
                            int len = 0;
                            Var arrayVar = newStmt.getLValue();
                            CSVar csArrayVar = csManager.getCSVar(csMethod.getContext(), arrayVar);
                            if (arrayLenLiteral instanceof IntLiteral intConstValue) {
                                len = intConstValue.getValue();
                                Obj arrayLenObj = heapModel.getMockObj(ARRAY_LENGTH_DESC,
                                        len, newArrayExp.getType());
                                CSObj csObj = csManager.getCSObj(defaultHctx, arrayLenObj);
                                solver.addVarPointsTo(csArrayVar.getContext(),
                                        arrayVar, csObj);
                            }

                        }
                    }
                    if (stmt instanceof Invoke invoke &&
                            "<java.lang.String: void <init>(java.lang.String)>"
                                    .equals(invoke.getInvokeExp().getMethodRef().resolve().getSignature())) {
                        CSVar from = csManager.getCSVar(csMethod.getContext(), invoke.getInvokeExp().getArg(0));
                        CSVar to = csManager.getCSVar(csMethod.getContext(), ((InvokeSpecial) invoke.getInvokeExp()).getBase());
                        solver.addPFGEdge(from, to, PointerFlowEdge.Kind.LOCAL_ASSIGN);
                    }
                });
    }
}
