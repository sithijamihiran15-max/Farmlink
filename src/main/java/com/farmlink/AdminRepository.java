package com.farmlink;
import java.sql.SQLException;
import java.util.*;
/** Data-access abstraction for privileged administrator views and moderation. */
public interface AdminRepository {
    Map<String,Object> overview() throws SQLException;
    List<Map<String,Object>> users() throws SQLException;
    List<Map<String,Object>> harvests() throws SQLException;
    boolean unlistHarvest(int harvestId) throws SQLException;
    Map<String,Object> deleteImpact(int userId) throws SQLException;
    /** Permanently delete a buyer/farmer and their connected records atomically. */
    void deleteUser(int userId) throws SQLException;
}
