package com.farmlink;
import java.sql.*;
import java.util.*;
public interface HarvestRepository {
    List<Harvest> findAll(Map<String,String> filters) throws SQLException;
    Harvest find(int id) throws SQLException;
    Harvest create(int farmerId,Map<String,Object> data) throws SQLException;
    Harvest update(int id,Map<String,Object> data) throws SQLException;
    void delete(int id) throws SQLException;
}
