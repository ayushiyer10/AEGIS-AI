package com.anticheat.engine.model;

public class CheaterStats {

    private int total;
    private String status;

    public CheaterStats(int total, String status) {
        this.total = total;
        this.status = status;
    }

    public int getTotal() { return total; }
    public String getStatus() { return status; }
}
