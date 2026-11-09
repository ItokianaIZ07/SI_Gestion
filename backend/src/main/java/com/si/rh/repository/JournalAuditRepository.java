package com.si.rh.repository;

import com.si.rh.model.JournalAudit;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface JournalAuditRepository extends Repository<JournalAudit, Long>, JpaSpecificationExecutor<JournalAudit> {

    @Query("SELECT DISTINCT a.tableConcernee FROM JournalAudit a ORDER BY a.tableConcernee")
    List<String> tablesAudit();
}
