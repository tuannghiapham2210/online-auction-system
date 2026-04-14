package com.auction.model;

public abstract class Entity {
    protected int id; // Dùng protected để các lớp con kế thừa được

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}