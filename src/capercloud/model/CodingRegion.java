/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shuai
 */
public class CodingRegion {
    private ArrayList<Interval> regions;
    
    public CodingRegion() {
        this.regions = new ArrayList<>();
    }
    
    public void addInterval(int left, int right) {
        if (left <= right) {
            regions.add(new Interval(left, right));
        } else {
            regions.add(new Interval(right, left));
        }
    }
    
    public int count() {
        return regions.size();
    }
    
    public String selectIntervals(int startIntervalIndex, int startIntervalInc, int endIntervalIndex, int endIntervalInc) {
        //
        if (endIntervalIndex > regions.size()-1) {
            return null;
        }
        
        if (startIntervalIndex == endIntervalIndex) {
            String p = regions.get(startIntervalIndex).subInterval(startIntervalInc, endIntervalInc);
            return p;
            
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(regions.get(startIntervalIndex).rightInterval(startIntervalInc)).append(",");
            for (int i=startIntervalIndex+1; i<endIntervalIndex; i++) {
                sb.append(regions.get(i).toString()).append(",");
            }
            sb.append(regions.get(endIntervalIndex).leftInterval(endIntervalInc));
            return sb.toString();
        }
    }
    
    public String selectNegtiveIntervals(int startIntervalIndex, int startIntervalInc, int endIntervalIndex, int endIntervalInc) {
        //
        if (endIntervalIndex > regions.size()-1) {
            return null;
        }
        
        if (startIntervalIndex == endIntervalIndex) {
            String p = regions.get(startIntervalIndex).subInterval(startIntervalInc, endIntervalInc);
            return p;
            
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(regions.get(startIntervalIndex).leftInterval(startIntervalInc)).append(",");
            for (int i=startIntervalIndex+1; i<endIntervalIndex; i++) {
                sb.append(regions.get(i).toString()).append(",");
            }
            sb.append(regions.get(endIntervalIndex).rightInterval(endIntervalInc));
            return sb.toString();
        }
    }
    
    public String intervalsOf(int startPos, int endPos) {
        int totalLength = 0;
        int startIntervalIndex = 0;
        int endIntervalIndex = 0;
        int startIntervalInc = 0;
        int endIntervalInc = 0;
        
        for (Interval i : regions) {
            totalLength += i.length();
            if (startPos-totalLength>0) {
                startIntervalIndex++;
                continue;
            } else {
                startIntervalInc = startPos - totalLength + i.length();
                break;
            }
        }
        totalLength = 0;
        
        for (Interval i : regions) {
            totalLength += i.length();
            if (endPos-totalLength>0) {
                endIntervalIndex++;
                continue;
            } else {
                endIntervalInc = endPos - totalLength + i.length();
                break;
            }
        }
        
        return this.selectIntervals(startIntervalIndex, startIntervalInc, endIntervalIndex, endIntervalInc);
    }

    public String negtiveIntervalsOf(int startPos, int endPos) {
        int totalLength = 0;
        int startIntervalIndex = 0;
        int endIntervalIndex = 0;
        int startIntervalInc = 0;
        int endIntervalInc = 0;
        
        for (Interval i : regions) {
            totalLength += i.length();
            if (startPos-totalLength>0) {
                startIntervalIndex++;
                continue;
            } else {
                startIntervalInc = totalLength - startPos - 1;
                break;
            }
        }
        totalLength = 0;
        
        for (Interval i : regions) {
            totalLength += i.length();
            if (endPos-totalLength>0) {
                endIntervalIndex++;
                continue;
            } else {
                endIntervalInc = totalLength - endPos - 1;
                break;
            }
        }
        
        return this.selectNegtiveIntervals(startIntervalIndex, startIntervalInc, endIntervalIndex, endIntervalInc);
    }
}

class Interval {
    private int left;
    private int right;

    public Interval(int left, int right) {
        this.left = left;
        this.right = right;
    }
    
    public int length() {
        return right-left+1;
    }
    
    public String subInterval(int inc1, int inc2) {
        StringBuilder sb = new StringBuilder();
        if (inc1<inc2) {
            sb.append(left+inc1).append("-").append(left+inc2);
        } else {
            sb.append(left+inc2).append("-").append(left+inc1);
        }
        return sb.toString();
    }
    
    public String leftInterval(int inc) {
        StringBuilder sb = new StringBuilder();
        sb.append(left).append("-").append(left+inc);
        return sb.toString();
    }
    
    public String rightInterval(int inc) {
        StringBuilder sb = new StringBuilder();
        sb.append(left+inc).append("-").append(right);
        return sb.toString();
    }
    
    public int contains(int pos) {
        if (this.left<=pos&&pos<=right) {
            return pos-left;
        }
        return -1;
    }
    
    public int negtiveContains(int pos) {
        if (this.left<=pos&&pos<=right) {
            return right-pos;
        }
        return -1;
    }
    
    public int leftOffset(int pos) {
        return pos-left;
    }
    
    @Override
    public String toString() {
        return left + "-" + right;
    }
}