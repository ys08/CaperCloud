/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

/**
 *
 * @author shuai
 */
public class Range {
    private int startPos;
    private int endPos;
    
    public Range(int startPos, int endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }
    
    public int getLength() {
        return this.endPos - this.startPos + 1;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }
    
}
