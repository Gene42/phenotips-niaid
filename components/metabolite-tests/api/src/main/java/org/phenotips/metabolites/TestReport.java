package org.phenotips.metabolites;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Stored to a hibernate database.
 */
@Entity
@Table(name = "metabolite_test_reports")
public class TestReport implements Serializable
{
    @Id
    @GeneratedValue
    private Long reportId;

    public Long getId() {return reportId;}

    @Basic
    public String patientId;

    /** Unix time. */
    @Basic
    public long date;

    @Basic
    public int columnCount;

    @Basic
    @ElementCollection
    public List<String> columnOrder;

    @Basic
    @ElementCollection
    public List<String> data;
}
