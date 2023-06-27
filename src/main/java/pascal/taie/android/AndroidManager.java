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

package pascal.taie.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;

public class AndroidManager {

    private static final Logger logger = LogManager.getLogger(AndroidManager.class);

    private static final AndroidManager theManager = new AndroidManager();

    private final List<String> activities = new ArrayList<>();
    private final List<String> providers = new ArrayList<>();
    private final List<String> receivers = new ArrayList<>();
    private final List<String> services = new ArrayList<>();

    private List<JClass> activityClasses = null;
    private List<JClass> providerClasses = null;
    private List<JClass> receiverClasses = null;
    private List<JClass> serviceClasses = null;

    private ClassHierarchy hierarchy;

    private void getHierarchy() {
        hierarchy = World.get().getClassHierarchy();
    }

    private AndroidManager() {}

    public static AndroidManager get() {
        return theManager;
    }

    public List<String> getActivityNames() {
        return activities;
    }
    public List<String> getProviderNames() {
        return providers;
    }
    public List<String> getReceiverNames() {
        return receivers;
    }
    public List<String> getServiceNames() {
        return services;
    }

    public List<JClass> getActivityClasses() {
        if (activityClasses == null) {
            getHierarchy();
            JClass androidActivity = hierarchy.getClass("android.app.Activity");
            activityClasses = activities.stream().map(hierarchy::getClass).filter(Objects::nonNull).filter(activity -> hierarchy.isSubclass(androidActivity, activity)).toList();
        }
        return activityClasses;
    }
    public List<JClass> getProviderClasses() {
        if (providerClasses == null) {
            getHierarchy();
            JClass androidContentProvider = hierarchy.getClass("android.content.ContentProvider");
            providerClasses = providers.stream().map(hierarchy::getClass).filter(Objects::nonNull).filter(provider -> hierarchy.isSubclass(androidContentProvider, provider)).toList();
        }
        return providerClasses;
    }
    public List<JClass> getReceiverClasses() {
        if (receiverClasses == null) {
            getHierarchy();
            JClass androidBroadcastReceiver = hierarchy.getClass("android.content.BroadcastReceiver");
            receiverClasses = receivers.stream().map(hierarchy::getClass).filter(Objects::nonNull).filter(receiver -> hierarchy.isSubclass(androidBroadcastReceiver, receiver)).toList();
        }
        return receiverClasses;
    }
    public List<JClass> getServiceClasses() {
        if (serviceClasses == null) {
            getHierarchy();
            JClass androidService = hierarchy.getClass("android.app.Service");
            serviceClasses = services.stream().map(hierarchy::getClass).filter(Objects::nonNull).filter(service -> hierarchy.isSubclass(androidService, service)).toList();
        }
        return serviceClasses;
    }

    public void onWorldBuild(Options options, List<AnalysisConfig> analyses) {
        // process program main method
        String manifestXmlPath =  options.getManifestXmlPath();
        if (manifestXmlPath != null) {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser parser = factory.newSAXParser();
                ManifestXmlHandler handler = new ManifestXmlHandler();
                parser.parse(manifestXmlPath, handler);
            } catch (IOException e) {
                logger.error("AndroidManifest.xml file not found");
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        List<String> inputClasses = new ArrayList<>();
        inputClasses.addAll(options.getInputClasses());
        inputClasses.addAll(activities);
        inputClasses.addAll(providers);
        inputClasses.addAll(receivers);
        options.setInputClasses(inputClasses);
    }

    private class ManifestXmlHandler extends DefaultHandler {

        private boolean inApp = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName == "application") {
                inApp = true;
            }
            if (inApp) {
                switch (qName) {
                    case "activity":
                        String activityName = attributes.getValue("", "android:name");
                        activities.add(activityName);
                        break;
                    case "provider":
                        String providerName = attributes.getValue("", "android:name");
                        providers.add(providerName);
                        break;
                    case "receiver":
                        String receiverName = attributes.getValue("", "android:name");
                        receivers.add(receiverName);
                        break;
                    case "service":
                        String serviceName = attributes.getValue("", "android:name");
                        services.add(serviceName);
                        break;
                }
            }
            super.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName == "application") {
                inApp = false;
            }
        }
    }
}
