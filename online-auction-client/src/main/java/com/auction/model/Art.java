package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Art extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Art.class);
    private String artistName;

    public Art(String name, double startingPrice, String endTime, int sellerId, String artistName) {
        super(name, startingPrice, endTime, sellerId);
        this.artistName = artistName;
    }

    @Override
    public void printInfo() {
        logger.info("[Art] {} - Author: {}", name, artistName);
    }

    @Override
    public String getItemType() { return "ART"; }

    }
