package com.felipe.showeriocloud.Model;


import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

@DynamoDBTable(tableName = "BathStatisticsMonth")

public class UserAnalyticsDO {
    @SerializedName("totalLiters")
    private BigDecimal _totalLiters;
    @SerializedName("totalTime")
    private BigDecimal _totalTime;

    public UserAnalyticsDO() {
    }

    public BigDecimal get_totalLiters() {
        return _totalLiters;
    }

    public void set_totalLiters(BigDecimal _totalLiters) {
        this._totalLiters = _totalLiters;
    }

    public BigDecimal get_totalTime() {
        return _totalTime;
    }

    public void set_totalTime(BigDecimal _totalTime) {
        this._totalTime = _totalTime;
    }
}
