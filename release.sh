#!/usr/bin/env bash

show_help() {
    cat <<'EOF'
Usage: release.sh <version> [--dry-run] [--resume] [--skip-gradle] [--jdk <version>] [--verbose]

Arguments:
  <version>          Release version to set (e.g. 0.63.0)
  --dry-run          Test mode: runs all steps locally without pushing
                     to git remote or publishing to Maven Central.
                     Local git changes are undone at the end.
  --resume           Resume mode: skips all steps up to and including
                     the Maven deploy (steps 1-4) and runs only the
                     post-publish steps (5-6). Use when the script was
                     interrupted after a successful deploy: from then on
                     the release commit and tag are kept. Steps 5 and 6
                     are skipped when they are already done. Cannot be
                     combined with --dry-run. Publishes the Gradle plugin
                     if the Plugin Portal does not have the version yet,
                     for example after a --skip-gradle release.
  --skip-gradle      Maven-only release: skips every Gradle step (3b, 4b,
                     and the plugin version in the docs).
  --jdk <version>    JDK version to use for testing (default: 8)
  --verbose | -v     Echo every command as it runs (set -x)

Steps performed:
  1. Bump version in pom.xml and for-release-pom.xml         [skipped in --resume]
  2. git commit + annotated tag v<version>                   [skipped in --resume]
  3. mvn clean install  (JDK <version>)                      [skipped in --resume]
 3b. Gradle check: the Gradle integration tests and the plugin
     tests (needs JDK 17 for Gradle 8.14)                    [skipped in --resume]
  4. mvn deploy -P sign-release via for-release-pom.xml      [skipped in --resume]
     (or mvn verify in --dry-run: signs artifacts but does not upload)
 4b. publishPlugins to the Gradle Plugin Portal, from the tag
     (or publishPlugins --validate-only in --dry-run;
     skipped when the Portal already has the version)
  5. git push --follow-tags  (skipped in --dry-run)
  6. Bump to next development version and push  (skipped in --dry-run)
     - X.Y.Z releases become X.Y.(Z+1)-SNAPSHOT
     - Non-conforming versions become after-<version>-SNAPSHOT

After a real release, the development version is set automatically.
Use ./nextversion.sh <version> to override it if needed.
EOF
}

version=""
dry_run=false
resume=false
skip_gradle=false
verbose=false
jvtr=8

while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help|help)
            show_help; exit 0 ;;
        --dry-run)
            dry_run=true; shift ;;
        --resume)
            resume=true; shift ;;
        --skip-gradle)
            skip_gradle=true; shift ;;
        --verbose|-v)
            verbose=true; shift ;;
        --jdk)
            jvtr="$2"; shift 2 ;;
        --jdk=*)
            jvtr="${1#--jdk=}"; shift ;;
        *)
            if [[ -z "$version" ]]; then
                version="$1"; shift
            else
                echo "Unknown argument: $1" >&2
                show_help; exit 1
            fi ;;
    esac
done

if [[ -z "$version" ]]; then
    show_help; exit 1
fi

if [[ "$dry_run" == "true" && "$resume" == "true" ]]; then
    echo "ERROR: --dry-run and --resume are mutually exclusive." >&2
    exit 1
fi

set -euo pipefail
[[ "$verbose" == "true" ]] && set -x

plugin_on_portal() {
    curl -sf -o /dev/null "https://plugins.gradle.org/m2/io/github/karsaig/approvalcrest/io.github.karsaig.approvalcrest.gradle.plugin/${version}/io.github.karsaig.approvalcrest.gradle.plugin-${version}.pom"
}

portal_credentials_present() {
    [[ -n "${GRADLE_PUBLISH_KEY:-}" && -n "${GRADLE_PUBLISH_SECRET:-}" ]] && return 0
    grep -q '^gradle.publish.key=' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" 2>/dev/null
}

# Builds the plugin from the tag, so that its version is the release version even when pom.xml already holds the
# next development version (a late --resume).
publish_plugin() {
    _worktree="$(mktemp -d)/release-v${version}"
    git worktree add --detach "$_worktree" "v${version}"
    "$_worktree/approvalcrest-gradle/gradlew" -p "$_worktree/approvalcrest-gradle/approvalcrest-gradle-plugin" publishPlugins "$@"
    git worktree remove --force "$_worktree"
    _worktree=""
}

