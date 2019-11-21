package scheduler;

import models.Event;
import models.Solution;
import models.Task;
import models.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Simulator {

    public static void createEvents(Solution solution,Runner runner){
        if(solution.energy!=Double.MAX_VALUE)
            return;
        Map<Task,Vm> mapping = solution.mapping;
        for(int i=0;i<solution.schedulingSequence.size();i++){
            Task task= solution.schedulingSequence.get(i);
            double ast= 0;
            double est=0;
            boolean flag=false;
            for(Task parent: task.parentTasks){
                if(mapping.get(parent).id==mapping.get(task).id){
                    est=Math.max(est, solution.actualFinishTimes.get(parent));
                }
                else{
                    est=Math.max(est,solution.actualFinishTimes.get(parent)+solution.actualCommTimes.get(parent).get(task));
                }
            }
            for(int j=i-1;j>=0;j--){
                Task predecessor = solution.schedulingSequence.get(j);
                if(mapping.get(predecessor).id==mapping.get(task).id){
                    flag=true;
                    ast=Math.max(est,solution.actualFinishTimes.get(predecessor));
                    break;
                }
            }
            if(flag==false)
                ast=est;
            double aft= ast+ solution.actualExecTimes.get(task);
            solution.setActualTimes(task,ast,aft);
        }
        Map<Vm, List<Event>> timeline = solution.timeline;
        for(Task task: solution.schedulingSequence){
            Vm vm= mapping.get(task);
            List<Event> vmEvents;
            if(timeline.get(vm)==null){
                vmEvents = new ArrayList<>();
            }
            else{
                vmEvents=timeline.get(vm);
            }
            vmEvents.add(new Event(solution.actualStartTimes.get(task),solution.actualFinishTimes.get(task),vm.maxMips,-1,"EXECUTION"));
            for(Task child: task.childTasks){
                if(mapping.get(child).id != mapping.get(task).id){
                    vmEvents.add(new Event(solution.actualFinishTimes.get(task), solution.actualFinishTimes.get(task)+solution.actualCommTimes.get(task).get(child),vm.minMips,runner.vmToVmUplinkPower.get(mapping.get(task).id).get(mapping.get(child).id) , "TRANSFER"));
                }
            }
            timeline.put(vm,vmEvents);
        }
    }

    public static double calculateMakespan(Solution solution){
        double makespan = 0;
        for(Task task:solution.schedulingSequence){
            if(task.childTasks.size()==0){
                makespan=Math.max(makespan,solution.actualFinishTimes.get(task));
            }
        }
        solution.time=makespan;
        return makespan;
    }

    public static double calculateEnergy(Solution solution){
        double makespan=solution.time;
        double energy=0;
        for(Vm vm : solution.timeline.keySet()){
            double execTime=0;
            List<Event> vmEvemts= solution.timeline.get(vm);
            for(Event event: vmEvemts){
                if(event.eventType.contentEquals("EXECUTION")) {
                    execTime += event.finishTime - event.startTime;
                    energy+=(vm.coefficient*Math.pow(event.mips/1000,3)*(event.finishTime-event.startTime));
                }
                else if(event.eventType.contentEquals("TRANSFER")){
                    energy+=(event.uplinkPower*(event.finishTime-event.startTime));
                }
            }
            double idleEnergy= vm.coefficient*Math.pow(vm.minMips/1000,3)*(makespan-execTime);
            energy+=idleEnergy;
        }
        solution.energy=energy;
        return energy;
    }
    public static void simulate(Solution solution,Runner runner){
        createEvents(solution,runner);
        calculateMakespan(solution);
        calculateEnergy(solution);
    }
}
