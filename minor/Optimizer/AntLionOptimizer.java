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

    private List<Integer> crowdingDistance(List<Integer> front, int remaining, List<List<Double>> tempFitnessArchive){
//        List<Pair<Integer, Double>> crowdDsi = new ArrayList<>();
        HashMap<Integer,Double> crowdDist = new HashMap<>();
        for(int c = 0; c < front.size(); c++){
            crowdDist.put(front.get(c),0.0);
        }
        List<Pair<Integer, Pair<Double, Double>>> fitnessValuesWIndex = new ArrayList<>();
        int l = front.size();
        for(int x = 0; x < l; x++){
            Double time = tempFitnessArchive.get(front.get(x)).get(0);
            Double energy = tempFitnessArchive.get(front.get(x)).get(1);
            fitnessValuesWIndex.add(Pair.create(front.get(x), Pair.create(time, energy)));
        }
        fitnessValuesWIndex.sort(new Comparator<Pair<Integer, Pair<Double, Double>>>() {
            @Override
            public int compare(Pair<Integer, Pair<Double, Double>> t1, Pair<Integer, Pair<Double, Double>> t2) {
                return Double.compare(t2.getSecond().getFirst(),t1.getSecond().getFirst());
            }
        });
        crowdDist.put(fitnessValuesWIndex.get(0).getFirst(), Double.MAX_VALUE);
        crowdDist.put(fitnessValuesWIndex.get(l-1).getFirst(), Double.MAX_VALUE);
        Double minTime = fitnessValuesWIndex.get(0).getSecond().getFirst();
        Double maxTime = fitnessValuesWIndex.get(l-1).getSecond().getFirst();
        for(int x = 1; x < l-1; x++){
            Double timePrev = fitnessValuesWIndex.get(x-1).getSecond().getFirst();
            Double timeNext = fitnessValuesWIndex.get(x+1).getSecond().getFirst();
            if (crowdDist.get(front.get(x)) != Double.MAX_VALUE) {
                crowdDist.put(front.get(x), crowdDist.get(front.get(x))+Math.abs(timeNext-timePrev)/(maxTime-minTime));
            }
        }
        fitnessValuesWIndex.sort(new Comparator<Pair<Integer, Pair<Double, Double>>>() {
            @Override
            public int compare(Pair<Integer, Pair<Double, Double>> t1, Pair<Integer, Pair<Double, Double>> t2) {
                return Double.compare(t2.getSecond().getSecond(),t1.getSecond().getSecond());
            }
        });
        crowdDist.put(fitnessValuesWIndex.get(0).getFirst(), Double.MAX_VALUE);
        crowdDist.put(fitnessValuesWIndex.get(l-1).getFirst(), Double.MAX_VALUE);
        Double minEnergy = fitnessValuesWIndex.get(0).getSecond().getSecond();
        Double maxEnergy = fitnessValuesWIndex.get(l-1).getSecond().getSecond();
        for(int x = 1; x < l-1; x++){
            Double energyPrev = fitnessValuesWIndex.get(x-1).getSecond().getSecond();
            Double energyNext = fitnessValuesWIndex.get(x+1).getSecond().getSecond();
            if (crowdDist.get(front.get(x)) != Double.MAX_VALUE) {
                crowdDist.put(front.get(x), crowdDist.get(front.get(x))+Math.abs(energyNext-energyPrev)/(maxEnergy-minEnergy));
            }
        }
        List<Map.Entry<Integer,Double>> list = new LinkedList<Map.Entry<Integer,Double>>(crowdDist.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> m1, Map.Entry<Integer, Double> m2) {
                return m2.getValue().compareTo(m1.getValue());
            }
        });
        List<Integer> remainingSet = new ArrayList<>();
        for(int i=0;i<remaining;i++)
        {
            remainingSet.add(list.get(i).getKey());
        }
        return remainingSet;
    }

    private void updateArchive() throws IOException {
        List<List<Integer>> tempPositionArchive = new ArrayList<>();
        List<List<Double>> tempFitnessArchive = new ArrayList<>();
        tempPositionArchive.addAll(positionArchive);
        tempPositionArchive.addAll(antPosition);
        tempFitnessArchive.addAll(fitnessArchive);
        tempFitnessArchive.addAll(antFitness);
        fitnessArchive.clear();
        positionArchive.clear();
        Map<Integer, Integer> nP = new HashMap<>();
        Map<Integer, List<Integer>> sP = new HashMap<>();
        Map<Integer, Integer> rank = new HashMap<>();
        Map<Integer, List<Integer>> fronts = new HashMap<>();
//        HashSet<List<List<Double>>> hash = new HashSet<List<List<Double>>>();
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

        int currentArchiveSize = 0;
        for(int j = 1; j <= fronts.size(); j++){
            currentArchiveSize += fronts.get(j).size();
            List<Integer> front = fronts.get(j);
            if(currentArchiveSize>sizeOfArchive){
                List<Integer> toAdd = crowdingDistance(front, sizeOfArchive - fitnessArchive.size(), tempFitnessArchive);
                for(int it = 0; it < toAdd.size();it++){
                    int solIndex = toAdd.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
                }
                break;
            }else{
                for(int it = 0; it < front.size();it++){
                    int solIndex = front.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
                }
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
        FileWriter writer = new FileWriter("archive.txt", false);
        BufferedWriter buffer = new BufferedWriter(writer);
        for(int count = 0; count < fitnessArchive.size(); count++){
            buffer.write(fitnessArchive.get(count).get(0).toString()+" "+fitnessArchive.get(count).get(1).toString());
            buffer.newLine();
        }
        buffer.close();
    }

}
