package com.github.karsaig.approvalcrest.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that hands approvalcrest an {@link Instrumentation} instance, so that safe mode can
 * open locked JDK module packages without {@code sun.misc.Unsafe} and without {@code --add-opens}.
 *
 * <p>
 * Attach it at JVM startup:
 * </p>
 *
 * <pre>
 * -javaagent:/path/to/approvalcrest-agent.jar
 * </pre>
 *
 * <p>
 * When the JDK denies the {@code sun.misc.Unsafe} memory-access methods, which safe mode uses to
 * open modules, they throw. JDK 24 and later deny them with
 * {@code --sun-misc-unsafe-memory-access=deny}, and JEP 471 plans to make that the default.
 * {@code Instrumentation.redefineModule} is the supported replacement, and an agent is the only
 * way to obtain an {@code Instrumentation}.
 * Attaching at startup is unaffected by JEP 451, which restricts dynamic attach only.
 * </p>
 *
 * <p>
 * Without this agent nothing breaks: approvalcrest falls back to the Unsafe route where it still
 * works, and to getter-based serialization where it does not.
 * </p>
 */
public final class ApprovalcrestAgent {

    /**
     * Set once by {@link #premain} and read reflectively by approvalcrest.
     * <p>
     * This class is loaded by the system class loader because it is an agent class, while
     * approvalcrest itself may be loaded by another loader. Reading this field through
     * {@code ClassLoader.getSystemClassLoader()} is therefore the loader-independent way to reach
     * the instance, and it avoids putting a non-String value into the system properties of the
     * suite approvalcrest happens to be running inside.
     */
    private static volatile Instrumentation instrumentation;

    private ApprovalcrestAgent() {
    }

    /**
     * @return the instance supplied by the JVM, or null when this class was never loaded as an
     *         agent. Readable by anything on the classpath, as it must be for approvalcrest to
     *         reach it; kept behind an accessor so it cannot be replaced or cleared.
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Agent entry point. Stores the instance and does nothing else.
     * <p>
     * Deliberately no packages are opened here. An open has to name the module doing the
     * reflection, and only approvalcrest can determine which of its own copies that is; opening to
     * a module computed inside this class would work for one copy and silently fail for another.
     * </p>
     *
     * @param agentArgs ignored
     * @param inst      supplied by the JVM
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }
}
