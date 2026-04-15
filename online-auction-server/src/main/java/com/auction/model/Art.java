package com.auction.model;

public class Art extends Item {
    private String artistName;

    public Art(String name, double startingPrice, String endTime, int sellerId, String artistName) {
        super(name, startingPrice, endTime, sellerId);
        this.artistName = artistName;
    }
    public String getAuthor() {
        return artistName;
    }
    @Override
    public void printInfo() { System.out.println("[Nghệ thuật] " + name + " - Tác giả: " + artistName); }

    @Override
    public String getItemType() { return "ART"; }
}
