package org.recap.model.submitcollection;

import lombok.Data;

import org.recap.model.jpa.BibliographicEntity;

import java.util.List;

/**
 * Created by premkb on 22/10/17.
 */
@Data
public class NonBoundWithBibliographicEntityObject {
    private String owningInstitutionBibId;
    private List<BibliographicEntity> bibliographicEntityList;
}
