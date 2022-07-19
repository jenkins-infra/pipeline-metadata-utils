package org.jenkinsci.infra.tools;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hudson.reactor.Reactor;
import org.jvnet.hudson.reactor.ReactorException;
import org.jvnet.hudson.reactor.Task;
import org.jvnet.hudson.reactor.TaskBuilder;

import hudson.MockJenkins;
import hudson.init.InitMilestone;
import hudson.init.InitStrategy;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.InitReactorRunner;
import jenkins.model.Jenkins;

/**
 * Initializes a HyperLocalPluginManager instance, so that it can be used for testing.
 */
public class HyperLocalPluginManagerInit {
    private static final Logger LOG = Logger.getLogger(HyperLocalPluginManagerInit.class.getName());
    public InitMilestone lastMilestone;
    public HyperLocalPluginManager pluginManager;

    public HyperLocalPluginManager initializeHyperLocalPluginManager(String pluginDir) {
        try {
            pluginManager = new HyperLocalPluginManager(pluginDir, false);

            // Set up mocks
            Jenkins.JenkinsHolder mockJenkinsHolder = mock(Jenkins.JenkinsHolder.class);
            MockJenkins mJ = new MockJenkins();
            Jenkins mockJenkins = mJ.getMockJenkins(pluginManager);
            when(mockJenkinsHolder.getInstance()).thenReturn(mockJenkins);

            java.lang.reflect.Field jenkinsHolderField = Jenkins.class.getDeclaredField("HOLDER");
            jenkinsHolderField.setAccessible(true);
            jenkinsHolderField.set(null, mockJenkinsHolder);

            InitStrategy initStrategy = new InitStrategy();
            executeReactor(initStrategy, pluginManager.diagramPlugins(initStrategy));

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Plugin Manager failed to initialize", ex);
        }
        return pluginManager;
    }

    /**
     * Executes a reactor.
     *
     * @param is
     *           If non-null, this can be consulted for ignoring some tasks. Only
     *           used during the initialization of Jenkins.
     */
    private void executeReactor(final InitStrategy is, TaskBuilder... builders)
            throws IOException, InterruptedException, ReactorException {
        Reactor reactor = new Reactor(builders) {
            /**
             * Sets the thread name to the task for better diagnostics.
             */
            @Override
            protected void runTask(Task task) throws Exception {
                if (is != null && is.skipInitTask(task))
                    return;

                String taskName = task.getDisplayName();

                Thread t = Thread.currentThread();
                String name = t.getName();

                try (ACLContext context = ACL.as2(ACL.SYSTEM2)) { // full access in the initialization thread
                    if (taskName != null) {
                        t.setName(taskName);
                    }
                    super.runTask(task);
                } finally {
                    t.setName(name);
                }
            }
        };

        new InitReactorRunner() {
            @Override
            protected void onInitMilestoneAttained(InitMilestone milestone) {
                lastMilestone = milestone;
            }
        }.run(reactor);
    }
}