package Optimizer;

import models.Solution;
import org.apache.commons.math3.util.Pair;
import scheduler.Runner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

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

    private void updateArchive() throws IOException {
        List<List<Integer>> tempPositionArchive = new ArrayList<>();
        List<List<Double>> tempFitnessArchive = new ArrayList<>();
        tempPositionArchive.addAll(positionArchive);
        tempPositionArchive.addAll(antPosition);
        tempFitnessArchive.addAll(fitnessArchive);
        tempFitnessArchive.addAll(antFitness);
        Map<Integer, Integer> nP = new HashMap<>();
        Map<Integer, List<Integer>> sP = new HashMap<>();
        Map<Integer, Integer> rank = new HashMap<>();
        Map<Integer, List<Integer>> fronts = new HashMap<>();

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
                if(fronts.containsKey(1))
                {
                    List<Integer> tempSet = fronts.get(1);
                    tempSet.add(i);
                    fronts.put(1,tempSet);
                }
                else
                {
                    List<Integer> tempSet = new ArrayList<>();
                    tempSet.add(i);
                    fronts.put(1,tempSet);
                }
            }
        }

        int i=1;
        while(fronts.containsKey(i))
        {
            List<Integer> nextFront = new ArrayList<>();
            List<Integer> currentFront = fronts.get(i);
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
                fronts.put(i,nextFront);
        }
        List<Double> crowdDist = new ArrayList<>();
        for(int c = 0; c < tempFitnessArchive.size(); c++){
            crowdDist.add(0.0);
        }
        for(int j = 1; j <= fronts.size(); j++){
            List<Pair<Integer, Pair<Double, Double>>> fitnessValuesWIndex = new ArrayList<>();
            int l = fronts.get(j).size();
            for(int x = 0; x < l; x++){
                Double time = antFitness.get(fronts.get(j).get(x)).get(0);
                Double energy = antFitness.get(fronts.get(j).get(x)).get(1);
                fitnessValuesWIndex.add(Pair.create(fronts.get(j).get(x), Pair.create(time, energy)));
            }
            fitnessValuesWIndex.sort(new Comparator<Pair<Integer, Pair<Double, Double>>>() {
                @Override
                public int compare(Pair<Integer, Pair<Double, Double>> t1, Pair<Integer, Pair<Double, Double>> t2) {
                    return Double.compare(t1.getSecond().getFirst(),t2.getSecond().getFirst());
                }
            });
            crowdDist.set(fitnessValuesWIndex.get(0).getFirst(), Double.MAX_VALUE);
            crowdDist.set(fitnessValuesWIndex.get(l-1).getFirst(), Double.MAX_VALUE);
            Double minTime = fitnessValuesWIndex.get(0).getSecond().getFirst();
            Double maxTime = fitnessValuesWIndex.get(l-1).getSecond().getFirst();
            for(int x = 1; x < l-1; x++){
                Double timePrev = antFitness.get(fronts.get(j).get(x-1)).get(0);
                Double timeNext = antFitness.get(fronts.get(j).get(x+1)).get(0);
                if (crowdDist.get(fronts.get(j).get(x)) != Double.MAX_VALUE) {
                    crowdDist.set(fronts.get(j).get(x), crowdDist.get(fronts.get(j).get(x))+Math.abs(timeNext-timePrev)/(maxTime-minTime));
                }
            }
            fitnessValuesWIndex.sort(new Comparator<Pair<Integer, Pair<Double, Double>>>() {
                @Override
                public int compare(Pair<Integer, Pair<Double, Double>> t1, Pair<Integer, Pair<Double, Double>> t2) {
                    return Double.compare(t1.getSecond().getSecond(),t2.getSecond().getSecond());
                }
            });
            crowdDist.set(fitnessValuesWIndex.get(0).getFirst(), Double.MAX_VALUE);
            crowdDist.set(fitnessValuesWIndex.get(l-1).getFirst(), Double.MAX_VALUE);
            Double minEnergy = fitnessValuesWIndex.get(0).getSecond().getFirst();
            Double maxEnergy = fitnessValuesWIndex.get(l-1).getSecond().getFirst();
            for(int x = 1; x < l-1; x++){
                Double energyPrev = antFitness.get(fronts.get(j).get(x-1)).get(1);
                Double energyNext = antFitness.get(fronts.get(j).get(x+1)).get(1);
                if (crowdDist.get(fronts.get(j).get(x)) != Double.MAX_VALUE) {
                    crowdDist.set(fronts.get(j).get(x), crowdDist.get(fronts.get(j).get(x))+Math.abs(energyNext-energyPrev)/(maxEnergy-minEnergy));
                }
            }
        }
        crowdDist.get(0);
        int temp = 0;
        for(int j = 1; j <= fronts.size(); j++){
            temp += fronts.get(j).size();
            if(temp>sizeOfArchive){

            }else{

            }
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

    public void startOptimisation(Runner runner) throws IOException {
        this.initializeArchives();
        for(int i=0;i<maxIterations;i++){
            calculateFitness(runner);
            updateArchive();
        }
        FileWriter writer = new FileWriter("archive.txt", true);
        BufferedWriter buffer = new BufferedWriter(writer);
        for(int count = 0; count < fitnessArchive.size(); count++){
            buffer.write(fitnessArchive.get(count).get(0).toString()+" "+fitnessArchive.get(count).get(1).toString());
        }
        buffer.close();
    }

}
