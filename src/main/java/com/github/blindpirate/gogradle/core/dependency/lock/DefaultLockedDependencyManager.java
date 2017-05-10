package com.github.blindpirate.gogradle.core.dependency.lock;

import com.github.blindpirate.gogradle.core.dependency.ResolvedDependency;
import com.github.blindpirate.gogradle.core.dependency.produce.ExternalDependencyFactory;
import com.github.blindpirate.gogradle.util.DataExchange;
import com.github.blindpirate.gogradle.util.IOUtils;
import org.gradle.api.Project;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.blindpirate.gogradle.core.GolangConfiguration.BUILD;
import static com.github.blindpirate.gogradle.core.GolangConfiguration.TEST;
import static com.github.blindpirate.gogradle.util.DataExchange.parseYaml;

@Singleton
public class DefaultLockedDependencyManager extends ExternalDependencyFactory implements LockedDependencyManager {

    public static final String WARNING = "# This file is generated by gogradle automatically, "
            + "you should NEVER modify it manually.\n";
    @Inject
    private Project project;

    private static final String LOCK_FILE = "gogradle.lock";

    @Override
    public void lock(Collection<? extends ResolvedDependency> flatBuildDependencies,
                     Collection<? extends ResolvedDependency> flatTestDependencies) {
        List<Map<String, Object>> buildNotations = toNotations(flatBuildDependencies);
        List<Map<String, Object>> testNotations = toNotations(flatTestDependencies);
        GogradleLockModel model = GogradleLockModel.of(buildNotations, testNotations);
        String content = DataExchange.toYaml(model);
        content = insertWarning(content);
        IOUtils.write(project.getRootDir(), LOCK_FILE, content);
    }

    private String insertWarning(String content) {
        return WARNING + content;
    }

    private List<Map<String, Object>> toNotations(Collection<? extends ResolvedDependency> flatDependencies) {
        List<Map<String, Object>> ret = flatDependencies.stream()
                .map(ResolvedDependency::toLockedNotation)
                .collect(Collectors.toList());
        ret.forEach(this::deactivateTransitive);
        return ret;
    }

    private void deactivateTransitive(Map<String, Object> map) {
        map.put("transitive", false);
    }

    @Override
    public String identityFileName() {
        return LOCK_FILE;
    }

    @Override
    protected List<Map<String, Object>> adapt(File file) {
        GogradleLockModel model = parseYaml(file, GogradleLockModel.class);
        return model.getDependencies(BUILD);
    }

    @Override
    protected List<Map<String, Object>> adaptTest(File file) {
        GogradleLockModel model = parseYaml(file, GogradleLockModel.class);
        return model.getDependencies(TEST);
    }
}
