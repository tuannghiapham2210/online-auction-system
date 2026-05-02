package com.auction.model;

public abstract class Entity {
    //mỗi thực thể sẽ có một id duy nhất
    //dùng protected để các lớp con kế thừa được

    protected int id; 

    //getters và setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}