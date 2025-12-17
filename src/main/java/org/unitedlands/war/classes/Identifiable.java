package org.unitedlands.war.classes;

import java.util.UUID;

public interface Identifiable {
    UUID getId();
    void setId(UUID id);
}