package my.example.plugin;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;

import java.util.Optional;

public class ReflectionAnalysis implements Plugin {


    protected final ReflectionModel reflectionModel = new ReflectionModel();
    public Solver solver;
    public CSManager csManager;
    private ContextSelector contextSelector;
    private ClassHierarchy hierarchy;
    private TypeSystem typeSystem;
    private ContextSelector selector;
    private HeapModel heapModel;
    private Context defaultHctx;

    @Override
    public void onFinish() {
        Plugin.super.onFinish();
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Plugin.super.onNewCallEdge(edge);
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        contextSelector = solver.getContextSelector();
        hierarchy = solver.getHierarchy();
        typeSystem = solver.getTypeSystem();
        selector = solver.getContextSelector();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
        defaultHctx = solver.getContextSelector().getEmptyContext();
    }

    @Override
    public void onStart() {
        Plugin.super.onStart();
    }

    @Override
    public void onNewMethod(JMethod method) {
        Plugin.super.onNewMethod(method);
    }


    /**
     * 新csmethod
     * 在此方法中实现soot中classForName classNewInstance constructorNewInstance等逻辑
     *
     * @param csMethod cs方法
     */
    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        getImplicitTargets(csMethod);
    }

    private void getImplicitTargets(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        for (Stmt stmt : method.getIR().getStmts()) {
            if (stmt instanceof Invoke invokeStmt) {
                MethodRef methodRef = invokeStmt.getMethodRef();
                switch (methodRef.getDeclaringClass().getName()) {
                    case "java.lang.reflect.Method":
                        if ("java.lang.Object invoke(java.lang.Object,java.lang.Object[])"
                                .equals(methodRef.getSubsignature().toString())) {
                            reflectionModel.methodInvoke(csMethod, stmt);
                        }
                        break;
                    case "java.lang.Class":
                        if ("java.lang.Object newInstance()"
                                .equals(methodRef.getSubsignature().toString())) {
                            reflectionModel.classNewInstance(csMethod, stmt);
                        } else if ("java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])"
                                .equals(methodRef.getSubsignature().toString())) {
                            reflectionModel.classGetMethod(csMethod, stmt);
                        }
                        break;
                    case "java.lang.reflect.Constructor":
                        if ("java.lang.Object newInstance(java.lang.Object[])"
                                .equals(methodRef.getSubsignature().toString())) {
                            reflectionModel.contructorNewInstance(csMethod, stmt);
                        }
//                        JMethod.class.getMethod()
                        break;
                    default:
                        break;
                }
                if ("java.lang.Class forName(java.lang.String)".equals(methodRef.getSubsignature())) {
                    reflectionModel.classForName(csMethod, stmt);
                }
//                JMethod.class.getMethod();
            }
        }
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        Plugin.super.onUnresolvedCall(recv, context, invoke);
    }


    public class ReflectionModel {
        public void methodInvoke(CSMethod method, Stmt stmt) {
            // TODO NotImplementation

        }

        public void classNewInstance(CSMethod csMethod, Stmt stmt) {
            // TODO NotImplementation

        }

        public void contructorNewInstance(CSMethod csMethod, Stmt stmt) {
            // TODO NotImplementation

        }

        public void classForName(CSMethod csMethod, Stmt stmt) {
            // TODO NotImplementation
            Invoke invokeStmt = (Invoke) stmt;
            Var result = invokeStmt.getLValue();
            Var className = invokeStmt.getInvokeExp().getArg(0);
            // 获取className的CSVar。这里context就是csMethod的context
            CSVar csClassName = csManager.getCSVar(csMethod.getContext(), className);
            PointsToSet csClassNamePointsToSet = solver.getPointsToSetOf(csClassName);
            for (CSObj csObj : csClassNamePointsToSet) {
                Obj object = csObj.getObject();
                if ("java.lang.String".equals(object.getType().getName())) {
                    // 获取类名。如果是Null，说明CSObj并不是常量
                    Context context = csClassName.getContext();
                    String classNameString = CSObjs.toString(csObj);
                    if (classNameString == null) {
                        return;
                    }
                    JClass klass = hierarchy.getClass(classNameString);
                    if (klass == null) {
                        return;
                    }
                    solver.initializeClass(klass);
                    if (result != null) {
                        Obj clsObj = heapModel.getConstantObj(
                                ClassLiteral.get(klass.getType()));
                        CSObj csClsObj = csManager.getCSObj(defaultHctx, clsObj);
                        solver.addVarPointsTo(context, result, csClsObj);
                    }
                }
            }
        }

        /**
         * O_i^String \belong pt(mName) c^- \belong pt(c')
         * -------------------------------------------------
         * pt(m) contains {
         * {m_s^t} if c^- = c^t and o_i^String \belong SC
         * {m_u^t} if c^- = c^t and o_i^String not \belong SC
         * {m_s^u} if c^- = c^u and o_i^String \belong SC
         * {m_u^u} if c^- = c^u and o_i^String not \belong SC
         *
         * s.t_r=u
         * s.n_m = val(o_i^String)
         * s.p = u
         *
         * @param csMethod cs方法
         * @param stmt     支撑
         */
        public void classGetMethod(CSMethod csMethod, Stmt stmt) {
//            solver.addStmts
        }

    }


    // csVar的对象集合发生变化。执行P-FORNAME和P-GETMETHOD
    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
//        csVar.getVar().get
    }
}
