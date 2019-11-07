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
//            if(!this.dominates(fitness)){
//                elitePosition=antPosition.get(i);
//                eliteFitness=fitness;
//            }
            antFitness.set(i,fitness);
        }
    }

    private void findElite() {
        double minTime=Double.MAX_VALUE;
        double minEnergy=Double.MAX_VALUE;
        for(int i=0;i<antFitness.size();i++){
//            if(!this.dominates(antFitness.get(i))){
//                eliteFitness=antFitness.get(i);
//                elitePosition=antPosition.get(i);
//            }
            if((antFitness.get(i).get(0)<minTime && antFitness.get(i).get(1)<minEnergy)||(antFitness.get(i).get(0)<minTime && antFitness.get(i).get(1)<=minEnergy)||(antFitness.get(i).get(0)<=minTime && antFitness.get(i).get(1)<minEnergy))
            {
                minTime=antFitness.get(i).get(0);
                minEnergy=antFitness.get(i).get(1);
            }
        }
        List<Double> temp = new ArrayList<>();
        temp.add(minTime);
        temp.add(minEnergy);
        eliteFitness=temp;
    }

    private void updateArchive() {
        List<List<Integer>> tempPositionArchive = new ArrayList<>();
        List<List<Double>> tempFitnessArchive = new ArrayList<>();
        tempPositionArchive.addAll(positionArchive);
        tempPositionArchive.addAll(antPosition);
        tempFitnessArchive.addAll(fitnessArchive);
        tempFitnessArchive.addAll(antFitness);
        Map<Integer, Boolean> hashMap= new HashMap<>();
        for(int i=0;i<tempFitnessArchive.size();i++){
            for(int j=i+1;j<tempFitnessArchive.size();j++){
                if(tempFitnessArchive.get(i).equals(tempFitnessArchive.get(j))){
                    hashMap.put(i, false);
                    hashMap.put(j, false);
                }
                else{
                    if(dominates(tempFitnessArchive.get(i),tempFitnessArchive.get(j))){
                        hashMap.put(j,false);
                    }
                    else{
                        hashMap.put(i,false);
                        break;
                    }
                }
            }
        }
        positionArchive.clear();
        fitnessArchive.clear();
        for(int i=0;i<tempFitnessArchive.size();i++){
            if(!hashMap.containsKey(i)){
                positionArchive.add(tempPositionArchive.get(i));
                fitnessArchive.add(tempFitnessArchive.get(i));
            }
        }
    }

    private boolean dominates(List<Double> f2) {
//        if( ( (this.eliteFitness.get(0)<=f2.get(0)) && (this.eliteFitness.get(1)<=f2.get(1)) ) && ( (this.eliteFitness.get(0)<f2.get(0)) || (this.eliteFitness.get(1)<f2.get(1)) ) )
        if(this.eliteFitness.get(0)<f2.get(0)&&this.eliteFitness.get(1)<f2.get(1))
            return true;

        return false;
    }

    private boolean dominates(List<Double> f1, List<Double> f2) {
        if( ( (f1.get(0)<=f2.get(0)) && (f1.get(1)<=f2.get(1)) ) && ( (f1.get(0)<f2.get(0)) || (f1.get(1)<f2.get(1)) ) )
            return true;
        return false;
    }

    public void startOptimisation(Runner runner){
        this.initializeArchives();
        for(int i=0;i<maxIterations;i++){
            calculateFitness(runner);
            findElite();
            updateArchive();
        }
    }

}
