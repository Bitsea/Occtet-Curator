package eu.occtet.boc.fossreport.service;

import java.util.ArrayList;
import java.util.List;

public final class PathUtilities {


        /**
         * splits the given string into single paths and cleans them
         * @param path
         * @return
         */
        public static List<String> cleanAndSplits(String path){
            if (path == null || path.isBlank()) return new ArrayList<>();
            String[] lines = path.split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank())
                    cleaned.add(cleanPath(line));
            }
            return cleaned;
        }

        public static String cleanPath(String path){
            if (path == null || path.isBlank()) return null;

            return path.replaceAll("\\p{Cntrl}","") // Control characters
                    .replaceAll("_x[0-9A-Fa-f]{4}_", "") // Exel style junk
                    .trim();
        }

    public static int secondLastDirectoryIndex(int level, String separator, String path) {
        if (level <= 0) return path.length();
        return secondLastDirectoryIndex(--level,separator, path.substring(0, path.lastIndexOf(separator)));
    }


    }
