package eu.occtet.boc.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "SOFTWARE_COMPONENT")
@EntityListeners(AuditingEntityListener.class)
public class SoftwareComponent {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @Column(name = "SOFTWARE_COMPONENT_NAME", nullable = false)
    private String name;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "PURL")
    private String purl;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "DETAILS_URL")
    private String detailsUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "SOFTWARE_COMPONENT_LICENSE_LINK",
            joinColumns = @JoinColumn(name = "SOFTWARE_COMPONENT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "LICENSE_ID", referencedColumnName = "ID"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<License> licenses;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "SOFTWARE_COMPONENT_VULNERABILITY_LINK",
            joinColumns = @JoinColumn(name = "SOFTWARE_COMPONENT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "VULNERABILITY_ID", referencedColumnName = "ID"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Vulnerability> vulnerabilities;

    public SoftwareComponent() {
    }

    public SoftwareComponent(String softwareName, String version,
                             List<License> license) {
        this.name = softwareName;
        this.version = version;
        this.licenses = license;
        this.curated = false;
    }

    public SoftwareComponent(String softwareName, String version,
                             List<License> license, String url) {
        this.name = softwareName;
        this.version = version;
        this.licenses = license;
        this.curated = false;
        this.detailsUrl= url;
    }

    public SoftwareComponent(String softwareName, String version){
        this.name = softwareName;
        this.version = version;
        this.curated = false;
        this.licenses = new ArrayList<>();
    }
    public SoftwareComponent(
            String name,
            String version,
            String purl,
            boolean curated,
            List<License> licenses,
            String url
    ) {
        this.name = name;
        this.version = version;
        this.purl = purl;
        this.curated = curated;
        this.licenses = licenses==null? new ArrayList<>() : licenses;
        this.detailsUrl= url;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public void addLicense(License license){
        if (this.licenses == null){
            this.licenses = new ArrayList<>();
        }
        this.licenses.add(license);
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public Boolean getCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}
