/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.java.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;

class StandardBeanInfo extends SimpleBeanInfo {

    // Prefixes for methods that set or get a Property
    private static final String PREFIX_IS = "is"; //$NON-NLS-1$

    private static final String PREFIX_GET = "get"; //$NON-NLS-1$

    private static final String PREFIX_SET = "set"; //$NON-NLS-1$

    // Prefix and suffix for Event related methods
    private static final String PREFIX_ADD = "add"; //$NON-NLS-1$

    private static final String PREFIX_REMOVE = "remove"; //$NON-NLS-1$

    private static final String SUFFIX_LISTEN = "Listener"; //$NON-NLS-1$

    private static final String STR_NORMAL = "normal"; //$NON-NLS-1$

    private static final String STR_INDEXED = "indexed"; //$NON-NLS-1$

    private static final String STR_VALID = "valid"; //$NON-NLS-1$

    private static final String STR_INVALID = "invalid"; //$NON-NLS-1$

    private static final String STR_PROPERTY_TYPE = "PropertyType"; //$NON-NLS-1$

    private static final String STR_IS_CONSTRAINED = "isConstrained"; //$NON-NLS-1$

    private static final String STR_SETTERS = "setters"; //$NON-NLS-1$

    private static final String STR_GETTERS = "getters"; //$NON-NLS-1$

    private boolean explicitMethods = false;

    private boolean explicitProperties = false;

    private boolean explicitEvents = false;

    private org.java.beans.BeanInfo explicitBeanInfo = null;

    private org.java.beans.EventSetDescriptor[] events = null;

    private org.java.beans.MethodDescriptor[] methods = null;

    private org.java.beans.PropertyDescriptor[] properties = null;

    private org.java.beans.BeanDescriptor beanDescriptor = null;

    org.java.beans.BeanInfo[] additionalBeanInfo = null;

    private Class<?> beanClass;

    private int defaultEventIndex = -1;

    private int defaultPropertyIndex = -1;

    private static PropertyComparator comparator = new PropertyComparator();

    private Object[] icon = new Object[4];

    private boolean canAddPropertyChangeListener;

    private boolean canRemovePropertyChangeListener;

    StandardBeanInfo(Class<?> beanClass, org.java.beans.BeanInfo explicitBeanInfo, Class<?> stopClass)
            throws org.java.beans.IntrospectionException {
        this.beanClass = beanClass;
        /*--------------------------------------------------------------------------------------
         * There are 3 aspects of BeanInfo that must be supplied:
         * a) PropertyDescriptors
         * b) MethodDescriptors
         * c) EventSetDescriptors
         * Each of these may be optionally provided in the explicitBeanInfo object relating to
         * this bean.  Where the explicitBeanInfo provides one of these aspects, it is used
         * without question and no introspection of the beanClass is performed for that aspect.
         * There are also 3 optional items of BeanInfo that may be provided by the 
         * explicitBeanInfo object:
         * 1) BeanDescriptor
         * 2) DefaultEventIndex
         * 3) DefaultPropertyIndex
         * These aspects of the beanClass cannot be derived through introspection of the class.
         * If they are not provided by the explicitBeanInfo, then they must be left null in the 
         * returned BeanInfo, otherwise they will be copied from the explicitBeanInfo 
         --------------------------------------------------------------------------------------*/
        if (explicitBeanInfo != null) {
            this.explicitBeanInfo = explicitBeanInfo;
            events = explicitBeanInfo.getEventSetDescriptors();
            methods = explicitBeanInfo.getMethodDescriptors();
            properties = explicitBeanInfo.getPropertyDescriptors();
            defaultEventIndex = explicitBeanInfo.getDefaultEventIndex();
            if (defaultEventIndex < 0 || defaultEventIndex >= events.length) {
                defaultEventIndex = -1;
            }
            defaultPropertyIndex = explicitBeanInfo.getDefaultPropertyIndex();
            if (defaultPropertyIndex < 0
                    || defaultPropertyIndex >= properties.length) {
                defaultPropertyIndex = -1;
            }
            additionalBeanInfo = explicitBeanInfo.getAdditionalBeanInfo();

            if (events != null)
                explicitEvents = true;
            if (methods != null)
                explicitMethods = true;
            if (properties != null)
                explicitProperties = true;
        }

        if (methods == null) {
            methods = introspectMethods();
        }

        if (properties == null) {
            properties = introspectProperties(stopClass);
        }

        if (events == null) {
            events = introspectEvents();
        }
    }

