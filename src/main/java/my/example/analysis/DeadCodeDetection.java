package my.example.analysis;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.dataflow.analysis.LiveVariable;
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.fact.NodeResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

import java.util.HashSet;
import java.util.Set;

public class DeadCodeDetection extends MethodAnalysis<Set<Stmt>> {

    // declare field ID
    public static final String ID = "my-deadcode";

    // implement constructor
    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    // implement analyze(IR) method
    @Override
    public Set<Stmt> analyze(IR ir) {
        // obtain results of dependent analyses
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        NodeResult<Stmt, CPFact> constants = ir.getResult(ConstantPropagation.ID);
        NodeResult<Stmt, SetFact<Var>> liveVars = ir.getResult(LiveVariable.ID);
        // analysis logic
        Set<Stmt> deadCode = new HashSet<>();
        return deadCode;
    }
}
