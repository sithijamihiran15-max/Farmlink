package com.farmlink;
import java.sql.SQLException;
import java.util.*;
/** Admin business-service interface; authorization is enforced server-side. */
public interface AdminService {
    Map<String,Object> overview(Account actor) throws SQLException;
    List<Map<String,Object>> users(Account actor) throws SQLException;
    List<Map<String,Object>> harvests(Account actor) throws SQLException;
    void unlist(Account actor,int harvestId) throws SQLException;
    Map<String,Object> deleteImpact(Account actor,int targetUserId) throws SQLException;
    void deleteUser(Account actor,int targetUserId) throws SQLException;
}
