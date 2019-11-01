package models;

public class Event {

    public double startTime;

    public double finishTime;

    public double mips;

    public String eventType;

    public Event(double startTime, double finishTime, double mips, String eventType){
        this.startTime=startTime;
        this.eventType=eventType;
        this.finishTime=finishTime;
        this.mips=mips;
    }
}
