package jscover.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

import static jscover.maven.TestType.*;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileMojoTest {
    private FileMojo mojo = new FileMojo();

    protected File getFilePath(String pathname) {
        if (System.getProperty("user.dir").endsWith("JSCover-maven-plugin"))
            pathname = "plugin-parent/file-system/" + pathname;
        return new File(pathname).getAbsoluteFile();
    }

    @Before
    public void setUp() throws Exception {
        deleteDirectory(getFilePath("../data/target"));
        ReflectionUtils.setVariableValueInObject(mojo, "srcDir", getFilePath("../data/src"));
        ReflectionUtils.setVariableValueInObject(mojo, "testDirectory", getFilePath("../data/src/test/javascript"));
        ReflectionUtils.setVariableValueInObject(mojo, "reportDir", getFilePath("../data/target"));
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "jasmine-code-*pass.html");
        ReflectionUtils.setVariableValueInObject(mojo, "instrumentPathArgs", Arrays.asList("--no-instrument=main/webapp/js/vendor/", "--no-instrument=test"));
        ReflectionUtils.setVariableValueInObject(mojo, "excludeArgs", Arrays.asList("--exclude=main/java", "--exclude=main/resources", "--exclude-reg=test/java$"));
        ReflectionUtils.setVariableValueInObject(mojo, "testType", Jasmine);
        ReflectionUtils.setVariableValueInObject(mojo, "lineCoverageMinimum", 100);
        ReflectionUtils.setVariableValueInObject(mojo, "branchCoverageMinimum", 100);
        ReflectionUtils.setVariableValueInObject(mojo, "functionCoverageMinimum", 100);
        String webDriverClass = System.getProperty("webDriverClass");
        if (webDriverClass != null)
          ReflectionUtils.setVariableValueInObject(mojo, "webDriverClassName", webDriverClass);
        //ReflectionUtils.setVariableValueInObject(mojo, "webDriverClassName", "org.openqa.selenium.firefox.FirefoxDriver");
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists())
            return;
        for (File file : dir.listFiles())
            if (file.isFile())
                file.delete();
            else
                deleteDirectory(file);
        dir.delete();
    }

    @Test
    public void shouldFailIfTestDirectoryNotSubDirectory() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testDirectory", getFilePath("../data/target"));
        try {
            mojo.execute();
            fail("Should have thrown exception");
        } catch(MojoExecutionException e) {
            String regex = "'testDirectory' '.*/data/target' should be a sub-directory of 'srcDir' '.*/data/src'";
            if (File.separatorChar == '\\')
                regex = regex.replaceAll("/", "\\\\\\\\");
            Pattern pattern = Pattern.compile(regex);
            assertThat(String.format("Message was: '%s'", e.getMessage()), pattern.matcher(e.getMessage()).matches(), is(true));
        }
    }

    @Test
    public void shouldPassJasmine() throws Exception {
        mojo.execute();
        assertThat(new File(getFilePath("../data/target"), "jscoverage.json").exists(), equalTo(true));
        assertThat(new File(getFilePath("../data/target"), "jscover.lcov").exists(), equalTo(false));
        assertThat(new File(getFilePath("../data/target"), "cobertura.xml").exists(), equalTo(false));
    }

    @Test
    public void shouldPassJasmineWithoutLocalStorage() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "localStorage", false);
        mojo.execute();
    }

    @Test
    public void shouldGenerateLCOVReport() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "reportLCOV", true);
        mojo.execute();

        assertThat(new File(getFilePath("../data/target"), "jscover.lcov").exists(), equalTo(true));
    }

    @Test
    public void shouldGenerateCoberturaXML() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "reportCoberturaXML", true);
        mojo.execute();

        assertThat(new File(getFilePath("../data/target"), "cobertura-coverage.xml").exists(), equalTo(true));
    }

    @Test
    public void shouldFailIfExcludePathArgumentInvalid() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "excludeArgs", Arrays.asList("exclude=main"));
        try {
            mojo.execute();
            fail("Should have thrown exception");
        } catch(MojoExecutionException e) {
            assertThat(e.getMessage(), equalTo("Invalid exclude argument 'exclude=main'"));
        }
    }

    @Test
    public void shouldFailJasmineIfLineCoverageTooLow() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "lineCoverageMinimum", 101);
        try {
            mojo.execute();
            fail("Should have thrown exception");
        } catch(MojoFailureException e) {
            assertThat(e.getMessage(), equalTo("Line coverage 100 less than 101"));
        }
    }

    @Test
    public void shouldFailJasmineIfBranchCoverageTooLow() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "branchCoverageMinimum", 101);
        try {
            mojo.execute();
            fail("Should have thrown exception");
        } catch(MojoFailureException e) {
            assertThat(e.getMessage(), equalTo("Branch coverage 100 less than 101"));
        }
    }

    @Test
    public void shouldFailJasmineIfFunctionCoverageTooLow() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "functionCoverageMinimum", 101);
        try {
            mojo.execute();
            fail("Should have thrown exception");
        } catch(MojoFailureException e) {
            assertThat(e.getMessage(), equalTo("Function coverage 100 less than 101"));
        }
    }

    @Test
    public void shouldPassQUnit() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "qunit-*pass.html");
        ReflectionUtils.setVariableValueInObject(mojo, "testType", QUnit);
        mojo.execute();
    }

    @Test(expected = MojoFailureException.class)
    public void shouldFailQUnit() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "qunit-*fail.html");
        ReflectionUtils.setVariableValueInObject(mojo, "testType", QUnit);
        mojo.execute();
    }

    @Test(expected = MojoFailureException.class)
    public void shouldFailJasmine() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "jasmine-*fail.html");
        ReflectionUtils.setVariableValueInObject(mojo, "testType", Jasmine);
        mojo.execute();
    }
}