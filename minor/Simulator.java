import models.Event;
import models.Solution;
import models.Task;
import models.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Simulator {

    public static void createEvents(Solution solution){
        double makespan=0;
        double energy=0;
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
                    est=Math.max(est,solution.actualFinishTimes.get(parent)+solution.actualCommTimes.get(parent));
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
            vmEvents.add(new Event(solution.actualStartTimes.get(task),solution.actualFinishTimes.get(task),vm.maxMips,"EXECUTION"));
            for(Task child: task.childTasks){
                if(mapping.get(child).id != mapping.get(task).id){
                    vmEvents.add(new Event(solution.actualFinishTimes.get(task), solution.actualFinishTimes.get(task)+solution.actualCommTimes.get(task),vm.minMips, "TRANSFER"));
                }
            }
            timeline.put(vm,vmEvents);
        }
    }

    public static double calculateMakespan(){
        //CODE HERE
        return 0;
    }

    public static double calculateEnergy(){
        //CODE HERE
        return 0;
    }
}