package eu.occtet.bocfrontend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nats")
public record ConfigNatsProperties (

    String stream_name,
    String stream_subjects_config,
    String send_subject_ort_run,
    String send_subject_spdx,
    String send_subject_export,
    String send_subject_foss,
    String send_subject_vulnerabilities,
    String send_subject_ort_result,
    String objectStoreTtl,
    String send_subject_download){}

