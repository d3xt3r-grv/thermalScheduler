package Optimizer;

import Plotter.ScatterPlot;
import models.Solution;
import org.apache.commons.math3.util.Pair;
import simulator.Runner;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("unchecked")
public class AntLionOptimizer {

    public int maxIterations;

    public int searchAgents;

    public int numTasks;

    public int numObj;

    public int maxArchiveSize;

    public int nVm;

    public int currentArchiveNum;

    public int currIter;

    public List<List<Integer>> positionArchive;

    public List<List<Double>> fitnessArchive;

    public List<Double> ranks;

    public List<List<Integer>> antPosition;

    public List<List<Double>> antFitness;

    public List<Integer> elitePosition;

    public List<Double> eliteFitness;


    public AntLionOptimizer(int maxIterations, int numTasks, int searchAgents, int numObj, int maxArchiveSize, int nVm){

        // INITIALISING ARRAYS AND VALUES
        positionArchive= new ArrayList<>();
        fitnessArchive= new ArrayList<>();
        ranks=new ArrayList<>();
        antFitness= new ArrayList<>();
        antPosition= new ArrayList<>();
        elitePosition= new ArrayList<>();
        eliteFitness = new ArrayList<>();
        this.nVm=nVm;
        this.maxIterations=maxIterations;
        this.searchAgents=searchAgents;
        this.numObj=numObj;
        this.numTasks=numTasks;
        this.maxArchiveSize = maxArchiveSize;
        this.currentArchiveNum=0;
        this.currIter=0;
    }


    public void initializeArchives(){

        // INSERTING RANDOM VALUES FOR THE FIRST ITERATION
        for(int i=0;i<searchAgents;i++) {
            List<Integer> temp = new ArrayList<>();
            for (int j = 0; j < numTasks; j++) {
                temp.add((int) (Math.random() * nVm));
            }
            antPosition.add(temp);
        }

        // INSERTING INFINITE VALUES FOR FITNESS AS INITIALISATION
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

        // CALCULATING VALUES OF TIME AND ENERGY
        antFitness.clear();
        for(int i=0;i<antPosition.size();i++){
            Solution solution= new Solution(runner,antPosition.get(i));
            List<Double> fitness=solution.calculateObjectives(runner);

            // UPDATING ELITE IF BETTER FITNESS VALUE FOUND
            if(dominates(fitness,eliteFitness)){
                elitePosition=antPosition.get(i);
                eliteFitness=fitness;
            }
            antFitness.add(fitness);
        }
    }


