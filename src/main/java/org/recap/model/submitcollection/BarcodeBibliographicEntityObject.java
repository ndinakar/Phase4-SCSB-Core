package org.recap.model.submitcollection;

import lombok.Data;

import org.recap.model.jpa.BibliographicEntity;

import java.util.Objects;

/**
 * Created by premkb on 14/10/17.
 */
@Data
public class BarcodeBibliographicEntityObject {

    private String barcode;

    private String owningInstitutionBibId;

    private BibliographicEntity bibliographicEntity;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BarcodeBibliographicEntityObject that = (BarcodeBibliographicEntityObject) o;

        if (barcode != null ? !barcode.equals(that.barcode) : that.barcode != null) return false;
        if (!Objects.equals(owningInstitutionBibId, that.owningInstitutionBibId))
            return false;
        return Objects.equals(bibliographicEntity, that.bibliographicEntity);
    }

    @Override
    public int hashCode() {
        int result = barcode != null ? barcode.hashCode() : 0;
        result = 31 * result + (owningInstitutionBibId != null ? owningInstitutionBibId.hashCode() : 0);
        result = 31 * result + (bibliographicEntity != null ? bibliographicEntity.hashCode() : 0);
        return result;
    }

}
