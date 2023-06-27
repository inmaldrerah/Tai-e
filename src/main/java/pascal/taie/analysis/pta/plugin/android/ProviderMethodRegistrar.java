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

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.JClass;

public class ProviderMethodRegistrar extends AndroidMethodRegistrar {

    private static final List<String> METHOD_SUBSIGS = List.of(
        "boolean onCreate()",
        "android.net.Uri insert(android.net.Uri,android.content.ContentValues)",
        "android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)",
        "int update(android.net.Uri,android.content.ContentValues,java.lang.String,java.lang.String[])",
        "int delete(android.net.Uri,java.lang.String,java.lang.String[])",
        "java.lang.String getType(android.net.Uri)"
    );

    public ProviderMethodRegistrar(JClass clazz, Solver solver) {
        super(clazz, solver);
    }

    public void registerAll() {
        super.registerAll(METHOD_SUBSIGS);
    }
}