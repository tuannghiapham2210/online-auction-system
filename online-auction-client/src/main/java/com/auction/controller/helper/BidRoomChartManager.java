package com.auction.controller.helper;

import com.auction.controller.ChartNodeController;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp chịu trách nhiệm quản lý biểu đồ biến động giá của BidRoom.
 */
public class BidRoomChartManager {
    private static final Logger logger = LoggerFactory.getLogger(BidRoomChartManager.class);

    private AreaChart<String, Number> priceChart;
    private XYChart.Series<String, Number> priceSeries;
    private String lastTickTimeStamp = "";
    private int tickSpaceCounter = 0;

    /**
     * Khởi tạo biểu đồ và Series dữ liệu.
     */
    public void initChart(AreaChart<String, Number> priceChart) {
        this.priceChart = priceChart;
        this.priceSeries = new XYChart.Series<>();
        this.priceSeries.setName("Giá");
        this.priceChart.setAnimated(true);
        this.priceChart.getData().clear();
        this.priceChart.getData().add(priceSeries);
    }

    /**
     * Xóa các điểm dữ liệu cũ trên đồ thị.
     */
    public void clearData() {
        if (priceSeries != null) {
            priceSeries.getData().clear();
        }
        lastTickTimeStamp = "";
        tickSpaceCounter = 0;
    }

    /**
     * Thêm điểm dữ liệu mới vào biểu đồ.
     */
    public void addDataPoint(String timeStamp, double price, boolean isPulseActive) {
        if (priceSeries == null) return;

        String uniqueTimeStamp = generateUniqueTimeStamp(timeStamp);
        XYChart.Data<String, Number> newData = new XYChart.Data<>(uniqueTimeStamp, price);

        StackPane customNode = createChartNode(price);
        newData.setNode(customNode);

        priceSeries.getData().add(newData);

        // Giữ tối đa 10 điểm trên biểu đồ
        while (priceSeries.getData().size() > 10) {
            priceSeries.getData().remove(0);
        }

        updateYAxisBounds();

        if (isPulseActive && customNode != null && customNode.getUserData() instanceof ChartNodeController) {
            ((ChartNodeController) customNode.getUserData()).setPulseActive(true);
        }
    }

    /**
     * Tự động tạo custom node FXML biểu diễn điểm trên biểu đồ.
     */
    private StackPane createChartNode(double price) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/chart_node.fxml"));
            StackPane customNode = loader.load();
            ChartNodeController controller = loader.getController();
            if (controller != null) {
                controller.setPrice(price);
                customNode.setUserData(controller);
            }
            return customNode;
        } catch (Exception e) {
            logger.error("Failed to load chart_node.fxml", e);
            return new StackPane();
        }
    }

    /**
     * Cập nhật biên trên và biên dưới của trục Y để hiển thị tối ưu nhất.
     */
    public void updateYAxisBounds() {
        if (priceChart == null) return;
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        if (yAxis == null) return;

        double maxPrice = 0;
        double minPrice = Double.MAX_VALUE;
        boolean hasData = false;

        if (priceSeries != null && priceSeries.getData() != null) {
            for (XYChart.Data<String, Number> data : priceSeries.getData()) {
                if (data.getYValue() != null) {
                    double val = data.getYValue().doubleValue();
                    if (val > maxPrice) maxPrice = val;
                    if (val < minPrice) minPrice = val;
                    hasData = true;
                }
            }
        }

        if (hasData) {
            yAxis.setAutoRanging(false);
            double range = maxPrice - minPrice;
            double margin = range > 0 ? range * 0.20 : maxPrice * 0.20;
            if (margin <= 0) margin = 50.0;

            double lower = Math.max(0, minPrice - margin);
            double upper = maxPrice + margin;

            yAxis.setLowerBound(lower);
            yAxis.setUpperBound(upper);

            double diff = upper - lower;
            yAxis.setTickUnit(diff / 5.0);
        } else {
            yAxis.setAutoRanging(true);
        }
    }

    /**
     * Tạo nhãn mốc thời gian duy nhất trên trục X để tránh trùng lặp đè lên nhau.
     */
    private String generateUniqueTimeStamp(String timeStamp) {
        if (timeStamp.equals(lastTickTimeStamp)) {
            tickSpaceCounter++;
        } else {
            lastTickTimeStamp = timeStamp;
            tickSpaceCounter = 0;
        }
        StringBuilder unique = new StringBuilder(timeStamp);
        for (int i = 0; i < tickSpaceCounter; i++) {
            unique.append(" ");
        }
        return unique.toString();
    }
}
