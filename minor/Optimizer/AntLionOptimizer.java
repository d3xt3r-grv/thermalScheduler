package Optimizer;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class AntLionOptimizer {

    public List<List<Integer>> positionArchive;

    public List<Pair<Double, Double>> fitnessArchive;

    public List<Integer> ranks;

    public List<List<Integer>> particlePosition;

    public List<Pair<Double, Double>> particleFitness;

    public List<Integer> elitePosition;

    public Pair<Double, Double> eliteFitness;

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
        particlePosition= new ArrayList<>();
        particleFitness= new ArrayList<>();
        elitePosition= new ArrayList<>();
        eliteFitness = new Pair(0,0);
        upperBound= new ArrayList<>();
        lowerBound= new ArrayList<>();
        for(int i=0;i<numTasks;i++){
            upperBound.add((double) ub);
            lowerBound.add(1.0);
        }
        this.maxIterations=maxIterations;
        this.searchAgents=searchAgents;
        this.numObj=numObj;
        this.numTasks=numTasks;
        this.sizeOfArchive=sizeOfArchive;
        this.currentArchiveNum=0;
        this.currIter=0;
        this.initializeArchives();
    }

    public void initializeArchives(){
        for(int i=0;i<sizeOfArchive;i++){
            List<Integer> temp = new ArrayList<>();
            for(int j=0;j<numTasks;j++){
                temp.add((int) (Math.random()*(upperBound.get(0))+1));
            }
            particlePosition.add(temp);
        }

    }

}
