#!/bin/bash
java -Xmx2048m -jar post_process/mzidentml-lib-1.6.10.jar Tandem2mzid output output.mzid -outputFragmentation false -decoyRegex "###REV###" -databaseFileFormatID MS:1001348 -massSpecFileFormatID MS:1001062 -idsStartAtZero false -compress false
java -Xmx2048m -jar post_process/mzidentml-lib-1.6.10.jar FalseDiscoveryRate output.mzid output_fdr.mzid -decoyRegex "###REV###" -decoyValue 1 -cvTerm MS:1001330 -betterScoresAreLower true -compress false
java -Xmx2048m -jar post_process/mzidentml-lib-1.6.10.jar Threshold output_fdr.mzid result.mzid -isPSMThreshold true -cvAccessionForScoreThreshold MS:1002354 -threshValue 0.01 -betterScoresAreLower true -deleteUnderThreshold true -compress false
python upload_data.py AKIAIWNERGLUEYZL7N7Q 2vg5/PqUH1DGRTi1ONYRXwf9lfrV6Mblf2vFIb4U capercloud-output result.mzid
