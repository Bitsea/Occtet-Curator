package eu.occtet.boc.ai.copyrightFilter.factory;


import eu.occtet.boc.entity.InformationFile;
import org.springframework.stereotype.Component;

@Component
public class InformationFileFactory {

    public InformationFile createInfoFile(String fileName, String context, String content, String path){
        return new InformationFile(fileName, context, content, path);

    }
}
