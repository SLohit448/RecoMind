package com.recomind.dao;

import com.recomind.model.AuditLog;
import java.util.List;

/** Data access for the audit trail of admin actions. */
public interface AuditDao {
    void insert(AuditLog entry);
    /** Newest entries first. */
    List<AuditLog> findRecent(int limit);
}