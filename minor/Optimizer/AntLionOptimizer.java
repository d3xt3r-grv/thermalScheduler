package Optimizer;

import models.Solution;
import org.apache.commons.math3.util.Pair;
import scheduler.Runner;

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

    private void rankingProcess() {
        ranks.clear();
        double minTime=Double.MAX_VALUE;
        double maxTime=Double.MIN_VALUE;
        double minEnergy=Double.MAX_VALUE;
        double maxEnergy=Double.MIN_VALUE;
        for(int i=0;i<fitnessArchive.size();i++){
            minTime=Math.min(minTime,fitnessArchive.get(i).get(0));
            maxTime=Math.max(maxTime,fitnessArchive.get(i).get(0));
            minEnergy=Math.min(minEnergy,fitnessArchive.get(i).get(1));
            maxEnergy=Math.max(maxEnergy,fitnessArchive.get(i).get(1));
            ranks.add(0.0);
        }
        Double timeDensityParameter = (maxTime-minTime)/20;
        Double energyDensityParameter = (maxEnergy - minEnergy) /20 ;
        for(int i=0;i<fitnessArchive.size();i++){
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
            int index=rouletteWheelSelection(ranks);
            fitnessArchive.remove(index);
            positionArchive.remove(index);
            ranks.remove(index);
        }
    }

    private int rouletteWheelSelection(List<Double> weights) {
        List<Double> cumSum = new ArrayList<>();
        cumSum.add((double)weights.get(0));
        for(int i=1;i<weights.size();i++){
            cumSum.add((double)cumSum.get(i-1)+weights.get(i));
        }
        double p=Math.random()*cumSum.get(cumSum.size()-1);
        int index=-1;
        for(int idx=0;idx<cumSum.size();idx++){
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
            I=1+10*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations/2)
            I=1+100*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(3/4))
            I=1+1000*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(0.9))
            I=1+10000*((double)currIter/(double)maxIterations);
        else if ((double)currIter>(double)maxIterations*(0.95))
            I=1+100000*((double)currIter/(double)maxIterations);
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
            cumsum.add(i,cumsum.get(i-1)+list.get(i));
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
        }
//        List<Integer> ans= new ArrayList<>();
//        for (int j = 0; j < numTasks; j++) {
//            ans.add((int) (Math.random() * nVm));
//        }
        return ans;
    }

    public void startOptimisation(Runner runner){
        this.initializeArchives();
        for(currIter=0;currIter<maxIterations;currIter++){
            calculateFitness(runner);
            updateArchive();
            if(positionArchive.size()> maxArchiveSize){
                rankingProcess();
                handleFullArchive();
            }
            rankingProcess();
            int index=rouletteWheelSelection(inverse(ranks));
            if(index==-1)
                index=0;
            int randomAntLionPos=(int) Math.random()*positionArchive.size();
            List<Integer> randomAntLionPosition = positionArchive.get(randomAntLionPos);
            elitePosition=positionArchive.get(index);
            antPosition.clear();
            for(int i=0;i<searchAgents;i++){
                List<Double> randomWalkAroundAntLion = randomWalk(randomAntLionPosition);
                List<Double> randomWalkAroundElite = randomWalk(elitePosition);
                for(int j=0;j<numTasks;j++){
                    randomWalkAroundAntLion.set(j,randomWalkAroundElite.get(j)+randomWalkAroundAntLion.get(j));
                }
                List<Integer> vmAllocation = new ArrayList<>();
                vmAllocation=spvRule(randomWalkAroundAntLion);
                antPosition.add(vmAllocation);
            }
        }
    }

}
