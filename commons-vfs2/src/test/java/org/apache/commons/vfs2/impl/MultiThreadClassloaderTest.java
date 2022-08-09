/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.impl;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.VFSClassLoader;

import static org.junit.Assert.*;
import org.junit.jupiter.api.RepeatedTest;

/**
 * MultiThreadClassloaderTest test cases.
 */
public class MultiThreadClassloaderTest {

    public static class MockClassloader extends ClassLoader {
        MockClassloader() {
            super(null);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            return null;
        }

        /**
         * This method will not return any hit to VFSClassLoader#testGetResourcesJARs.
         */
        @Override
        public Enumeration<URL> getResources(final String name) {
            return Collections.enumeration(Collections.emptyList());
        }
    }

    private static class H {
        public boolean failed = false;
    }
    @RepeatedTest(5)
    public void testthreads() throws Exception {
        final H failed = new H();
        Logger log = Logger.getLogger("main");
        ExecutorService tp = Executors.newFixedThreadPool(2);
        ArrayList<Future<String>> workers = new ArrayList<>();
        DefaultFileSystemManager fs = new StandardFileSystemManager();
        fs.init();
        String jarfile = System.getProperty("test.classloader.lib");
        FileObject jar = fs.resolveFile("jar://" + jarfile);
        log.log(Level.INFO, "Resolved as: " + jar + "; class: " + jar.getClass().getName());

        ClassLoader mock_classloader = new MockClassloader();
        ClassLoader c1 = new VFSClassLoader(jar, fs, mock_classloader);
        ClassLoader c2 = new VFSClassLoader(jar, fs, mock_classloader);
        ClassLoader c3 = new VFSClassLoader(jar, fs, mock_classloader);

        final String clazz = "org.apache.commons.vfs2.impl.VFSClassLoader";

        for (int idx = 0; idx < 3; idx++) {
            Callable<String> task = () -> {
                try {
                    Class<?> res = c1.loadClass(clazz);
                    return "Thread: " + Thread.currentThread().getName() + "; class: " + res.getName() + "; instance: " + System.identityHashCode(res);
                }
                catch (Exception e) {
                    log.log(Level.INFO,"Thread: "+Thread.currentThread().getName(), e);
                    failed.failed=true;
                    return "failed";
                }
            };
            Callable<String> task1 = () -> {
                try {
                    Class<?> res = c2.loadClass(clazz);
                    return "Thread: " + Thread.currentThread().getName() + "; class: " + res.getName() + "; instance: " + System.identityHashCode(res);
                }
                catch (Exception e) {
                    log.log(Level.INFO,"Thread: "+Thread.currentThread().getName(), e);
                    failed.failed=true;
                    return "failed";
                }
            };
            Callable<String> task2 = () -> {
                try {
                    Class<?> res = c3.loadClass(clazz);
                    return "Thread: " + Thread.currentThread().getName() + "; class: " + res.getName() + "; instance: " + System.identityHashCode(res);
                }
                catch (Exception e) {
                    log.log(Level.INFO,"Thread: "+Thread.currentThread().getName(), e);
                    failed.failed=true;
                    return "failed";
                }
            };
            workers.add(tp.submit(task));
            workers.add(tp.submit(task1));
            workers.add(tp.submit(task2));
        }
        for (final Future<String> t : workers) {
            assertFalse(failed.failed);
            String c = t.get();
            log.log(Level.INFO, c);
        }
        tp.shutdown();
    }
}
