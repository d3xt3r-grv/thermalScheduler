package Optimizer;

import models.Solution;
import org.apache.commons.math3.util.Pair;
import scheduler.Runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class AntLionOptimizer {

    public List<List<Integer>> positionArchive;

    public List<List<Double>> fitnessArchive;

    public List<Integer> ranks;

    public List<List<Integer>> antPosition;

    public List<List<Double>> antFitness;

    public List<Integer> elitePosition;

    public List<Double> eliteFitness;

    public List<Double> upperBound;

    public List<Double> lowerBound;

    public int maxIterations;

    public int searchAgents;

    public int numTasks;

    public int numObj;

    public int sizeOfArchive;

    public int currentArchiveNum;

    public int currIter;


    public AntLionOptimizer(int maxIterations, int numTasks, int searchAgents, int numObj, int sizeOfArchive, int ub){

        positionArchive= new ArrayList<>();
        fitnessArchive= new ArrayList<>();
        ranks=new ArrayList<>();
        antFitness= new ArrayList<>();
        antPosition= new ArrayList<>();
        elitePosition= new ArrayList<>();
        eliteFitness = new ArrayList<>();
        upperBound= new ArrayList<>();
        lowerBound= new ArrayList<>();
        for(int i=0;i<numTasks;i++){
            upperBound.add((double) ub);
            lowerBound.add(0.0);
        }
        this.maxIterations=maxIterations;
        this.searchAgents=searchAgents;
        this.numObj=numObj;
        this.numTasks=numTasks;
        this.sizeOfArchive=sizeOfArchive;
        this.currentArchiveNum=0;
        this.currIter=0;
    }

    public void initializeArchives(){
        for(int i=0;i<searchAgents;i++) {
            List<Integer> temp = new ArrayList<>();
            for (int j = 0; j < numTasks; j++) {
                temp.add((int) (Math.random() * (upperBound.get(0)+1)));
            }
            antPosition.add(temp);
        }
        for(int i=0;i<searchAgents;i++){
            List<Double> temp=new ArrayList<>();
            for(int j=0;j<numObj;j++){
                temp.add(Double.MAX_VALUE);
            }
            antFitness.add(temp);
        }
        elitePosition=antPosition.get(0);
        eliteFitness=antFitness.get(0);
    }

    public void calculateFitness(Runner runner){
        for(int i=0;i<antPosition.size();i++){
            Solution solution= new Solution(runner,antPosition.get(i));
            List<Double> fitness=solution.calculateObjectives();
            if(dominates(fitness,eliteFitness)){
                elitePosition=antPosition.get(i);
                eliteFitness=fitness;
            }
            antFitness.set(i,fitness);
        }
    }

    private void updateArchive() {
        List<List<Integer>> tempPositionArchive = new ArrayList<>();
        List<List<Double>> tempFitnessArchive = new ArrayList<>();
        tempPositionArchive.addAll(positionArchive);
        tempPositionArchive.addAll(antPosition);
        tempFitnessArchive.addAll(fitnessArchive);
        tempFitnessArchive.addAll(antFitness);
        Map<Integer, Integer> nP = new HashMap<>();
        Map<Integer, List<Integer>> sP = new HashMap<>();
        Map<Integer, Integer> rank = new HashMap<>();
        Map<Integer, List<Integer>> front = new HashMap<>();

        for(int i=0;i<tempFitnessArchive.size();i++)
        {
            int tempNp=0;
            List<Integer> tempSp = new ArrayList<>();
            for(int j=0;j<tempFitnessArchive.size();j++)
            {
                if(i!=j)
                {
                    if(dominates(tempFitnessArchive.get(i),tempFitnessArchive.get(j)))
                    {
                        tempSp.add(j);
                    }
                    else if(dominates(tempFitnessArchive.get(j),tempFitnessArchive.get(i)))
                    {
                        tempNp++;
                    }
                }
            }
            nP.put(i,tempNp);
            sP.put(i,tempSp);
            if(tempNp==0)
            {
                rank.put(i,1);
                if(front.containsKey(1))
                {
                    List<Integer> tempSet = front.get(1);
                    tempSet.add(i);
                    front.put(1,tempSet);
                }
                else
                {
                    List<Integer> tempSet = new ArrayList<>();
                    tempSet.add(i);
                    front.put(1,tempSet);
                }
            }
        }

        int i=1;
        while(front.containsKey(i))
        {
            List<Integer> nextFront = new ArrayList<>();
            List<Integer> currentFront = front.get(i);
            for(int j=0;j<currentFront.size();j++)
            {
                int sol = currentFront.get(j);
                List<Integer> tempSp = sP.get(sol);
                for(int k=0;k<tempSp.size();k++)
                {
                    int q=tempSp.get(k);
                    nP.put(q,nP.get(q)-1);
                    if(nP.get(q)==0)
                    {
                        rank.put(q,i+1);
                        nextFront.add(q);
                    }
                }
            }
            i++;
            if(nextFront.size()>0)
                front.put(i,nextFront);
        }
    }

    private boolean dominates(List<Double> f1, List<Double> f2) {
        double time1=f1.get(0),time2=f2.get(0),energy1=f1.get(1),energy2=f2.get(1);
        boolean all=false,any=false;
        if(time1<=time2 && energy1<=energy2)
            all=true;
        if(time1<time2 || energy1<energy2)
            any=true;
        return all && any;
    }

    public void startOptimisation(Runner runner){
        this.initializeArchives();
        for(int i=0;i<maxIterations;i++){
            calculateFitness(runner);
            updateArchive();
        }
    }

}
