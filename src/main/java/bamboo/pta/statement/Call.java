/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.statement;

import bamboo.pta.element.CallSite;
import bamboo.pta.element.Variable;

/**
 * Represents a call statement r = o.m()/r = T.m();
 */
public class Call implements Statement {

    private final CallSite callSite;

    private final Variable lhs;

    public Call(CallSite callSite, Variable lhs) {
        this.callSite = callSite;
        this.lhs = lhs;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Variable getLHS() {
        return lhs;
    }

    @Override
    public Kind getKind() {
        return Kind.CALL;
    }

    @Override
    public String toString() {
        return lhs != null
                ? lhs + " = " + callSite.toString()
                : callSite.toString();
    }
}