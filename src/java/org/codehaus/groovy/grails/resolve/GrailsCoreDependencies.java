/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.resolve;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.codehaus.groovy.grails.resolve.config.*;
import groovy.lang.Closure;

/**
 * Encapsulates information about the core dependencies of Grails.
 * 
 * This may eventually expand to expose information such as Spring version etc.
 * and be made available in the binding for user dependency declarations.
 */
public class GrailsCoreDependencies {
    
    public final String grailsVersion;
    
    public GrailsCoreDependencies(String grailsVersion) {
        this.grailsVersion = grailsVersion;
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, String... excludes) {
        for (ModuleRevisionId mrid : dependencies) {
            EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, "build");
            if (excludes != null) {
                for (String exclude : excludes) {
                    descriptor.exclude(exclude);
                }
            }
            dependencyManager.registerDependency(scope, descriptor);
        }
    }
    
    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     */
    public Closure createDeclaration() {
        return new Closure(this, this) {
            public Object doCall() {
                
                System.out.println("in root block");
                
                DependencyConfigurationConfigurer rootDelegate = (DependencyConfigurationConfigurer)getDelegate();
                
                rootDelegate.log("warn");
                
                // Repositories
                
                rootDelegate.repositories(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        
                        System.out.println("in repositories block");
                        RepositoriesConfigurer repositoriesDelegate = (RepositoriesConfigurer)getDelegate();
                     
                        repositoriesDelegate.grailsPlugins();
                        repositoriesDelegate.grailsHome();
                        
                        return null;
                    }
                });
                
                // Dependencies
                
                rootDelegate.dependencies(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        
                        System.out.println("in dependencies block");
                        JarDependenciesConfigurer dependenciesDelegate = (JarDependenciesConfigurer)getDelegate(); 
                        IvyDependencyManager dependencyManager = dependenciesDelegate.getDependencyManager();
                        
                        boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided();
                        String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile";
                        String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime";

                        // dependencies needed by the Grails build system
                        ModuleRevisionId[] buildDependencies = {
                            ModuleRevisionId.newInstance("org.tmatesoft.svnkit", "svnkit", "1.3.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant", "1.7.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-launcher", "1.7.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-junit", "1.7.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-nodeps", "1.7.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-trax", "1.7.1"),
                            ModuleRevisionId.newInstance("jline", "jline", "0.9.94"),
                            ModuleRevisionId.newInstance("org.fusesource.jansi", "jansi", "1.2.1"),
                            ModuleRevisionId.newInstance("xalan","serializer", "2.7.1"),
                            ModuleRevisionId.newInstance("org.grails", "grails-docs", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-scripts", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", "1.5.8"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-log4j12", "1.5.8"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.test", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.0_jdk5")
                        };
                        registerDependencies(dependencyManager, "build", buildDependencies);
                        
                        
                        // depenencies needed when creating docs
                        ModuleRevisionId[] docDependencies = {
                            ModuleRevisionId.newInstance("org.xhtmlrenderer", "core-renderer","R8"),
                            ModuleRevisionId.newInstance("com.lowagie","itext", "2.0.8"),
                            ModuleRevisionId.newInstance("radeox", "radeox", "1.0-b2")
                        };
                        registerDependencies(dependencyManager, "docs", docDependencies);
                        
                        
                        // dependencies needed during development, but not for deployment
                        ModuleRevisionId[] providedDependencies = {
                            ModuleRevisionId.newInstance("javax.servlet", "servlet-api", "2.5"),
                            ModuleRevisionId.newInstance("javax.servlet", "jsp-api","2.1")
                        };
                        registerDependencies(dependencyManager, "provided", providedDependencies);
                        
                        
                        // dependencies needed at compile time
                        ModuleRevisionId[] groovyDependencies = {
                            ModuleRevisionId.newInstance("org.codehaus.groovy", "groovy-all", "1.8.0-beta-4-SNAPSHOT")
                        };                        
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, groovyDependencies, "jline");
                        
                        ModuleRevisionId[] commonsExcludingLoggingAndXmlApis = {
                            ModuleRevisionId.newInstance("commons-beanutils", "commons-beanutils", "1.8.0"),
                            ModuleRevisionId.newInstance("commons-el", "commons-el", "1.0"),
                            ModuleRevisionId.newInstance("commons-validator", "commons-validator", "1.3.1")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, commonsExcludingLoggingAndXmlApis, "commons-logging", "xml-apis");

                        ModuleRevisionId[] compileDependencies = {
                            ModuleRevisionId.newInstance("org.coconut.forkjoin", "jsr166y", "070108"),
                            ModuleRevisionId.newInstance("org.codehaus.gpars", "gpars", "0.9"),
                            ModuleRevisionId.newInstance("aopalliance", "aopalliance", "1.0"),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.0_jdk5"),
                            ModuleRevisionId.newInstance("commons-codec", "commons-codec", "1.4"),
                            ModuleRevisionId.newInstance("commons-collections", "commons-collections", "3.2.1"),
                            ModuleRevisionId.newInstance("commons-io", "commons-io", "1.4"),
                            ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.4"),
                            ModuleRevisionId.newInstance("javax.transaction", "jta", "1.1"),
                            ModuleRevisionId.newInstance("org.hibernate", "ejb3-persistence", "1.0.2.GA"),
                            ModuleRevisionId.newInstance("opensymphony", "sitemesh", "2.4"),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-crud", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-gorm", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-spring", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.core", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.aop", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.aspects", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.asm", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.beans", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.context", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.context.support", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.expression", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.instrument", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.jdbc", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.jms", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.orm", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.oxm", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.transaction", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.web", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.web.servlet", "3.0.3.RELEASE"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", "1.5.8")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, compileDependencies);
                        
                        
                        // dependencies needed for running tests
                        ModuleRevisionId[] testDependencies = {
                            ModuleRevisionId.newInstance("junit", "junit", "4.8.1"),
                            ModuleRevisionId.newInstance("org.grails", "grails-test", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "org.springframework.test", "3.0.3.RELEASE")
                        };
                        registerDependencies(dependencyManager, "test", testDependencies);
                        
                        
                        // dependencies needed at runtime only
                        ModuleRevisionId[] runtimeDependencies = {
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjweaver", "1.6.8"),
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjrt", "1.6.8"),
                            ModuleRevisionId.newInstance("cglib", "cglib-nodep", "2.1_3"),
                            ModuleRevisionId.newInstance("commons-fileupload", "commons-fileupload", "1.2.1"),
                            ModuleRevisionId.newInstance("oro", "oro", "2.0.8"),
                            ModuleRevisionId.newInstance("javax.servlet", "jstl", "1.1.2"),
                            // data source
                            ModuleRevisionId.newInstance("commons-dbcp", "commons-dbcp", "1.3"),
                            ModuleRevisionId.newInstance("commons-pool", "commons-pool", "1.5.5"),
                            ModuleRevisionId.newInstance("hsqldb", "hsqldb", "1.8.0.10"),
                            ModuleRevisionId.newInstance("com.h2database", "h2", "1.2.144"),
                            // JSP support
                            ModuleRevisionId.newInstance("apache-taglibs", "standard", "1.1.2"),
                            ModuleRevisionId.newInstance("xpp3", "xpp3_min", "1.1.3.4.O")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, runtimeDependencies);
                        
                        ModuleRevisionId[] ehcacheDependencies = {
                            ModuleRevisionId.newInstance("net.sf.ehcache", "ehcache-core", "1.7.1")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, ehcacheDependencies, "jms", "commons-logging", "servlet-api");

                        ModuleRevisionId[] loggingDependencies = {
                            ModuleRevisionId.newInstance("log4j", "log4j", "1.2.16"),
                            ModuleRevisionId.newInstance("org.slf4j", "jcl-over-slf4j", "1.5.8"),
                            ModuleRevisionId.newInstance("org.slf4j", "jul-to-slf4j", "1.5.8"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-log4j12", "1.5.8")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, loggingDependencies, "mail", "jms", "jmxtools", "jmxri");

                        return null;
                        
                    } 
                }); // end depenencies closure
                
                return null;
            }
            
        }; // end root closure 

    }
    
}