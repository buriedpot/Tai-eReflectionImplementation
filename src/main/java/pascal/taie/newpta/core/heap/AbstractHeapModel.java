/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta.core.heap;

import pascal.taie.ir.exp.NewExp;
import pascal.taie.java.TypeManager;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;

import java.util.Map;

import static pascal.taie.java.classes.StringReps.STRING;
import static pascal.taie.java.classes.StringReps.STRING_BUFFER;
import static pascal.taie.java.classes.StringReps.STRING_BUILDER;
import static pascal.taie.java.classes.StringReps.THROWABLE;
import static pascal.taie.util.CollectionUtils.newHybridMap;
import static pascal.taie.util.CollectionUtils.newMap;

/**
 * All heap models should inherit this class, and we can define
 * some uniform behaviors of heap modeling here.
 */
abstract class AbstractHeapModel implements HeapModel {

    private final TypeManager typeManager;

    private final Type string;

    private final Type stringBuilder;

    private final Type stringBuffer;

    private final Type throwable;

    private final Map<NewExp, NewObj> objs = newMap();

    private final Map<Type, Map<Object, ConstantObj<?>>> constantObjs = newHybridMap();

    /**
     * The merged object representing string constants.
     */
    private final MergedObj mergedSC;

    private final Map<Type, MergedObj> mergedObjs = newMap();

    private final EnvObjs envObjs;

    protected AbstractHeapModel(TypeManager typeManager) {
        this.typeManager = typeManager;
        string = typeManager.getClassType(STRING);
        stringBuilder = typeManager.getClassType(STRING_BUILDER);
        stringBuffer = typeManager.getClassType(STRING_BUFFER);
        throwable = typeManager.getClassType(THROWABLE);
        mergedSC = new MergedObj(string, "<Merged string constants>");
        envObjs = new EnvObjs(typeManager);
    }

    @Override
    public Obj getObj(NewExp newExp) {
        Type type = newExp.getType();
        NewObj obj = objs.computeIfAbsent(newExp, NewObj::new);
        if (PTAOptions.get().isMergeStringObjects() &&
                type.equals(string)) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeStringBuilders() &&
                (type.equals(stringBuilder) || type.equals(stringBuffer))) {
            return getMergedObj(type, obj);
        }
        if (PTAOptions.get().isMergeExceptionObjects() &&
                typeManager.isSubtype(throwable, type)) {
            return getMergedObj(type, obj);
        }
        return doGetObj(obj);
    }

    @Override
    public <T> Obj getConstantObj(Type type, T value) {
        Obj obj = doGetConstantObj(type, value);
        if (PTAOptions.get().isMergeStringConstants() &&
                type.equals(string)) {
            mergedSC.addRepresentedObj(obj);
            return mergedSC;
        }
        return obj;
    }

    protected <T> Obj doGetConstantObj(Type type, T value) {
        return constantObjs.computeIfAbsent(type, t -> newMap())
                .computeIfAbsent(value, v -> new ConstantObj<>(type, v));
    }

    @Override
    public <T> Obj getMockObj(T value) {
        throw new UnsupportedOperationException();
    }

    /**
     * The method which controls the heap modeling for normal objects.
     */
    protected abstract Obj doGetObj(NewObj obj);

    /**
     * @param type the type of the objects to be merged
     * @param obj the object to be merged
     * @return the merged object
     */
    private Obj getMergedObj(Type type, Obj obj) {
        MergedObj mergedObj = mergedObjs.computeIfAbsent(
                type, (k) -> new MergedObj(type, "<Merged " + type + ">"));
        mergedObj.addRepresentedObj(obj);
        return mergedObj;
    }

    @Override
    public EnvObjs getEnvObjs() {
        return envObjs;
    }
}