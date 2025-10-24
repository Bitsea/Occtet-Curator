/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.fossreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.RowDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FossReportUtilities {

    private static final Logger log = LoggerFactory.getLogger(FossReportUtilities.class);

    private static final Pattern versionPattern= Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);

    private static final Pattern filePattern= Pattern.compile("file|files|Files|File|FILES|FILE", Pattern.CASE_INSENSITIVE);

   public static RowDto convertMapToRowDto(Map<String, Object> map){
        ObjectMapper mapper = new ObjectMapper();
        RowDto rowDto = mapper.convertValue(map, RowDto.class);
        return rowDto;
    }

    public static List<String> separateCombinedLicenses(String licenseIds){
        // remove the "COMBINED"
        licenseIds = licenseIds.replace("COMBINED","").trim();
        // split by "OR"
        String[] orLicenseIds = StringUtils.splitByWholeSeparator(licenseIds," OR ");
        // split by "AND"
        List<String> splitLicenseIds= new ArrayList<>();
        Arrays.stream(orLicenseIds).forEach(
                l -> splitLicenseIds.addAll( Arrays.asList( StringUtils.splitByWholeSeparator(l," AND "))));
        log.debug(" identified {} separate licenseIds: {}",splitLicenseIds.size(), StringUtils.joinWith(",",splitLicenseIds));

        return splitLicenseIds;
    }



    /**
     * extracts version of softwarecomponent
     * @param nameAndVersion
     * @return
     */
    public static String extractVersion(String nameAndVersion) {
        if(StringUtils.isEmpty(nameAndVersion)) return "";
        String VERSION_IS_MADE_OF="0123456789.";
        String version="";
        if(versionPattern.matcher(nameAndVersion).find() && (nameAndVersion.contains(".")|| nameAndVersion.contains(" "))) {
            int i = nameAndVersion.length() - 1;
            while (VERSION_IS_MADE_OF.indexOf(nameAndVersion.charAt(i)) >= 0 || version.length() > 0) {
                version = nameAndVersion.charAt(i) + version;
                i--;
                if (i < 0) break;
                if (nameAndVersion.charAt(i) == ' ') break;
            }
            if(version.isEmpty()&& nameAndVersion.contains(" ")){
                String[] array= nameAndVersion.split(" ");
                String end = array[array.length-1];
                if(versionPattern.matcher(end).find() && !filePattern.matcher(nameAndVersion).find()){
                    version= end;
                }
            }
        }
        log.debug("extracted version: {}", version);
        return version;
    }

    /**
     * extracts version from parentNameAndVersion/componentNameAndVersion
     * @param componentVersion
     * @param componentNameAndVersion
     * @return name of component
     */
    public static String extractVersionOfComponentName( String componentNameAndVersion, String componentVersion){
        String componentName;
        if(!StringUtils.isEmpty(componentVersion)) {
            componentName = componentNameAndVersion.replace(componentVersion,"").trim();
        } else {
            componentName = componentNameAndVersion;
        }
        log.debug("extracted componentName: {}", componentName);
        return componentName;
    }

    /**
     * extracts the cveDictionaryEntries
     * @param vulnerabilityList
     * @return
     */
    public static String extractCveDictionaryEntry(String vulnerabilityList){
        if(StringUtils.isEmpty(vulnerabilityList)) return "";
        if (vulnerabilityList.contains("_x000D_")) {
            vulnerabilityList = vulnerabilityList.replaceAll("_x000D_", "\n");
        }
        String[] cveDi= vulnerabilityList.split("\\n");
        String cve = "";
        Pattern p = Pattern.compile(" CVE([^,]*)");
        for (int i = 0; i< cveDi.length ; i++ ) {
            Matcher m = p.matcher(cveDi[i]);
            while (m.find()) {
                cve += "CVE" + m.group(1) + " ";
            }
        }
        log.debug("extracted cveDictionaryEntries: {}", cve);
        return cve;
    }

    /**
     *
     * @param vulnerabilityList
     * @return
     */
    public static String extractSeverity(String vulnerabilityList){
        if(StringUtils.isEmpty(vulnerabilityList)) return "";
        String[] cveDi= vulnerabilityList.split("\\n");
        String severity = "";
        Pattern p = Pattern.compile("([^,]*)CVE");
        for (int i = 0; i< cveDi.length ; i++ ) {
            Matcher m = p.matcher(cveDi[i]);
            while (m.find()) {
                severity +=  m.group(1);
            }
        }
        log.debug("extracted severity: {}", severity);
        return severity;
    }

    public static String getRelativePath(String fullPath, String basePath) {
        int start=basePath.endsWith(File.separator)? basePath.length():basePath.length()+1;
        return fullPath.substring(start);
    }



    public static String extractRelativePath(String filePaths, String basePathForRelative) {
        if(filePaths == null || filePaths.isEmpty() || basePathForRelative == null || basePathForRelative.isEmpty()) {
            return filePaths;
        }
        String[] paths= filePaths.split("\n");
        for(int i=0; i< paths.length; i++){
            paths[i] = FossReportUtilities.getRelativePath(paths[i], basePathForRelative);
        }
        return String.join("\n", paths);
    }


    public static Boolean wasCombined(String name){
        return name.contains("COMBINED")||name.contains("AND")|| name.contains("OR");
    }

    public static List<String> getCopyrights(String copyRightText){
        if(copyRightText == null || copyRightText.isEmpty()) return new ArrayList<>();
        String cleaned = copyRightText.replaceAll("_x000D_", "\n");
        String[] lines = cleaned.split("\n");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.trim().toLowerCase().startsWith("copyright")) {
                result.add(line);
            } else if (!result.isEmpty()) {
                // Append to the last element
                int lastIndex = result.size() - 1;
                result.set(lastIndex, result.get(lastIndex) + " " + line);
            }
        }
        return result;
    }

    public static String determineBasePath(String filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            log.debug("return empty base path");
            return "\\";
        }
        String[] paths = filePaths.split("\n");

        if (paths.length == 1) {
            return paths[0];

        } else {

            // find common prefix
            String prefix = paths[0];
            for (int i = 1; i < paths.length; i++) {
                while (paths[i].indexOf(prefix) != 0) {
                    // determine platform
                    if (paths[i].contains("\\"))
                        prefix = prefix.substring(0, prefix.lastIndexOf("\\"));
                    else
                        prefix = prefix.substring(0, prefix.lastIndexOf("/"));
                    if (prefix.isEmpty()) {
                        return "";
                    }
                }
            }

            log.debug("common path {}", prefix);
            return prefix;
        }
    }



}