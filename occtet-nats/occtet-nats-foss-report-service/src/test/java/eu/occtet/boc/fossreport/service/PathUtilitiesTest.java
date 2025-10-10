package eu.occtet.boc.fossreport.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PathUtilitiesTest {

    @Test
    void testCleanAndSplit() {
        String input = "react-dom/LICENSE_x000D_\n react-dom/README.md\n react-dom/package.json\n" +
                "chromium/third_party/android_protobuf/OWNERS_x000D_";

        List<String> expected = List.of(
                "react-dom/LICENSE",
                "react-dom/README.md",
                "react-dom/package.json",
                "chromium/third_party/android_protobuf/OWNERS"
        );

        List<String> actual = PathUtilities.cleanAndSplits(input);

        assertEquals(expected, actual);
    }
}
