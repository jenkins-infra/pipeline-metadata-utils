package org.jenkinsci.pipeline_steps_doc_generator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.structs.describable.DescribableParameter;
import org.jenkinsci.plugins.structs.describable.HeterogeneousObjectType;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jvnet.hudson.reactor.Reactor;
import org.jvnet.hudson.reactor.ReactorException;
import org.jvnet.hudson.reactor.Task;
import org.jvnet.hudson.reactor.TaskBuilder;
import org.jenkinsci.infra.tools.HyperLocalPluginManager;

import hudson.MockJenkins;
import hudson.init.InitMilestone;
import hudson.init.InitStrategy;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.InitReactorRunner;
import jenkins.model.Jenkins;

/**
 * Process and find all the Pipeline steps definied in Jenkins plugins.
 */
public class PipelineStepExtractor {
    private static final Logger LOG = Logger.getLogger(PipelineStepExtractor.class.getName());
    public InitMilestone lastMilestone;
    public HyperLocalPluginManager pluginManager;

    public Map<String, Map<String, List<QuasiDescriptor>>> findSteps(String pluginDir) {
        Map<String, Map<String, List<QuasiDescriptor>>> completeListing = new HashMap<>();
        try {
            // setup
            if (pluginDir == null) {
                pluginManager = new HyperLocalPluginManager(false);
            } else {
                pluginManager = new HyperLocalPluginManager(pluginDir, false);
            }

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
            List<StepDescriptor> steps = pluginManager.getPluginStrategy().findComponents(StepDescriptor.class);

            // gather current and depricated steps
            Map<String, List<QuasiDescriptor>> required = processSteps(false, steps);
            Map<String, List<QuasiDescriptor>> optional = processSteps(true, steps);

            for (String req : required.keySet()) {
                Map<String, List<QuasiDescriptor>> newList = new HashMap<>();
                newList.put("Steps", required.get(req));
                completeListing.put(req, newList);
            }
            for (String opt : optional.keySet()) {
                Map<String, List<QuasiDescriptor>> exists = completeListing.get(opt);
                if (exists == null) {
                    exists = new HashMap<>();
                }
                exists.put("Advanced/Deprecated Steps", optional.get(opt));
                completeListing.put(opt, exists);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Step generation failed", ex);
        }

        return completeListing;
    }

    private Map<String, List<QuasiDescriptor>> processSteps(boolean optional, List<StepDescriptor> steps) {
        Map<String, List<QuasiDescriptor>> required = new HashMap<>();
        for (StepDescriptor d : getStepDescriptors(optional, steps)) {
            String pluginName = pluginManager.getPluginNameForDescriptor(d);
            required.computeIfAbsent(pluginName, k -> new ArrayList<>())
                    .add(new QuasiDescriptor(d, null));
            getMetaDelegates(d).forEach(delegateDescriptor -> {
                String nestedPluginName = pluginManager.getPluginNameForDescriptor(delegateDescriptor);
                required.computeIfAbsent(nestedPluginName, k -> new ArrayList<>())
                        .add(new QuasiDescriptor(delegateDescriptor, d));
            }); // TODO currently not handling metasteps with other parameters, either required
                // or (like GenericSCMStep) not
        }
        return required;
    }

    protected static Stream<Descriptor<?>> getMetaDelegates(Descriptor<?> d) {
        if (d instanceof StepDescriptor && ((StepDescriptor) d).isMetaStep()) {
            DescribableModel<?> m = DescribableModel.of(d.clazz);
            Collection<DescribableParameter> parameters = m.getParameters();
            if (parameters.size() == 1) {
                DescribableParameter delegate = parameters.iterator().next();
                if (delegate.isRequired()) {
                    if (delegate.getType() instanceof HeterogeneousObjectType) {
                        return ((HeterogeneousObjectType) delegate.getType()).getTypes()
                                .values().stream().map(PipelineStepExtractor::getDescriptor);
                    }
                }
            }
        }
        return Stream.empty();
    }

    private static Descriptor<?> getDescriptor(DescribableModel<?> delegateOptionSchema) {
        Class<?> delegateOptionType = delegateOptionSchema.getType();
        return Jenkins.get().getDescriptor(delegateOptionType.asSubclass(Describable.class));
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

    public Collection<? extends StepDescriptor> getStepDescriptors(boolean advanced, List<StepDescriptor> all) {
        TreeSet<StepDescriptor> t = new TreeSet<>(new StepDescriptorComparator());
        for (StepDescriptor d : all) {
            if (d.isAdvanced() == advanced) {
                t.add(d);
            }
        }
        return t;
    }

    private static class StepDescriptorComparator implements Comparator<StepDescriptor>, Serializable {
        @Override
        public int compare(StepDescriptor o1, StepDescriptor o2) {
            return o1.getFunctionName().compareTo(o2.getFunctionName());
        }

        private static final long serialVersionUID = 1L;
    }
}