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
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;

public class ServiceMethodRegistrar extends AndroidMethodRegistrar {

    private static final List<String> METHOD_SUBSIGS = List.of(
        "void onCreate()",
        "void onStart(android.content.Intent,int)",
        "int onStartCommand(android.content.Intent,int,int)",
        "android.os.IBinder onBind(android.content.Intent)",
        "void onRebind(android.content.Intent)",
        "boolean onUnbind(android.content.Intent)",
        "void onDestroy()"
    );

    private static final List<String> GCMINTENTSERVICE_METHOD_SUBSIGS = List.of(
        "void onDeletedMessages(android.content.Context,int)",
        "void onError(android.content.Context,java.lang.String)",
        "void onMessage(android.content.Context,android.content.Intent)",
        "void onRecoverableError(android.content.Context,java.lang.String)",
        "void onRegistered(android.content.Context,java.lang.String)",
        "void onUnregistered(android.content.Context,java.lang.String)"
    );

    private static final List<String> GCMLISTENERSERVICE_METHOD_SUBSIGS = List.of(
        "void onDeletedMessages()",
        "void onMessageReceived(java.lang.String,android.os.Bundle)",
        "void onMessageSent(java.lang.String)",
        "void onSendError(java.lang.String,java.lang.String)"
    );

    private static final List<String> HOSTAPDUSERVICE_METHOD_SUBSIGS = List.of(
        "byte[] processCommandApdu(byte[],android.os.Bundle)",
        "void onDeactivated(int)"
    );

    private final JClass clazz;

    private final JClass gcmIntentServiceClass;
    private final JClass gcmListenerClass;
    private final JClass hostApduServiceClass;

    private final ClassHierarchy hierarchy;

    public ServiceMethodRegistrar(JClass clazz, Solver solver) {
        super(clazz, solver);
        this.clazz = clazz;
        this.hierarchy = solver.getHierarchy();
        this.gcmIntentServiceClass = this.hierarchy.getClass("com.google.android.gcm.GCMBaseIntentService");
        this.gcmListenerClass = this.hierarchy.getClass("com.google.android.gms.gcm.GcmListenerService");
        this.hostApduServiceClass = this.hierarchy.getClass("android.nfc.cardemulation.HostApduService");
    }

    public void registerAll() {
        super.registerAll(METHOD_SUBSIGS);
        if (hierarchy.isSubclass(gcmIntentServiceClass, clazz)) {
            super.registerAll(GCMINTENTSERVICE_METHOD_SUBSIGS);
        } else if (hierarchy.isSubclass(gcmListenerClass, clazz)) {
            super.registerAll(GCMLISTENERSERVICE_METHOD_SUBSIGS);
        } else if (hierarchy.isSubclass(hostApduServiceClass, clazz)) {
            super.registerAll(HOSTAPDUSERVICE_METHOD_SUBSIGS);
        }
    }
}