# Update documentation version references to the released version.
# Uses context-aware patterns so it works regardless of what version
# is currently in the docs (handles stale, manual, or inconsistent values).
update_doc_versions() {
    local new_ver="$1"
    local file="$2"
    # Pattern 1: <version>ANY</version> within a <dependency> or <plugin> block containing com.github.karsaig.
    # The range must also end at </plugin>: otherwise it runs on to the next </dependency> and rewrites
    # the versions of unrelated plugins on the way.
    sed -i -E '/groupId.*com\.github\.karsaig/,/<\/(dependency|plugin)>/{s|<version>[^<]+</version>|<version>'"${new_ver}"'</version>|;}' "$file"
    # Pattern 2: inline artifact coordinates like com.github.karsaig:approvalcrest[-...]:VERSION
    sed -i -E 's|(com\.github\.karsaig:approvalcrest[a-z0-9-]*):[0-9][0-9a-zA-Z._-]*|\1:'"${new_ver}"'|g' "$file"
    # Pattern 3: jar file names like approvalcrest-agent-VERSION.jar, used by the -javaagent
    # documentation. Anchored on the artifact name so prose such as "before 1.5.1" is untouched.
    sed -i -E 's|(approvalcrest[a-z0-9-]*)-[0-9][0-9a-zA-Z._-]*\.jar|\1-'"${new_ver}"'.jar|g' "$file"
    # Pattern 4: repository paths like .../approvalcrest-agent/VERSION/approvalcrest-agent-...
    sed -i -E 's|(approvalcrest[a-z0-9-]*)/[0-9][0-9a-zA-Z._-]*/|\1/'"${new_ver}"'/|g' "$file"
}

# Pattern 5: the Gradle plugin request, id("io.github.karsaig.approvalcrest") version "VERSION" (Kotlin DSL) or
# id 'io.github.karsaig.approvalcrest' version 'VERSION' (Groovy DSL). Separate, because a --skip-gradle release
# must not move the docs to a plugin version that does not exist.
update_plugin_doc_version() {
    local new_ver="$1"
    local file="$2"
    sed -i -E 's|(id[ (]["'"'"']io\.github\.karsaig\.approvalcrest["'"'"']\)? version ["'"'"'])[^"'"'"']+|\1'"${new_ver}"'|g' "$file"
}

# State tracking for cleanup
_commit_made=false
_tag_made=false
_published=false
_push_done=false
_plugin_published_now=false
_worktree=""

cleanup() {
    # Always remove versionsBackup files — they are never needed after versions:commit
    find . -name "*.versionsBackup" -delete 2>/dev/null || true
    if [[ -n "$_worktree" ]]; then
        git worktree remove --force "$_worktree" 2>/dev/null || true
    fi
    # If push already succeeded the release is done — leave local state as-is
    if [[ "$_push_done" == "true" ]]; then
        return
    fi
    # Central cannot withdraw a published release, so the release commit and tag must stay for --resume.
    if [[ "$_published" == "true" ]]; then
        echo "ERROR: the release is published, but the script stopped before it finished." >&2
        echo "       The release commit and tag v${version} are kept. To finish: ./release.sh ${version} --resume" >&2
        return
    fi
    if [[ "$_tag_made" == "true" ]]; then
        git tag -d "v${version}" 2>/dev/null || true
    fi
    if [[ "$_commit_made" == "true" ]]; then
        git reset --hard HEAD~1 || true
    else
        # versions:set ran but commit was never made — restore pom files
        git checkout -- . 2>/dev/null || true
    fi
}
trap cleanup EXIT

# Pre-flight: check tag existence
if [[ "$resume" == "true" ]]; then
    # In resume mode the tag should already exist — warn if it doesn't
    if ! git tag -l "v${version}" | grep -q "v${version}"; then
        echo "WARNING: tag 'v${version}' not found locally. Proceeding anyway." >&2
    fi
else
    if git tag -l "v${version}" | grep -q "v${version}"; then
        if git ls-remote --tags origin "refs/tags/v${version}" 2>/dev/null | grep -q "v${version}"; then
            echo "ERROR: tag 'v${version}' already exists and has been pushed to remote." >&2
            echo "       Use a different version number." >&2
        else
            echo "ERROR: tag 'v${version}' already exists locally (not yet pushed)." >&2
            echo "       To remove it and retry: git tag -d 'v${version}'" >&2
        fi
        exit 1
    fi
fi

if [[ "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    next_dev="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))-SNAPSHOT"
else
    next_dev="after-${version}-SNAPSHOT"
fi

# Pre-flight: a resume only continues this release. After a later release, it would set the version back.
if [[ "$resume" == "true" ]]; then
    head_version=$(git show HEAD:pom.xml | sed -n 's:.*<version>\(.*\)</version>.*:\1:p' | head -1)
    if [[ "$head_version" != "${version}" && "$head_version" != "${next_dev}" ]]; then
        echo "ERROR: HEAD is at version ${head_version}. --resume for ${version} works only while HEAD is at ${version} or ${next_dev}." >&2
        exit 1
    fi
fi

# Pre-flight: the Gradle plugin. Nothing has changed yet, so a failure here costs nothing.
if [[ "$skip_gradle" == "false" ]]; then
    if [[ "$resume" == "false" ]] && plugin_on_portal; then
        echo "ERROR: the Gradle Plugin Portal already has io.github.karsaig.approvalcrest ${version}." >&2
        echo "       Use a different version number, or --skip-gradle." >&2
        exit 1
    fi
    # publishPlugins needs the keys in a dry run too: --validate-only checks them.
    if ! portal_credentials_present && ! { [[ "$resume" == "true" ]] && plugin_on_portal; }; then
        echo "ERROR: no Gradle Plugin Portal credentials. Set GRADLE_PUBLISH_KEY and GRADLE_PUBLISH_SECRET," >&2
        echo "       or gradle.publish.key and gradle.publish.secret in ~/.gradle/gradle.properties, or use --skip-gradle." >&2
        exit 1
    fi
