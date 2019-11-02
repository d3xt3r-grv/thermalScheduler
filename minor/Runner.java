import models.Solution;
import models.Task;
import models.Vm;

import java.io.*;
import java.util.*;

public class Runner {
    public List<Vm> vmList;

    public List<Task> taskList;

    public List<List<Integer>> graph;

    public Task getTaskById(int id){
        for (Task task : taskList) {
            if (task.id == id) {
                return task;
            }
        }
        return null;
    }

    public double getAvgBandwidth(){
        double sum=0;
        for (Vm vm : vmList) {
            sum+=vm.bw;
        }
        return sum/vmList.size();
    }

    public double getAvgMips(){
        double sum=0;
        for (Vm vm : vmList) {
            sum+=vm.maxMips;
        }
        return sum/vmList.size();
    }

    public Vm getVmById(int id){
        for (Vm vm : vmList) {
            if (vm.id == id) {
                return vm;
            }
        }
        return null;
    }

    public void calculateRanks(Task root)
    {
        double rank = calRank(root);
    }

    public double calRank(Task task)
    {
        if(task.rank==-1)
        {
            if(task.childTasks.size()==0)
            {
                task.rank=task.avgExecTime;
            }
            else
            {
                task.rank=task.avgCommTime;
                double maxChildRank = Double.MIN_VALUE;
                for(Task childTask : task.childTasks)
                {
                    if(maxChildRank<calRank(childTask))
                    {
                        maxChildRank=calRank(childTask);
                    }
                }
                task.rank+=maxChildRank;
            }
        }
        return task.rank;
    }

    public Runner(){
        this.vmList= new ArrayList<>();
        this.taskList= new ArrayList<>();
        this.graph= new ArrayList<>();
    }
    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();
        File workflow = new File("workflow.txt");
        BufferedReader reader = new BufferedReader(new FileReader(workflow));
        String line = null;
        line = reader.readLine();
        int nVm;
        nVm = Integer.parseInt(line);
        for (int i = 0; i < nVm; i++) {
            line = reader.readLine();
            String[] vmParam = line.split(" ");
            runner.vmList.add(new Vm(i, Double.parseDouble(vmParam[0]), Double.parseDouble(vmParam[1]), Double.parseDouble(vmParam[2]), Double.parseDouble(vmParam[3])));
        }
        int nTasks;
        line = reader.readLine();
        nTasks = Integer.parseInt(line);
        for (int i = 0; i < nTasks; i++) {
            line = reader.readLine();
            String[] taskParam = line.split(" ");
            runner.taskList.add(new Task(i, Double.parseDouble(taskParam[0]), Double.parseDouble(taskParam[1])));
        }
        for (int i = 0; i < nTasks; i++) {
            line = reader.readLine();
            String[] temp = line.split(" ");
            List<Integer> tempList = new ArrayList<>();
            for (int j = 0; j < nTasks; j++) {
                tempList.add(Integer.parseInt(temp[j]));
            }
            runner.graph.add(tempList);
        }
        for (int i = 0; i < nTasks; i++) {
            Task curr = runner.getTaskById(i);
            List<Integer> children = runner.graph.get(i);
            for (int j = 0; j < nTasks; j++) {
                if (children.get(j) == 1) {
                    Task child = runner.getTaskById(j);
                    curr.childTasks.add(child);
                    child.parentTasks.add(curr);
                }
            }
        }
        double avgBw = runner.getAvgBandwidth();
        double avgMips = runner.getAvgMips();
        for (Task task : runner.taskList) {
            task.avgCommTime = task.sizeOfOutput / avgBw;
            task.avgExecTime = task.size / avgMips;
        }

        Task root = null;
        for (Task task : runner.taskList) {
            if (task.parentTasks.size() == 0) {
                root = task;
                break;
            }
        }

        runner.calculateRanks(root);
        Collections.sort(runner.taskList, new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                return Double.compare(t1.rank, task.rank);
            }
        });
        Solution initialSolution =  new Solution();
        for(int i=0;i<nTasks;i++){
            Task t=runner.taskList.get(i);
            Vm v = runner.getVmById((int) (Math.random()*nVm));
            Map<Task,Vm> map= new HashMap<>();
            map.put(t,v);
            initialSolution.solution.add(map);
        }
    }

}
