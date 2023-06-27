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

package pascal.taie.analysis.pta.plugin.android;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

public class AndroidMethodRegistrar {

    private final JClass clazz;

    private final Solver solver;

    private final Set<Obj> thisObjs;

    public AndroidMethodRegistrar(JClass clazz, Solver solver) {
        this.clazz = clazz;
        this.solver = solver;
        this.thisObjs = Set.of(solver.getHeapModel().getMockObj(Descriptor.ENTRY_DESC, "<synthesized>", clazz.getType()));
    }

    protected void registerAll(List<String> subsigs) {
        JMethod clinit = getMethod(Subsignature.get(Subsignature.CLINIT));
        if (clinit != null) {
            solver.addEntryPoint(new EntryPoint(clinit,
                    new DefaultParamProvider(clinit)));
        }
        List<JMethod> inits = getInits();
        inits.stream()
                .forEach(init -> solver.addEntryPoint(new EntryPoint(init,
                        new DefaultParamProvider(init))));
        subsigs.stream()
                .map(subsig -> getMethod(Subsignature.get(subsig)))
                .filter(method -> method != null)
                .forEach(method -> solver.addEntryPoint(new EntryPoint(method,
                        new DefaultParamProvider(method))));
    }

    private JMethod getMethod(Subsignature subsig) {
        JClass clazz = this.clazz;
        JMethod method = clazz.getDeclaredMethod(subsig);
        while (clazz != null && method == null) {
            clazz = clazz.getSuperClass();
            method = Optional.ofNullable(clazz).map(c -> c.getDeclaredMethod(subsig)).orElse(null);
        }
        return method;
    }

    private List<JMethod> getInits() {
        return this.clazz.getDeclaredMethods().stream().filter(method -> method.getName().equals("<init>")).toList();
    }

    private class DefaultParamProvider implements ParamProvider {

        private final JMethod method;

        DefaultParamProvider(JMethod method) {
            this.method = method;
        }

        @Override
        public Set<Obj> getThisObjs() {
            return thisObjs;
        }

        @Override
        public Set<Obj> getParamObjs(int i) {
            assert i < method.getParamCount();
            return Set.of();
        }
    }
}