fi

if [[ "$dry_run" == "true" ]]; then
    echo "========================================"
    echo "  DRY RUN MODE"
    echo "  Running all release steps locally."
    echo "  Nothing will be pushed to git remote."
    echo "  Nothing will be published to Maven Central."
    echo "  Local git changes will be undone at the end."
    echo "========================================"
fi

if [[ "$resume" == "true" ]]; then
    echo "========================================"
    echo "  RESUME MODE"
    echo "  Skipping steps 1-4 (version bump, build, deploy)."
    echo "  Running post-publish steps only."
    echo "========================================"
fi

if [[ "$resume" == "false" ]]; then
    mvn versions:set -DnewVersion="${version}"
    mvn versions:commit

    mvn -f for-release-pom.xml versions:set -DnewVersion="${version}"
    mvn -f for-release-pom.xml versions:commit

    git commit -a -m "Release version ${version}"
    _commit_made=true
    git tag -a "v${version}" -m "Release ${version}"
    _tag_made=true

    mvn clean install -Djava.version.to.run="${jvtr}"
    if [[ "$skip_gradle" == "false" ]]; then
        ./approvalcrest-gradle/gradlew -p approvalcrest-gradle check --configuration-cache --warning-mode=fail
    fi
fi

if [[ "$dry_run" == "true" ]]; then
    mvn clean verify -f for-release-pom.xml -P sign-release
    if [[ "$skip_gradle" == "false" ]]; then
        publish_plugin --validate-only
    fi
    echo "========================================"
    echo "  DRY RUN COMPLETE"
    echo "  All checks passed. No remote changes made."
    echo "  Local state restored to pre-release."
    echo "========================================"
else
    if [[ "$resume" == "false" ]]; then
        # The Portal rejects bad keys or metadata here, before the deploy that cannot be undone.
        if [[ "$skip_gradle" == "false" ]]; then
            publish_plugin --validate-only
        fi
        mvn clean -f for-release-pom.xml
        mvn -f for-release-pom.xml deploy -P sign-release
    fi
    _published=true

    if [[ "$skip_gradle" == "false" ]]; then
        if plugin_on_portal; then
            echo "The Gradle Plugin Portal already has ${version}, skipping the plugin publication."
        else
            publish_plugin
            _plugin_published_now=true
        fi
    fi

    if [[ "$resume" == "true" ]] && git ls-remote --tags origin "refs/tags/v${version}" | grep -q "v${version}"; then
        echo "Tag v${version} is already on origin, skipping the push."
    else
        git push --follow-tags
    fi
    _push_done=true

    # Bump to next development version
    # A resumed run may find the bump already committed; then it only has to push it. The committed pom.xml
    # decides: a run that stopped between versions:set and the commit has to do the bump again.
    committed_version=$(git show HEAD:pom.xml | sed -n 's:.*<version>\(.*\)</version>.*:\1:p' | head -1)
    if [[ "$committed_version" == "${next_dev}" ]]; then
        echo "Version is already ${next_dev}, skipping the bump."
        if [[ "$_plugin_published_now" == "true" ]]; then
            # A late plugin publication after a --skip-gradle release: only the plugin version in the docs is behind.
            if [[ -n "$(git status --porcelain -- README.md docs)" ]]; then
                echo "WARNING: README.md or docs/ has uncommitted changes, so the plugin version in the docs stays as it is." >&2
            else
                update_plugin_doc_version "${version}" README.md
                for f in docs/*.md; do
                    update_plugin_doc_version "${version}" "$f"
                done
                if [[ -n "$(git status --porcelain -- README.md docs)" ]]; then
                    git commit -m "Point the docs at Gradle plugin ${version}" -- README.md docs
                fi
            fi
        fi
        git push
        printf '\033[92m========================================\n  Release %s completed successfully!\n========================================\033[0m\n' "${version}"
        exit 0
    fi

    mvn versions:set -DnewVersion="${next_dev}"
    mvn versions:commit
    mvn -f for-release-pom.xml versions:set -DnewVersion="${next_dev}"
    mvn -f for-release-pom.xml versions:commit

    update_doc_versions "${version}" README.md
    for f in docs/*.md; do
        update_doc_versions "${version}" "$f"
    done
    if [[ "$skip_gradle" == "false" ]]; then
        update_plugin_doc_version "${version}" README.md
        for f in docs/*.md; do
            update_plugin_doc_version "${version}" "$f"
        done
    fi

    git commit -a -m "Next development version ${next_dev}"
    git push
fi

if [[ "$skip_gradle" == "true" && "$dry_run" == "false" ]]; then
    echo "The Gradle plugin was not released. To release it later: ./release.sh ${version} --resume"
fi

printf '\033[92m========================================\n  Release %s completed successfully!\n========================================\033[0m\n' "${version}"
