package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {

    public int id;

    public double size;

    public Map<Task,Double> sizeOfOutput;

    public List<Task> parentTasks;

    public List<Task> childTasks;

    public Map<Task,Double> avgCommTime;

    public double avgExecTime;

    public double rank;

    public Task(int id, double size){
        this.id=id;
        this.size=size;
        this.sizeOfOutput=new HashMap<>();
        this.parentTasks= new ArrayList<>();
        this.childTasks = new ArrayList<>();
        this.rank=-1;
        avgCommTime=new HashMap<>();
    }

}
