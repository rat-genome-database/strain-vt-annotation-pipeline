package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.Strain;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.MemoryMonitor;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mtutaj on Feb 24, 2023
 */
public class StrainVtAnnotation {

    private String version;
    private Dao dao;
    private int createdBy;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        StrainVtAnnotation manager = (StrainVtAnnotation) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {
        Date dateStart = new Date();
        Date cutoffDate = Utils.addMinutesToDate(dateStart, -5);

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

        log.info(getVersion());

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("  started at: "+sdt.format(dateStart));
        log.info("  "+dao.getConnectionInfo());

        CounterPool counters = new CounterPool();

        List<Annotation> qtlVtAnnots = dao.getQtlVtAnnotations();
        counters.add("qtl VT annotations", qtlVtAnnots.size());

        Set<Integer> annotsUpToDate = new HashSet<>();

        for( Annotation a: qtlVtAnnots ) {

            List<Strain> strains = dao.getStrainAssociationsForQTL(a.getAnnotatedObjectRgdId());
            for( Strain strain: strains ) {
                Annotation strainAnnot = (Annotation) a.clone();
                strainAnnot.setAnnotatedObjectRgdId(strain.getRgdId());
                strainAnnot.setObjectSymbol(strain.getSymbol());
                strainAnnot.setObjectName(strain.getName());
                strainAnnot.setRgdObjectKey(RgdId.OBJECT_KEY_STRAINS);
                strainAnnot.setEvidence("EXP");
                strainAnnot.setCreatedBy(getCreatedBy());
                strainAnnot.setCreatedDate(new Date());
                strainAnnot.setLastModifiedBy(getCreatedBy());
                strainAnnot.setOriginalCreatedDate(a.getCreatedDate());

                Annotation annotInRgd = dao.getAnnotation(strainAnnot);
                if( annotInRgd!=null ) {
                    annotsUpToDate.add(annotInRgd.getKey());
                } else {
                    dao.insertAnnotation(strainAnnot);
                    counters.increment("strain VT annots inserted");
                }
            }
        }

        // handle up-to-date annots
        for( int key: annotsUpToDate ) {
            dao.updateLastModified(key, getCreatedBy());
            counters.increment("strain VT annots up-to-date");
        }

        // handle obsolete annotations
        List<Annotation> obsoleteAnnots = dao.getObsoleteStrainVTAnnotations(getCreatedBy(), cutoffDate);
        dao.deleteAnnotations(obsoleteAnnots);
        counters.add("strain VT annots deleted", obsoleteAnnots.size());

        log.info(dumpCountersRightAligned(counters));

        memoryMonitor.stop();
        log.info(memoryMonitor.getSummary());

        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(dateStart.getTime(), System.currentTimeMillis());
        log.info(msg);
        log.info("");
    }
    /**
     * dump counters in alphabetic order, right-aligning the numeric values
     * so the last digit of every counter is at the same column
     */
    String dumpCountersRightAligned(CounterPool counters) {
        TreeSet<String> names = new TreeSet<>();
        Enumeration<String> en = counters.getCounterNames();
        while( en.hasMoreElements() ) {
            names.add(en.nextElement());
        }

        // build raw "name : value" pairs and compute max raw line length
        List<String> rawNames = new ArrayList<>();
        List<String> rawValues = new ArrayList<>();
        int maxLen = 0;
        for( String name: names ) {
            String val = Utils.formatThousands(counters.get(name));
            rawNames.add(name);
            rawValues.add(val);
            int rawLen = name.length() + 3 + val.length(); // "name : value"
            if( rawLen > maxLen ) {
                maxLen = rawLen;
            }
        }

        StringBuilder buf = new StringBuilder("========\n");
        for( int i=0; i<rawNames.size(); i++ ) {
            String name = rawNames.get(i);
            String val = rawValues.get(i);
            int pad = maxLen - (name.length() + 3 + val.length());
            buf.append(name).append(" : ");
            for( int j=0; j<pad; j++ ) {
                buf.append(' ');
            }
            buf.append(val).append("\n");
        }
        buf.append("=========\n");
        return buf.toString();
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBy() {
        return createdBy;
    }
}
