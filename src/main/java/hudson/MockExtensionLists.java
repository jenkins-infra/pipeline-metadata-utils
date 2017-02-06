package hudson;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.jenkinsci.infra.tools.HyperLocalPluginManager;

import static org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;
import hudson.model.Hudson;
import jenkins.model.Jenkins;

/**
 * A mocked way to get at {@link ExtensionList}s. In {@code hudson} package due to protected access in {@link ExtensionList}.
 */
public class MockExtensionLists {
    private static Map<String, ExtensionList> extensionLists = new HashMap<String, ExtensionList>();

    public ExtensionList getMockExtensionList(HyperLocalPluginManager hlpm, Jenkins hudson, Class type) {
        if(extensionLists.get(type.getName()) != null) {
            return extensionLists.get(type.getName());
        } else {
            MockExtensionList mockList = new MockExtensionList(hlpm, hudson, type);
            extensionLists.put(type.getName(), mockList.getMockExtensionList());
            return mockList.getMockExtensionList();
        }
    }

    private class MockExtensionList {
        ExtensionList mockExtensionList;

        public MockExtensionList(HyperLocalPluginManager hlpm, Jenkins hudson, Class type) {
            ExtensionList realList = ExtensionList.create(hudson, type);
            mockExtensionList = spy(realList);

            doReturn("Locking resources").when(mockExtensionList).getLoadLock();
            doAnswer(mockLoad(hlpm)).when(mockExtensionList).load();
        }

        private <T> Answer<List<ExtensionComponent<T>>> mockLoad(HyperLocalPluginManager hlpm) {
            return new Answer<List<ExtensionComponent<T>>>() {
                public List<ExtensionComponent<T>> answer(InvocationOnMock invocation) throws Throwable {
                    return hlpm.getPluginStrategy().findComponents(mockExtensionList.extensionType, (Hudson)null);
                }
            };
        }

        public ExtensionList getMockExtensionList(){
            return mockExtensionList;
        }
    }
}