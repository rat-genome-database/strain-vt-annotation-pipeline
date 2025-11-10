package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.dao.impl.AssociationDAO;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.Strain;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mtutaj
 * @since 5/30/2017
 * All database code lands here
 */
public class Dao {

    private AnnotationDAO annotationDAO = new AnnotationDAO();
    private AssociationDAO associationDAO = new AssociationDAO();

    private Logger logDeleted = LogManager.getLogger("deleted");
    private Logger logInserted = LogManager.getLogger("inserted");

    public String getConnectionInfo() {
        return annotationDAO.getConnectionInfo();
    }

    public List<Annotation> getQtlVtAnnotations() throws Exception {
        String query = """
            SELECT a.*,r.species_type_key
            FROM full_annot a,rgd_ids r
            WHERE annotated_object_rgd_id=rgd_id AND object_status='ACTIVE' AND aspect=? AND object_key=?
            """;
        return annotationDAO.executeAnnotationQuery(query, "V", RgdId.OBJECT_KEY_QTLS);
    }

    public List<Strain> getStrainAssociationsForQTL(int qtlRgdId) throws Exception {
        return associationDAO.getStrainAssociationsForQTL(qtlRgdId);
    }

    public Annotation getAnnotation(Annotation annot) throws Exception {
        return annotationDAO.getAnnotation(annot);
    }

    public int updateLastModified(int fullAnnotKey, int lastModifiedBy) throws Exception{
        return annotationDAO.updateLastModified(fullAnnotKey, lastModifiedBy);
    }

    public void insertAnnotation( Annotation a ) throws Exception {
        logInserted.debug(a.dump("|"));
        annotationDAO.insertAnnotation(a);
    }

    public List<Annotation> getObsoleteStrainVTAnnotations(int createdBy, Date dt) throws Exception{

        List<Annotation> annots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(createdBy, dt, "V");

        // remove any non-strain annotations
        annots.removeIf(a -> a.getRgdObjectKey() != RgdId.OBJECT_KEY_STRAINS);

        return annots;
    }

    public void deleteAnnotations(List<Annotation> annots) throws Exception {

        List<Integer> keys = new ArrayList<>(annots.size());
        for( Annotation a: annots ) {
            logDeleted.debug(a.dump("|"));
            keys.add(a.getKey());
        }
        annotationDAO.deleteAnnotations(keys);
    }
}
