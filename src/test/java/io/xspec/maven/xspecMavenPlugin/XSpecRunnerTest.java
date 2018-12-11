/**
 * Copyright © 2018, Christophe Marchand, XSpec organization
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.xspec.maven.xspecMavenPlugin;

import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.XdmNode;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;
import static org.junit.Assert.*;
import top.marchand.maven.saxon.utils.SaxonOptions;

/**
 *
 * @author cmarchand
 */
public class XSpecRunnerTest {
    
    private static File baseDirectory;
    private static final Log LOG = new SystemStreamLog();
    
    public static File getBaseDirectory() throws URISyntaxException {
        if(baseDirectory==null) {
            baseDirectory = new File(XSpecRunner.class.getClassLoader().getResource("").toURI()).getParentFile().getParentFile();
            baseDirectory = new File(baseDirectory, "target/surefire-reports/tests");
            baseDirectory.mkdirs();
        }
        return baseDirectory;
    }
    
    @Test
    public void extractCssResourceTest() throws Exception {
        XSpecRunner runner = new XSpecRunner(LOG, getBaseDirectory());
        runner.setResources(
                new DefaultXSpecImplResources(), 
                new DefaultSchematronImplResources(), 
                new DefaultXSpecPluginResources());
        RunnerOptions options = new RunnerOptions(getBaseDirectory());
        runner.setEnvironment(
                new Properties(), options);
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        runner.setCatalogWriterExtender(new TestCatalogWriterExtender(classesUri));

        runner.init(new SaxonOptions());
        runner.extractCssResource();
        File expectedFile = new File(getBaseDirectory(), "target/xspec-reports/resources/test-report.css");
        assertTrue(expectedFile.getAbsolutePath()+" does not exists", expectedFile.exists());
    }
    
    @Test(expected = IllegalStateException.class)
    public void initBeforeSetResources() throws Exception {
        XSpecRunner runner = new XSpecRunner(LOG, getBaseDirectory());
        runner.init(new SaxonOptions());
        fail("calling init(SaxonOption) before setResources(...) should throw an IllegalStateException");
    }
    
    @Test(expected = IllegalStateException.class)
    public void initTwiceTest() throws Exception {
        XSpecRunner runner = new XSpecRunner(LOG, getBaseDirectory());
        runner.setResources(
                new DefaultXSpecImplResources(), 
                new DefaultSchematronImplResources(), 
                new DefaultXSpecPluginResources());
        SaxonOptions saxonOptions = new SaxonOptions();
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        runner.setCatalogWriterExtender(new TestCatalogWriterExtender(classesUri));

        runner.init(saxonOptions);
        LOG.debug("calling runner.init a second time");
        runner.init(saxonOptions);
        fail("init shouldn't be call twice without throwing an IllegalStateException");
    }

    @Test
    public void processXsltXspecTest() throws Exception {
        XSpecRunner runner = new XSpecRunner(LOG, getBaseDirectory());
        runner.setResources(
                new DefaultXSpecImplResources(), 
                new DefaultSchematronImplResources(), 
                new DefaultXSpecPluginResources());
        SaxonOptions saxonOptions = new SaxonOptions();
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        runner.setCatalogWriterExtender(new TestCatalogWriterExtender(classesUri));
        runner.init(saxonOptions);
        File xspecFile = new File(getBaseDirectory().getParentFile().getParentFile().getParentFile(), "src/test/resources/filesToTest/xsltTestCase/xsl1.xspec");
        XdmNode node = runner.getXmlStuff().getDocumentBuilder().build(xspecFile);
        assertNotNull("node is null", node);
        assertNotNull("node baseUri is null", node.getBaseURI());
        runner.initProcessedFiles(1);
        boolean ret = runner.processXsltXSpec(node);
        assertTrue("XSpec failed", ret);
    }
}