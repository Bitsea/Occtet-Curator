package eu.occtet.boc.ai.licenseMatcher.tools;

import eu.occtet.boc.model.SPDXLicenseDetails;
import eu.occtet.boc.ai.licenseMatcher.service.LicenseTemplateWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI Tool to fetch license information from a given URL
 * here spdx URL to fetch spdx information for a license
 */
@Component
public class LicenseTool {

    private static final Logger log =LoggerFactory.getLogger(LicenseTool.class);


    @Tool(description = "get specific license information for one licenseId")
    public SPDXLicenseDetails getLicenseInformation(@ToolParam(description = "url to fetch license details") String url){
        log.debug("using Licensetool");
        try {
            LicenseTemplateWebService ltWebService = new LicenseTemplateWebService();

            return ltWebService.readDefaultLicenseInfos(url);
        }catch(Exception e){
            log.error("String url {} could not be called, {}", url , e.getMessage());
            return null;
        }
    }
}
