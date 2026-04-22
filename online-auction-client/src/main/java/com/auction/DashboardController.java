package com.auction;

import com.auction.model.Item;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

// ✅ THÊM SESSION
import com.auction.Session;

public class DashboardController {

    @FXML private Button btnLogout;
    @FXML private javafx.scene.layout.FlowPane itemGrid;

    // ✅ THÊM BUTTON ĐĂNG SẢN PHẨM
    @FXML private Button btnAddItem;

    @FXML
    public void initialize() {
        loadDataFromServer();

        // ✅ FIX: CHỈ SELLER MỚI THẤY NÚT
        if (Session.role == null || !Session.role.equalsIgnoreCase("seller")) {
            btnAddItem.setVisible(false);
        }
    }

    // ================= ADD ITEM =================
    @FXML
    private void handleAddItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_item.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) itemGrid.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Đăng sản phẩm");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= BID ROOM =================
    private void openBidRoom(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("bid_room.fxml"));
            Parent root = loader.load();

            BidRoomController bidRoomCtrl = loader.getController();

            int myUserId = 1;

            bidRoomCtrl.setAuctionData(
                    item.getId(),
                    item.getName(),
                    item.getStartingPrice(),
                    myUserId
            );

            Stage stage = (Stage) itemGrid.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 750));
            stage.setTitle("Phòng Đấu Giá: " + item.getName());

        } catch (Exception e) {
            System.err.println("Lỗi khi mở phòng đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= ITEM CARD =================
    private javafx.scene.layout.VBox createItemCard(com.auction.model.Item item) {

        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
        card.setSpacing(10);
        card.getStyleClass().add("item-card");
        card.setPadding(new javafx.geometry.Insets(15));
        card.setPrefWidth(280);

        javafx.scene.layout.HBox badgeBox = new javafx.scene.layout.HBox();
        javafx.scene.control.Label badge = new javafx.scene.control.Label("LIVE");
        badge.getStyleClass().add("badge-live");
        badgeBox.getChildren().add(badge);

        javafx.scene.layout.Region imageRegion = new javafx.scene.layout.Region();
        imageRegion.setPrefHeight(150);
        imageRegion.setStyle("-fx-background-color: #2D3748; -fx-background-radius: 10;");

        javafx.scene.control.Label subtitle =
                new javafx.scene.control.Label("LÔ-" + item.getId() + " • " + item.getItemType());
        subtitle.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        javafx.scene.control.Label title = new javafx.scene.control.Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setPrefHeight(50);
        title.setWrapText(true);

        javafx.scene.layout.HBox priceHBox = new javafx.scene.layout.HBox();
        priceHBox.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        javafx.scene.layout.VBox priceVBox = new javafx.scene.layout.VBox();

        javafx.scene.control.Label priceLabel =
                new javafx.scene.control.Label("GIÁ KHỞI ĐIỂM");
        priceLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        javafx.scene.control.Label priceValue =
                new javafx.scene.control.Label("$" + item.getStartingPrice());
        priceValue.getStyleClass().add("card-price");

        priceVBox.getChildren().addAll(priceLabel, priceValue);
        priceHBox.getChildren().add(priceVBox);

        // ✅ NÚT VÀO PHÒNG
        javafx.scene.control.Button btnEnter =
                new javafx.scene.control.Button("Vào Phòng");

        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setPrefHeight(40);
        btnEnter.getStyleClass().add("btn-orange");

        javafx.scene.layout.VBox.setMargin(
                btnEnter,
                new javafx.geometry.Insets(10, 0, 0, 0)
        );

        btnEnter.setOnAction(e -> openBidRoom(item));

        card.getChildren().addAll(
                badgeBox,
                imageRegion,
                subtitle,
                title,
                priceHBox,
                btnEnter
        );

        return card;
    }

    // ================= LOAD DATA =================
    private void loadDataFromServer() {

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");
            out.println(request.toString());

            String responseStr = in.readLine();

            System.out.println("[DASHBOARD] Server gửi về: " + responseStr);

            if (responseStr != null) {

                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                if (response.get("status").getAsString().equals("SUCCESS")) {

                    JsonArray dataArray = response.getAsJsonArray("data");

                    List<Item> items = new java.util.ArrayList<>();

                    for (int i = 0; i < dataArray.size(); i++) {

                        JsonObject obj = dataArray.get(i).getAsJsonObject();

                        String name = obj.has("name") ? obj.get("name").getAsString() : "Chưa có tên";
                        double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0;
                        String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                        int sellerId = obj.has("sellerId") ? obj.get("sellerId").getAsInt() : 0;

                        String type = obj.has("warranty") ? "ELECTRONICS" : "ART";
                        String extraInfo = obj.has("warranty") ? obj.get("warranty").getAsString() : "N/A";

                        Item item = com.auction.factory.ItemFactory.createItem(
                                type, name, startPrice, endTime, sellerId, extraInfo
                        );

                        int id = obj.get("id").getAsInt();
                        item.setId(id);

                        items.add(item);
                    }

                    javafx.application.Platform.runLater(() -> {
                        itemGrid.getChildren().clear();

                        for (Item item : items) {
                            itemGrid.getChildren().add(createItemCard(item));
                        }
                    });
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi mạng: " + e.getMessage());
        }
    }

    // ================= LOGOUT =================
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