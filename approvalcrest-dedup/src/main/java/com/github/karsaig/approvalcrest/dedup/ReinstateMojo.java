package com.github.karsaig.approvalcrest.dedup;

import com.github.karsaig.approvalcrest.ApprovedFileType;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Replaces all pointer files with standalone approved files containing the canonical content,
 * then removes all canonical files from the shared approvals directory.
 *
 * <p>Use this to migrate the shared directory location (reinstate, then dedup with the new config),
 * or to fully opt out of the shared file system.
 *
 * <p>Run with: {@code mvn approvalcrest:reinstate}
 */
@Mojo(name = "reinstate")
public class ReinstateMojo extends AbstractDedupMojo {

    @Override
    public void execute() throws MojoExecutionException {
        try {
            ApprovalReinstate reinstater = new ApprovalReinstate(workingDirectory(), scanDir(), sharedDir(), dryRun(),
                    ApprovedFileType.parse(types()));
            ApprovalReinstate.ReinstateResult result = reinstater.reinstate();
            getLog().info(result.toString());
        } catch (IOException e) {
            throw new MojoExecutionException("Reinstate failed", e);
        }
    }
}
