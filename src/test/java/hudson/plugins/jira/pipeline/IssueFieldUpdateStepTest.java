package hudson.plugins.jira.pipeline;

import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.PrintStream;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by aleksandr on 04.07.16.
 */
public class IssueFieldUpdateStepTest {
    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject
    IssueFieldUpdateStep.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void configRoundTrip() throws Exception {
        configRoundTrip("EXAMPLE-1", "my-field", "Field Value");
    }

    private void configRoundTrip(String issueKey, String fieldName, String fieldValue) throws Exception {
        IssueFieldUpdateStep configRoundTrip = new StepConfigTester(jenkinsRule)
                .configRoundTrip(new IssueFieldUpdateStep(issueKey, fieldName, fieldValue));

        assertEquals(issueKey, configRoundTrip.getIssueKey());
        assertEquals(fieldName, configRoundTrip.getFieldName());
        assertEquals(fieldValue, configRoundTrip.getFieldValue());
    }

    @Test
    public void testUpdateIssueFieldByKey() throws Exception {
        JiraSession session = mock(JiraSession.class);
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        final String issueKey = "EXAMPLE-1";
        final String fieldName = "my-field";
        final String fieldValue = "Field Value";

        final List<Object> assertCalledParams = new ArrayList<Object>();

        Mockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String issueKeyFromArgs = invocation.getArgumentAt(0, String.class);
                String fieldNameFromArgs = invocation.getArgumentAt(1, String.class);
                String fieldValueFromArgs = invocation.getArgumentAt(2, String.class);
                System.out.println("issueKey: " + issueKeyFromArgs);
                System.out.println("fieldName: " + fieldNameFromArgs);
                System.out.println("fieldValue: " + fieldValueFromArgs);
                assertThat(issueKeyFromArgs, equalTo(issueKey));
                assertThat(fieldNameFromArgs, equalTo(fieldName));
                assertThat(fieldValueFromArgs, equalTo(fieldValue));
                assertCalledParams.addAll(Arrays.asList(invocation.getArguments()));
                return null;
            }
        }).when(session).updateIssueFieldValue(
                Mockito.<String> anyObject(),
                Mockito.<String> anyObject(),
                Mockito.<String> anyObject()
        );

        Run mockRun = mock(Run.class);
        Job mockJob = mock(Job.class);
        when(mockRun.getParent()).thenReturn(mockJob);

        TaskListener mockTaskListener = mock(TaskListener.class);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(mockJob.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        Map<String, Object> r = new HashMap<String, Object>();
        r.put("issueKey", issueKey);
        r.put("fieldName", fieldName);
        r.put("fieldValue", fieldValue);
        IssueFieldUpdateStep step = (IssueFieldUpdateStep) descriptor.newInstance(r);

        StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(Run.class)).thenReturn(mockRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        assertThat(assertCalledParams, hasSize(0));

        IssueFieldUpdateStep.StepExecution start = (IssueFieldUpdateStep.StepExecution) step.start(ctx);
        start.run();

        assertThat(assertCalledParams, hasSize(3));
    }
}