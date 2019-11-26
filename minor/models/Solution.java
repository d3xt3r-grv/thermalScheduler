package models;

import simulator.Runner;
import simulator.Simulator;

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


    public Solution(Runner runner, List<Integer> vmAllocation){
        this.schedulingSequence =runner.taskList;
        this.mapping= new HashMap<>();
        this.time=Double.MAX_VALUE;
        this.energy=Double.MAX_VALUE;
        this.actualCommTimes=new HashMap<>();
        this.actualExecTimes= new HashMap<>();
        this.actualStartTimes= new HashMap<>();
        this.actualFinishTimes = new HashMap<>();
        this.timeline=new HashMap<>();

        for(int i=0;i<vmAllocation.size();i++){
            mapping.put(schedulingSequence.get(i),runner.vmList.get(vmAllocation.get(i)));
        }
        setActualCosts(runner);
    }


    public void setActualCosts(Runner runner){
        for(Task t:mapping.keySet()){
            actualExecTimes.put(t,t.size/mapping.get(t).maxMips);
            Map<Task,Double> childCommTimes=new HashMap<>();
            for(Task child:t.childTasks){
                if(runner.vmToVmBandwidth.get(mapping.get(t).id).get(mapping.get(child).id)==Double.MAX_VALUE){
                    childCommTimes.put(child,0.0);
                }
                else
                    childCommTimes.put(child,t.sizeOfOutput.get(child)/runner.vmToVmBandwidth.get(mapping.get(t).id).get(mapping.get(child).id));
            }
            actualCommTimes.put(t,childCommTimes);
        }
    }


    public void setActualTimes(Task t, Double ast, Double aft){
        actualStartTimes.put(t,ast);
        actualFinishTimes.put(t,aft);
    }


    public List<Double> calculateObjectives(Runner runner){
        Simulator.simulate(this,runner);
        List<Double> objectives= new ArrayList<>();
        objectives.add(this.time);
        objectives.add(this.energy);
        return objectives;
    }
}