    private List<Integer> crowdingDistance(List<Integer> front, int remaining, List<List<Double>> tempFitnessArchive){

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
                return Double.compare(t1.getSecond().getFirst(),t2.getSecond().getFirst());
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
                crowdDist.put(front.get(x), crowdDist.get(front.get(x))+(timeNext-timePrev)/(maxTime-minTime));
            }
        }

        fitnessValuesWIndex.sort(new Comparator<Pair<Integer, Pair<Double, Double>>>() {
            @Override
            public int compare(Pair<Integer, Pair<Double, Double>> t1, Pair<Integer, Pair<Double, Double>> t2) {
                return Double.compare(t1.getSecond().getSecond(),t2.getSecond().getSecond());
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
                crowdDist.put(front.get(x), crowdDist.get(front.get(x))+(energyNext-energyPrev)/(maxEnergy-minEnergy));
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


    private List<Integer> updateArchiveNDSort(){

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

        // SORTING ALL SOLUTIONS INTO FRONTS
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
        List<Integer> elite = new ArrayList<>();

        for(int j = 1; j <= fronts.size(); j++){
            int idx=0;
            currentArchiveSize += fronts.get(j).size();
            List<Integer> front = fronts.get(j);

            if(currentArchiveSize>maxArchiveSize)
            {
                List<Integer> toAdd = crowdingDistance(front, maxArchiveSize - fitnessArchive.size(), tempFitnessArchive);
                for(int it = 0; it < toAdd.size();it++){
                    int solIndex = toAdd.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
                    if(j==1){
                        elite.add(idx);
                    }
                    idx++;
                }
                break;
            }
            else
            {
                for(int it = 0; it < front.size();it++){
                    int solIndex = front.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
                    if(j==1){
                        elite.add(idx);
                    }
                    idx++;
                }
            }
        }
        return elite;
    }


    private void rankingProcess(int size) {
        ranks.clear();
        double minTime=Double.MAX_VALUE;
        double maxTime=Double.MIN_VALUE;
        double minEnergy=Double.MAX_VALUE;
        double maxEnergy=Double.MIN_VALUE;

        for(int i=0;i<size;i++){
            minTime=Math.min(minTime,fitnessArchive.get(i).get(0));
            maxTime=Math.max(maxTime,fitnessArchive.get(i).get(0));
            minEnergy=Math.min(minEnergy,fitnessArchive.get(i).get(1));
            maxEnergy=Math.max(maxEnergy,fitnessArchive.get(i).get(1));
            ranks.add(0.0);
        }

        Double timeDensityParameter = (maxTime-minTime)/20;
        Double energyDensityParameter = (maxEnergy - minEnergy) /20 ;

        for(int i=0;i<size;i++){
            ranks.set(i,0.0);
            for(int j=0;j<fitnessArchive.size();j++){
                int flag=0;
                if((Math.abs(fitnessArchive.get(i).get(0)-fitnessArchive.get(j).get(0))<timeDensityParameter) && (Math.abs(fitnessArchive.get(i).get(1)-fitnessArchive.get(j).get(1))<energyDensityParameter)){
                    flag=1;
                }
                if(flag==1){
                    ranks.set(i,ranks.get(i)+1);
                }
            }
        }
    }


    private int rouletteWheelSelection(List<Double> weights,int size) {
        List<Double> cumSum = new ArrayList<>();
        cumSum.add((double)weights.get(0));

        for(int i=1;i<size;i++){
            cumSum.add((double)cumSum.get(i-1)+weights.get(i));
        }

        double p=Math.random()*cumSum.get(size-1);
        int index=-1;

        for(int idx=0;idx<size;idx++){
            if(cumSum.get(idx)>p){
                index=idx;
                break;
            }
        }
        return index;
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


    private List<Double> inverse(List<Double> ranks) {

        List<Double> weights= new ArrayList<>();
        for(double r :ranks){
            weights.add(1.0/r);
        }
        return weights;
    }


    private List<Integer> eliteEffect(List<Integer> ant) {

        int I=numTasks/4;
        if ((double)currIter>(double)maxIterations/10)
            I=2+2*I/3;
        else if ((double)currIter>(double)maxIterations/2)
            I=2+2*2*I/(3*3);
        else if ((double)currIter>(double)maxIterations*(3/4))
            I=2+2*2*2*I/(3*3*3);
        else if ((double)currIter>(double)maxIterations*(0.9))
            I=2+2*2*2*2*I/(3*3*3*3);
        else if ((double)currIter>(double)maxIterations*(0.95))
            I=2+2*2*2*2*2*I/(3*3*3*3*3);
        else;

        int lowerBound = (int) (Math.random()*(numTasks-I));
        Collections.copy(ant.subList(lowerBound,lowerBound+I),elitePosition.subList(lowerBound,lowerBound+I));
        return ant;
    }

    private List<Integer> createAntPosition(List<Integer> randomAntLionPosition) {

        int I=numTasks/2;
        if ((double)currIter>(double)maxIterations/10)
            I=2+2*I/3;
        else if ((double)currIter>(double)maxIterations/2)
            I=2+2*2*I/(3*3);
        else if ((double)currIter>(double)maxIterations*(3/4))
            I=2+2*2*2*I/(3*3*3);
        else if ((double)currIter>(double)maxIterations*(0.9))
            I=2+2*2*2*2*I/(3*3*3*3);
        else if ((double)currIter>(double)maxIterations*(0.95))
            I=2+2*2*2*2*2*I/(3*3*3*3*3);
        else;

        int lowerBound = (int)(Math.random()*(numTasks-I));
        List<Integer> ant= new ArrayList<>();
        ant.addAll(randomAntLionPosition);
        Collections.reverse(ant.subList(lowerBound,lowerBound+I));
        return ant;
    }


    private void printELite() {
        System.out.println("The Elite around which walk is done in next iteration: "+elitePosition.toString()+ " Time and energy values: "+ eliteFitness.toString());
    }


    private void printRandomAntLion(int randomAntLionPos) {
        System.out.println("The anlion around which walk is done in next iteration: "+positionArchive.get(randomAntLionPos).toString()+ " Time and energy values: "+ fitnessArchive.get(randomAntLionPos).toString());
    }


    private void printSectionBreak() {

        System.out.println("--------------------------------------------------------------------");
        System.out.println();
        System.out.println("--------------------------------------------------------------------");
        System.out.println();
        System.out.println("--------------------------------------------------------------------");
        System.out.println();
    }


    private void printArchives(List<Integer> elite) {
        for(int i=0;i<elite.size();i++){
            System.out.println("Solution "+ (i+1) + " " + positionArchive.get(elite.get(i)).toString()+" Time and Energy values: "+ fitnessArchive.get(elite.get(i)).toString());
        }
    }

    public void startOptimisation(Runner runner) throws IOException {

        ScatterPlot plotComparison= new ScatterPlot("Ant Lion Optimiser iterative development");
        ScatterPlot results = new ScatterPlot("Ant Lion Optimiser results");
        this.initializeArchives();

        System.out.println("--------------------------ARCHIVE INITIALIZED WITH RANDOM VALUES---------------------------");
        printSectionBreak();

        for(currIter=0;currIter<maxIterations;currIter++){
            calculateFitness(runner);
            List<Integer> elite = updateArchiveNDSort();

            System.out.println("Current Iteration: "+ currIter +  " \n The solutions in the best front of this iteration and their fitness values: " );

            printArchives(elite);
            printSectionBreak();
            rankingProcess(elite.size());

            int index=rouletteWheelSelection(inverse(ranks),elite.size());
            if(index==-1)
                index=0;

            elitePosition=positionArchive.get(index);
            eliteFitness=fitnessArchive.get(index);

            if(currIter==(0.2)*maxIterations || currIter==(0.4)*maxIterations || currIter==(0.6)*maxIterations || currIter==(0.8)*maxIterations ||currIter==maxIterations-1)
            {
                plotComparison.addValues(fitnessArchive,currIter);
            }

            int randomAntLionPos=(int) (Math.random()*positionArchive.size());
            List<Integer> randomAntLionPosition = positionArchive.get(randomAntLionPos);

            printELite();
            printRandomAntLion(randomAntLionPos);
            printSectionBreak();
            antPosition.clear();

            for(int i = 0; i< searchAgents; i++){
                List<Integer> ant = createAntPosition(randomAntLionPosition);
                ant= eliteEffect(ant);
                antPosition.add(ant);
            }
        }

        results.addValues(fitnessArchive,maxIterations);
        results.plot();
        plotComparison.plot();
    }


}
