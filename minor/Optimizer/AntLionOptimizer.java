package Optimizer;

import models.Solution;
import org.apache.commons.math3.util.Pair;
import scheduler.Runner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("unchecked")
public class AntLionOptimizer {

    public List<List<Integer>> positionArchive;

    public List<List<Double>> fitnessArchive;

    public List<Double> ranks;

    public List<List<Integer>> antPosition;

    public List<List<Double>> antFitness;

    public List<Integer> elitePosition;

    public List<Double> eliteFitness;

//    public List<Integer> elitePosition1;
//
//    public List<Double> eliteFitness1;

    public List<Double> upperBound;

    public List<Double> lowerBound;

    public int maxIterations;

    public int searchAgents;

    public int numTasks;

    public int numObj;

    public int maxArchiveSize;

    public int nVm;

    public int currentArchiveNum;

    public int currIter;


    public AntLionOptimizer(int maxIterations, int numTasks, int searchAgents, int numObj, int maxArchiveSize, int nVm){

        positionArchive= new ArrayList<>();
        fitnessArchive= new ArrayList<>();
        ranks=new ArrayList<>();
        antFitness= new ArrayList<>();
        antPosition= new ArrayList<>();
        elitePosition= new ArrayList<>();
        eliteFitness = new ArrayList<>();
        upperBound= new ArrayList<>();
        lowerBound= new ArrayList<>();
        this.nVm=nVm;
        for(int i=0;i<numTasks;i++){
            upperBound.add((double) nVm);
            lowerBound.add(1.0);
        }
        this.maxIterations=maxIterations;
        this.searchAgents=searchAgents;
        this.numObj=numObj;
        this.numTasks=numTasks;
        this.maxArchiveSize = maxArchiveSize;
        this.currentArchiveNum=0;
        this.currIter=0;
    }

