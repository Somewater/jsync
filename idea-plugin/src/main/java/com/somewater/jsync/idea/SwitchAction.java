package com.somewater.jsync.idea;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class SwitchAction extends AnAction {
    @Override 
    public void update(@NotNull AnActionEvent e) {
        setTextAccordingToState(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        IController controller = ApplicationManager.getApplication().getComponent(IController.class);
        if (controller.isEnabled()) {
            controller.stop();
        } else {
            controller.start();
        }
        setTextAccordingToState(e);
    }

    private void setTextAccordingToState(AnActionEvent e) {
        IController controller = ApplicationManager.getApplication().getComponent(IController.class);
        if (controller.isEnabled()) {
            e.getPresentation().setText("Enable Jsync Integration");
        } else {
            e.getPresentation().setText("Disable Jsync Integration");
        }
    }
}
