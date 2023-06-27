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

package pascal.taie.analysis.pta.plugin.util;

import java.util.List;

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

/**
 * Provides utility methods to conveniently handle {@link Invoke}.
 */
public final class InvokeUtils {

    /**
     * Special number representing the base variable of an invocation.
     */
    public static final int BASE = -1;

    /**
     * String representation of base variable.
     */
    private static final String BASE_STR = "base";

    /**
     * Special number representing the variable that receivers
     * the result of the invocation.
     */
    public static final int RESULT = -2;

    /**
     * String representation of result variable.
     */
    private static final String RESULT_STR = "result";

    public static final int ALL = -3;

    private static final String ALL_STR = "all";

    private InvokeUtils() {
    }

    /**
     * Coverts string to index.
     */
    public static int toInt(String s) {
        return switch (s.toLowerCase()) {
            case BASE_STR -> BASE;
            case RESULT_STR -> RESULT;
            case ALL_STR -> ALL;
            default -> Integer.parseInt(s);
        };
    }

    /**
     * Converts index to string.
     */
    public static String toString(int index) {
        return switch (index) {
            case BASE -> BASE_STR;
            case RESULT -> RESULT_STR;
            case ALL -> ALL_STR;
            default -> Integer.toString(index);
        };
    }

    /**
     * Retrieves variable from a call site and index.
     */
    public static Var getVar(Invoke callSite, int index) {
        InvokeExp invokeExp = callSite.getInvokeExp();
        return switch (index) {
            case BASE -> ((InvokeInstanceExp) invokeExp).getBase();
            case RESULT -> callSite.getResult();
            case ALL -> null;
            default -> invokeExp.getArg(index);
        };
    }

    /**
     * Retrieves a variable list from a call site and index.
     */
    public static List<Var> getVars(Invoke callSite, int index) {
        InvokeExp invokeExp = callSite.getInvokeExp();
        return switch (index) {
            case BASE -> toVarList(((InvokeInstanceExp) invokeExp).getBase());
            case RESULT -> toVarList(callSite.getResult());
            case ALL -> invokeExp.getArgs().parallelStream().filter(var -> var != null).toList();
            default -> toVarList(invokeExp.getArg(index));
        };
    }

    private static List<Var> toVarList(Var var) {
        if (var != null) {
            return List.of(var);
        } else {
            return List.of();
        }
    }
}
