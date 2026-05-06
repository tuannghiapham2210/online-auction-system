package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vehicle extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Vehicle.class);

    private String engineType; // Gas, Electric, Diesel...

    public Vehicle(String name, double startingPrice, String endTime, int sellerId, String engineType) {
        super(name, startingPrice, endTime, sellerId);
        this.engineType = engineType;
    }

    @Override
    public void printInfo() {
        logger.info("Vehicle item: {} - Engine type: {}", name, engineType);
    }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }
}
