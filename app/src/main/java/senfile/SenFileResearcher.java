package senfile;

import senfile.factories.SenFileFactory;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.SenMesh;
import senfile.statistics.GroupedContentsDumper;
import senfile.statistics.ValueOfInterestGetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SenFileResearcher {

    private static ValueOfInterestGetter valueOfInterest = setupValueOfInterest();

    private static final boolean VERBOSE_DUMP = true;

    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();

//        SenFile senFile = SenFileFactory.getMap(GameMap.WOOD);

        dumpOne();
//        dumpMany();

//        renderMesh("_1VONKEL");
//        renderMesh("FLOTTY");
//        renderMesh("BRITTA");
//        renderMesh("BERRY");
//        renderMesh("AFTONBLADET");

//        renderRandomMesh();

        long endTime = System.nanoTime();
        System.out.printf("\nCompleted in %.0fms\n", (endTime - startTime) / 1e6);
    }

    private static ValueOfInterestGetter setupValueOfInterest() {
        return new ValueOfInterestGetter(
                m -> null,
                c -> null,
                o -> null,
                mapi -> null,
                subo -> subo.transparency,
//                subo -> String.format("%02X %02X %02X %02X",
//                                      subo.rawBytes[4],
//                                      subo.rawBytes[5],
//                                      subo.rawBytes[6],
//                                      subo.rawBytes[7]),
                obji -> null
        );
    }

    private static void dumpOne() throws IOException {
        String filePath = Util.ROOT_DIR + "scene_ica/OBJECTS.SEN";
//        filePath = Util.ROOT_DIR + "menu/CHARACTERS.SEN";
        filePath = GameMap.WOOD.senFilePath;
//        filePath = Util.ROOT_DIR + "scene_aqua/AQUAMALL.SEN";

        List<SenFile> senFiles = getSingleSenFile(filePath);
        dump(senFiles);
    }

    private static void renderRandomMesh() throws IOException {
        List<SenFile> senFiles = getAllSenFiles();
        Random r = new Random();
        SenFile senFile = senFiles.get(r.nextInt(senFiles.size()));
        List<SenMesh> meshes = senFile.meshes;
        SenMesh mesh = meshes.get(r.nextInt(meshes.size()));

        System.out.println("Rendering mesh: " + mesh.name + " from " + senFile.title);
        RenderMesh.render(mesh, true);
    }

    private static void renderMesh(String nameToFind) throws IOException {

//        List<SenFile> senFiles = getAllSenFiles();
        List<SenFile> senFiles = getSingleSenFile(Util.ROOT_DIR + "scene_aqua/OBJECTS.SEN");

        Optional<SenMesh> meshOptional = senFiles
                .stream()
                .flatMap(senFile -> senFile.meshes.stream())
                .filter(mesh -> mesh.name.equals(nameToFind))
                .findAny();

        if (!meshOptional.isPresent()) {
            throw new IllegalArgumentException("Mesh " + nameToFind + " not found");
        }
        SenMesh mesh = meshOptional.get();

//        System.out.println("Vertex[] vertices = {");
//        for (Vertex vertex : mesh.getVertices()) {
//            System.out.print(vertex.toCodeString());
//            System.out.println(",");
//        }
//        System.out.println("};");

        SenFile firstSenFile = senFiles.get(0);
//        firstSenFile.getMeshes().forEach(m -> System.out.println(m.name));
        SuboElement suboElement = firstSenFile.subo.elements.get(0);

//        for (SuboElement.FaceInfo faceInfo : suboElement.faceInfos) {
//            byte[] vi = faceInfo.vertexIndices;
//            System.out.printf("%d, %d, %d, %d,\n", vi[0], vi[1], vi[2], vi[3]);
//        }

//        for (SuboElement.FaceInfo faceInfo : suboElement.faceInfos) {
//            short mapiIndex = faceInfo.restShorts[1];
//            System.out.printf("%d, ", mapiIndex);
//        }

//        for (int i = 0; i < 5; i++) {
//            MapiElement element = firstSenFile.getMapi().elements[i];
//            byte[] b = element.textureCoordBytes;
//            System.out.printf("%d, %d, %d, %d, %d, %d, %d, %d,\n",
//                              b[0] & 0xFF,
//                              b[1] & 0xFF,
//                              b[2] & 0xFF,
//                              b[3] & 0xFF,
//                              b[4] & 0xFF,
//                              b[5] & 0xFF,
//                              b[6] & 0xFF,
//                              b[7] & 0xFF);
//        }

        RenderMesh.render(mesh, true);
    }

    private static void dumpMany() throws IOException {
        List<SenFile> senFiles = getAllSenFiles();

        dump(senFiles);
//        for (SenFile senFile : senFiles) {
////            if (senFile.getMeshes().size() != senFile.getObji().elements.length) {
////                System.out.println(senFile.getTitle() + " --- " + senFile.getMeshes().size() + " --- " + senFile.getObji().elements.length);
////            }
//            System.out.println(senFile.getTitle() + " --- " + senFile.getMeshes().size() + " --- " + senFile.getSubo().elements.length);
//        }
    }

    private static List<SenFile> getAllSenFiles() throws IOException {
        List<String> senFilePaths = Util.pathToAllSenFilesInDirectory(Util.ROOT_DIR);
        List<SenFile> senFiles = new ArrayList<>(senFilePaths.size());
        for (String senFilePath : senFilePaths) {
            SenFile senFile = SenFileFactory.fromFile(senFilePath);
            senFiles.add(senFile);
        }
        return senFiles;
    }

    private static List<SenFile> getSingleSenFile(String filePath) throws IOException {
        List<SenFile> senFiles = new ArrayList<>();
        SenFile senFile = SenFileFactory.fromFile(filePath);
        senFiles.add(senFile);

        senFile.dumpInfo();
        return senFiles;
    }

    private static void dump(List<SenFile> senFiles) {
        if (VERBOSE_DUMP) {
            GroupedContentsDumper.dumpVerbose(senFiles, valueOfInterest);
        } else {
            GroupedContentsDumper.dump(senFiles, valueOfInterest);
        }
    }

}
