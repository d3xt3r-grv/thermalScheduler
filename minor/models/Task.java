package models;

import java.util.ArrayList;
import java.util.List;

public class Task {

    public int id;

    public double size;

    public double sizeOfOutput;

    public List<Task> parentTasks;

    public List<Task> childTasks;

    public double ast;

    public double aft;

    public double avgCommTime;

    public double avgExecTime;

    public double rank;

    public Task(int id, double size, double sizeOfOutput){
        this.id=id;
        this.size=size;
        this.sizeOfOutput=sizeOfOutput;
        this.parentTasks= new ArrayList<>();
        this.childTasks = new ArrayList<>();
        this.ast=-1;
        this.aft=-1;
        this.rank=-1;
    }

}
