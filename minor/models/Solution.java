package models;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solution {

    public double time;

    public double energy;

    public List<Task> schedulingSequence;

    public Map<Task, Vm> mapping;

    public Map<Task, Double> actualStartTimes;

    public Map<Task, Double> actualFinishTimes;

    public Map<Task, Double> actualExecTimes;

    public Map<Task, Map<Task,Double>> actualCommTimes;

    public Map<Vm, List<Event>> timeline;

    public Solution(){
        this.schedulingSequence =new ArrayList<>();
        this.mapping= new HashMap<>();
        this.time=Double.MAX_VALUE;
        this.energy=Double.MAX_VALUE;
        this.actualCommTimes=new HashMap<>();
        this.actualExecTimes= new HashMap<>();
        this.actualStartTimes= new HashMap<>();
        this.actualFinishTimes = new HashMap<>();
        this.timeline=new HashMap<>();
    }

    public void setActualCosts(Task t, Vm v){
        this.actualExecTimes.put(t,t.size/v.maxMips);
        //this.actualCommTimes.put(t,t.sizeOfOutput/v.bw);
        for(Task child: t.childTasks){

        }
    }

    public void setActualTimes(Task t, Double ast, Double aft){
        actualStartTimes.put(t,ast);
        actualFinishTimes.put(t,aft);
    }
}