    @Override
    public org.java.beans.BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }

    @Override
    public org.java.beans.EventSetDescriptor[] getEventSetDescriptors() {
        return events;
    }

    @Override
    public org.java.beans.MethodDescriptor[] getMethodDescriptors() {
        return methods;
    }

    @Override
    public org.java.beans.PropertyDescriptor[] getPropertyDescriptors() {
        return properties;
    }

    @Override
    public org.java.beans.BeanDescriptor getBeanDescriptor() {
        if (beanDescriptor == null) {
            if (explicitBeanInfo != null) {
                beanDescriptor = explicitBeanInfo.getBeanDescriptor();
            }
            if (beanDescriptor == null) {
                beanDescriptor = new BeanDescriptor(beanClass);
            }
        }
        return beanDescriptor;
    }

    @Override
    public int getDefaultEventIndex() {
        return this.defaultEventIndex;
    }

    @Override
    public int getDefaultPropertyIndex() {
        return this.defaultPropertyIndex;
    }

    void mergeBeanInfo(BeanInfo beanInfo, boolean force)
            throws org.java.beans.IntrospectionException {
        if (force || !explicitProperties) {
            org.java.beans.PropertyDescriptor[] superDescs = beanInfo.getPropertyDescriptors();
            if (superDescs != null) {
                if (getPropertyDescriptors() != null) {
                    properties = mergeProps(superDescs, beanInfo
                            .getDefaultPropertyIndex());
                } else {
                    properties = superDescs;
                    defaultPropertyIndex = beanInfo.getDefaultPropertyIndex();
                }
            }
        }

        if (force || !explicitMethods) {
            org.java.beans.MethodDescriptor[] superMethods = beanInfo.getMethodDescriptors();
            if (superMethods != null) {
                if (methods != null) {
                    methods = mergeMethods(superMethods);
                } else {
                    methods = superMethods;
                }
            }
        }

        if (force || !explicitEvents) {
            org.java.beans.EventSetDescriptor[] superEvents = beanInfo
                    .getEventSetDescriptors();
            if (superEvents != null) {
                if (events != null) {
                    events = mergeEvents(superEvents, beanInfo
                            .getDefaultEventIndex());
                } else {
                    events = superEvents;
                    defaultEventIndex = beanInfo.getDefaultEventIndex();
                }
            }
        }
    }

    /*
     * merge the PropertyDescriptor with superclass
     */
    private org.java.beans.PropertyDescriptor[] mergeProps(org.java.beans.PropertyDescriptor[] superDescs,
                                                           int superDefaultIndex) throws org.java.beans.IntrospectionException {
        // FIXME:change to OO way as EventSetD and MethodD
        HashMap<String, org.java.beans.PropertyDescriptor> subMap = internalAsMap(properties);
        String defaultPropertyName = null;
        if (defaultPropertyIndex >= 0
                && defaultPropertyIndex < properties.length) {
            defaultPropertyName = properties[defaultPropertyIndex].getName();
        } else if (superDefaultIndex >= 0
                && superDefaultIndex < superDescs.length) {
            defaultPropertyName = superDescs[superDefaultIndex].getName();
        }

        for (int i = 0; i < superDescs.length; i++) {
            org.java.beans.PropertyDescriptor superDesc = superDescs[i];
            String propertyName = superDesc.getName();
            if (!subMap.containsKey(propertyName)) {
                subMap.put(propertyName, superDesc);
                continue;
            }

            Object value = subMap.get(propertyName);
            // if sub and super are both PropertyDescriptor
            Method subGet = ((org.java.beans.PropertyDescriptor) value).getReadMethod();
            Method subSet = ((org.java.beans.PropertyDescriptor) value).getWriteMethod();
            Method superGet = superDesc.getReadMethod();
            Method superSet = superDesc.getWriteMethod();

            Class<?> superType = superDesc.getPropertyType();
            Class<?> superIndexedType = null;
            Class<?> subType = ((org.java.beans.PropertyDescriptor) value).getPropertyType();
            Class<?> subIndexedType = null;

            if (value instanceof org.java.beans.IndexedPropertyDescriptor) {
                subIndexedType = ((org.java.beans.IndexedPropertyDescriptor) value)
                        .getIndexedPropertyType();
            }
            if (superDesc instanceof org.java.beans.IndexedPropertyDescriptor) {
                superIndexedType = ((org.java.beans.IndexedPropertyDescriptor) superDesc)
                        .getIndexedPropertyType();
            }

            // if superDesc is PropertyDescriptor
            if (superIndexedType == null) {
                org.java.beans.PropertyDescriptor subDesc = (org.java.beans.PropertyDescriptor) value;
                // Sub is PropertyDescriptor
                if (subIndexedType == null) {
                    // Same property type
                    if (subType != null && superType != null
                            && subType.getName() != null
                            && subType.getName().equals(superType.getName())) {
                        if (superGet != null
                                && (subGet == null || superGet.equals(subGet))) {
                            subDesc.setReadMethod(superGet);
                        }
                        if (superSet != null
                                && (subSet == null || superSet.equals(subSet))) {
                            subDesc.setWriteMethod(superSet);
                        }
                        if (subType == boolean.class && subGet != null
                                && superGet != null) {
                            if (superGet.getName().startsWith(PREFIX_IS)) {
                                subDesc.setReadMethod(superGet);
                            }
                        }
                    } else { // Different type
                        if ((subGet == null || subSet == null)
                                && (superGet != null)) {
                            subDesc = new org.java.beans.PropertyDescriptor(propertyName,
                                    superGet, superSet);
                            if (subGet != null) {
                                String subGetName = subGet.getName();
                                Method method = null;
                                org.java.beans.MethodDescriptor[] introspectMethods = introspectMethods();
                                for (org.java.beans.MethodDescriptor methodDesc : introspectMethods) {
                                    method = methodDesc.getMethod();
                                    if (method != subGet
                                            && subGetName.equals(method
                                                    .getName())
                                            && method.getParameterTypes().length == 0
                                            && method.getReturnType() == superType) {
                                        subDesc.setReadMethod(method);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else { // Sub is IndexedPropertyDescriptor and super is PropertyDescriptor
                    if (superType != null
                            && (superType.isArray())
                            && (superType.getComponentType().getName()
                                    .equals(subIndexedType.getName()))) {
                        if ((subGet == null) && (superGet != null)) {
                            subDesc.setReadMethod(superGet);
                        }
                        if ((subSet == null) && (superSet != null)) {
                            subDesc.setWriteMethod(superSet);
                        }
                    } // different type do nothing
                    // sub is indexed pd and super is normal pd
                    if (subIndexedType == boolean.class
                            && superType == boolean.class) {
                        Method subIndexedSet = ((org.java.beans.IndexedPropertyDescriptor) subDesc)
                                .getIndexedWriteMethod();
                        if (subGet == null && subSet == null
                                && subIndexedSet != null && superGet != null) {
                            try {
                                subSet = beanClass.getDeclaredMethod(
                                        subIndexedSet.getName(), boolean.class);
                            } catch (Exception e) {
                                // ignored
                            }
                            if (subSet != null) {
                                // Cast sub into PropertyDescriptor
                                subDesc = new org.java.beans.PropertyDescriptor(propertyName,
                                        superGet, subSet);
                            }
                        }
                    }
                }
                subMap.put(propertyName, subDesc);
            } else { // Super is IndexedPropertyDescriptor
                if (subIndexedType == null) { // Sub is PropertyDescriptor
                    if (subType != null
                            && subType.isArray()
                            && (subType.getComponentType().getName()
                                    .equals(superIndexedType.getName()))) {
                        // Same type
                        if (subGet != null) {
                            superDesc.setReadMethod(subGet);
                        }
                        if (subSet != null) {
                            superDesc.setWriteMethod(subSet);
                        }
                        subMap.put(propertyName, superDesc);
                    } else {
                        // subDesc is PropertyDescriptor
                        // superDesc is IndexedPropertyDescriptor

                        // fill null subGet or subSet method with superClass's
                        if (subGet == null || subSet == null) {
                            Class<?> beanSuperClass = beanClass.getSuperclass();
                            String methodSuffix = capitalize(propertyName);
                            Method method = null;
                            if (subGet == null) {
                                // subGet is null
                                if (subType == boolean.class) {
                                    try {
                                        method = beanSuperClass
                                                .getDeclaredMethod(PREFIX_IS
                                                        + methodSuffix);
                                    } catch (Exception e) {
                                        // ignored
                                    }
                                } else {
                                    try {
                                        method = beanSuperClass
                                                .getDeclaredMethod(PREFIX_GET
                                                        + methodSuffix);
                                    } catch (Exception e) {
                                        // ignored
                                    }
                                }
                                if (method != null
                                        && !Modifier.isStatic(method
                                                .getModifiers())
                                        && method.getReturnType() == subType) {
                                    ((org.java.beans.PropertyDescriptor) value)
                                            .setReadMethod(method);
                                }
                            } else {
                                // subSet is null
                                try {
                                    method = beanSuperClass.getDeclaredMethod(
                                            PREFIX_SET + methodSuffix, subType);
                                } catch (Exception e) {
                                    // ignored
                                }
                                if (method != null
                                        && !Modifier.isStatic(method
                                                .getModifiers())
                                        && method.getReturnType() == void.class) {
                                    ((org.java.beans.PropertyDescriptor) value)
                                            .setWriteMethod(method);
                                }
                            }
                        }
                        subMap.put(propertyName, (org.java.beans.PropertyDescriptor) value);
                    }
                } else if (subIndexedType.getName().equals(
                        superIndexedType.getName())) {
                    // Sub is IndexedPropertyDescriptor and Same type
                    org.java.beans.IndexedPropertyDescriptor subDesc = (org.java.beans.IndexedPropertyDescriptor) value;
                    if ((subGet == null) && (superGet != null)) {
                        subDesc.setReadMethod(superGet);
                    }
                    if ((subSet == null) && (superSet != null)) {
                        subDesc.setWriteMethod(superSet);
                    }
                    org.java.beans.IndexedPropertyDescriptor superIndexedDesc = (org.java.beans.IndexedPropertyDescriptor) superDesc;

                    if ((subDesc.getIndexedReadMethod() == null)
                            && (superIndexedDesc.getIndexedReadMethod() != null)) {
                        subDesc.setIndexedReadMethod(superIndexedDesc
                                .getIndexedReadMethod());
                    }

                    if ((subDesc.getIndexedWriteMethod() == null)
                            && (superIndexedDesc.getIndexedWriteMethod() != null)) {
                        subDesc.setIndexedWriteMethod(superIndexedDesc
                                .getIndexedWriteMethod());
                    }

                    subMap.put(propertyName, subDesc);
                } // Different indexed type, do nothing
            }
            mergeAttributes((org.java.beans.PropertyDescriptor) value, superDesc);
        }

        org.java.beans.PropertyDescriptor[] theDescs = new org.java.beans.PropertyDescriptor[subMap.size()];
        subMap.values().toArray(theDescs);

        if (defaultPropertyName != null && !explicitProperties) {
            for (int i = 0; i < theDescs.length; i++) {
                if (defaultPropertyName.equals(theDescs[i].getName())) {
                    defaultPropertyIndex = i;
                    break;
                }
            }
        }
        return theDescs;
    }

    private String capitalize(String name) {
        if (name == null) {
            return null;
        }
        // The rule for decapitalize is that:
        // If the first letter of the string is Upper Case, make it lower case
        // UNLESS the second letter of the string is also Upper Case, in which case no
        // changes are made.
        if (name.length() == 0 || (name.length() > 1 && Character.isUpperCase(name.charAt(1)))) {
            return name;
        }
        
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    private static void mergeAttributes(org.java.beans.PropertyDescriptor subDesc,
                                        org.java.beans.PropertyDescriptor superDesc) {
        // FIXME: this is just temp workaround, need more elegant solution to
        // handle this
        subDesc.hidden |= superDesc.hidden;
        subDesc.expert |= superDesc.expert;
        subDesc.preferred |= superDesc.preferred;
        subDesc.bound |= superDesc.bound;
        subDesc.constrained |= superDesc.constrained;
        subDesc.name = superDesc.name;
        if (subDesc.shortDescription == null
                && superDesc.shortDescription != null) {
            subDesc.shortDescription = superDesc.shortDescription;
        }
        if (subDesc.displayName == null && superDesc.displayName != null) {
            subDesc.displayName = superDesc.displayName;
        }
    }

    /*
     * merge the MethodDescriptor
     */
    private org.java.beans.MethodDescriptor[] mergeMethods(org.java.beans.MethodDescriptor[] superDescs) {
        HashMap<String, org.java.beans.MethodDescriptor> subMap = internalAsMap(methods);

        for (org.java.beans.MethodDescriptor superMethod : superDescs) {
            String methodName = getQualifiedName(superMethod.getMethod());
            org.java.beans.MethodDescriptor method = subMap.get(methodName);
            if (method == null) {
                subMap.put(methodName, superMethod);
            } else {
                method.merge(superMethod);
            }
        }
        org.java.beans.MethodDescriptor[] theMethods = new org.java.beans.MethodDescriptor[subMap.size()];
        subMap.values().toArray(theMethods);
        return theMethods;
    }

    private org.java.beans.EventSetDescriptor[] mergeEvents(org.java.beans.EventSetDescriptor[] otherEvents,
                                                            int otherDefaultIndex) {
        HashMap<String, org.java.beans.EventSetDescriptor> subMap = internalAsMap(events);
        String defaultEventName = null;
        if (defaultEventIndex >= 0 && defaultEventIndex < events.length) {
            defaultEventName = events[defaultEventIndex].getName();
        } else if (otherDefaultIndex >= 0
                && otherDefaultIndex < otherEvents.length) {
            defaultEventName = otherEvents[otherDefaultIndex].getName();
        }

        for (org.java.beans.EventSetDescriptor event : otherEvents) {
            String eventName = event.getName();
            org.java.beans.EventSetDescriptor subEvent = subMap.get(eventName);
            if (subEvent == null) {
                subMap.put(eventName, event);
            } else {
                subEvent.merge(event);
            }
        }

        org.java.beans.EventSetDescriptor[] theEvents = new org.java.beans.EventSetDescriptor[subMap.size()];
        subMap.values().toArray(theEvents);

        if (defaultEventName != null && !explicitEvents) {
            for (int i = 0; i < theEvents.length; i++) {
                if (defaultEventName.equals(theEvents[i].getName())) {
                    defaultEventIndex = i;
                    break;
                }
            }
        }
        return theEvents;
    }

    private static HashMap<String, org.java.beans.PropertyDescriptor> internalAsMap(
            org.java.beans.PropertyDescriptor[] propertyDescs) {
        HashMap<String, org.java.beans.PropertyDescriptor> map = new HashMap<String, org.java.beans.PropertyDescriptor>();
        for (int i = 0; i < propertyDescs.length; i++) {
            map.put(propertyDescs[i].getName(), propertyDescs[i]);
        }
        return map;
    }

    private static HashMap<String, org.java.beans.MethodDescriptor> internalAsMap(
            org.java.beans.MethodDescriptor[] theDescs) {
        HashMap<String, org.java.beans.MethodDescriptor> map = new HashMap<String, org.java.beans.MethodDescriptor>();
        for (int i = 0; i < theDescs.length; i++) {
            String qualifiedName = getQualifiedName(theDescs[i].getMethod());
            map.put(qualifiedName, theDescs[i]);
        }
        return map;
    }

    private static HashMap<String, org.java.beans.EventSetDescriptor> internalAsMap(
            org.java.beans.EventSetDescriptor[] theDescs) {
        HashMap<String, org.java.beans.EventSetDescriptor> map = new HashMap<String, org.java.beans.EventSetDescriptor>();
        for (int i = 0; i < theDescs.length; i++) {
            map.put(theDescs[i].getName(), theDescs[i]);
        }
        return map;
    }

    private static String getQualifiedName(Method method) {
        String qualifiedName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes != null) {
            for (int i = 0; i < paramTypes.length; i++) {
                qualifiedName += "_" + paramTypes[i].getName(); //$NON-NLS-1$
            }
        }
        return qualifiedName;
    }

    /**
     * Introspects the supplied class and returns a list of the public methods
     * of the class
     * 
     * @return An array of MethodDescriptors with the public methods. null if
     *         there are no public methods
     */
    private org.java.beans.MethodDescriptor[] introspectMethods() {
        return introspectMethods(false, beanClass);
    }

    private org.java.beans.MethodDescriptor[] introspectMethods(boolean includeSuper) {
        return introspectMethods(includeSuper, beanClass);
    }

    private org.java.beans.MethodDescriptor[] introspectMethods(boolean includeSuper,
                                                                Class<?> introspectorClass) {

        // Get the list of methods belonging to this class
        Method[] basicMethods = includeSuper ? introspectorClass.getMethods()
                : introspectorClass.getDeclaredMethods();

        if (basicMethods == null || basicMethods.length == 0)
            return null;

        ArrayList<org.java.beans.MethodDescriptor> methodList = new ArrayList<org.java.beans.MethodDescriptor>(
                basicMethods.length);

        // Loop over the methods found, looking for public non-static methods
        for (int i = 0; i < basicMethods.length; i++) {
            int modifiers = basicMethods[i].getModifiers();
            if (Modifier.isPublic(modifiers)) {
                // Allocate a MethodDescriptor for this method
                org.java.beans.MethodDescriptor theDescriptor = new org.java.beans.MethodDescriptor(
                        basicMethods[i]);
                methodList.add(theDescriptor);
            }
        }

        // Get the list of public methods into the returned array
        int methodCount = methodList.size();
        org.java.beans.MethodDescriptor[] theMethods = null;
        if (methodCount > 0) {
            theMethods = new org.java.beans.MethodDescriptor[methodCount];
            theMethods = methodList.toArray(theMethods);
        }

        return theMethods;
    }

    /**
     * Introspects the supplied class and returns a list of the Properties of
     * the class
     * 
     * @param stopClass -
     *            the to introspecting at
     * @return The list of Properties as an array of PropertyDescriptors
     * @throws org.java.beans.IntrospectionException
     */
    @SuppressWarnings("unchecked")
    private org.java.beans.PropertyDescriptor[] introspectProperties(Class<?> stopClass)
            throws org.java.beans.IntrospectionException {

        // Get descriptors for the public methods
        org.java.beans.MethodDescriptor[] methodDescriptors = introspectMethods();

        if (methodDescriptors == null) {
            return null;
        }

        ArrayList<org.java.beans.MethodDescriptor> methodList = new ArrayList<org.java.beans.MethodDescriptor>();
        // Loop over the methods found, looking for public non-static methods
        for (int index = 0; index < methodDescriptors.length; index++) {
            int modifiers = methodDescriptors[index].getMethod().getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                methodList.add(methodDescriptors[index]);
            }
        }

        // Get the list of public non-static methods into an array
        int methodCount = methodList.size();
        org.java.beans.MethodDescriptor[] theMethods = null;
        if (methodCount > 0) {
            theMethods = new org.java.beans.MethodDescriptor[methodCount];
            theMethods = methodList.toArray(theMethods);
        }

        if (theMethods == null) {
            return null;
        }

        HashMap<String, HashMap> propertyTable = new HashMap<String, HashMap>(
                theMethods.length);

        // Search for methods that either get or set a Property
        for (int i = 0; i < theMethods.length; i++) {
            introspectGet(theMethods[i].getMethod(), propertyTable);
            introspectSet(theMethods[i].getMethod(), propertyTable);
        }

        // fix possible getter & setter collisions
        fixGetSet(propertyTable);

        // If there are listener methods, should be bound.
        org.java.beans.MethodDescriptor[] allMethods = introspectMethods(true);
        if (stopClass != null) {
            org.java.beans.MethodDescriptor[] excludeMethods = introspectMethods(true,
                    stopClass);
            if (excludeMethods != null) {
                ArrayList<org.java.beans.MethodDescriptor> tempMethods = new ArrayList<org.java.beans.MethodDescriptor>();
                for (org.java.beans.MethodDescriptor method : allMethods) {
                    if (!isInSuper(method, excludeMethods)) {
                        tempMethods.add(method);
                    }
                }
                allMethods = tempMethods
                        .toArray(new org.java.beans.MethodDescriptor[0]);
            }
        }
        for (int i = 0; i < allMethods.length; i++) {
            introspectPropertyListener(allMethods[i].getMethod());
        }
        // Put the properties found into the PropertyDescriptor array
        ArrayList<org.java.beans.PropertyDescriptor> propertyList = new ArrayList<org.java.beans.PropertyDescriptor>();

        for (Map.Entry<String, HashMap> entry : propertyTable.entrySet()) {
            String propertyName = entry.getKey();
            HashMap table = entry.getValue();
            if (table == null) {
                continue;
            }
            String normalTag = (String) table.get(STR_NORMAL);
            String indexedTag = (String) table.get(STR_INDEXED);

            if ((normalTag == null) && (indexedTag == null)) {
                continue;
            }

            Method get = (Method) table.get(STR_NORMAL + PREFIX_GET);
            Method set = (Method) table.get(STR_NORMAL + PREFIX_SET);
            Method indexedGet = (Method) table.get(STR_INDEXED + PREFIX_GET);
            Method indexedSet = (Method) table.get(STR_INDEXED + PREFIX_SET);

            org.java.beans.PropertyDescriptor propertyDesc = null;
            if (indexedTag == null) {
                propertyDesc = new org.java.beans.PropertyDescriptor(propertyName, get, set);
            } else {
                try {
                    propertyDesc = new org.java.beans.IndexedPropertyDescriptor(propertyName,
                            get, set, indexedGet, indexedSet);
                } catch (org.java.beans.IntrospectionException e) {
                    // If the getter and the indexGetter is not compatible, try
                    // getter/setter is null;
                    propertyDesc = new IndexedPropertyDescriptor(propertyName,
                            null, null, indexedGet, indexedSet);
                }
            }
            // RI set propretyDescriptor as bound. FIXME
            // propertyDesc.setBound(true);
            if (canAddPropertyChangeListener && canRemovePropertyChangeListener) {
                propertyDesc.setBound(true);
            } else {
                propertyDesc.setBound(false);
            }
            if (table.get(STR_IS_CONSTRAINED) == Boolean.TRUE) { //$NON-NLS-1$
                propertyDesc.setConstrained(true);
            }
            propertyList.add(propertyDesc);
        }

        org.java.beans.PropertyDescriptor[] theProperties = new org.java.beans.PropertyDescriptor[propertyList
                .size()];
        propertyList.toArray(theProperties);
        return theProperties;
    }

    private boolean isInSuper(org.java.beans.MethodDescriptor method,
                              org.java.beans.MethodDescriptor[] excludeMethods) {
        for (org.java.beans.MethodDescriptor m : excludeMethods) {
            if (method.getMethod().equals(m.getMethod())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private void introspectPropertyListener(Method theMethod) {
        String methodName = theMethod.getName();
        Class<?>[] param = theMethod.getParameterTypes();
        if (param.length != 1) {
            return;
        }
        if (methodName.equals("addPropertyChangeListener")
                && param[0].equals(org.java.beans.PropertyChangeListener.class))
            canAddPropertyChangeListener = true;
        if (methodName.equals("removePropertyChangeListener")
                && param[0].equals(PropertyChangeListener.class))
            canRemovePropertyChangeListener = true;
    }

    @SuppressWarnings("unchecked")
    private static void introspectGet(Method theMethod,
            HashMap<String, HashMap> propertyTable) {

        String methodName = theMethod.getName();
        int prefixLength = 0;
        String propertyName;
        Class propertyType;
        Class[] paramTypes;
        HashMap table;
        ArrayList<Method> getters;

        if (methodName == null) {
            return;
        }

        if (methodName.startsWith(PREFIX_GET)) {
            prefixLength = PREFIX_GET.length();
        }

        if (methodName.startsWith(PREFIX_IS)) {
            prefixLength = PREFIX_IS.length();
        }

        if (prefixLength == 0) {
            return;
        }

        propertyName = org.java.beans.Introspector.decapitalize(methodName.substring(prefixLength));

        // validate property name
        if (!isValidProperty(propertyName)) {
            return;
        }

        // validate return type
        propertyType = theMethod.getReturnType();

        if (propertyType == null || propertyType == void.class) {
            return;
        }

        // isXXX return boolean
        if (prefixLength == 2) {
            if (!(propertyType == boolean.class)) {
                return;
            }
        }

        // validate parameter types
        paramTypes = theMethod.getParameterTypes();
        if (paramTypes.length > 1
                || (paramTypes.length == 1 && paramTypes[0] != int.class)) {
            return;
        }

        table = propertyTable.get(propertyName);
        if (table == null) {
            table = new HashMap();
            propertyTable.put(propertyName, table);
        }

        getters = (ArrayList<Method>) table.get(STR_GETTERS);
        if (getters == null) {
            getters = new ArrayList<Method>();
            table.put(STR_GETTERS, getters);
        }

        // add current method as a valid getter
        getters.add(theMethod);
    }

    @SuppressWarnings("unchecked")
    private static void introspectSet(Method theMethod,
            HashMap<String, HashMap> propertyTable) {

        String methodName = theMethod.getName();
        if (methodName == null) {
            return;
        }
        String propertyName;
        Class returnType;
        Class[] paramTypes;

        // setter method should never return type other than void
        returnType = theMethod.getReturnType();
        if (returnType != void.class) {
            return;
        }

        if (methodName == null || !methodName.startsWith(PREFIX_SET)) {
            return;
        }

        propertyName = org.java.beans.Introspector.decapitalize(methodName.substring(PREFIX_SET.length()));

        // validate property name
        if (!isValidProperty(propertyName)) {
            return;
        }

        // It seems we do not need to validate return type

        // validate param types
        paramTypes = theMethod.getParameterTypes();

        if (paramTypes.length == 0 || paramTypes.length > 2
                || (paramTypes.length == 2 && paramTypes[0] != int.class)) {
            return;
        }

        HashMap table = propertyTable.get(propertyName);
        if (table == null) {
            table = new HashMap();
            propertyTable.put(propertyName, table);
        }

        ArrayList<Method> setters = (ArrayList<Method>) table.get(STR_SETTERS);
        if (setters == null) {
            setters = new ArrayList<Method>();
            table.put(STR_SETTERS, setters);
        }

        // handle constrained
        Class[] exceptions = theMethod.getExceptionTypes();
        for (Class e : exceptions) {
            if (e.equals(PropertyVetoException.class)) {
                table.put(STR_IS_CONSTRAINED, Boolean.TRUE); //$NON-NLS-1$
            }
        }

        // add new setter
        setters.add(theMethod);
    }

    /**
     * Checks and fixs all cases when several incompatible checkers / getters
     * were specified for single property.
     * 
     * @param propertyTable
     * @throws org.java.beans.IntrospectionException
     */
    private void fixGetSet(HashMap<String, HashMap> propertyTable)
            throws org.java.beans.IntrospectionException {

        if (propertyTable == null) {
            return;
        }

        for (Map.Entry<String, HashMap> entry : propertyTable.entrySet()) {
            HashMap<String, Object> table = entry.getValue();
            ArrayList<Method> getters = (ArrayList<Method>) table
                    .get(STR_GETTERS);
            ArrayList<Method> setters = (ArrayList<Method>) table
                    .get(STR_SETTERS);

            Method normalGetter = null;
            Method indexedGetter = null;
            Method normalSetter = null;
            Method indexedSetter = null;

            Class<?> normalPropType = null;
            Class<?> indexedPropType = null;

            if (getters == null) {
                getters = new ArrayList<Method>();
            }

            if (setters == null) {
                setters = new ArrayList<Method>();
            }

            // retrieve getters
            Class<?>[] paramTypes = null;
            String methodName = null;
            for (Method getter : getters) {
                paramTypes = getter.getParameterTypes();
                methodName = getter.getName();
                // checks if it's a normal getter
                if (paramTypes == null || paramTypes.length == 0) {
                    // normal getter found
                    if (normalGetter == null
                            || methodName.startsWith(PREFIX_IS)) {
                        normalGetter = getter;
                    }
                }

                // checks if it's an indexed getter
                if (paramTypes != null && paramTypes.length == 1
                        && paramTypes[0] == int.class) {
                    // indexed getter found
                    if (indexedGetter == null
                            || methodName.startsWith(PREFIX_GET)
                            || (methodName.startsWith(PREFIX_IS) && !indexedGetter
                                    .getName().startsWith(PREFIX_GET))) {
                        indexedGetter = getter;
                    }
                }
            }

            // retrieve normal setter
            if (normalGetter != null) {
                // Now we will try to look for normal setter of the same type.
                Class<?> propertyType = normalGetter.getReturnType();

                for (Method setter : setters) {
                    if (setter.getParameterTypes().length == 1
                            && propertyType
                                    .equals(setter.getParameterTypes()[0])) {
                        normalSetter = setter;
                        break;
                    }
                }
            } else {
                // Normal getter wasn't defined. Let's look for the last
                // defined setter

                for (Method setter : setters) {
                    if (setter.getParameterTypes().length == 1) {
                        normalSetter = setter;
                    }
                }
            }

            // retrieve indexed setter
            if (indexedGetter != null) {
                // Now we will try to look for indexed setter of the same type.
                Class<?> propertyType = indexedGetter.getReturnType();

                for (Method setter : setters) {
                    if (setter.getParameterTypes().length == 2
                            && setter.getParameterTypes()[0] == int.class
                            && propertyType
                                    .equals(setter.getParameterTypes()[1])) {
                        indexedSetter = setter;
                        break;
                    }
                }
            } else {
                // Indexed getter wasn't defined. Let's look for the last
                // defined indexed setter

                for (Method setter : setters) {
                    if (setter.getParameterTypes().length == 2
                            && setter.getParameterTypes()[0] == int.class) {
                        indexedSetter = setter;
                    }
                }
            }

            // determine property type
            if (normalGetter != null) {
                normalPropType = normalGetter.getReturnType();
            } else if (normalSetter != null) {
                normalPropType = normalSetter.getParameterTypes()[0];
            }

            // determine indexed getter/setter type
            if (indexedGetter != null) {
                indexedPropType = indexedGetter.getReturnType();
            } else if (indexedSetter != null) {
                indexedPropType = indexedSetter.getParameterTypes()[1];
            }

            // convert array-typed normal getters to indexed getters
            if (normalGetter != null && normalGetter.getReturnType().isArray()) {

            }

            // RULES
            // These rules were created after performing extensive black-box
            // testing of RI

            // RULE1
            // Both normal getter and setter of the same type were defined;
            // no indexed getter/setter *PAIR* of the other type defined
            if (normalGetter != null && normalSetter != null
                    && (indexedGetter == null || indexedSetter == null)) {
                table.put(STR_NORMAL, STR_VALID);
                table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                table.put(STR_NORMAL + STR_PROPERTY_TYPE, normalPropType);
                continue;
            }

            // RULE2
            // normal getter and/or setter was defined; no indexed
            // getters & setters defined
            if ((normalGetter != null || normalSetter != null)
                    && indexedGetter == null && indexedSetter == null) {
                table.put(STR_NORMAL, STR_VALID);
                table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                table.put(STR_NORMAL + STR_PROPERTY_TYPE, normalPropType);
                continue;
            }

            // RULE3
            // mix of normal / indexed getters and setters are defined. Types
            // are compatible
            if ((normalGetter != null || normalSetter != null)
                    && (indexedGetter != null || indexedSetter != null)) {
                // (1)!A!B!C!D
                if (normalGetter != null && normalSetter != null
                        && indexedGetter != null && indexedSetter != null) {
                    if (indexedGetter.getName().startsWith(PREFIX_GET)) {
                        table.put(STR_NORMAL, STR_VALID);
                        table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                        table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                        table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                normalPropType);

                        table.put(STR_INDEXED, STR_VALID);
                        table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                        table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                        table.put(STR_INDEXED + STR_PROPERTY_TYPE,
                                indexedPropType);
                    } else {
                        if (normalPropType != boolean.class
                                && normalGetter.getName().startsWith(PREFIX_IS)) {
                            table.put(STR_INDEXED, STR_VALID);
                            table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                            table.put(STR_INDEXED + STR_PROPERTY_TYPE,
                                    indexedPropType);
                        } else {
                            table.put(STR_NORMAL, STR_VALID);
                            table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                            table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                            table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                    normalPropType);
                        }
                    }
                    continue;
                }

                // (2)!AB!C!D
                if (normalGetter != null && normalSetter == null
                        && indexedGetter != null && indexedSetter != null) {
                    table.put(STR_NORMAL, STR_VALID);
                    table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                    table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                    table.put(STR_NORMAL + STR_PROPERTY_TYPE, normalPropType);

                    table.put(STR_INDEXED, STR_VALID);
                    if (indexedGetter.getName().startsWith(PREFIX_GET)) {
                        table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                    }
                    table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                    table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                    continue;
                }

                // (3)A!B!C!D
                if (normalGetter == null && normalSetter != null
                        && indexedGetter != null && indexedSetter != null) {
                    table.put(STR_INDEXED, STR_VALID);
                    if (indexedGetter.getName().startsWith(PREFIX_GET)) {
                        table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                    }
                    table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                    table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                    continue;
                }

                // (4)!AB!CD
                if (normalGetter != null && normalSetter == null
                        && indexedGetter != null && indexedSetter == null) {
                    if (indexedGetter.getName().startsWith(PREFIX_GET)) {
                        table.put(STR_NORMAL, STR_VALID);
                        table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                        table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                        table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                normalPropType);

                        table.put(STR_INDEXED, STR_VALID);
                        table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                        table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                        table.put(STR_INDEXED + STR_PROPERTY_TYPE,
                                indexedPropType);
                    } else {
                        table.put(STR_NORMAL, STR_VALID);
                        table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                        table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                        table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                normalPropType);
                    }
                    continue;
                }

                // (5)A!B!CD
                if (normalGetter == null && normalSetter != null
                        && indexedGetter != null && indexedSetter == null) {
                    if (indexedGetter.getName().startsWith(PREFIX_GET)) {
                        table.put(STR_NORMAL, STR_VALID);
                        table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                        table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                        table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                normalPropType);

                        table.put(STR_INDEXED, STR_VALID);
                        table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                        table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                        table.put(STR_INDEXED + STR_PROPERTY_TYPE,
                                indexedPropType);
                    } else {
                        table.put(STR_NORMAL, STR_VALID);
                        table.put(STR_NORMAL + PREFIX_GET, normalGetter);
                        table.put(STR_NORMAL + PREFIX_SET, normalSetter);
                        table.put(STR_NORMAL + STR_PROPERTY_TYPE,
                                normalPropType);
                    }
                    continue;
                }

                // (6)!ABC!D
                if (normalGetter != null && normalSetter == null
                        && indexedGetter == null && indexedSetter != null) {
                    table.put(STR_INDEXED, STR_VALID);
                    table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                    table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                    table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                    continue;
                }

                // (7)A!BC!D
                if (normalGetter == null && normalSetter != null
                        && indexedGetter == null && indexedSetter != null) {
                    table.put(STR_INDEXED, STR_VALID);
                    table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                    table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                    table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                    continue;
                }
            }

            // RULE4
            // no normal normal getter / setter.
            // Only indexed getter and/or setter is given
            // no normal setters / getters defined
            if (normalSetter == null && normalGetter == null
                    && (indexedGetter != null || indexedSetter != null)) {
                if (indexedGetter != null
                        && indexedGetter.getName().startsWith(PREFIX_IS)) {
                    if (indexedSetter != null) {
                        table.put(STR_INDEXED, STR_VALID);
                        table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                        table.put(STR_INDEXED + STR_PROPERTY_TYPE,
                                indexedPropType);
                    }
                    continue;
                }
                table.put(STR_INDEXED, STR_VALID);
                table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                continue;
            }
            
            // RULE5
            // Both indexed getter and setter methods are defined
            // no normal getter/setter *PAIR* of the other type defined
            if ((normalSetter != null || normalGetter != null)
                    && indexedGetter != null && indexedSetter != null) {
                table.put(STR_INDEXED, STR_VALID);
                table.put(STR_INDEXED + PREFIX_GET, indexedGetter);
                table.put(STR_INDEXED + PREFIX_SET, indexedSetter);
                table.put(STR_INDEXED + STR_PROPERTY_TYPE, indexedPropType);
                continue;
            }

            // default rule - invalid property
            table.put(STR_NORMAL, STR_INVALID);
            table.put(STR_INDEXED, STR_INVALID);            
        }

    }

    /**
     * Introspects the supplied Bean class and returns a list of the Events of
     * the class
     * 
     * @return the events
     * @throws org.java.beans.IntrospectionException
     */
    @SuppressWarnings("unchecked")
    private org.java.beans.EventSetDescriptor[] introspectEvents() throws IntrospectionException {
        // Get descriptors for the public methods
        // FIXME: performance
        MethodDescriptor[] theMethods = introspectMethods();

        if (theMethods == null)
            return null;

        HashMap<String, HashMap> eventTable = new HashMap<String, HashMap>(
                theMethods.length);

        // Search for methods that add an Event Listener
        for (int i = 0; i < theMethods.length; i++) {
            introspectListenerMethods(PREFIX_ADD, theMethods[i].getMethod(),
                    eventTable);
            introspectListenerMethods(PREFIX_REMOVE, theMethods[i].getMethod(),
                    eventTable);
            introspectGetListenerMethods(theMethods[i].getMethod(), eventTable);
        }

        ArrayList<org.java.beans.EventSetDescriptor> eventList = new ArrayList<org.java.beans.EventSetDescriptor>();
        for (Map.Entry<String, HashMap> entry : eventTable.entrySet()) {
            HashMap table = entry.getValue();
            Method add = (Method) table.get(PREFIX_ADD);
            Method remove = (Method) table.get(PREFIX_REMOVE);

            if ((add == null) || (remove == null)) {
                continue;
            }

            Method get = (Method) table.get(PREFIX_GET);
            Class<?> listenerType = (Class) table.get("listenerType"); //$NON-NLS-1$
            Method[] listenerMethods = (Method[]) table.get("listenerMethods"); //$NON-NLS-1$
            org.java.beans.EventSetDescriptor eventSetDescriptor = new org.java.beans.EventSetDescriptor(
                    Introspector.decapitalize(entry.getKey()), listenerType, listenerMethods, add,
                    remove, get);

            eventSetDescriptor.setUnicast(table.get("isUnicast") != null); //$NON-NLS-1$
            eventList.add(eventSetDescriptor);
        }

        org.java.beans.EventSetDescriptor[] theEvents = new org.java.beans.EventSetDescriptor[eventList
                .size()];
        eventList.toArray(theEvents);

        return theEvents;
    }

    /*
     * find the add, remove listener method
     */
    @SuppressWarnings("unchecked")
    private static void introspectListenerMethods(String type,
            Method theMethod, HashMap<String, HashMap> methodsTable) {
        String methodName = theMethod.getName();
        if (methodName == null) {
            return;
        }

        if (!((methodName.startsWith(type)) && (methodName
                .endsWith(SUFFIX_LISTEN)))) {
            return;
        }

        String listenerName = methodName.substring(type.length());
        String eventName = listenerName.substring(0, listenerName
                .lastIndexOf(SUFFIX_LISTEN));
        if ((eventName == null) || (eventName.length() == 0)) {
            return;
        }

        Class[] paramTypes = theMethod.getParameterTypes();
        if ((paramTypes == null) || (paramTypes.length != 1)) {
            return;
        }

        Class<?> listenerType = paramTypes[0];

        if (!EventListener.class.isAssignableFrom(listenerType)) {
            return;
        }

        if (!listenerType.getName().endsWith(listenerName)) {
            return;
        }

        HashMap table = methodsTable.get(eventName);
        if (table == null) {
            table = new HashMap();
        }
        // put listener type
        if (table.get("listenerType") == null) { //$NON-NLS-1$
            table.put("listenerType", listenerType); //$NON-NLS-1$
            table.put("listenerMethods", //$NON-NLS-1$
                    introspectListenerMethods(listenerType));
        }
        // put add / remove
        table.put(type, theMethod);

        // determine isUnicast()
        if (type.equals(PREFIX_ADD)) {
            Class[] exceptionTypes = theMethod.getExceptionTypes();
            if (exceptionTypes != null) {
                for (int i = 0; i < exceptionTypes.length; i++) {
                    if (exceptionTypes[i].getName().equals(
                            TooManyListenersException.class.getName())) {
                        table.put("isUnicast", "true"); //$NON-NLS-1$//$NON-NLS-2$
                        break;
                    }
                }
            }
        }

        methodsTable.put(eventName, table);
    }

    private static Method[] introspectListenerMethods(Class<?> listenerType) {
        Method[] methods = listenerType.getDeclaredMethods();
        ArrayList<Method> list = new ArrayList<Method>();
        for (int i = 0; i < methods.length; i++) {
            Class<?>[] paramTypes = methods[i].getParameterTypes();
            if (paramTypes.length != 1) {
                continue;
            }

            if (EventObject.class.isAssignableFrom(paramTypes[0])) {
                list.add(methods[i]);
            }
        }
        Method[] matchedMethods = new Method[list.size()];
        list.toArray(matchedMethods);
        return matchedMethods;
    }

    @SuppressWarnings("unchecked")
    private static void introspectGetListenerMethods(Method theMethod,
            HashMap<String, HashMap> methodsTable) {
        String type = PREFIX_GET;

        String methodName = theMethod.getName();
        if (methodName == null) {
            return;
        }

        if (!((methodName.startsWith(type)) && (methodName
                .endsWith(SUFFIX_LISTEN + "s")))) { //$NON-NLS-1$
            return;
        }

        String listenerName = methodName.substring(type.length(), methodName
                .length() - 1);
        String eventName = listenerName.substring(0, listenerName
                .lastIndexOf(SUFFIX_LISTEN));
        if ((eventName == null) || (eventName.length() == 0)) {
            return;
        }

        Class[] paramTypes = theMethod.getParameterTypes();
        if ((paramTypes == null) || (paramTypes.length != 0)) {
            return;
        }

        Class returnType = theMethod.getReturnType();
        if ((returnType.getComponentType() == null)
                || (!returnType.getComponentType().getName().endsWith(
                        listenerName))) {
            return;
        }

        HashMap table = methodsTable.get(eventName);
        if (table == null) {
            table = new HashMap();
        }
        // put add / remove
        table.put(type, theMethod);
        methodsTable.put(eventName, table);
    }

    private static boolean isValidProperty(String propertyName) {
        return (propertyName != null) && (propertyName.length() != 0);
    }

    private static class PropertyComparator implements
            Comparator<org.java.beans.PropertyDescriptor> {
        public int compare(org.java.beans.PropertyDescriptor object1,
                           org.java.beans.PropertyDescriptor object2) {
            return object1.getName().compareTo(object2.getName());
        }

    }

    // TODO
    void init() {
        if (this.events == null) {
            events = new EventSetDescriptor[0];
        }
        if (this.properties == null) {
            this.properties = new PropertyDescriptor[0];
        }

        if (properties != null) {
            String defaultPropertyName = (defaultPropertyIndex != -1 ? properties[defaultPropertyIndex]
                    .getName()
                    : null);
            Arrays.sort(properties, comparator);
            if (null != defaultPropertyName) {
                for (int i = 0; i < properties.length; i++) {
                    if (defaultPropertyName.equals(properties[i].getName())) {
                        defaultPropertyIndex = i;
                        break;
                    }
                }
            }
        }
    }
}
