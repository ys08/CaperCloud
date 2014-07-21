#!/usr/bin/python

import subprocess
import sys
import os

txml = sys.argv[1]
fdr = sys.argv[2]

out_dir = "out_tmp"
basename = os.path.basename(txml)
name = ''.join(basename.split('.')[:-1])

#run percolator
cmd1 = "java -jar backend/IPeak_release/mzidentml-lib.jar XtandemPercolator " + txml + " " + out_dir + " -decoyRegex ###REV### -compress false"
subprocess.call(cmd1.split())
file1 = out_dir + "/" + name + "AddP.mzid"

#threshold
file2 = "result.mzid"
cmd2 = "java -jar backend/IPeak_release/mzidentml-lib.jar Threshold " + file1 + " " + file2 + "  -isPSMThreshold true -cvAccessionForScoreThreshold MS:1001491 -threshValue " + fdr + " -betterScoresAreLower true -deleteUnderThreshold true -compress false" 
subprocess.call(cmd2.split())

