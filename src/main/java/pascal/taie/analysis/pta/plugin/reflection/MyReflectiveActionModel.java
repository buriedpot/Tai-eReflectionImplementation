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

import com.google.common.collect.Lists;
import pascal.taie.World;
import pascal.taie.WorldBuilder;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.mock.UJMethod;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.INSTANCE_STORE;
import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.STATIC_STORE;

/**
 * Models reflective-action methods, currently supports
 * <ul>
 *     <li>Class.forName(String)
 *     <li>Class.forName(String,boolean,ClassLoader)
 *     <li>Class.newInstance()
 *     <li>Constructor.newInstance(Object[])
 *     <li>Method.invoke(Object,Object[])
 *     <li>Field.get(Object)
 *     <li>Field.set(Object,Object)
 *     <li>Array.newInstance(Class,int)
 * </ul>
 * TODO: check accessibility
 */
class MyReflectiveActionModel extends AbstractModel {

    /**
     * Description for objects created by reflective newInstance() calls.
     */
    private final static String REF_OBJ_DESC = "ReflectiveObj";

    private final static String META_DESC = "ReflectionMetaObj";
    private final ClassType method;


    private final Subsignature initNoArg;

    private final ContextSelector selector;

    private final TypeSystem typeSystem;

    private final ClassType unknown;

    MyReflectiveActionModel(Solver solver) {
        super(solver);
        initNoArg = Subsignature.getNoArgInit();
        selector = solver.getContextSelector();
        typeSystem = solver.getTypeSystem();
        unknown = typeSystem.getClassType("myUnknownTypeBBADD");
        method = typeSystem.getClassType(ClassNames.METHOD);

    }

    @Override
    protected void registerVarAndHandler() {
        JMethod classForName = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class forName(java.lang.String)>");
        registerRelevantVarIndexes(classForName, 0);
        registerAPIHandler(classForName, this::classForName);

//        JMethod classForName2 = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class forName(java.lang.String,boolean,java.lang.ClassLoader)>");
//        // TODO: take class loader into account
//        registerRelevantVarIndexes(classForName2, 0);
//        registerAPIHandler(classForName2, this::classForName);
//
//        JMethod classNewInstance = hierarchy.getJREMethod("<java.lang.Class: java.lang.Object newInstance()>");
//        registerRelevantVarIndexes(classNewInstance, BASE);
//        registerAPIHandler(classNewInstance, this::classNewInstance);
//
//        JMethod constructorNewInstance = hierarchy.getJREMethod("<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>");
//        registerRelevantVarIndexes(constructorNewInstance, BASE);
//        registerAPIHandler(constructorNewInstance, this::constructorNewInstance);
//
        JMethod methodInvoke = hierarchy.getJREMethod("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>");
        registerRelevantVarIndexes(methodInvoke, BASE, 0);
        registerAPIHandler(methodInvoke, this::methodInvoke);
//
//        JMethod fieldGet = hierarchy.getJREMethod("<java.lang.reflect.Field: java.lang.Object get(java.lang.Object)>");
//        registerRelevantVarIndexes(fieldGet, BASE, 0);
//        registerAPIHandler(fieldGet, this::fieldGet);
//
//        JMethod fieldSet = hierarchy.getJREMethod("<java.lang.reflect.Field: void set(java.lang.Object,java.lang.Object)>");
//        registerRelevantVarIndexes(fieldSet, BASE, 0);
//        registerAPIHandler(fieldSet, this::fieldSet);
//
//        JMethod arrayNewInstance = hierarchy.getJREMethod("<java.lang.reflect.Array: java.lang.Object newInstance(java.lang.Class,int)>");
//        registerRelevantVarIndexes(arrayNewInstance, 0);
//        registerAPIHandler(arrayNewInstance, this::arrayNewInstance);
    }

