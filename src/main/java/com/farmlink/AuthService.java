package com.farmlink;
import java.sql.*;
import java.util.*;
public interface AuthService {
    Account register(Map<String,Object> data) throws SQLException;
    Account authenticate(String nic,String password) throws SQLException;
    String newSession(Account account) throws SQLException;
    Account currentUser(String token) throws SQLException;
    void signOut(String token) throws SQLException;
    Account updateProfile(Account actor,Map<String,Object> data) throws SQLException;
}
