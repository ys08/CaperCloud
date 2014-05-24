#!/usr/bin/python

import datetime
import sys
import os
import subprocess

from xml.etree import ElementTree as ET # for picking through the X!Tandem config file to find filenames

currentCfgID=-1  # cfgstack member -1 is the core config
cfgCore={}
cfgStack=[]

# routine to retrieve config, with error checking
def loadConfig():
	startTime = datetime.datetime.utcnow()

	if (len(sys.argv) < 2) :
		raise RuntimeError("syntax: " + sys.argv[0] + " <xtandem_paramters_file>")

	# read the cluster config file (encoded as json, possibly with comments)
	selectCoreConfig() # cluster config is common to all potential job configs
	setConfig( "xtandemParametersLocalPath", sys.argv[1]) # this xtandem xml config info gets processed a bit later
	print "job started"

	# use job config filename as basename if none given
	setConfig("baseName", getConfig("baseName",os.path.basename(sys.argv[1])))
	setConfig("coreBaseName", getConfig("baseName")) # in case of multi-param batch run

	setConfig("jobTimeStamp", startTime.strftime("%Y%m%d%H%M%S"))

# routine to retrieve config, with error checking
def getConfig(key, default = None, required = True):
	if ( (currentCfgID >= 0) and (key in cfgStack[currentCfgID]) ) :
		return cfgStack[currentCfgID][ key ]
	if (key in cfgCore) : # in the config stuff shared by all configs in the stack?
		if (None != cfgCore[ key ]) :
			return cfgCore[ key ]
	if ( None != default ) :
		return default
	if ( required ) :
		raise Exception("no value given for %s" % key)
	return None

def getCoreJobDir() :
	if len(cfgStack) > 1 : # multi config batch job
		baseName = getConfig( "coreBaseName" ) # enforce bucket naming rules
		job_ts = getConfig("jobTimeStamp")  
		jobDir = '%s_runs/%s' % ( baseName , job_ts )
	else :
		jobDir = getJobDir()
	return jobDir
    
# use a given copy of config
def selectConfig(n=None):
	global currentCfgID
	if None != n : # just getting handle on the current selection?
		currentCfgID = n
	if (-1 == currentCfgID) :
		return cfgCore
	else :
		return cfgStack[currentCfgID]

# use the core config as the current set/get target
def selectCoreConfig() :
	return selectConfig(-1)

def getJobDir() :
	if len(cfgStack) > 1 : # multi config batch job
		setConfig("jobDir", getCoreJobDir()+"/"+getConfig( "baseName" ))
		# save results to local file?  (defeated by setting "resultsFilename":"")
		if not len(getConfig("resultsFilename","")): # no results name set yet
			setCoreConfig("resultsFilename",getCoreJobDir()+"/results.txt")            
		jobDir = getConfig( "jobDir" )
	else :    
		baseName = getConfig( "baseName" ) # enforce bucket naming rules
		job_ts = getConfig("jobTimeStamp")  
		jobDir = '%s_runs/%s' % ( baseName , job_ts )
		resultsDir = baseName
		setConfig( "jobDir", jobDir )
		jobDir = getConfig( "jobDir" )
		# save results to local file?  (defeated by setting "resultsFilename":"")
		if not len(getConfig("resultsFilename","")): # no results name set yet
			setCoreConfig("resultsFilename",jobDir+"/results.txt")
	if not os.path.exists(jobDir) :
		os.makedirs(jobDir)
	return jobDir

# set value in the current config
def setConfig(key,val) :
	if (-1 == currentCfgID) :
		setCoreConfig(key,val)
	else :
		print "set %d %s=%s"%(currentCfgID,key,val)
		cfgStack[currentCfgID][ key ] = val

# set value shared by all configs in the stack
def setCoreConfig(key,val) :
	print "set core %s=%s"%(key,val)
	cfgCore[ key ] = val
	for cfg in cfgStack :
		if key in cfg :
			del cfg[key]

# add to the config stack for use in multi-search scenario
def pushConfig() :
	cfgCopy ={}
	cfgStack.extend([cfgCopy])

# save the given string locally
def saveStringToFile(contents,filename) :
	text = str(contents)
	full_filename = filename
	try:
		f = open(filename,"w")
		f.write(text)
		f.close()   
	except Exception, exception:
		print "failed saving text to " + full_filename
		print "exiting with error"
		exit(-1)

