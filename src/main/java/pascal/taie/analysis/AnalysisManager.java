/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;
import pascal.taie.config.Scope;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Timer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * Creates and executes analyses based on given analysis plan.
 */
public class AnalysisManager {

    private static final Logger logger = LogManager.getLogger(AnalysisManager.class);

    private List<JMethod> scope;

    /**
     * Executes the analysis plan.
     */
    public void execute(List<AnalysisConfig> analysisPlan) {
        analysisPlan.forEach(config -> Timer.runAndCount(
                () -> runAnalysis(config), config.getId()));
    }

    private void runAnalysis(AnalysisConfig config) {
        try {
            // Create analysis instance
            Class<?> clazz = Class.forName(config.getAnalysisClass());
            Constructor<?> ctor = clazz.getConstructor(AnalysisConfig.class);
            Object analysis = ctor.newInstance(config);
            // Run the analysis
            if (analysis instanceof IntraproceduralAnalysis) {
                runIntraproceduralAnalysis((IntraproceduralAnalysis) analysis);
            } else if (analysis instanceof InterproceduralAnalysis) {
                runInterproceduralAnalysis((InterproceduralAnalysis) analysis);
            } else {
                logger.warn(clazz + " is not an analysis");
            }
        } catch (ClassNotFoundException | NoSuchMethodException |
                InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new AnalysisException("Failed to initialize " +
                    config.getAnalysisClass(), e);
        }
    }

    private void runIntraproceduralAnalysis(IntraproceduralAnalysis analysis) {
        getScope().parallelStream()
                .forEach(m -> {
                    IR ir = m.getIR();
                    Object result = analysis.analyze(ir);
                    if (result != null) {
                        ir.storeResult(analysis.getId(), result);
                    }
                });
    }

    private List<JMethod> getScope() {
        if (scope == null) {
            switch (World.getOptions().getScope()) {
                case Scope.APP -> {
                    scope = World.getClassHierarchy()
                            .applicationClasses()
                            .map(JClass::getDeclaredMethods)
                            .flatMap(Collection::stream)
                            .filter(m -> !m.isAbstract() && !m.isNative())
                            .toList();
                }
                case Scope.REACHABLE -> {
                    CallGraph<?, JMethod> callGraph = World.getResult(CallGraphBuilder.ID);
                    scope = callGraph.reachableMethods().toList();
                }
                case Scope.ALL -> {
                    scope = World.getClassHierarchy()
                            .allClasses()
                            .map(JClass::getDeclaredMethods)
                            .flatMap(Collection::stream)
                            .filter(m -> !m.isAbstract() && !m.isNative())
                            .toList();
                }
                default -> throw new ConfigException(
                        "Unexpected scope option: " + World.getOptions().getScope());
            }
            logger.info("{} methods in scope ({}) of intra-procedural analysis",
                    scope.size(), World.getOptions().getScope());
        }
        return scope;
    }

    private void runInterproceduralAnalysis(InterproceduralAnalysis analysis) {
        Object result = analysis.analyze();
        if (result != null) {
            World.storeResult(analysis.getId(), result);
        }
    }
}
