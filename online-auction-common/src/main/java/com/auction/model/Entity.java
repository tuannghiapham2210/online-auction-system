package com.auction.model;

/**
 * Lớp trừu tượng cơ sở (Base class) cho tất cả các thực thể trong hệ thống.
 * Cung cấp định danh (ID) duy nhất để quản lý và truy xuất trong Database.
 */
public abstract class Entity {

    protected int id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}