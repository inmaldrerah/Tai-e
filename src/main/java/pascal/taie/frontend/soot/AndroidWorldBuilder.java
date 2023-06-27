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

package pascal.taie.frontend.soot;

import pascal.taie.android.AndroidManager;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AndroidWorldBuilder extends SootWorldBuilder {

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        AndroidManager.get().onWorldBuild(options, analyses);
        if (!options.isAndroidAnalysis()) {
            super.build(options, analyses);
            return;
        }
        initSoot(options, analyses, this);
        // set arguments and run soot
        List<String> args = new ArrayList<>();
        // set class path
        Collections.addAll(args, "-cp", getClassPath(options));
        // set android-jars path
        Collections.addAll(args, "--android-jars", options.getAndroidJarsPath());
        // set apk
        Collections.addAll(args, "--process-dir", options.getApkPath());
        // add input classes
        args.addAll(getInputClasses(options));
        runSoot(args.toArray(new String[0]));
    }

    protected static void initSoot(Options options, List<AnalysisConfig> analyses,
                                AndroidWorldBuilder builder) {
        // reset Soot
        G.reset();

        // set Soot options
        soot.options.Options.v().set_output_dir(
                new File(options.getOutputDir(), "sootOutput").toString());
        soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk);
        soot.options.Options.v().set_process_multiple_dex(true);
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_app(true);
        // exclude jdk classes from application classes
        soot.options.Options.v().set_exclude(List.of("jdk.*", "apple.laf.*"));
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_no_writeout_body_releasing(true);
        soot.options.Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        soot.options.Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");
        // --prepend-classpath to include android components in output
        soot.options.Options.v().set_prepend_classpath(true);
        if (options.isAllowPhantom()) {
            soot.options.Options.v().set_allow_phantom_refs(true);
        }
        if (options.isPreBuildIR()) {
            // we need to set this option to false when pre-building IRs,
            // otherwise Soot throws RuntimeException saying
            // "No method source set for method ...".
            // TODO: figure out the reason of "No method source"
            soot.options.Options.v().set_drop_bodies_after_load(false);
        }

        Scene scene = G.v().soot_Scene();
        addBasicClasses(scene);
        addReflectionLogClasses(analyses, scene);

        // Configure Soot transformer
        Transform transform = new Transform(
                "wjtp.tai-e", new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> opts) {
                builder.build(options, Scene.v());
            }
        });
        PackManager.v()
                .getPack("wjtp")
                .add(transform);
    }
}
