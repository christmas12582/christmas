package com.lottery.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class Share {
    private Integer id;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date sharetime;

    private Integer businessid;

    private Integer userid;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getSharetime() {
        return sharetime;
    }

    public void setSharetime(Date sharetime) {
        this.sharetime = sharetime;
    }

    public Integer getBusinessid() {
        return businessid;
    }

    public void setBusinessid(Integer businessid) {
        this.businessid = businessid;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }
}