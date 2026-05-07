package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Electronics extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Electronics.class);

    private int warrantyMonths;

    public Electronics(String name, double startingPrice, String endTime, int sellerId, int warrantyMonths) {
        super(name, startingPrice, endTime, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {

        logger.info("[Electronics] {} - Warranty: {} months", name, warrantyMonths);
    }

    @Override
    public String getItemType() { return "ELECTRONICS"; }

    }

