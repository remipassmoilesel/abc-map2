package org.abcmap.core.project;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.abcmap.TestUtils;
import org.abcmap.core.project.dao.LayerIndexDAO;
import org.abcmap.core.project.layer.FeatureLayer;
import org.abcmap.core.utils.Utils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;

/**
 * Test project writer and reader
 */
public class ProjectReaderWriterTest {

    private static final GeometryFactory geom = JTSFactoryFinder.getGeometryFactory();

    @BeforeClass
    public static void beforeTests() throws IOException {
        TestUtils.mainManagerInit();
    }

    @Test
    public void tests() {

        try {
            Path tempDirectory = TestUtils.PLAYGROUND_DIRECTORY.resolve("projectIoTest");

            // clean previous directories
            if (Files.isDirectory(tempDirectory)) {
                Utils.deleteDirectories(tempDirectory.toFile());
            }
            Files.createDirectories(tempDirectory);

            //  directory where will be saved the new project
            Path newProjectTempDirectory = tempDirectory.resolve("newProjectDirectory");
            Files.createDirectories(newProjectTempDirectory);

            ProjectWriter writer = new ProjectWriter();
            ProjectReader reader = new ProjectReader();

            // test creation
            Project newProject = writer.createNew(newProjectTempDirectory);
            newProject.getMetadataContainer().updateValue(PMConstants.TITLE, "New title of the death");

            assertTrue("Creation test",
                    Files.isRegularFile(newProjectTempDirectory.resolve(ProjectWriter.PROJECT_TEMP_NAME + ".data.db")));

            // add styles
            for (int i = 1; i < 10; i++) {
                newProject.getStyle(TestUtils.getRandomColor(), TestUtils.getRandomColor(), i);
            }

            // layer index test
            newProject.addNewFeatureLayer("Second layer", true, 1);

            LayerIndexDAO lidao = new LayerIndexDAO(newProject.getDatabasePath());
            writer.writeMetadatas(newProject, newProject.getDatabasePath());
            assertTrue("AbstractLayer index test", lidao.readAllEntries().size() == 2);

            FeatureLayer l = (FeatureLayer) newProject.getLayers().get(0);
            for (int i = 0; i < 100; i++) {
                l.addShape(geom.createPoint(new Coordinate(i, i)));
            }

            // Writing test
            Path savedProject = tempDirectory.resolve("savedProject.abm");
            writer.write(newProject, savedProject);

            assertTrue("Writing test", Files.isRegularFile(savedProject));

            // Basic project equality test
            assertTrue("Basic project equality test", newProject.equals(newProject));

            // Basic read test. Project is read twice, and we check if projects are same
            Path openProjectTempDirectory1 = tempDirectory.resolve("openProjectTempDirectory1");
            Path openProjectTempDirectory2 = tempDirectory.resolve("openProjectTempDirectory2");
            Files.createDirectories(openProjectTempDirectory1);
            Files.createDirectories(openProjectTempDirectory2);

            Project p1 = reader.read(openProjectTempDirectory1, savedProject);
            Project p2 = reader.read(openProjectTempDirectory2, savedProject);

            assertTrue("Metadata reading test 1", newProject.getMetadataContainer().equals(p1.getMetadataContainer()));
            assertTrue("Metadata reading test 2", p1.getMetadataContainer().equals(p2.getMetadataContainer()));

            assertTrue("Layers reading test 1", newProject.getLayers().equals(p1.getLayers()));
            assertTrue("Layers reading test 2", p1.getLayers().equals(p2.getLayers()));

            assertTrue("Project reading test 1", newProject.equals(p1));
            assertTrue("Project reading test 2", p1.equals(p2));

            // re-test all after closing projects
            newProject.close();
            p1.close();
            p2.close();

            Path openProjectTempDirectory3 = tempDirectory.resolve("openProjectTempDirectory3");
            Path openProjectTempDirectory4 = tempDirectory.resolve("openProjectTempDirectory4");
            Files.createDirectories(openProjectTempDirectory3);
            Files.createDirectories(openProjectTempDirectory4);

            Project p3 = reader.read(openProjectTempDirectory3, savedProject);
            Project p4 = reader.read(openProjectTempDirectory4, savedProject);

            assertTrue("Metadata reading test 3", p1.getMetadataContainer().equals(p2.getMetadataContainer()));
            assertTrue("Layers reading test 3", p1.getLayers().equals(p2.getLayers()));
            assertTrue("Project reading test 3", p1.equals(p2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
