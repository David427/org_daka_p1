package com.revature.repositories;

import com.revature.models.TestOne;
import com.revature.persistence.Column;
import com.revature.persistence.Id;
import com.revature.persistence.Table;
import com.revature.util.JdbcConnection;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.reflections.scanners.Scanners.TypesAnnotated;

public class ModelRepoImplTest implements ModelRepo {
    static Connection conn = JdbcConnection.getConnection();
    static Reflections reflections = new Reflections("com.revature");

    @Test
    @Override
    public void addRecord() {
        // Creating an object here only for testing purposes. The real method will have an object passed in.
        Object greatObject = new TestOne(15489, "omega_test", "action", false, 12345);

        Class<?> c = greatObject.getClass();
        Field[] fields = c.getDeclaredFields();
        int numOfFields = fields.length;
        Table table = c.getAnnotation(Table.class);
        String tableName = table.name();

        // Build column names and ?'s for the SQL query.
        String cn = "";
        String id = "";
        String qm = "";

        // Removing the field marked with @Id from fields[] so that JDBC doesn't think it has a parameterized value.
        for (int i = 0; i < numOfFields; i++) {
            if (fields[i].isAnnotationPresent(Id.class)) {
                Column column = fields[i].getAnnotation(Column.class);

                Id idField = fields[i].getAnnotation(Id.class);
                id = idField.type();
                fields = ArrayUtils.remove(fields, i);
                numOfFields = fields.length;

                try {
                    cn += column.name();

                    if (i < numOfFields - 1) {
                        cn += ", ";
                        id += ",";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (int i = 0; i < numOfFields; i++) {
            fields[i].setAccessible(true);
            Column column = fields[i].getAnnotation(Column.class);

            try {
                cn += column.name();
                qm += "?";

                if (i < numOfFields - 1) {
                    cn += ", ";
                    qm += ",";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String sql = "INSERT INTO " + tableName + " (" + cn + ") VALUES (" + id + qm + ")";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            for (int i = 0; i < numOfFields; i++) {
                switch (fields[i].getType().toString()) {
                    case "int":
                        ps.setInt(i + 1, fields[i].getInt(greatObject));
                        break;
                    case "long":
                        ps.setLong(i + 1, fields[i].getLong(greatObject));
                        break;
                    case "short":
                        ps.setShort(i + 1, fields[i].getShort(greatObject));
                        break;
                    case "byte":
                        ps.setByte(i + 1, fields[i].getByte(greatObject));
                        break;
                    case "class java.lang.String":
                        ps.setString(i + 1, (String) fields[i].get(greatObject));
                        break;
                    case "boolean":
                        ps.setBoolean(i + 1, fields[i].getBoolean(greatObject));
                        break;
                    case "double":
                        ps.setDouble(i + 1, fields[i].getDouble(greatObject));
                        break;
                    case "float":
                        ps.setFloat(i + 1, fields[i].getFloat(greatObject));
                        break;
                    default:
                        System.out.println("Unsupported: " + fields[i].getType().toString());
                }
            }
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Override
    public Object getRecord()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException
    {
        // Setting params for testing purposes.
        String tableName = "table_1";
        int id = 2;

        String primaryKeyName = "";
        Set<Class<?>> entities = reflections.get(TypesAnnotated.with(Table.class).asClass());

        for (Class<?> e : entities) {
            String fqcn = e.getName();
            Table entity = e.getAnnotation(Table.class);

            if (entity.name().equals(tableName)) {
                Class<?> c = Class.forName(fqcn);
                Field[] fields = c.getDeclaredFields();
                int numOfFields = fields.length;

                for (Field field : fields) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);

                    if (field.isAnnotationPresent(Id.class)) {
                        primaryKeyName = column.name();
                    }
                }

                Constructor<?> ctor = c.getConstructor();
                Object output = ctor.newInstance();
                Field[] outputFields = output.getClass().getDeclaredFields();

                for (Field f : outputFields) {
                    f.setAccessible(true);
                }

                String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKeyName + " = ?";

                try {
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData rsMetaData = rs.getMetaData();

                    while (rs.next()) {
                        for (int i = 0; i < numOfFields; i++) {
                            switch (fields[i].getType().toString()) {
                                case "int":
                                    outputFields[i].setInt(output, rs.getInt(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "long":
                                    outputFields[i].setLong(output, rs.getLong(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "short":
                                    outputFields[i].setShort(output, rs.getShort(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "byte":
                                    outputFields[i].setByte(output, rs.getByte(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "class java.lang.String":
                                    outputFields[i].set(output, rs.getString(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "boolean":
                                    outputFields[i].setBoolean(output, rs.getBoolean(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "double":
                                    outputFields[i].setDouble(output, rs.getDouble(rsMetaData.getColumnName(i + 1)));
                                    break;
                                case "float":
                                    outputFields[i].setFloat(output, rs.getFloat(rsMetaData.getColumnName(i + 1)));
                                    break;
                                default:
                                    System.out.println("Unsupported: " + fields[i].getType().toString());
                            }
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return output;
            }
        }
        return null;
    }

    @Test
    @Override
    public List<?> getAllRecords() throws ClassNotFoundException {
        // Setting params for testing purposes.
        String tableName = "table_1";
        Set<Class<?>> entities = reflections.get(TypesAnnotated.with(Table.class).asClass());
        // assertNotNull(entities);

        for (Class<?> e : entities) {
            String fqcn = e.getName();
            Table entity = e.getAnnotation(Table.class);

            if (entity.name().equals(tableName)) {
                Class<?> c = Class.forName(fqcn);
                Field[] fields = c.getDeclaredFields();
                int numOfFields = fields.length;
            }
        }
        return null;
    }

    @Override
    public void updateRecord() {
        //TODO: String Object
    }

    @Override
    public void deleteRecord() {
        //TODO: String Object
    }

    //region HELPER METHODS
    private static List<String> getFieldNames(Field[] fields) {
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields)
            fieldNames.add(field.getName());
        return fieldNames;
    }

    private static List<String> getMethodNames(Method[] methods) {
        List<String> methodNames = new ArrayList<>();
        for (Method method : methods)
            methodNames.add(method.getName());
        return methodNames;
    }
    //endregion
}
