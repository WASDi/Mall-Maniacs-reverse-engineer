package senfile.statistics;

import senfile.SenFile;
import senfile.Util;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.ObjiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.Mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupedContentsDumper {

    public static void dump(List<SenFile> senFiles, ValueOfInterestGetter valueOfInterest) {
        Map<Object, List<FileMeshDesignator>> value2occurrences = mapValue2Occurrences(senFiles, valueOfInterest);
        List<ValueOccurrences> valueOccurrences = new ArrayList<>();
        for (Map.Entry<Object, List<FileMeshDesignator>> entry : value2occurrences.entrySet()) {
            Object value = entry.getKey();
            List<FileMeshDesignator> occurrences = entry.getValue();
            valueOccurrences.add(new ValueOccurrences(value, occurrences.size()));
        }


        try {
            valueOccurrences.sort(Comparator.comparingInt(a -> (Integer) a.value));
        } catch (ClassCastException ex) {
            try {
                valueOccurrences.sort(Comparator.comparingInt(a -> (Short) a.value));
            } catch (ClassCastException ex2) {
                Collections.sort(valueOccurrences);
            }
        }


        System.out.println("                  Value - Occurrences");
        for (ValueOccurrences valueOccurrence : valueOccurrences) {
            System.out.println(valueOccurrence);
        }

    }

    public static void dumpVerbose(List<SenFile> senFiles, ValueOfInterestGetter valueOfInterest) {
        Map<Object, List<FileMeshDesignator>> value2occurrences = mapValue2Occurrences(senFiles, valueOfInterest);

        for (Map.Entry<Object, List<FileMeshDesignator>> entry : value2occurrences.entrySet()) {
            Object value = entry.getKey();
            List<FileMeshDesignator> occurrences = entry.getValue();
            if (value instanceof Number) {
                System.out.printf("VALUE = %d (0x%X)\nOCCURRENCES = %d\n", value, value, occurrences.size());
            } else {
                System.out.printf("VALUE = %s\nOCCURRENCES = %d\n", value.toString(), occurrences.size());
            }

            Set<String> uniqueFiles = new HashSet<>();
            Set<String> uniqueMeshNames = new HashSet<>();
            Set<FileMeshDesignator> uniqueDesignators = new HashSet<>();

            for (FileMeshDesignator designator : occurrences) {
                uniqueFiles.add(designator.file);
                uniqueMeshNames.add(designator.mesh);
                uniqueDesignators.add(designator);
            }

//            System.out.println("... for files ");
//            List<String> sortedUniqueFiles = new ArrayList<>(uniqueFiles);
//            sortedUniqueFiles.sort(String::compareTo);
//            for (String uniqueFile : sortedUniqueFiles) {
//                System.out.println(uniqueFile);
//            }

//            System.out.println("... for meshes ");
//            List<String> sortedUniqueMeshes = new ArrayList<>(uniqueMeshNames);
//            sortedUniqueMeshes.sort(String::compareTo);
//            for (String uniqueMesh : sortedUniqueMeshes) {
//                System.out.println(uniqueMesh);
//            }

            System.out.println("... for designators ");
            List<FileMeshDesignator> sortedUniqueDesignators = new ArrayList<>(uniqueDesignators);
            sortedUniqueDesignators.sort(FileMeshDesignator::compareTo);
            for (FileMeshDesignator uniqueDesignator : sortedUniqueDesignators) {
                System.out.println(uniqueDesignator);
            }

            System.out.println("\n-----\n");
        }


    }

    private static Map<Object, List<FileMeshDesignator>> mapValue2Occurrences(List<SenFile> senFiles,
                                                                              ValueOfInterestGetter valueOfInterestGetter) {
        Map<Object, List<FileMeshDesignator>> value2occurrences = new HashMap<>();
        for (SenFile senFile : senFiles) {
            for (Mesh mesh : senFile.getMeshes()) {
                if (Util.IGNORE_UNDERLINES && mesh.name.charAt(0) == '_') {
                    continue;
                }
                Object valueOfInterest = valueOfInterestGetter.getFor(mesh);
                if (valueOfInterest == null) {
                    continue;
                }
                FileMeshDesignator designator = new FileMeshDesignator(senFile.getTitle(), mesh.name);
                List<FileMeshDesignator> occurrencesForValue = value2occurrences.computeIfAbsent(valueOfInterest, x -> new ArrayList<>());
                occurrencesForValue.add(designator);
            }
            for (MapiElement mapiElement : senFile.getMapi().elements) {
                Object valueOfInterest = valueOfInterestGetter.forMapi.apply(mapiElement);
                if (valueOfInterest == null) {
                    continue;
                }
                FileMeshDesignator designator = new FileMeshDesignator(senFile.getTitle(), "_MAPI_");
                List<FileMeshDesignator> occurrencesForValue = value2occurrences.computeIfAbsent(valueOfInterest, x -> new ArrayList<>());
                occurrencesForValue.add(designator);
            }
            for (SuboElement element : senFile.getSubo().elements) {
                Object valueOfInterest;
                try {
                    valueOfInterest = valueOfInterestGetter.forSubo.apply(element);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    continue;
                }
                if (valueOfInterest == null) {
                    continue;
                }
                FileMeshDesignator designator = new FileMeshDesignator(senFile.getTitle(), "_SUBO_");
                List<FileMeshDesignator> occurrencesForValue = value2occurrences.computeIfAbsent(valueOfInterest, x -> new ArrayList<>());
                occurrencesForValue.add(designator);
            }
            for (ObjiElement element : senFile.getObji().elements) {
                if (Util.IGNORE_UNDERLINES && element.nameOfMesh.charAt(0) == '_') {
                    continue;
                }
                Object valueOfInterest = valueOfInterestGetter.forObji.apply(element);
                if (valueOfInterest == null) {
                    continue;
                }
                FileMeshDesignator designator = new FileMeshDesignator(senFile.getTitle(), element.nameOfMesh);
                List<FileMeshDesignator> occurrencesForValue = value2occurrences.computeIfAbsent(valueOfInterest, x -> new ArrayList<>());
                occurrencesForValue.add(designator);
            }
        }
        return value2occurrences;
    }

}