    public void initializeArchives(){
        for(int i=0;i<searchAgents;i++) {
            List<Integer> temp = new ArrayList<>();
            for (int j = 0; j < numTasks; j++) {
                temp.add((int) (Math.random() * (upperBound.get(0))));
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
        antFitness.clear();
        for(int i=0;i<antPosition.size();i++){
            Solution solution= new Solution(runner,antPosition.get(i));
            List<Double> fitness=solution.calculateObjectives();
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

    private void updateArchiveNDSort() throws IOException {
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
        List<Integer> index= crowdingDistance(fronts.get(1),1,tempFitnessArchive);
        elitePosition=tempPositionArchive.get(index.get(0));
        eliteFitness=tempFitnessArchive.get(index.get(0));
//        elitePosition1=tempPositionArchive.get(index.get(1));
//        eliteFitness1=tempFitnessArchive.get(index.get(1));
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
//        List<Integer> elite = new ArrayList<>();
        for(int j = 1; j <= fronts.size(); j++){
            int idx=0;
            currentArchiveSize += fronts.get(j).size();
            List<Integer> front = fronts.get(j);
            if(currentArchiveSize>maxArchiveSize){
                List<Integer> toAdd = crowdingDistance(front, maxArchiveSize - fitnessArchive.size(), tempFitnessArchive);
                for(int it = 0; it < toAdd.size();it++){
                    int solIndex = toAdd.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
//                    if(j==1){
//                        elite.add(idx);
//                    }
                    idx++;
                }
                break;
            }else{
                for(int it = 0; it < front.size();it++){
                    int solIndex = front.get(it);
                    fitnessArchive.add(tempFitnessArchive.get(solIndex));
                    positionArchive.add(tempPositionArchive.get(solIndex));
//                    if(j==1){
//                        elite.add(idx);
//                    }
                    idx++;
                }
            }
        }
//        if(print==true){
//            FileWriter writer = new FileWriter("archive.txt",true);
//            BufferedWriter buffer = new BufferedWriter(writer);
//            for(int ids: fronts.get(1)){
//                buffer.write(tempFitnessArchive.get(ids).get(0).toString()+" "+tempFitnessArchive.get(ids).get(1).toString());
//                buffer.newLine();
//            }
//            buffer.close();
//        }
//        return elite;
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
            hashMap.put(i,true);
            for(int j=i-1;j>=0;j--){
                if(tempFitnessArchive.get(i).equals(tempFitnessArchive.get(j))){
                    hashMap.put(i, false);
                    hashMap.put(j, false);
                }
                else{
                    if(dominates(tempFitnessArchive.get(i),tempFitnessArchive.get(j))){
                        hashMap.put(j,false);
                    }
                    else if(dominates(tempFitnessArchive.get(j),tempFitnessArchive.get(i))){
                        hashMap.put(i,false);
                        break;
                    }
                }
            }
        }
        positionArchive.clear();
        fitnessArchive.clear();
        for(int i=0;i<tempFitnessArchive.size();i++){
            if(hashMap.get(i)){
                positionArchive.add(tempPositionArchive.get(i));
                fitnessArchive.add(tempFitnessArchive.get(i));
            }
        }
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

    private void handleFullArchive() {
        for(int i=0;i<fitnessArchive.size()- maxArchiveSize;i++){
            int index=rouletteWheelSelection(ranks,maxArchiveSize);
            fitnessArchive.remove(index);
            positionArchive.remove(index);
            ranks.remove(index);
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

    private List<Double> randomWalk(List<Integer> antlion) {
        List<Double> walk = new ArrayList<>();
        double I=1.0;
        if ((double)currIter>(double)maxIterations/10)
            I=1+100*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations/2)
            I=1+1000*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(3/4))
            I=1+10000*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(0.9))
            I=1+100000*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(0.95))
            I=1+1000000*((double)currIter/(double)maxIterations);
        else;
//        for(int i=0;i<antlion.size();i++){
//            antlion.set(i,antlion.get(i)+1);
//        }
        lowerBound.clear();
        upperBound.clear();
        for(int i=0;i<this.numTasks;i++){
            lowerBound.add(1.0/I);
            upperBound.add(nVm/I);
        }
        for(int i=0;i<this.numTasks;i++){
            if(Math.random()>0.5){
                lowerBound.set(i,lowerBound.get(i)+antlion.get(i)+1);
            }
            else lowerBound.set(i,-lowerBound.get(i)+antlion.get(i)+1);
            if(Math.random()>0.5){
                upperBound.set(i,upperBound.get(i)+antlion.get(i)+1);
            }
            else upperBound.set(i,-upperBound.get(i)+antlion.get(i)+1);
        }
        for(int i=0;i<this.numTasks;i++){
            List<Double> cumsum = new ArrayList<>();
            cumsum.add(0.0);
            for(int j=0;j<maxIterations;j++){
                if(Math.random()>=0.5)
                    cumsum.add(1.0);
                else cumsum.add(-1.0);
            }
            cumsum=cummulativeSum(cumsum);
            double c=lowerBound.get(i);
            double d=upperBound.get(i);
            double a=Double.MAX_VALUE;
            double b=Double.MIN_VALUE;
            for(int j=0;j<cumsum.size();j++){
                a=Math.min(a,cumsum.get(j));
                b=Math.max(b,cumsum.get(j));
            }
            for(int j=0;j<cumsum.size();j++) {
                cumsum.set(j,c+(((cumsum.get(j)-a)*(d-c))/(b-a)));
            }
            walk.add(cumsum.get(currIter));
        }
        return walk;
    }

    private List<Double> cummulativeSum(List<Double> list) {
        List<Double> cumsum = new ArrayList<>();
        cumsum.add(list.get(0));
        for(int i=1;i<list.size();i++){
            cumsum.add(cumsum.get(i-1)+list.get(i));
        }
        return cumsum;
    }

    private List<Integer> spvRule(List<Double> randomWalkAroundAntLion) {
        List<Integer> indexes = new ArrayList<>();
        List<Integer> ans= new ArrayList<>();
        for(int i=0;i<randomWalkAroundAntLion.size();i++)
        {
            indexes.add(i);
                ans.add(i);
        }
        indexes.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer integer, Integer t1) {
                return Double.compare(randomWalkAroundAntLion.get(integer),randomWalkAroundAntLion.get(t1));
            }
        });
        ans.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer integer, Integer t1) {
                return Integer.compare(indexes.get(integer),indexes.get(t1));
            }
        });
        for(int i=0;i<numTasks;i++){
            ans.set(i,ans.get(i)%nVm);
//            ans.set(i,(int) (Math.random() * nVm));
        }
        return ans;
    }

