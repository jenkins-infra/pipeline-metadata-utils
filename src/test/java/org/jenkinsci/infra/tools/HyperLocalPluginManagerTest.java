package org.jenkinsci.infra.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jenkinsci.pipeline_steps_doc_generator.PipelineStepExtractor;
import org.jenkinsci.pipeline_steps_doc_generator.QuasiDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hudson.PluginWrapper;

public class HyperLocalPluginManagerTest {
    private final static String pluginDir = HyperLocalPluginManagerTest.class.getResource("/git-plugin").getPath();
    private static PipelineStepExtractor pse = new PipelineStepExtractor();
    private static Map<String, Map<String, List<QuasiDescriptor>>> steps;
    private static final Logger LOG = Logger.getLogger(PipelineStepExtractor.class.getName());
    private static int count = 0;

    @BeforeAll
    public static void init() {
        steps = pse.findSteps(pluginDir);
        for (String plugin : steps.keySet()) {
            LOG.info("processing " + plugin);
            List<QuasiDescriptor> byPlugin = steps.get(plugin).get("Steps");
            // PluginWrapper thePlugin = pse.pluginManager.getPlugin(plugin);
            // String displayName = thePlugin == null ? "Jenkins Core" :
            // thePlugin.getDisplayName();
            // LOG.info(displayName);
            // for (QuasiDescriptor qd : byPlugin) {
            // LOG.info(qd.getSymbol());
            // count++;
            // }
            count += byPlugin.size();
        }
        LOG.info("Total " + count + " steps found in " + steps.size() + " plugins");
    }

    @Test
    public void areTotalStepsFortySeven() {
        assertEquals(47, count);
    }

    @Test
    public void isLastMilestoneCompletedInitialization() {
        assertEquals("Completed initialization", pse.lastMilestone.toString());
    }
}
