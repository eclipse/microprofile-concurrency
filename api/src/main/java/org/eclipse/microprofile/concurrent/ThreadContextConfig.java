/*
 * Copyright (c) 2018,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.concurrent;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

/**
 * <p>Annotates a CDI injection point for a {@link ThreadContext} such that the container
 * creates a new instance, which is identified within an application by its unique name.
 * The unique name is generated as the fully qualified class name (with each component delimited by <code>.</code>)
 * and the injection point's field name or method name and parameter position, all delimited by <code>/</code>,
 * unless annotated with the {@link NamedInstance} qualifier,
 * in which case the unique name is specified by the {@link NamedInstance#value value} attribute of that qualifier.</p>
 *
 * <p>For example, the following injection points share a single
 * {@link ThreadContext} instance,</p>
 *
 * <pre><code> &commat;Inject &commat;NamedInstance("tc1") &commat;ThreadContextConfig(propagated = { ThreadContext.CDI, ThreadContext.APPLICATION })
 * ThreadContext threadContext1;
 *
 * &commat;Inject
 * void setTask(&commat;NamedInstance("tc1") ThreadContext contextPropagator) {
 *     task = contextPropagator.contextualTask(...);
 * }
 *
 * &commat;Inject
 * void setContextSnapshot(&commat;NamedInstance("tc1") ThreadContext contextPropagator) {
 *     contextSnapshot = contextPropagator.currentContextExecutor();
 * }
 * </code></pre>
 *
 * <p>Alternatively, the following injection points each represent a distinct
 * {@link ThreadContext} instance,</p>
 *
 * <pre><code> &commat;Inject &commat;ThreadContextConfig(propagated = { ThreadContext.SECURITY, ThreadContext.APPLICATION })
 * ThreadContext tc2;
 *
 * &commat;Inject &commat;ThreadContextConfig(cleared = ThreadContext.SECURITY, unchanged = ThreadContext.TRANSACTION)
 * ThreadContext tc3;
 * </code></pre>
 *
 * <p>A <code>ThreadContext</code> must fail to inject, raising
 * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
 * on application startup,
 * if multiple injection points are annotated to create instances with the same name.</p>
 *
 * <p>A <code>ThreadContext</code> must fail to inject, raising
 * {@link javax.enterprise.inject.spi.DeploymentException DeploymentException}
 * on application startup, if more than one provider provides the same thread context
 * {@link org.eclipse.microprofile.concurrent.spi.ThreadContextProvider#getThreadContextType type}.
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE })
public @interface ThreadContextConfig {
    /**
     * <p>Defines the set of thread context types to clear from the thread
     * where the action or task executes. The previous context is resumed
     * on the thread after the action or task ends.</p>
     *
     * <p>For example, if the user specifies {@link ThreadContext#TRANSACTION} in this set,
     * then when a action or task runs, the current transaction is cleared/suspended from
     * the execution thread so that actions and tasks can start and
     * end transactions of their choosing, to independently perform their
     * own transactional work, as needed.</p>
     *
     * <p>{@link ThreadContext#ALL_REMAINING} is automatically appended to the
     * set of cleared context if neither the {@link #propagated} set nor the
     * {@link #unchanged} set include {@link ThreadContext#ALL_REMAINING}.</p>
     *
     * <p>Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.</p>
     *
     * <p>A <code>ThreadContext</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #propagated} and/or {@link #unchanged} set
     * includes one or more of the same types as this set.</p>
     */
    String[] cleared() default {};

    /**
     * <p>Defines the set of thread context types to capture from the thread
     * that contextualizes an action or task. This context is later
     * re-established on the thread(s) where the action or task executes.</p>
     *
     * <p>The default set of propagated thread context types is
     * {@link ThreadContext#ALL_REMAINING}, which includes all available
     * thread context types that support capture and propagation to other
     * threads, except for those that are explicitly {@link #cleared}
     * or {@link #unchanged}.</p>
     *
     * <p>Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.</p>
     *
     * <p>Thread context types which are not otherwise included in this set or
     * in the {@link #unchanged} set are cleared from the thread of execution
     * for the duration of the action or task.</p>
     *
     * <p>A <code>ThreadContext</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #cleared} and/or {@link #unchanged} set
     * includes one or more of the same types as this set.</p>
     */
    String[] propagated() default { ThreadContext.ALL_REMAINING };

    /**
     * <p>Defines a set of thread context types that are essentially ignored,
     * in that they are neither captured nor are they propagated or cleared
     * from thread(s) that execute the action or task.</p>
     *
     * <p>Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.
     *
     * <p>The configuration <code>unchanged</code> context is provided for
     * advanced patterns where it is desirable to leave certain context types
     * on the executing thread.</p>
     *
     * <p>For example, to run as the current application, but under the
     * transaction of the thread where the task executes:</p>
     * <pre><code> &commat;Inject &commat;ThreadContextConfig(unchanged = ThreadContext.TRANSACTION,
     *                              propagated = ThreadContext.APPLICATION,
     *                              cleared = ThreadContext.ALL_REMAINING)
     * ThreadContext threadContext;
     * ...
     * task = threadContext.contextualRunnable(new MyTransactionalTask());
     * ...
     * // on another thread,
     * tx.begin();
     * ...
     * task.run(); // runs under the transaction due to 'unchanged'
     * tx.commit();
     * </code></pre>
     *
     * <p>A <code>ThreadContext</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #cleared} and/or {@link #propagated} set
     * includes one or more of the same types as this set.</p>
     */
    String[] unchanged() default {};
    
        /**
        * Util class used for inline creation of {@link ThreadContextConfig} annotation instances.
        */
        public final class Literal extends AnnotationLiteral<ThreadContextConfig> implements ThreadContextConfig {

            public static final Literal DEFAULT_INSTANCE = 
                of(new String[]{}, new String[]{}, new String[]{ThreadContext.ALL_REMAINING});

            private static final long serialVersionUID = 1L;

            private final String[] cleared;
            private final String[] unchanged;
            private final String[] propagated;

            public String[] cleared() {
                return cleared;
            }

            public String[] unchanged() {
                return unchanged;
            }
            public String[] propagated() {
                return propagated;
            }

            public static Literal of(String[] cleared, String[] unchanged, String[] propagated) {
                return new Literal(cleared, unchanged, propagated);
            }

            private Literal(String[] cleared, String[] unchanged, String[] propagated) {
                this.cleared = cleared;
                this.unchanged = unchanged;
                this.propagated = propagated;
            }
        }
}