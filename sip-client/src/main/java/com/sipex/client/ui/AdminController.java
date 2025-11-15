package com.sipex.client.ui;

import com.google.gson.reflect.TypeToken;
import com.sipex.client.service.HttpClientService;
import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.CallLog;
import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalMessagesLabel;
    @FXML private Label todayMessagesLabel;
    @FXML private Label todayCallDurationLabel;

    @FXML private TableView<User> usersTableView;
    @FXML private TableView<Group> groupsTableView;
    @FXML private TableView<CallLog> callLogsTableView;

    @FXML
    public void initialize() {
        setupTables();
        loadStatistics();
        loadUsers();
        loadGroups();
        loadCallLogs();
    }

    private void setupTables() {
        // 用户表
        TableColumn<User, Long> userIdCol = (TableColumn<User, Long>) usersTableView.getColumns().get(0);
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<User, String> usernameCol = (TableColumn<User, String>) usersTableView.getColumns().get(1);
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> sipUriCol = (TableColumn<User, String>) usersTableView.getColumns().get(2);
        sipUriCol.setCellValueFactory(new PropertyValueFactory<>("sipUri"));

        TableColumn<User, String> statusCol = (TableColumn<User, String>) usersTableView.getColumns().get(3);
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<User, String> createdAtCol = (TableColumn<User, String>) usersTableView.getColumns().get(4);
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // 群组表
        TableColumn<Group, Long> groupIdCol = (TableColumn<Group, Long>) groupsTableView.getColumns().get(0);
        groupIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Group, String> groupNameCol = (TableColumn<Group, String>) groupsTableView.getColumns().get(1);
        groupNameCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));

        TableColumn<Group, Long> creatorIdCol = (TableColumn<Group, Long>) groupsTableView.getColumns().get(2);
        creatorIdCol.setCellValueFactory(new PropertyValueFactory<>("creatorId"));

        TableColumn<Group, String> descCol = (TableColumn<Group, String>) groupsTableView.getColumns().get(3);
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Group, String> groupCreatedAtCol = (TableColumn<Group, String>) groupsTableView.getColumns().get(4);
        groupCreatedAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // 通话记录表
        TableColumn<CallLog, Long> callIdCol = (TableColumn<CallLog, Long>) callLogsTableView.getColumns().get(0);
        callIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<CallLog, String> callerCol = (TableColumn<CallLog, String>) callLogsTableView.getColumns().get(1);
        callerCol.setCellValueFactory(new PropertyValueFactory<>("caller"));

        TableColumn<CallLog, String> calleeCol = (TableColumn<CallLog, String>) callLogsTableView.getColumns().get(2);
        calleeCol.setCellValueFactory(new PropertyValueFactory<>("callee"));

        TableColumn<CallLog, String> callTypeCol = (TableColumn<CallLog, String>) callLogsTableView.getColumns().get(3);
        callTypeCol.setCellValueFactory(new PropertyValueFactory<>("callType"));

        TableColumn<CallLog, String> callStatusCol = (TableColumn<CallLog, String>) callLogsTableView.getColumns().get(4);
        callStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<CallLog, Integer> durationCol = (TableColumn<CallLog, Integer>) callLogsTableView.getColumns().get(5);
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));

        TableColumn<CallLog, String> callCreatedAtCol = (TableColumn<CallLog, String>) callLogsTableView.getColumns().get(6);
        callCreatedAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void loadStatistics() {
        try {
            var type = new TypeToken<ApiResponse<Map<String, Object>>>(){}.getType();
            ApiResponse<Map<String, Object>> response = HttpClientService.get("/api/admin/statistics", type);
            
            if (response.getCode() == 200 && response.getData() != null) {
                Map<String, Object> stats = response.getData();
                totalUsersLabel.setText(stats.get("totalUsers").toString());
                totalMessagesLabel.setText(stats.get("totalMessages").toString());
                todayMessagesLabel.setText(stats.get("todayMessages").toString());
                
                Object duration = stats.get("todayCallDuration");
                long durationSeconds = duration instanceof Double ? ((Double) duration).longValue() : (Long) duration;
                todayCallDurationLabel.setText(durationSeconds + "秒");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try {
            var type = new TypeToken<ApiResponse<List<User>>>(){}.getType();
            ApiResponse<List<User>> response = HttpClientService.get("/api/admin/users", type);

            if (response.getCode() == 200 && response.getData() != null) {
                usersTableView.setItems(FXCollections.observableArrayList(response.getData()));
                System.out.println("✅ 成功加载 " + response.getData().size() + " 个用户");
            } else {
                System.err.println("❌ 加载用户失败: code=" + response.getCode() + ", message=" + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ 加载用户异常:");
            e.printStackTrace();
        }
    }

    private void loadGroups() {
        try {
            var type = new TypeToken<ApiResponse<List<Group>>>(){}.getType();
            ApiResponse<List<Group>> response = HttpClientService.get("/api/admin/groups", type);

            if (response.getCode() == 200 && response.getData() != null) {
                groupsTableView.setItems(FXCollections.observableArrayList(response.getData()));
                System.out.println("✅ 成功加载 " + response.getData().size() + " 个群组");
            } else {
                System.err.println("❌ 加载群组失败: code=" + response.getCode() + ", message=" + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ 加载群组异常:");
            e.printStackTrace();
        }
    }

    private void loadCallLogs() {
        try {
            var type = new TypeToken<ApiResponse<List<CallLog>>>(){}.getType();
            ApiResponse<List<CallLog>> response = HttpClientService.get("/api/admin/calls", type);

            if (response.getCode() == 200 && response.getData() != null) {
                callLogsTableView.setItems(FXCollections.observableArrayList(response.getData()));
                System.out.println("✅ 成功加载 " + response.getData().size() + " 条通话记录");
            } else {
                System.err.println("❌ 加载通话记录失败: code=" + response.getCode() + ", message=" + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ 加载通话记录异常:");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadStatistics();
        loadUsers();
        loadGroups();
        loadCallLogs();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("刷新");
        alert.setHeaderText(null);
        alert.setContentText("数据已刷新");
        alert.show();
    }
}

