/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 */
package redhat.che.functional.tests;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.redhat.arquillian.che.annotations.Workspace;
import com.redhat.arquillian.che.resource.Stack;

import redhat.che.functional.tests.fragments.window.AskForValueDialog;

@RunWith(Arquillian.class)
@Workspace(stackID = Stack.VERTX)
public class AnalyticsErrorMarkersTestCase extends AbstractCheFunctionalTest {
    private static final Logger LOG = Logger.getLogger(AnalyticsErrorMarkersTestCase.class);

    @FindBy(id = "gwt-debug-askValueDialog-window")
    private AskForValueDialog askForValueDialog;

    @FindBy(className = "currentLine")
    private WebElement currentLine;

    private static final String pomDependency =
                      "<dependency>\n"
                    + "<groupId>ch.qos.logback</groupId>\n"
                    + "<artifactId>logback-core</artifactId>\n"
                    + "<version>1.1.10</version>\n"
                    + "</dependency>\n";
    private static final String pomExpectedError = "Package ch.qos.logback:logback-core-1.1.10 is vulnerable: CVE-2017-5929";
    private static final Integer pomExpectedErrorLine = 40;
    private static final Integer pomInjectionEntryPoint = 37;

    @Before
    public void importProject() {
        LOG.info("Starting: " + this.getClass().getName());
        openBrowser();
    }

    @After
    public void deleteDependency() {
        editorPart.codeEditor().hideErrors(pomExpectedErrorLine);
        editorPart.codeEditor().setCursorToLine(pomInjectionEntryPoint);
        editorPart.codeEditor().deleteNextLines(5);
        editorPart.codeEditor().waitUnitlPomDependencyIsNotVisible();
        editorPart.tabsPanel().waintUntilFocusedTabSaves();
    }

    @Test
    public void bayesianErrorShownOnOpenFile(){
        //creating invalid dependency
        openPomXml();
        editorPart.codeEditor().setCursorToLine(pomInjectionEntryPoint);
        editorPart.codeEditor().writeDependency(pomDependency);

        boolean annotationFound = editorPart.codeEditor().verifyAnnotationErrorIsPresent(pomExpectedError, pomExpectedErrorLine);
        if (isProdPreview()) {
            Assert.assertFalse(bayesianErrorNotVisible, annotationFound);
        } else {
            Assert.assertTrue("Annotation error is not visible.", annotationFound);
        }

        //checking if error marker is visible after re-opening the file
        editorPart.tabsPanel().closeActiveTab(driver);

        openPomXml();
        editorPart.codeEditor().setCursorToLine(pomInjectionEntryPoint);

        annotationFound = editorPart.codeEditor().verifyAnnotationErrorIsPresent(pomExpectedError, pomExpectedErrorLine);
        if (isProdPreview()) {
            Assert.assertFalse(bayesianErrorNotVisible, annotationFound);
        } else {
            Assert.assertTrue("Annotation error is not visible.", annotationFound);
        }
    }

	private void openPomXml() {
		vertxProject.getResource("pom.xml").open();
		Graphene.waitGui().withTimeout(10, TimeUnit.SECONDS).until().element(currentLine).is().visible();
	}

}
