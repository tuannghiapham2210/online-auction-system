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

            // Hét lên mạng đòi danh sách
            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");
            out.println(request.toString());

            String responseStr = in.readLine();
            if (responseStr != null) {
                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                if (response.get("status").getAsString().equals("SUCCESS")) {
                    JsonArray dataArray = response.getAsJsonArray("data");

                    // ==========================================================
                    // ĐOẠN FIX LỖI ABSTRACT: Tự bóc JSON và dùng Factory để đúc
                    // ==========================================================
                    List<Item> items = new java.util.ArrayList<>();

                    for (int i = 0; i < dataArray.size(); i++) {
                        JsonObject obj = dataArray.get(i).getAsJsonObject();

                        // 1. Rút ruột thông tin cơ bản
                        String name = obj.has("name") ? obj.get("name").getAsString() : "Chưa có tên";
                        double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0;
                        String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                        int sellerId = obj.has("sellerId") ? obj.get("sellerId").getAsInt() : 0;

                        // 2. Phân loại hàng hóa (Gson Server sẽ tự chèn thuộc tính của class con)
                        // Nếu có thuộc tính "warranty", chắc chắn nó là Đồ điện tử
                        String type = obj.has("warranty") ? "ELECTRONICS" : "ART";
                        String extraInfo = obj.has("warranty") ? obj.get("warranty").getAsString() : "N/A";

                        // 3. Gọi Factory đúc đúng loại class con (Giải quyết dứt điểm Abstract)
                        Item item = com.auction.factory.ItemFactory.createItem(type, name, startPrice, endTime, sellerId, extraInfo);

                        items.add(item);
                    }
                    // ==========================================================

                    // Ném dữ liệu đã xử lý ngon lành lên Bảng
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