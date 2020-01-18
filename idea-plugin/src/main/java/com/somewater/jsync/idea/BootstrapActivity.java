package com.somewater.jsync.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.Messages;
import com.somewater.jsync.core.conf.HostPort;
import org.jetbrains.annotations.NotNull;

public class BootstrapActivity extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        JSyncConfService confService = ServiceManager.getService(JSyncConfService.class);
        if (confService.getConf().getServerHost().isPresent()) {
            confService.setHostPort(new HostPort(confService.getConf().getServerHost().get(),
                    confService.getConf().getServerPort()));
            confService.setInited();
            startPlugin();
        } else {
            try {
                HostPort hostPort = new com.somewater.jsync.core.network.FindServer().find();
                confService.setHostPort(hostPort);
                confService.setInited();
                startPlugin();
            } catch (RuntimeException ex) {
                Messages.showInfoMessage("JSync service not found", "ERROR");
                return;
            }
        };
    }

    private void startPlugin() {
        IController listener = ApplicationManager.getApplication().getComponent(IController.class);
        listener.start();
    }
}
