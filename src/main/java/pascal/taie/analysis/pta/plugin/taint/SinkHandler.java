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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles sinks in taint analysis.
 */
class SinkHandler extends Handler {

    private final List<Sink> sinks;

    SinkHandler(HandlerContext context) {
        super(context);
        sinks = context.config().sinks();
    }

    Set<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        Set<TaintFlow> taintFlows = Sets.newOrderedSet();
        sinks.forEach(sink -> {
            if (sink instanceof ParamSink paramSink) {
                int i = paramSink.index();
                result.getCallGraph()
                        .edgesInTo(paramSink.method())
                        // TODO: handle other call edges
                        .filter(e -> e.getKind() != CallKind.OTHER)
                        .map(Edge::getCallSite)
                        .forEach(sinkCall -> {
                            Var arg = InvokeUtils.getVar(sinkCall, i);
                            SinkPoint sinkPoint = new SinkPoint(sinkCall, i);
                            result.getPointsToSet(arg)
                                    .stream()
                                    .filter(manager::isTaint)
                                    .map(manager::getSourcePoint)
                                    .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                                    .forEach(taintFlows::add);
                        });
            } else if (sink instanceof ReturnSink returnSink) {
                // TODO: find a way to get/generate sinkPoint for entry methods
                result.getCallGraph()
                        .edgesInTo(returnSink.method())
                        // TODO: handle other call edges
                        .filter(e -> e.getKind() != CallKind.OTHER)
                        .map(Edge::getCallSite)
                        .forEach(sinkCall -> {
                            SinkPoint sinkPoint = new SinkPoint(sinkCall, -1);
                            returnSink.method()
                                    .getIR()
                                    .getReturnVars()
                                    .stream()
                                    .flatMap(ret -> result.getPointsToSet(ret).stream())
                                    .filter(manager::isTaint)
                                    .map(manager::getSourcePoint)
                                    .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                                    .forEach(taintFlows::add);
                        });
            }
        });
        if (callSiteMode) {
            Map<JMethod, ParamSink> sinkMap = sinks.stream()
                    .filter(s -> s instanceof ParamSink)
                    .map(s -> (ParamSink) s)
                    .collect(Collectors.toMap(ParamSink::method, s -> s));
            // scan all reachable call sites to search sink calls
            result.getCallGraph()
                    .reachableMethods()
                    .filter(m -> !m.isAbstract())
                    .flatMap(m -> m.getIR().invokes(false))
                    .forEach(callSite -> {
                        JMethod callee = callSite.getMethodRef().resolveNullable();
                        ParamSink sink = sinkMap.get(callee);
                        if (sink != null) {
                            int i = sink.index();
                            Var arg = InvokeUtils.getVar(callSite, i);
                            SinkPoint sinkPoint = new SinkPoint(callSite, i);
                            result.getPointsToSet(arg)
                                    .stream()
                                    .filter(manager::isTaint)
                                    .map(manager::getSourcePoint)
                                    .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                                    .forEach(taintFlows::add);
                        }
                    });
        }
        return taintFlows;
    }
}
