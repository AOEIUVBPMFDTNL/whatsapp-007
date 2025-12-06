package com.whatsapp.android.service.impl.init;

import axolotl.AxolotlManager;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ContactBookManager {

    private final Connection connection;
    public final AxolotlManager axolotlManager;

    public ContactBookManager(AxolotlManager axolotlManager, Connection connection) {
        this.axolotlManager = axolotlManager;
        this.connection = connection;
        initializeDatabase();
    }

    /**
     * 初始化数据库表
     */
    private void initializeDatabase() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS contact_book ( id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT NOT NULL UNIQUE )");
        } catch (Exception e) {
            log.error("初始化通讯录表异常", e);
        }
    }

    /**
     * 添加单个手机号
     */
    public void addPhoneNumber(String phoneNumber) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("INSERT INTO contact_book (phone_number) VALUES (?)");
            preparedStatement.setString(1, phoneNumber);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            log.error("添加手机号异常: {}", phoneNumber, e);
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 删除手机号
     */
    public void deletePhoneNumber(String phoneNumber) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("DELETE FROM contact_book WHERE phone_number = ?");
            preparedStatement.setString(1, phoneNumber);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            log.error("删除手机号异常: {}", phoneNumber, e);
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 批量插入手机号
     */
    public void batchInsertPhoneNumbers(List<String> phoneNumbers) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("INSERT INTO contact_book (phone_number) VALUES (?)");
            for (String phoneNumber : phoneNumbers) {
                preparedStatement.setString(1, phoneNumber);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (Exception e) {
            log.error("批量插入手机号异常", e);
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 查询所有手机号
     */
    public List<String> getAllPhoneNumbers() {
        String userName = axolotlManager.getUserName_();
        try {
            KeyLockUtil.lock(userName);
            List<String> phoneNumbers = new ArrayList<>();
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = axolotlManager.GetPreparedStatement("SELECT phone_number FROM contact_book");
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    phoneNumbers.add(resultSet.getString("phone_number"));
                }
            } catch (Exception e) {
                log.error("查询所有手机号异常", e);
            } finally {
                closeResultSet(resultSet);
                axolotlManager.closePreparedStatement(preparedStatement);
            }
            return phoneNumbers;
        } finally {
            KeyLockUtil.unlock(userName);
        }
    }

    /**
     * 关闭 ResultSet
     */
    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception e) {
                log.error("关闭 ResultSet 异常", e);
            }
        }
    }
}
