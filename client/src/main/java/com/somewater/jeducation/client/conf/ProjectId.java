package com.somewater.jeducation.client.conf;

import com.somewater.jeducation.core.util.StringUtil;

import java.nio.file.Path;
import java.util.Optional;

public class ProjectId {

    private final String projectName;

    public ProjectId(Path projectDir, Optional<String> projectName) {
        this.projectName = projectName.filter(n -> !n.isEmpty()).orElseGet(() -> {
            String dirName = projectDir.getName(projectDir.getNameCount() - 1).getFileName().toString();
            if (projectDir.getNameCount() > 1) {
                String prevDirName = projectDir.getName(projectDir.getNameCount() - 2).getFileName().toString();
                if (prevDirName.startsWith("week")) {
                    try {
                        int weekNum = Integer.parseInt(prevDirName.substring("week".length()));
                        dirName = "week" + weekNum + "_" + dirName;
                    } catch (NumberFormatException e) { }
                }
            }
            dirName = StringUtil.removeUnsupportedPathSymbols(dirName);
            return dirName;
        });
        if (this.projectName.isEmpty()) {
            throw new AssertionError("Empty or unsupported project name");
        }
    }

    public String getName() {
        return projectName;
    }
}
