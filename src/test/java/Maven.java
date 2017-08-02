

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.landawn.abacus.util.FileFilter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.PropertiesUtil;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.stream.Stream;

public class Maven {
    @Test
    public void test_name() throws Exception {
        final String sourceVersion = "0.8";
        final String targetVersion = PropertiesUtil.load(new File("./build.properties")).get("version");
        final String commonMavenPath = "C:/Users/haiyangl/landawn/Abacus/maven/";
        final String sourcePath = commonMavenPath + sourceVersion;
        final String targetPath = commonMavenPath + targetVersion;
        final File sourceDir = new File(sourcePath);
        final File targetDir = new File(targetPath);

        IOUtil.deleteAllIfExists(targetDir);

        targetDir.mkdir();

        IOUtil.copy(sourceDir, targetDir);

        Stream.of(IOUtil.listFiles(targetDir, true, new FileFilter() {
            @Override
            public boolean accept(File parentDir, File file) {
                // TODO Auto-generated method stub
                return file.getName().endsWith(".asc") || file.getName().endsWith("bundle.jar");
            }
        })).forEach(new Consumer<File>() {
            @Override
            public void accept(File t) {
                t.delete();
            }
        });

        Stream.of(IOUtil.listFiles(targetDir)).forEach(new Consumer<File>() {
            @Override
            public void accept(File file) {
                IOUtil.renameTo(file, file.getName().replace(sourceVersion, targetVersion));
            }
        });

        Stream.of(IOUtil.listFiles(targetDir, true, new FileFilter() {
            @Override
            public boolean accept(File parentDir, File file) {
                // TODO Auto-generated method stub
                return file.getName().endsWith(".pom") || file.getName().endsWith(".xml") || file.getName().endsWith(".txt");
            }
        })).forEach(new Consumer<File>() {
            @Override
            public void accept(File t) {
                final List<String> lines = IOUtil.readLines(t);
                final List<String> newLines = new ArrayList<>(lines.size());
                for (String line : lines) {
                    newLines.add(line.replaceAll(sourceVersion, targetVersion));
                }
                IOUtil.writeLines(t, newLines);
            }
        });
    }
}
