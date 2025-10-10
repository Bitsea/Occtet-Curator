package eu.occtet.bocfrontend.service;

import org.springframework.stereotype.Service;

@Service
public class Utilities {

    public String handleCasing(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "";
        }
        String separated = arg.replaceAll("([a-z])([A-Z])", "$1 $2");
        String firstLetter = separated.substring(0, 1).toUpperCase();
        return firstLetter + separated.substring(1);
    }
}
