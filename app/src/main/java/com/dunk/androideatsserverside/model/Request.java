package com.dunk.androideatsserverside.model;

import java.util.List;

public class Request {

    private String address;
    private String name;
    private String phone;
    private String status;
    private String total;
    private List<Order> foods;


    public Request() {
    }

    public Request(String address, String name, String phone, String status, String total, List<Order> foods) {
        this.address = address;
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.total = total;
        this.foods = foods;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Order> getFoods() {
        return foods;
    }

    public void setFoods(List<Order> foods) {
        this.foods = foods;
    }
}
