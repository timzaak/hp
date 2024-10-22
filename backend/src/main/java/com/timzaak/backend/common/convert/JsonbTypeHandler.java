package com.timzaak.backend.common.convert;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

// from https://github.com/onozaty/mybatis-postgresql-typehandlers/blob/master/src/main/java/com/github/onozaty/mybatis/pg/type/json/JsonTypeHandler.java
public class JsonbTypeHandler<T> extends BaseTypeHandler<T> {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    private final Class<T> javaType;

    public JsonbTypeHandler(Class<T> javaType) {
        this.javaType = javaType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            var obj = new PGobject();
            obj.setType("jsonb");
            obj.setValue(mapper.writeValueAsString(parameter));
            ps.setObject(i, obj);
        } catch (JsonProcessingException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toJavaTypeObject(rs.getObject(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toJavaTypeObject(rs.getObject(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {

        return toJavaTypeObject(cs.getObject(columnIndex));
    }

    private T toJavaTypeObject(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            return mapper.readValue(value.toString(), javaType);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }
}