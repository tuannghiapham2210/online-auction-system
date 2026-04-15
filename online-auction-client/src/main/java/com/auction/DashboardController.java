package com.auction;

import com.auction.model.Item;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class DashboardController {

    @FXML private Button btnLogout;
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, String> colEndTime;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        loadDataFromServer();
    }

    private void loadDataFromServer() {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");
            out.println(request.toString());

            String responseStr = in.readLine();
            if (responseStr != null) {
                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
                if (response.get("status").getAsString().equals("SUCCESS")) {
                    JsonArray dataArray = response.getAsJsonArray("data");
                    Gson gson = new Gson();
                    List<Item> items = gson.fromJson(dataArray, new TypeToken<List<Item>>(){}.getType());
                    ObservableList<Item> observableItems = FXCollections.observableArrayList(items);
                    itemTable.setItems(observableItems);
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi mạng: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}