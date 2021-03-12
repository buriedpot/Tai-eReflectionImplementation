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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.Var;

/**
 * Representation of monitorenter/monitorexit instruction.
 */
public class Monitor extends AbstractStmt {

    // TODO: hide Op? To achieve this, we can replace the constructors
    //  to static factory methods, e.g., Monitor.newEnter(var).
    //  But for consistency, we also need to modify all other Stmt
    //  and replace their constructors by static factory methods.
    public enum Op {
        ENTER("enter"), EXIT("exit");

        private final String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Op op;

    /**
     * Reference of the object to be locked/unlocked.
     */
    private final Var objectRef;

    public Monitor(Op op, Var objectRef) {
        this.op = op;
        this.objectRef = objectRef;
    }

    public boolean isEnter() {
        return op == Op.ENTER;
    }

    public boolean isExit() {
        return op == Op.EXIT;
    }

    public Var getObjectRef() {
        return objectRef;
    }

    @Override
    public String toString() {
        return "monitor" + op + " " + objectRef;
    }
}