def main():
	loadConfig()

	xtandemParametersLocalPath = getConfig("xtandemParametersLocalPath")
	jobName = getConfig("baseName")
	jobDirMain = getCoreJobDir() # gets baseName, or umbrella name for multi-config batch job
	nmappers = 1
	configfiles = []
	try:
		for line in open(xtandemParametersLocalPath,'r') :
			if ("<?xml" in line) or ("<bioml>" in line) :  # a regular xtandem config file
				configfiles = [xtandemParametersLocalPath]
				pushConfig() # add a single config
				break
			else : # assume each line is the name of an xtandem config file
				line = line.strip()
				if (len(line) > 0) :
					print "adding parameters file %s to run" % line
					configfiles.extend([line])
					pushConfig() # add another config to batch run
	except Exception, inst:
		print "Unexpected error opening X!Tandem parameters file %s: %s" % (xtandemParametersLocalPath, inst)
		if (not os.path.exists(xtandemParametersLocalPath)) :
			print "quitting"
			exit(1)

	nSharedFileIDs = 1
	nParamFiles = 0

	for xtandemParametersLocalPath in configfiles: # peruse each config file and do needed setup and file xfers
		print "begin processing parameters file %s" % xtandemParametersLocalPath
		selectConfig(nParamFiles)
		# different jobdirs for different jobs in a multi-paramfile setup
		setConfig("baseName",os.path.basename(xtandemParametersLocalPath))
		setConfig("xtandemParametersLocalPath",xtandemParametersLocalPath)
		nParamFiles = nParamFiles+1
		jobDir = getJobDir()
		#
		# write the xtandem parameters file to the per-job directory as a matter of record
		# but first fix up its internal references for S3 use
		# note this loop structure is for support of nested param files
		xtandemParametersLocalPathList = [ getConfig("xtandemParametersLocalPath") ]
		setConfig("refineSetting","") # assume no refine step unless config shows otherwise
		defaultparamsLocalPath = ""
		taxonomyLocalPath = ""
		proteintaxon = ""
		spectrumName = ""
		outputLocalPath = ""
		outputS3CompatibleName = ""
		databaseRefs = []
		mainXtandemParametersName = xtandemParametersLocalPathList[0]
		setConfig("mainXtandemParametersName",mainXtandemParametersName)
		for xtandemParametersLocalPath in xtandemParametersLocalPathList : # this loop handles nested param files
			xtandemParametersName = xtandemParametersLocalPath
			# now examine to xtandem parameters file to see what needs copying to mapreduce cluster - looking for this:
			#<bioml>
			#  <note type="input" label="list path, taxonomy information">c:/Inetpub/wwwroot/ISB/data/mrtest.taxonomy.xml</note>
			#  <note type="input" label="protein, taxon">mydatabase</note>
			#  <note type="input" label="spectrum, path">c:/Inetpub/wwwroot/ISB/data/mrtest.123.mzXML</note>
			#  <note type="input" label="output, path">c:/Inetpub/wwwroot/ISB/data/mrtest.tandem</note>
			#  <note type="input" label="list path, default parameters">c:/Inetpub/wwwroot/ISB/data/parameters/isb_default_input_kscore.xml</note>
			try:
				tree = ET.parse(xtandemParametersLocalPath)
			except Exception, inst:
				print "Unexpected error opening X!Tandem parameters file %s: %s" % (xtandemParametersLocalPath, inst)
			notes = tree.getiterator("note")
			for note in notes :
				if (note.get("label") != None) :
					if (note.attrib["label"] == "list path, taxonomy information" ) and ("" == taxonomyLocalPath) :
						taxonomyLocalPath = note.text
					elif (note.attrib["label"] == "list path, default parameters" ) :
						xtandemParametersLocalPathList.extend([note.text]) # process this in next loop pass
						defaultXtandemParametersName = note.text
					elif ((note.attrib["label"] == "spectrum, path" ) and ("" == spectrumName)) :
						setConfig("sharedFile_spectrum%d" % nSharedFileIDs, note.text)
						nSharedFileIDs = nSharedFileIDs+1
						spectrumName = note.text
					elif ((note.attrib["label"] == "protein, taxon" ) and ("" == proteintaxon) ):
						proteintaxon = note.text;
						databaseRefs.extend([ note.text ]) # we'll look this up in the taxonomy file and make sure it gets up to S3
					elif (note.attrib["label"] == "output, path" ) and ("" == outputLocalPath) : # don't let default step on explicit
						outputName = note.text
						setConfig("outputName",outputName)
						setConfig("outputLocalPath",outputLocalPath)
					elif (note.attrib["label"] == "refine" ) and ("" == getConfig("refineSetting")) :
						setConfig("refineSetting",note.text)

		# now examine the taxonomy file itself for database references - something like this
		# <?xml version="1.0"?>
		#  <bioml label="x! taxon-to-file matching list">
		#  <taxon label="mydatabase">
		#      <file format="peptide" URL="c:/Inetpub/wwwroot/ISB/data/dbase/yeast_orfs_all_REV.20060126.short.fasta" />
		#    </taxon>
		#  </bioml>
		#
		# make sure the referenced database is in S3 in the shared files area (that is, assume it gets
		# reused from job to job) and rewrite the S3 copy of the taxonomy file to point at that
		try:
			tree = ET.parse(taxonomyLocalPath)
		except Exception, inst:
			print "Unexpected error opening X!Tandem taxonomy file %s: %s" % (taxonomyLocalPath, inst)
		taxons = tree.getiterator("taxon")
		satisfiedDatabaseRefs = []
		for taxon in taxons :
			if ( taxon.attrib["label"] in databaseRefs ) :
				satisfiedDatabaseRefs.extend( [ taxon.attrib["label"] ] )
				dfiles = taxon.getiterator("file")
				for dfile in dfiles : # one or more database files
					localDatabaseName = dfile.attrib["URL"]
					setConfig("sharedFile_database%d" % nSharedFileIDs , localDatabaseName) # this causes file to upload to S3
					databaseName = localDatabaseName
					nSharedFileIDs = nSharedFileIDs+1
		for ref in databaseRefs :
			if not ref in satisfiedDatabaseRefs :
				print 'ERROR: could not find a reference to "%s" in taxonomy file %s.  Exiting with error.' %(ref,taxonomyLocalPath)
				exit(1)
	# create the mapper1 input file
	# there is only one reducer key
	# each line of mapper input file tells mapper to take the nth of every m spectra
	mapper_mult=4
	# we output "mapper_mult" times as many pairs as we have mappers, so if anything goes wrong with one
	# the others can level that out instead of somebody getting a double load
	mapper1InputFile = '%s/mapper1-input-values' % jobDirMain
	mapperInputs = ""
	for count in range(nmappers*mapper_mult) :
		if (count > 0) :
			mapperInputs = mapperInputs + "\n"  # avoid a final newline - it causes an extra entry
		entry = '%5d %5d' % (count+1 , nmappers*mapper_mult) # want consistent line length so hadoop weights equally
		mapperInputs = mapperInputs + entry
	saveStringToFile(mapperInputs,mapper1InputFile)


	# and now execute
	nParamFiles = 0
	for xtandemParametersLocalPath in configfiles: # peruse each config file and do needed setup and file xfers
		selectConfig(nParamFiles)
		nParamFiles = nParamFiles+1
		jobDir = getJobDir()
		baseName = getConfig("baseName")
		print "processing %s" % xtandemParametersLocalPath
		print datetime.datetime.utcnow()

		mapperInputFile=mapper1InputFile
		reducerOutFile=""
		xtandemCmd="./mrtandem"
		if ("yes"==getConfig("refineSetting")) :
			nsteps = 3
		else :
			nsteps = 2
		for step in range(1,nsteps+1) :
			mapper = '%s -mapper%d_%d /tmp %s ' % ( xtandemCmd, step, nParamFiles, xtandemParametersLocalPath)
			reducer = '%s -reducer%d_%d /tmp %s ' % ( xtandemCmd, step, nParamFiles, xtandemParametersLocalPath )
			reducerOutFile = mapperInputFile+".next"
			cmd = "cat " + mapperInputFile + " | " + mapper + " | sort | " + reducer + " > " + reducerOutFile
			mapperInputFile = reducerOutFile
			cmd1 = "echo " + xtandemParametersLocalPath + " --------------------"
			subprocess.call(cmd, shell=True)

if __name__ == "__main__":
    main()