    /**
     * 类名称
     *
     * @param csVar  cName对象
     * @param pts    cName指向对象集
     * @param invoke class.forName调用语句
     */
    private void classForName(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            String className = CSObjs.toString(obj);
            Var result = invoke.getResult();
            if (className == null) {
                //{c^u} otherwise
//                Obj clsObj = heapModel.getMockObj(META_DESC, unknown.getJClass(), unknown);
                Obj clsObj = heapModel.getConstantObj(ClassLiteral.get(unknown));
                CSObj csObj = csManager.getCSObj(defaultHctx, clsObj);
                solver.addVarPointsTo(context, result, csObj);
                return;
            }
            JClass klass = hierarchy.getClass(className);

            if (klass == null) {
                // TODO known but not exists, must process?
                return;
            }
            solver.initializeClass(klass);
            if (result != null) {
                // {c^t} if o_i^String \belongs pt(cName)
                Obj clsObj = heapModel.getConstantObj(
                        ClassLiteral.get(klass.getType()));
                CSObj csObj = csManager.getCSObj(defaultHctx, clsObj);
                solver.addVarPointsTo(context, result, csObj);
            }

        });
    }

    private void classNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            JClass klass = CSObjs.toClass(obj);
            if (klass == null) {
                return;
            }
            JMethod init = klass.getDeclaredMethod(initNoArg);
            if (init == null) {
                return;
            }
            ClassType type = klass.getType();
            CSObj csNewObj = newReflectiveObj(context, invoke, type);
            addReflectiveCallEdge(context, invoke, csNewObj, init, null);
        });
    }

    private void constructorNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            JMethod constructor = CSObjs.toConstructor(obj);
            if (constructor == null) {
                return;
            }
            ClassType type = constructor.getDeclaringClass().getType();
            CSObj csNewObj = newReflectiveObj(context, invoke, type);
            addReflectiveCallEdge(context, invoke, csNewObj,
                    constructor, invoke.getInvokeExp().getArg(0));
        });
    }

    /**
     * 这个是创建constructor创建出来的Obj
     * 不是Ct或者cu
     *
     * @param context 上下文
     * @param invoke  调用
     * @param type    类型
     * @return {@link CSObj}
     */
    private CSObj newReflectiveObj(Context context, Invoke invoke, ReferenceType type) {
        Obj newObj = heapModel.getMockObj(REF_OBJ_DESC,
                invoke, type, invoke.getContainer());
        // TODO: double-check if the heap context is proper
        CSObj csNewObj = csManager.getCSObj(context, newObj);
        Var result = invoke.getResult();
        if (result != null) {
            solver.addVarPointsTo(context, result, csNewObj);
        }
        return csNewObj;
    }

    /**
     * 方法调用
     * x = (A) m.invoke(y, args)
     *
     * @param csVar  m
     * @param pts    分
     * @param invoke 调用
     */
    private void methodInvoke(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        // m对象集合pt(m)
        PointsToSet mtdObjs = args.get(0);
        // y的对象集合
        PointsToSet recvObjs = args.get(1);
        // 这个Var是args数组
        Var argsVar = invoke.getInvokeExp().getArg(1);
        Var y = invoke.getInvokeExp().getArg(0);
        PointsToSet inferResultMtdObjs = solver.makePointsToSet();


        int argsLength = inferArrayLengthByNewStmt(argsVar);
        List<String> originalArgTypes = inferArrayArgTypesByStoreArray(argsVar);
        List<List<String>> ptpResult = ptp(originalArgTypes);

        // 获取A类型(可能有多个可能的A类型)
        List<String> allPossibleA = getMethodInvokeCastTargetClass(invoke.getLValue());
        // 根据A类型，获取所有可能的tr类型(tr <<:A)
        List<String> allPossibleTr = getThisAndAllRelatedClasses(allPossibleA);
        if (allPossibleTr.size() == 0) allPossibleTr.add(null);


        mtdObjs.forEach(mtdObj -> {
            JMethod target = CSObjs.toMethod(mtdObj);
            // need inference
            if (target == null) {
                Object alloc = mtdObj.getObject().getAllocation();
                if (alloc instanceof UJMethod ujMethod) {
                    if (ujMethod.isKnown()) {
                        List<JMethod> allPossibleJMethods = MTD(ujMethod);
                        for (JMethod possibleTargetMethod : allPossibleJMethods) {
                            recvObjs.forEach(recvObj ->
                                    addReflectiveCallEdge(context, invoke, recvObj, possibleTargetMethod, argsVar)
                            );
                        }
                    }
                    // mu − ∈ pt(m)
                    // -------------------------
                    //pt(m) ⊇ { mt− | oti ∈ pt (y)}
                    //[I-InvTp]
                    if (ujMethod.isDeclaringClassUnknown()) {
                        recvObjs.forEach(csRecvObj -> {
                            String possibleDelaringClass = csRecvObj.getObject().getType().getName();
                            UJMethod inferResult = new UJMethod(
                                    possibleDelaringClass,
                                    ujMethod.returnType(),
                                    ujMethod.methodName(),
                                    ujMethod.parameters()
                            );
                            Obj inferResultMtdObj = heapModel.getMockObj(META_DESC, inferResult, method);
                            CSObj csObj = csManager.getCSObj(defaultHctx, inferResultMtdObj);
                            inferResultMtdObjs.addObject(csObj);
                        });

                    }

                    // mus ∈ pt(m) oui ∈ pt (y) s.tr ≪: A s.nm , u s.p ∈ Ptp(args)
                    //pt(m) ⊇ { mts | t ∈ M(s.tr , s.nm, s.p)}
                    //[I-InvS2T]

                    if (ujMethod.isDeclaringClassUnknown() && !ujMethod.isSubsignatureUnknown()) {
                        boolean hasUnknownTypeObject = false;
                        for (CSObj csRecvObj : recvObjs) {
                            if (csRecvObj.getObject().getType().equals(unknown)
                                    || csRecvObj.getObject().getType() == null) {
                                hasUnknownTypeObject = true;
                                break;
                            }
                        }
                        if (hasUnknownTypeObject) {
                            if (ujMethod.isReturnTypeUnknown() || allPossibleTr.contains(ujMethod.returnType())) {
                                if (!ujMethod.isMethodNameUnknown()) {
                                    if (!ujMethod.isParametersUnknown() &&
                                            belongsToPtp(ujMethod.parameters(), ptpResult)) {
                                        // 获取M(s.tr , s.nm, s.p)
                                        Set<String> possibleDeclaringClasses = M(ujMethod.returnType(), ujMethod.methodName(), ujMethod.parameters());
                                        for (String possibleDeclaringClass : possibleDeclaringClasses) {
                                            UJMethod inferResult = new UJMethod(possibleDeclaringClass,
                                                    ujMethod.returnType(), ujMethod.methodName(), ujMethod.parameters());
                                            Obj inferResultMtdObj = heapModel.getMockObj(META_DESC, inferResult, method);
                                            CSObj csObj = csManager.getCSObj(defaultHctx, inferResultMtdObj);
                                            inferResultMtdObjs.addObject(csObj);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // m−u ∈ pt(m)
                    // ------------------------
                    //pt(m) ⊇ { m−s | s.p ∈ Ptp(args), s.tr ≪: A, s.nm = u}
                    //[I-InvSig]
                    if (ujMethod.isParametersUnknown()) {
                        // 按理说应该用csobj的t来推断。这里从简，直接用var的type来推断。TODO: modify to csobj
                        /*for (CSObj csObj : csManager.getCSVar(context, argsVar).getPointsToSet()) {
                            Pointer arrayPtr = csManager.getArrayIndex(csObj);
                        }*/

                        // 以这两个参数来计算PTP

                        for (List<String> onePtp : ptpResult) {
                            for (String oneTr : allPossibleTr) {
                                // 构造m-s
                                UJMethod inferResult = new UJMethod(ujMethod.declaringClass(),
                                        oneTr, ujMethod.methodName(), onePtp);
                                Obj inferResultMtdObj = heapModel.getMockObj(META_DESC, inferResult, method);
                                CSObj csObj = csManager.getCSObj(defaultHctx, inferResultMtdObj);
                                inferResultMtdObjs.addObject(csObj);
                            }
                        }
                        // TODO 暂时不构造arg1到argk了
                    }
                    solver.addVarPointsTo(context, ((InvokeInstanceExp) invoke.getInvokeExp()).getBase(), inferResultMtdObjs);


                }


                // no need inference, directly add stmts (mts)
                // TODO 暂时不按照T-INV实现，先跑通
                // [T-Inv]


                return;
            }
            if (target.isStatic()) {
//                addReflectiveCallEdge(context, invoke, null, target, argsVar);
            } else {
//                recvObjs.forEach(recvObj ->
//                        addReflectiveCallEdge(context, invoke, recvObj, target, argsVar)
//                );
            }
        });
    }

    private void addReflectiveCallEdge(
            Context callerCtx, Invoke callSite,
            @Nullable CSObj recvObj, JMethod callee, Var args) {
        if (!callee.isConstructor() && !callee.isStatic()) {
            // dispatch for instance method (except constructor)
            assert recvObj != null : "recvObj is required for instance method";
            callee = hierarchy.dispatch(recvObj.getObject().getType(),
                    callee.getRef());
            if (callee == null) {
                return;
            }
        }
        CSCallSite csCallSite = csManager.getCSCallSite(callerCtx, callSite);
        Context calleeCtx;
        if (callee.isStatic()) {
            calleeCtx = selector.selectContext(csCallSite, callee);
        } else {
            calleeCtx = selector.selectContext(csCallSite, recvObj, callee);
            // pass receiver object to 'this' variable of callee
            solver.addVarPointsTo(calleeCtx, callee.getIR().getThis(), recvObj);
        }
        ReflectiveCallEdge callEdge = new ReflectiveCallEdge(csCallSite,
                csManager.getCSMethod(calleeCtx, callee), args);
        solver.addCallEdge(callEdge);
    }

    private void fieldGet(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        Context context = csVar.getContext();
        CSVar to = csManager.getCSVar(context, result);
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet fldObjs = args.get(0);
        PointsToSet baseObjs = args.get(1);
        fldObjs.forEach(fldObj -> {
            JField field = CSObjs.toField(fldObj);
            if (field == null) {
                return;
            }
            if (field.isStatic()) {
                StaticField sfield = csManager.getStaticField(field);
                solver.addPFGEdge(sfield, to, PointerFlowEdge.Kind.STATIC_LOAD);
            } else {
                Type declType = field.getDeclaringClass().getType();
                baseObjs.forEach(baseObj -> {
                    Type objType = baseObj.getObject().getType();
                    if (typeSystem.isSubtype(declType, objType)) {
                        InstanceField ifield = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(ifield, to, PointerFlowEdge.Kind.INSTANCE_LOAD);
                    }
                });
            }
        });
    }

    private void fieldSet(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        CSVar from = csManager.getCSVar(context, invoke.getInvokeExp().getArg(1));
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet fldObjs = args.get(0);
        PointsToSet baseObjs = args.get(1);
        fldObjs.forEach(fldObj -> {
            JField field = CSObjs.toField(fldObj);
            if (field == null) {
                return;
            }
            if (field.isStatic()) {
                StaticField sfield = csManager.getStaticField(field);
                solver.addPFGEdge(from, sfield, STATIC_STORE, sfield.getType());
            } else {
                Type declType = field.getDeclaringClass().getType();
                baseObjs.forEach(baseObj -> {
                    Type objType = baseObj.getObject().getType();
                    if (typeSystem.isSubtype(declType, objType)) {
                        InstanceField ifield = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(from, ifield, INSTANCE_STORE, ifield.getType());
                    }
                });
            }
        });
    }

    private void arrayNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            Type baseType = CSObjs.toType(obj);
            if (baseType == null || baseType instanceof VoidType) {
                return;
            }
            ArrayType arrayType = typeSystem.getArrayType(baseType, 1);
            CSObj csNewArray = newReflectiveObj(context, invoke, arrayType);
            solver.addVarPointsTo(context, result, csNewArray);
        });
    }

    /**
     * 推断出数组长度新支撑
     * 寻找arrayCSVar New语句，获取length
     * 目前只考虑初始化参数为int常量的情况
     *
     * @param arrayVar 数组变量
     * @return int
     */
    public int inferArrayLengthByNewStmt(Var arrayVar) {
        for (Stmt stmt : arrayVar.getMethod().getIR().getStmts()) {
            if (stmt instanceof New newStmt) {
                NewExp rValue = newStmt.getRValue();
                if (rValue instanceof NewArray newArrayStmt) {
                    Var lengthVar = newArrayStmt.getLength();
                    if (lengthVar.isConst()) {
                        Literal constValue = lengthVar.getConstValue();
                        if (constValue instanceof IntLiteral intConstValue) {
                            return intConstValue.getValue();
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 推断参数类型数组存储阵列
     * 利用对invoke语句第二个参数的装载过程推断每个参数的类型（非PTP，仍然需要下一步PTP过程）
     * 暂时先不用CSObj来推断。有些困难
     *
     * @param arrayVar 数组变量
     * @return {@link List}<{@link String}>
     */
    public List<String> inferArrayArgTypesByStoreArray(Var arrayVar) {
        List<String> result = new ArrayList<String>();
        arrayVar.getStoreArrays().forEach(storeArray -> {
            Var rValue = storeArray.getRValue();
            result.add(rValue.getType().getName());
        });
        return result;
    }

    public List<List<String>> ptp(List<String> originalTypes) {
        List<List<String>> eachTypeSuperTypes = new ArrayList<List<String>>();
        for (String originalType : originalTypes) {
            List<String> allSuperTypes = getThisAndAllSuperClasses(originalType);
            eachTypeSuperTypes.add(allSuperTypes);
        }
        return Lists.cartesianProduct(eachTypeSuperTypes);
    }


    /**
     * ptp2
     * 参考论文的，无法精确分析数组类型时的PTP实现
     * 参数1是pt(o-i.arr)的所有对象type。参数2是每个实参的declaredType
     * TODO 没完全理解
     *
     * @param arrTypes 加勒比海盗类型
     * @return {@link List}<{@link List}<{@link String}>>
     */
    public List<List<String>> ptp2(Collection<String> arrTypes, List<String> declaredTypes) {
        List<List<String>> eachTypeSuperTypes = new ArrayList<List<String>>();
        for (String declaredType : declaredTypes) {
            for (String arrType : arrTypes) {
//                if (get)
            }
        }
        return Lists.cartesianProduct(eachTypeSuperTypes);
    }


    /**
     * <:
     *
     * @param className 类名
     * @return {@link List}<{@link String}>
     */
    public List<String> getThisAndAllSuperClasses(String className) {
        List<String> result = new ArrayList<String>();
        JClass thisClass = hierarchy.getClass(className);
        while (thisClass != null) {
            result.add(thisClass.getName());
            thisClass = thisClass.getSuperClass();
        }
        return result;
    }

    /**
     * <:
     *
     * @param className 类名
     * @return {@link List}<{@link String}>
     */
    public List<JClass> getAllSuperClasses(String className) {
        List<JClass> result = new ArrayList<>();
        JClass thisClass = hierarchy.getClass(className);
        if (thisClass == null) {
            return null;
        }
        if (thisClass.getSuperClass() == null) {
            return new ArrayList<>();
        }
        thisClass = thisClass.getSuperClass();
        while (thisClass != null) {
            result.add(thisClass);
            thisClass = thisClass.getSuperClass();
        }
        return result;
    }

    /**
     * <<:
     *
     * @param className 类名
     * @return {@link List}<{@link String}>
     */
    public List<String> getThisAndAllRelatedClasses(String className) {
        JClass thisClass = hierarchy.getClass(className);
        if (thisClass == null) {
            return null;
        }
        Collection<JClass> allSubclassesIncludeSelf = hierarchy.getAllSubclassesOf(thisClass);
        Collection<JClass> allRelatedClasses = getAllSuperClasses(className);
        allRelatedClasses.addAll(allSubclassesIncludeSelf);
        return allRelatedClasses.stream().map(JClass::getName).collect(Collectors.toList());
    }

    /**
     * <<:
     *
     * @return {@link List}<{@link String}>
     */
    public List<String> getThisAndAllRelatedClasses(List<String> classNames) {
        List<String> result = new ArrayList<String>();
        Set<String> mediate = new HashSet<String>();
        for (String className : classNames) {
            mediate.addAll(getThisAndAllRelatedClasses(className));
        }
        return new ArrayList<>(mediate);
    }


    /**
     * get方法调用目标类
     * x = (A) m.invoke(y, args)
     * 获取类型A的值
     *
     * @param lValue l值
     * @return {@link List}<{@link String}>
     */
    public List<String> getMethodInvokeCastTargetClass(Var lValue) {
        // 获取lValue作为右值的所有Cast语句
        List<Cast> allCastAsRValue = getAllCastAsRValue(lValue);
        return allCastAsRValue.stream()
                .map(Cast::getRValue)
                .map(CastExp::getCastType)
                .map(Type::getName)
                .collect(Collectors.toList());

    }

    /**
     * 获取所有var作为右值的cast语句
     *
     * @param var var
     * @return {@link List}<{@link Stmt}>
     */
    public List<Cast> getAllCastAsRValue(Var var) {
        return var.getMethod().getIR().getStmts().stream().filter(stmt -> {
            boolean result = stmt instanceof Cast;
            if (result) {
                Cast cast = (Cast) stmt;
                result = cast.getRValue().getValue() == var;
            }
            return result;
        }).map(stmt -> (Cast) stmt).collect(Collectors.toList());
    }


    /**
     * 属于20元
     *
     * @param argTypes
     * @param ptp
     * @return boolean
     */
    public boolean belongsToPtp(List<String> argTypes, List<List<String>> ptp) {
        for (List<String> onePtp : ptp) {
            if (equalList(onePtp, argTypes)) {
                return true;
            }
        }
        return false;
    }


    public boolean equalList(List<String> list1, List<String> list2) {
        if (list1 == list2) {
            return true;
        }
        if ((list1 == null) ^ (list2 == null)) {
            return false;
        }

        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;

    }


    /**
     * the M function
     * 暂时不考虑returnType
     *
     * @param returnType 返回类型
     * @param methodName 方法名称
     * @param parameters
     * @return {@link Set}<{@link String}>
     */
    public Set<String> M(String returnType, String methodName, List<String> parameters) {
        return hierarchy.allClasses().filter(jClass ->
                !jClass.getDeclaredMethods().stream().filter(declaredMethod ->
                        declaredMethod.getName().equals(methodName) &&
                                equalList(declaredMethod.getParamTypes().stream()
                                        .map(Type::getName)
                                        .collect(Collectors.toList()), parameters)

                ).collect(Collectors.toSet()).isEmpty()
        ).map(JClass::getName).collect(Collectors.toSet());

    }


    public List<JMethod> MTD(UJMethod ujMethod) {
        List<JMethod> result = new ArrayList<>();
        String returnType = ujMethod.returnType();
        String methodName = ujMethod.methodName();
        List<String> parameters = ujMethod.parameters();
        hierarchy.allClasses().forEach(jClass -> {
            result.addAll(jClass.getDeclaredMethods().stream().filter(declaredMethod ->
                    declaredMethod.getName().equals(methodName) &&
                            equalList(declaredMethod.getParamTypes().stream()
                                    .map(Type::getName)
                                    .collect(Collectors.toList()), parameters)
            ).collect(Collectors.toSet()));
        });
        return result;

    }
}
