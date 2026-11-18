package com.farmlink;
import java.sql.*;
import java.util.*;
/** Repository abstraction; consumers do not depend on JDBC details. */
public interface AccountRepository {
    Account find(int id) throws SQLException;
    Account findByNic(String nic) throws SQLException;
    String passwordHash(String nic) throws SQLException;
    Account create(Map<String,Object> values,String passwordHash) throws SQLException;
    Account update(int id,Map<String,Object> values) throws SQLException;
    void session(String tokenHash,int userId,java.sql.Timestamp expires) throws SQLException;
    Account bySession(String tokenHash) throws SQLException;
    void invalidate(String tokenHash) throws SQLException;
}
