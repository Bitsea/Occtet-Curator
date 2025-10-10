package eu.occtet.boc.fossreport.service;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class FossReportUtilitiesTest {

    private static final Logger log = LoggerFactory.getLogger(FossReportUtilitiesTest.class);

    @Test
    void separateCombinedLicensesSplitsCorrectly() {
        String combinedLicenses = "COMBINED License1 AND License2 OR License3";
        List<String> result = FossReportUtilities.separateCombinedLicenses(combinedLicenses);
        assertEquals(3, result.size());
        assertTrue(result.contains("License1"));
        assertTrue(result.contains("License2"));
        assertTrue(result.contains("License3"));
    }

    @Test
    void extractVersionExtractsCorrectly() {
        String nameAndVersion = "ComponentName 1.2.3";
        String result = FossReportUtilities.extractVersion(nameAndVersion);
        assertEquals("1.2.3", result);
    }

    @Test
    void extractVersionHandlesInvalidInput() {
        String nameAndVersion = "ComponentName";
        String result = FossReportUtilities.extractVersion(nameAndVersion);
        assertEquals("", result);
    }

    @Test
    void extractCveDictionaryEntryExtractsCorrectly() {
        String vulnerabilityList = "High CVE-2023-1234, Medium CVE-2023-5678";
        String result = FossReportUtilities.extractCveDictionaryEntry(vulnerabilityList);
        assertEquals("CVE-2023-1234 CVE-2023-5678 ", result);
    }

    @Test
    void getRelativePathCalculatesCorrectly() {
        String fullPath = "C:\\base\\folder\\file.txt";
        String basePath = "C:\\base";
        String result = FossReportUtilities.getRelativePath(fullPath, basePath);
        assertEquals("folder\\file.txt", result);
    }

    @Test
    void wasCombinedIdentifiesCorrectly() {
        assertTrue(FossReportUtilities.wasCombined("License1 AND License2"));
        assertTrue(FossReportUtilities.wasCombined("License1 OR License2"));
        assertFalse(FossReportUtilities.wasCombined("License1"));
    }

    @Test
    void getCopyrightsCorrectly(){
        String copyright1 = "Copyright (c) 2005-2008, The Android Open Source Project";
        String copyright2 = "_x000D_Copyright (C) 1989, 1991 Free Software Foundation, Inc._x000D_59 Temple Place, " +
                "Suite 330, Boston, MA 02111-1307 USA";
        String expected2 = "Copyright (C) 1989, 1991 Free Software Foundation, Inc. 59 Temple Place, Suite 330, " +
                "Boston, MA 02111-1307 USA";
        String copyright3 = "Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved._x000D_ Copyright (c) 1993 John Brezak";
        String[] expected3 = {"Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.", "Copyright (c) 1993 John Brezak"};

        assertEquals(copyright1, FossReportUtilities.getCopyrights(copyright1).getFirst());
        assertEquals(expected2, FossReportUtilities.getCopyrights(copyright2).getFirst());
        assertTrue(List.of(expected3).containsAll(FossReportUtilities.getCopyrights(copyright3)));
    }


    @Test
    void extractRelativePathHandlesEmptyFilePaths() {
        String result = FossReportUtilities.extractRelativePath("", "C:\\base");
        assertEquals("", result);
    }


    @Test
    void determineBasePathHandlesSinglePath() {
        String filePaths = "C:\\base\\folder\\file.txt";
        String result = FossReportUtilities.determineBasePath(filePaths);
        assertEquals("C:\\base\\folder\\file.txt", result);
    }

    @Test
    void determineBasePathHandlesMultiplePathsWithCommonPrefix() {
        String filePaths = "C:\\base\\folder1\\file1.txt\nC:\\base\\folder1\\file2.txt\nC:\\base\\folder1\\subfolder\\file3.txt";
        String result = FossReportUtilities.determineBasePath(filePaths);
        assertEquals("C:\\base\\folder1", result);
    }

    @Test
    void determineBasePathHandlesMultiplePathsWithoutCommonPrefix() {
        String filePaths = "C:\\base\\folder1\\file1.txt\nC:\\base\\folder2\\file2.txt";
        String result = FossReportUtilities.determineBasePath(filePaths);
        assertEquals("C:\\base", result);
    }

    @Test
    void determineBasePathHandlesMultiple3PathsWithoutCommonPrefix() {
        String filePaths = "C:\\base\\folder1\\file1.txt\nC:\\base\\folder2\\file2.txt\nC:\\base\\folder2\\file3.txt";
        String result = FossReportUtilities.determineBasePath(filePaths);
        assertEquals("C:\\base", result);
    }

    @Test
    void determineBasePathHandlesInventoryNameCommonPrefix() {
        String filePaths = "C:\\base\\folder1\\file1.txt\nC:\\base\\folder2\\file2.txt\nC:\\base\\folder2\\file3.txt";
        String result = FossReportUtilities.determineBasePath(filePaths);
        assertEquals("C:\\base", result);
    }

    @Test
    void determineBasePathHandlesEmptyFilePaths() {
        String result = FossReportUtilities.determineBasePath("");
        assertEquals("\\", result);
    }

    @Test
    void determineBasePathHandlesNullFilePaths() {
        String result = FossReportUtilities.determineBasePath(null);
        assertEquals("\\", result);
    }


}
