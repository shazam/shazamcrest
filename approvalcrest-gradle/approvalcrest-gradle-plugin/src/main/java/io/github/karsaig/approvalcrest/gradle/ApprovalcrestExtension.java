package io.github.karsaig.approvalcrest.gradle;

import org.gradle.api.provider.Property;

/**
 * The {@code approvalcrest { }} block of a build script.
 */
public abstract class ApprovalcrestExtension {

    /**
     * When true, each {@code Test} task starts its JVM with {@code -javaagent} and the
     * {@code approvalcrest-agent} of the plugin version. Default: false.
     *
     * @return the property
     */
    public abstract Property<Boolean> getAttachAgent();
}
