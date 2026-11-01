package io.github.karsaig.approvalcrest.gradle;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.options.Option;

/**
 * Runs the approvalcrest dedup tool ({@code DedupCli}) on the project. {@code approvalcrestDedup}
 * replaces duplicate approved files with pointers; {@code approvalcrestReinstate} replaces the
 * pointers with their content again.
 */
@UntrackedTask(because = "It rewrites the approved files that are its inputs.")
public abstract class ApprovalcrestDedupTask extends JavaExec {

    private boolean reinstate;
    private boolean dryRun;
    private String types;

    @Input
    public boolean isReinstate() {
        return reinstate;
    }

    public void setReinstate(boolean reinstate) {
        this.reinstate = reinstate;
    }

    @Input
    public boolean isDryRun() {
        return dryRun;
    }

    // Not --dry-run: Gradle reserves that option for itself, and it would skip the task.
    @Option(option = "preview", description = "Prints what the tool would do, and writes and deletes nothing.")
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Optional
    @Input
    public String getTypes() {
        return types;
    }

    @Option(option = "types", description = "The approved file types to process: json, content, json,content or all.")
    public void setTypes(String types) {
        this.types = types;
    }

    @Override
    @TaskAction
    public void exec() {
        List<String> arguments = new ArrayList<>();
        if (reinstate) {
            arguments.add("--reinstate");
        }
        if (dryRun) {
            arguments.add("--dry-run");
        }
        if (types != null) {
            arguments.add("--types");
            arguments.add(types);
        }
        args(arguments);
        super.exec();
    }
}
