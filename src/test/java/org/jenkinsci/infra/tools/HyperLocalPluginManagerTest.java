package org.jenkinsci.infra.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HyperLocalPluginManagerTest {
    private final static String pluginDir = HyperLocalPluginManagerTest.class.getResource("/git-plugin").getPath();
    private static HyperLocalPluginManagerInit starter = new HyperLocalPluginManagerInit();
    private static HyperLocalPluginManager pluginManager;
    private static List<StepDescriptor> steps;

    @BeforeAll
    public static void init() {
        pluginManager = starter.initializeHyperLocalPluginManager(pluginDir);
        steps = pluginManager.getPluginStrategy().findComponents(StepDescriptor.class);
    }

    @Test
    public void TotalStepsShouldBeFortySeven() {
        assertEquals(47, steps.size());
    }

    @Test
    public void isGitStepPresent() {
        boolean containsGit = false;
        for (StepDescriptor step : steps) {
            if (step.getFunctionName() == "git") {
                containsGit = true;
            }
        }
        assertTrue(containsGit);
    }

    @Test
    public void isUnarchiveStepAdvanced() {
        for (StepDescriptor step : steps) {
            if (step.getFunctionName() == "unarchive") {
                assertTrue(step.isAdvanced());
                return;
            }
        }
        fail("unarchive step not found");
    }

    @Test
    public void checkoutShouldBelongToWorkflowSCMStep() {
        for (StepDescriptor step : steps) {
            if (step.getFunctionName() == "checkout") {
                String pluginName = pluginManager.getPluginNameForDescriptor(step);
                assertEquals("workflow-scm-step", pluginName);
                return;
            }
        }
        fail("checkout step not found");
    }

    @Test
    public void LastMilestoneShouldBeCompletedInitialization() {
        assertEquals("Completed initialization", starter.lastMilestone.toString());
    }
}
