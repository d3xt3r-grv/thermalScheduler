import models.Task;
import models.Vm;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Runner {
    public List<Vm> vmList;

    public List<Task> taskList;

    public List<List<Integer>> graph;

    public Runner(){
        this.vmList= new ArrayList<>();
        this.taskList= new ArrayList<>();
        this.graph= new ArrayList<>();
    }
    public static void main(String[] args) throws IOException {
        Runner runner= new Runner();
        File workflow =new File("workflow.txt");
        BufferedReader reader = new BufferedReader(new FileReader(workflow));
        String line = null;
        line = reader.readLine();
        int nVm;
        nVm= Integer.parseInt(line);
        for(int i=0;i<nVm;i++){
            line = reader.readLine();
            String[] vmParam=line.split(" ");
            runner.vmList.add(new Vm(i,Double.parseDouble(vmParam[0]),Double.parseDouble(vmParam[1]), Double.parseDouble(vmParam[2]), Double.parseDouble(vmParam[3])));
        }
        int nTasks;
        line=reader.readLine();
        nTasks=Integer.parseInt(line);
        for(int i=0;i<nTasks;i++){
            line= reader.readLine();
            String[] taksParam= line.split(" ");
            runner.taskList.add(new Task(i,Double.parseDouble(taksParam[0]),Double.parseDouble(taksParam[1])));
        }
        for(int i=0;i<nTasks;i++){
            line=reader.readLine();
            String[] temp=line.split(" ");
            List<Integer> tempList= new ArrayList<>();
            for(int j=0;j<nTasks;j++){
                tempList.add(Integer.parseInt(temp[j]));
            }
            runner.graph.add(tempList);
        }
    }
}
