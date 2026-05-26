package com.auction;

import com.auction.util.NumberUtil;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class ChartNodeController {

    @FXML private Tooltip dotTooltip;
    @FXML private Circle dotNode;
    @FXML private Label priceLabel;

    private ParallelTransition pulseAnimation;

    @FXML
    public void initialize() {
        Tooltip.install(dotNode, dotTooltip);
    }

    /**
     * Gán giá trị giá cho Chart Node.
     * @param price Giá hiện tại của điểm này.
     */
    public void setPrice(double price) {
        String formattedPrice = "$" + NumberUtil.format(price);
        priceLabel.setText(formattedPrice);
        dotTooltip.setText("Live: " + formattedPrice);
    }

    /**
     * Bật hoặc tắt hiệu ứng nhấp nháy (Pulse).
     * @param active true để bật, false để tắt.
     */
    public void setPulseActive(boolean active) {
        if (active) {
            if (pulseAnimation == null) {
                ScaleTransition st = new ScaleTransition(Duration.millis(800), dotNode);
                st.setByX(0.5);
                st.setByY(0.5);
                st.setAutoReverse(true);
                st.setCycleCount(Timeline.INDEFINITE);

                FadeTransition ft = new FadeTransition(Duration.millis(800), dotNode);
                ft.setFromValue(1.0);
                ft.setToValue(0.5);
                ft.setAutoReverse(true);
                ft.setCycleCount(Timeline.INDEFINITE);

                pulseAnimation = new ParallelTransition(st, ft);
            }
            pulseAnimation.play();
        } else {
            if (pulseAnimation != null) {
                pulseAnimation.stop();
            }
            // Khôi phục lại trạng thái ban đầu của dotNode
            dotNode.setScaleX(1.0);
            dotNode.setScaleY(1.0);
            dotNode.setOpacity(1.0);
        }
    }
}
