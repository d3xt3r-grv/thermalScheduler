package scheduler;

import Optimizer.AntLionOptimizer;
import models.Task;
import models.Vm;

import java.io.*;
import java.util.*;

public class Runner {

    public List<Vm> vmList;

    public List<Task> taskList;

    public List<List<Double>> graph;

    public List<List<Double>> channelBandwidth;

    public List<List<Double>> channelPower;

    public List<List<Double>> vmToVmBandwidth;

    public List<List<Double>> vmToVmUplinkPower;


    public Task getTaskById(int id){

        for (Task task : taskList) {
            if (task.id == id) {
                return task;
            }
        }
        return null;
    }


    public double getAvgBandwidth(){
        double harmonicSum=0;
        int nVm= vmList.size();

        for(int i=0;i<nVm;i++){
            for(int j=0;j<nVm;j++){
                if(vmToVmBandwidth.get(i).get(j)!=Double.MAX_VALUE){
                    harmonicSum+=1/vmToVmBandwidth.get(i).get(j);
                }
            }
        }
        return nVm*nVm/harmonicSum;
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
                double maxChildRank = Double.MIN_VALUE;

                for(Task childTask : task.childTasks)
                {
                    maxChildRank=Math.max(calRank(childTask)+task.avgCommTime.get(childTask),maxChildRank);
                }
                task.rank=maxChildRank;
                task.rank+=task.avgExecTime;
            }
        }
        return task.rank;
    }


    public Runner(){

        this.vmList= new ArrayList<>();
        this.taskList= new ArrayList<>();
        this.graph= new ArrayList<>();
        this.channelBandwidth=new ArrayList<>();
        this.channelPower=new ArrayList<>();
        this.vmToVmBandwidth=new ArrayList<>();
        this.vmToVmUplinkPower=new ArrayList<>();
    }


    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();

        File workflow = new File("input.txt");
        BufferedReader reader = new BufferedReader(new FileReader(workflow));

        String line = null;
        line = reader.readLine();
        int nHosts;
        nHosts=Integer.parseInt(line);
        line=reader.readLine();

        //Setting up Vm list
        int nVm;
        nVm = Integer.parseInt(line);
        for (int i = 0; i < nVm; i++) {
            line = reader.readLine();
            String[] vmParam = line.split(" ");
            runner.vmList.add(new Vm(i, Double.parseDouble(vmParam[0]), Integer.parseInt(vmParam[1]), Double.parseDouble(vmParam[2]), Double.parseDouble(vmParam[3])));
        }

        //Setting up task list
        int nTasks;
        line = reader.readLine();
        nTasks = Integer.parseInt(line);
        for (int i = 0; i < nTasks; i++) {
            line = reader.readLine();
            runner.taskList.add(new Task(i, Double.parseDouble(line)));
        }

        //Making adjacency matrix of the DAG
        for (int i = 0; i < nTasks; i++) {
            line = reader.readLine();
            String[] temp = line.split(" ");
            List<Double> tempList = new ArrayList<>();
            for (int j = 0; j < nTasks; j++) {
                tempList.add(Double.parseDouble(temp[j]));
            }
            runner.graph.add(tempList);
        }

        //Setting child and parent tasks with size of output map
        for (int i = 0; i < nTasks; i++) {
            Task curr = runner.getTaskById(i);
            List<Double> children = runner.graph.get(i);
            for (int j = 0; j < nTasks; j++) {
                if (children.get(j) != 0) {
                    Task child = runner.getTaskById(j);
                    curr.childTasks.add(child);
                    child.parentTasks.add(curr);
                    curr.sizeOfOutput.put(child,children.get(j));
                }
            }
        }

        //Setting channel bandwidth
        for(int i=0;i<nHosts;i++){
            line =reader.readLine();
            String[] temp = line.split(" ");
            List<Double> tempList = new ArrayList<>();
            for (int j = 0; j < nHosts; j++) {
                double bw=Double.parseDouble(temp[j]);
                if(bw==-1)
                    bw=Double.MAX_VALUE;
                tempList.add(bw);
            }
            runner.channelBandwidth.add(tempList);
        }

        //Setting host to host uplink channel power
        for(int i=0;i<nHosts;i++){
            line =reader.readLine();
            String[] temp = line.split(" ");
            List<Double> tempList = new ArrayList<>();
            for (int j = 0; j < nHosts; j++) {
                double pow=Double.parseDouble(temp[j]);
                if(pow==-1)
                    pow=0.0;
                tempList.add(pow);
            }
            runner.channelPower.add(tempList);
        }

        //Setting vmToVmBandwidth
        for(int i=0;i<nVm;i++){
            List<Double> tempList = new ArrayList<>();
            for(int j=0;j<nVm;j++){
                if(runner.vmList.get(i).host==runner.vmList.get(j).host){
                    tempList.add(Double.MAX_VALUE);
                }
                else tempList.add(runner.channelBandwidth.get(runner.vmList.get(i).host).get(runner.vmList.get(j).host));
            }
            runner.vmToVmBandwidth.add(tempList);
        }

        //Setting vmToVmUplinkPower
        for(int i=0;i<nVm;i++){
            List<Double> tempList = new ArrayList<>();
            for(int j=0;j<nVm;j++){
                if(runner.vmList.get(i).host==runner.vmList.get(j).host){
                    tempList.add(0.0);
                }
                else tempList.add(runner.channelPower.get(runner.vmList.get(i).host).get(runner.vmList.get(j).host));
            }
            runner.vmToVmUplinkPower.add(tempList);
        }

        //Get average bandwidth of the channel
        double avgBw = runner.getAvgBandwidth();

        //Get average mips of all the vm's in the cloud
        double avgMips = runner.getAvgMips();

        for (Task task : runner.taskList) {
            for(Task child: task.sizeOfOutput.keySet()){
                task.avgCommTime.put(child,task.sizeOfOutput.get(child)/avgBw);
            }
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

        List<Integer> vmAllocation = new ArrayList<>();
        for(int i=0;i<nTasks;i++){
            vmAllocation.add((int) (Math.random()*nVm));
        }

        AntLionOptimizer malo= new AntLionOptimizer(200,nTasks,200,2,nTasks<50?100:2*nTasks,nVm);
        malo.startOptimisation(runner);

        FileWriter writer = new FileWriter("archive.txt",false);
        BufferedWriter buffer = new BufferedWriter(writer);

        for(int count = 0; count < malo.fitnessArchive.size(); count++){
            buffer.write(malo.fitnessArchive.get(count).get(0).toString()+" "+malo.fitnessArchive.get(count).get(1).toString());
            buffer.newLine();
        }
        buffer.close();
    }
}
