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

package pascal.taie.analysis.pta;

import org.junit.After;
import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.graph.callgraph.CallGraphs;

public class MyReflectionTest {

    private static final String MYDIR = "myreflection";

    private static final String DIR = "reflection";

    @Test
    public void testSuperBasic() {
        Tests.testPTA(MYDIR, "SuperBasic", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }

    @Test
    public void testBasic() {
        Tests.testPTA(MYDIR, "Basic", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }
    @Test
    public void testArgsRefine() {
        Tests.testPTA(MYDIR, "ArgsRefine", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }

    @Test
    public void testInheritance() {
        Tests.testPTA(MYDIR, "Inheritance", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }
    @Test
    public void testRecvType() {
        Tests.testPTA(MYDIR, "RecvType", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }
    @Test
    public void testUnknownClassName() {
        Tests.testPTA(MYDIR, "UnknownClassName", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }

    @Test
    public void testUnknownMethodName() {
        Tests.testPTA(MYDIR, "UnknownMethodName", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];cs:2-call;action:dump");
//        Tests.testPTA(MYDIR, "UnknownMethodName", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }

    @Test
    public void testGetMethods() {
        Tests.testPTA(MYDIR, "GetMethods", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }

    @Test
    public void testDuplicateName() {
        Tests.testPTA(MYDIR, "DuplicateName", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis];action:dump");
    }
    @Test
    public void testGetMember() {
        Tests.testPTA(DIR, "GetMember", "plugins:[pascal.taie.analysis.pta.plugin.reflection.MyReflectionAnalysis]");
    }

}
