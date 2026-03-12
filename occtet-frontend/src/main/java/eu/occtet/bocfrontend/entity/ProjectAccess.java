package eu.occtet.bocfrontend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class ProjectAccess {

    @ManyToOne
    private Project project;

    private String username;

    private String role; // reader / curator
}