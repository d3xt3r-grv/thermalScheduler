package models;

public class Event {

    public double startTime;

    public double finishTime;

    public double mips;

    public double uplinkPower;

    public String eventType;

    public Event(double startTime, double finishTime, double mips,double uplinkPower, String eventType){
        this.startTime=startTime;
        this.eventType=eventType;
        this.finishTime=finishTime;
        this.mips=mips;
        this.uplinkPower=uplinkPower;
    }
}