//    public void startOptimisation(Runner runner) throws IOException {
//        this.initializeArchives();
//        for(currIter=0;currIter<maxIterations;currIter++){
//            System.out.println(currIter);
//            calculateFitness(runner);
////            updateArchive();
////            if(positionArchive.size()> maxArchiveSize){
////                rankingProcess();
////                handleFullArchive();
////            }
////            rankingProcess();
////            int index=rouletteWheelSelection(inverse(ranks));
////            if(index==-1)
////                index=0;
//            List<Integer> elite = updateArchiveNDSort(false);
//            rankingProcess(elite.size());
//            int index=rouletteWheelSelection(inverse(ranks),elite.size());
//            int index2=rouletteWheelSelection(inverse(ranks),elite.size());
//            if(index==-1)
//                index=0;
//            if(index2==-1)
//                index2=0;
//            elitePosition=positionArchive.get(index);
//            elitePosition1=positionArchive.get(index2);
//            int randomAntLionPos=(int) (Math.random()*positionArchive.size());
//            List<Integer> randomAntLionPosition = positionArchive.get(randomAntLionPos);
//            antPosition.clear();
//            for(int i=0;i<searchAgents;i++){
//                List<Double> randomWalkAroundAntLion = randomWalk(randomAntLionPosition);
//                List<Double> randomWalkAroundElite = randomWalk(elitePosition);
//                List<Double> randomWalkAroundElite1 = randomWalk(elitePosition1);
//                for(int j=0;j<numTasks;j++){
//                    randomWalkAroundAntLion.set(j,randomWalkAroundElite.get(j));
////                            +randomWalkAroundAntLion.get(j)+randomWalkAroundElite1.get(j));
//                }
//                List<Integer> vmAllocation = new ArrayList<>();
//                vmAllocation=spvRule(randomWalkAroundAntLion);
//                antPosition.add(vmAllocation);
//            }
//        }
////        updateArchiveNDSort(true);
//    }

    public void startOptimisation(Runner runner) throws IOException {
        this.initializeArchives();
        for(currIter=0;currIter<maxIterations;currIter++){
            System.out.println(currIter);
            calculateFitness(runner);
            updateArchiveNDSort();
            int randomAntLionPosIndex=(int) (Math.random()*positionArchive.size());
            List<Integer> randomAntLionPosition = positionArchive.get(randomAntLionPosIndex);
            antPosition.clear();
            for(int i=0;i<searchAgents;i++){
                List<Double> randomWalkAroundAntLion = randomWalk(randomAntLionPosition);
                List<Double> randomWalkAroundElite = randomWalk(elitePosition);
//                List<Double> randomWalkAroundElite1 = randomWalk(elitePosition1);
                for(int j=0;j<numTasks;j++){
                    randomWalkAroundAntLion.set(j,randomWalkAroundElite.get(j)
                            +randomWalkAroundAntLion.get(j));
                }
                List<Integer> vmAllocation = new ArrayList<>();
                vmAllocation=spvRule(randomWalkAroundAntLion);
                antPosition.add(vmAllocation);
            }
        }

    }

}
