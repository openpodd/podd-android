package org.cm.podd.report.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class Area {
    private int areaId;
    private int provinceId;
    private String provinceName;
    private int districtId;
    private String districtName;
    private int authorityId;
    private String authorityName;

    public static Area fromJson(JSONObject object) throws JSONException {
        Area area = new Area();
        area.areaId = object.getInt("area_id");
        area.provinceId = object.getInt("pv_id");
        area.provinceName = object.getString("pv_name");
        area.districtId = object.getInt("di_id");
        area.districtName = object.getString("di_name");
        area.authorityId = object.getInt("id");
        area.authorityName = object.getString("name");
        return area;
    }

    public String getFullName() {
        return authorityName +
                " " +
                districtName +
                " " +
                provinceName;
    }

    public String getDistrictAndProvinceName() {
        return districtName +
                " " +
                provinceName;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getDistrictId() {
        return districtId;
    }

    public void setDistrictId(int districtId) {
        this.districtId = districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public int getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(int authorityId) {
        this.authorityId = authorityId;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    @NonNull
    @Override
    public String toString() {
        return getAuthorityName();
    }
}
