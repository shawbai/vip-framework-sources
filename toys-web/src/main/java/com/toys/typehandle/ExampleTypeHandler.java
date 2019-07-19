package com.toys.typehandle;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

@MappedJdbcTypes(JdbcType.VARCHAR)
public class ExampleTypeHandler extends BaseTypeHandler<String> {

  public void setNonNullParameter(PreparedStatement preparedStatement, int i, String s,
      JdbcType jdbcType) throws SQLException {
    preparedStatement.setString(i,s);
  }

  public String getNullableResult(ResultSet resultSet, String s) throws SQLException {
    return resultSet.getString(s);
  }

  public String getNullableResult(ResultSet resultSet, int i) throws SQLException {
    return resultSet.getString(i);
  }

  public String getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
    return callableStatement.getString(i);
  }
}
