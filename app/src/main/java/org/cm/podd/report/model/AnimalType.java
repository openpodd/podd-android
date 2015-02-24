package org.cm.podd.report.model;

/**
 * Created by sudarat on 2/24/15 AD.
 */
public class AnimalType {
    private String name;
    private int sickCount;
    private int deathCount;

    public AnimalType(String name, int sickCount, int deathCount) {
        this.name = name;
        this.sickCount = sickCount;
        this.deathCount = deathCount;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSickCount() {
        return sickCount;
    }

    public void setSickCount(int sickCount) { this.sickCount = sickCount; }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) { this.deathCount = deathCount; }

    @Override
    public String toString() {
        return name;
    }
}